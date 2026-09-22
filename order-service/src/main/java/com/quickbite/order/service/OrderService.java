package com.quickbite.order.service;

import com.quickbite.order.client.PaymentClient;
import com.quickbite.order.client.RestaurantClient;
import com.quickbite.order.dto.*;
import com.quickbite.order.dto.external.MenuItemDto;
import com.quickbite.order.dto.external.PaymentRequestDto;
import com.quickbite.order.dto.external.PaymentResponseDto;
import com.quickbite.order.dto.external.RatingRequestDto;
import com.quickbite.order.dto.external.RestaurantDto;
import com.quickbite.order.entity.*;
import com.quickbite.order.exception.*;
import com.quickbite.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private static final Set<OrderStatus> CANCELLABLE_BY_CUSTOMER =
            Set.of(OrderStatus.CREATED, OrderStatus.PAYMENT_COMPLETED);

    /** Orders counted as "real" revenue: accepted by the restaurant, at any stage since. */
    private static final Set<OrderStatus> REVENUE_STATUSES =
            Set.of(OrderStatus.ACCEPTED, OrderStatus.PREPARING, OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERED);

    /** Orders still weighing on the kitchen right now - used to detect "high demand" for surge pricing. */
    private static final Set<OrderStatus> PENDING_LOAD_STATUSES =
            Set.of(OrderStatus.PAYMENT_COMPLETED, OrderStatus.ACCEPTED, OrderStatus.PREPARING);

    private final OrderRepository orderRepository;
    private final RestaurantClient restaurantClient;
    private final PaymentClient paymentClient;
    private final PaymentGatewayClient paymentGatewayClient;
    private final SurgePricingService surgePricingService;
    private final DeliverySimulator deliverySimulator;
    private final NotificationService notificationService;

    /**
     * The heart of the order flow: validate the restaurant and menu against
     * the live catalog, snapshot prices onto the order, then hand off to
     * payment-service. Every step talks to another microservice, so this
     * method alone demonstrates the Order -> Restaurant and Order -> Payment
     * inter-service communication the platform is built around.
     */
    @Transactional
    public OrderResponse placeOrder(Long customerId, PlaceOrderRequest request) {
        RestaurantDto restaurant = fetchRestaurantOrThrow(request.restaurantId());
        if (!restaurant.open()) {
            throw new RestaurantUnavailableException("'" + restaurant.name() + "' is not accepting orders right now");
        }

        Order order = Order.builder()
                .customerId(customerId)
                .restaurantId(request.restaurantId())
                .deliveryAddress(request.deliveryAddress())
                .paymentMethod(request.paymentMethod())
                .status(OrderStatus.CREATED)
                .totalAmount(BigDecimal.ZERO)
                .build();

        BigDecimal baseTotal = BigDecimal.ZERO;
        for (OrderItemRequest itemRequest : request.items()) {
            MenuItemDto menuItem = fetchMenuItemOrThrow(request.restaurantId(), itemRequest.menuItemId());
            if (!menuItem.available()) {
                throw new RestaurantUnavailableException("'" + menuItem.name() + "' is currently unavailable");
            }

            OrderItem orderItem = OrderItem.builder()
                    .menuItemId(menuItem.id())
                    .itemName(menuItem.name())
                    .unitPrice(menuItem.price())
                    .quantity(itemRequest.quantity())
                    .build();
            order.addItem(orderItem);
            baseTotal = baseTotal.add(orderItem.lineTotal());
        }

        // Demand-driven pricing: check how backed up this restaurant already is
        // right now, and whether we're in a known peak dining window.
        long pendingOrders = orderRepository.countByRestaurantAndStatusIn(request.restaurantId(), PENDING_LOAD_STATUSES);
        SurgePricingService.SurgeResult surge = surgePricingService.apply(baseTotal, pendingOrders);

        order.setBaseAmount(surge.baseAmount());
        order.setSurgeMultiplier(surge.multiplier());
        order.setSurgeReason(surge.reason());
        order.setTotalAmount(surge.finalAmount());

        Order saved = orderRepository.save(order);
        log.info("Order {} created for customer {} at restaurant {} (base {}, surge x{}, total {})",
                saved.getId(), customerId, request.restaurantId(), baseTotal, surge.multiplier(), surge.finalAmount());

        PaymentResponseDto payment = paymentGatewayClient.charge(
                new PaymentRequestDto(saved.getId(), customerId, surge.finalAmount(), request.paymentMethod().name()));

        saved.setStatus("SUCCESS".equals(payment.status())
                ? OrderStatus.PAYMENT_COMPLETED
                : OrderStatus.PAYMENT_FAILED);

        log.info("Order {} payment result: {}", saved.getId(), saved.getStatus());

        OrderResponse response = toResponse(saved);
        if (saved.getStatus() == OrderStatus.PAYMENT_COMPLETED) {
            notificationService.sendOrderConfirmation(customerId, response);
        }
        return response;
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long orderId, Long requesterId) {
        Order order = findOrderOrThrow(orderId);
        assertCustomerOrRestaurantOwner(order, requesterId);
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listCustomerOrders(Long customerId) {
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listRestaurantOrders(Long restaurantId, Long ownerId, OrderStatus statusFilter) {
        assertOwnsRestaurant(restaurantId, ownerId);
        List<Order> orders = statusFilter == null
                ? orderRepository.findByRestaurantIdOrderByCreatedAtDesc(restaurantId)
                : orderRepository.findByRestaurantIdAndStatusOrderByCreatedAtDesc(restaurantId, statusFilter);
        return orders.stream().map(this::toResponse).toList();
    }

    @Transactional
    public OrderResponse acceptOrder(Long orderId, Long ownerId) {
        Order order = findOrderOrThrow(orderId);
        assertOwnsRestaurant(order.getRestaurantId(), ownerId);

        if (order.getStatus() != OrderStatus.PAYMENT_COMPLETED) {
            throw new InvalidOrderStateException(
                    "Only a paid order can be accepted (current status: " + order.getStatus() + ")");
        }

        order.setStatus(OrderStatus.ACCEPTED);
        return toResponse(order);
    }

    @Transactional
    public OrderResponse rejectOrder(Long orderId, Long ownerId, String reason) {
        Order order = findOrderOrThrow(orderId);
        assertOwnsRestaurant(order.getRestaurantId(), ownerId);

        if (order.getStatus() != OrderStatus.PAYMENT_COMPLETED) {
            throw new InvalidOrderStateException(
                    "Only a paid, not-yet-accepted order can be rejected (current status: " + order.getStatus() + ")");
        }

        paymentClient.refund(orderId);
        order.setStatus(OrderStatus.REJECTED);
        order.setRejectionReason(reason == null || reason.isBlank() ? "Rejected by restaurant" : reason);
        log.info("Order {} rejected by owner {} and refunded", orderId, ownerId);
        return toResponse(order);
    }

    @Transactional
    public OrderResponse advanceStatus(Long orderId, Long ownerId, OrderStatus target) {
        Order order = findOrderOrThrow(orderId);
        assertOwnsRestaurant(order.getRestaurantId(), ownerId);

        OrderStatus current = order.getStatus();
        boolean allowed = (current == OrderStatus.ACCEPTED && target == OrderStatus.PREPARING)
                || (current == OrderStatus.PREPARING && target == OrderStatus.OUT_FOR_DELIVERY)
                || (current == OrderStatus.OUT_FOR_DELIVERY && target == OrderStatus.DELIVERED);

        if (!allowed) {
            throw new InvalidOrderStateException("Cannot move order from " + current + " to " + target);
        }

        order.setStatus(target);

        if (target == OrderStatus.OUT_FOR_DELIVERY) {
            assignDeliveryPartner(order);
        }

        OrderResponse response = toResponse(order);
        if (target == OrderStatus.DELIVERED) {
            notificationService.sendDeliveryConfirmation(order.getCustomerId(), response);
        }
        return response;
    }

    /**
     * Simulates dispatching a real delivery partner: picks a name/phone,
     * an ETA, and a destination point near the restaurant (see
     * DeliverySimulator for why the destination is approximated rather than
     * geocoded). GET /orders/{id}/tracking interpolates a moving position
     * from this assignment for as long as the order stays OUT_FOR_DELIVERY.
     */
    private void assignDeliveryPartner(Order order) {
        RestaurantDto restaurant = fetchRestaurantOrThrow(order.getRestaurantId());
        var assignment = deliverySimulator.assign(restaurant.latitude(), restaurant.longitude());

        order.setDeliveryPartnerName(assignment.partnerName());
        order.setDeliveryPartnerPhone(assignment.partnerPhone());
        order.setDeliveryEtaMinutes(assignment.etaMinutes());
        order.setDeliveryStartedAt(Instant.now());
        order.setDestinationLatitude(assignment.destinationLat());
        order.setDestinationLongitude(assignment.destinationLng());
    }

    /**
     * Live (simulated) delivery tracking. Position is a pure function of how
     * much of the estimated delivery time has elapsed, so this never drifts
     * out of sync no matter how often - or rarely - the client polls it.
     */
    @Transactional(readOnly = true)
    public DeliveryTrackingResponse getTracking(Long orderId, Long requesterId) {
        Order order = findOrderOrThrow(orderId);
        assertCustomerOrRestaurantOwner(order, requesterId);

        if (order.getStatus() != OrderStatus.OUT_FOR_DELIVERY && order.getStatus() != OrderStatus.DELIVERED) {
            throw new InvalidOrderStateException("Tracking is only available once an order is out for delivery");
        }

        RestaurantDto restaurant = fetchRestaurantOrThrow(order.getRestaurantId());

        if (order.getStatus() == OrderStatus.DELIVERED) {
            return new DeliveryTrackingResponse(order.getId(), order.getStatus().name(),
                    order.getDeliveryPartnerName(), order.getDeliveryPartnerPhone(),
                    restaurant.latitude(), restaurant.longitude(),
                    order.getDestinationLatitude(), order.getDestinationLongitude(),
                    order.getDestinationLatitude(), order.getDestinationLongitude(),
                    100, 0, true);
        }

        long elapsedSeconds = Instant.now().getEpochSecond() - order.getDeliveryStartedAt().getEpochSecond();
        long totalSeconds = order.getDeliveryEtaMinutes() * 60L;
        double progress = totalSeconds <= 0 ? 1.0 : Math.min(1.0, elapsedSeconds / (double) totalSeconds);

        double currentLat = restaurant.latitude() + (order.getDestinationLatitude() - restaurant.latitude()) * progress;
        double currentLng = restaurant.longitude() + (order.getDestinationLongitude() - restaurant.longitude()) * progress;
        int etaRemaining = (int) Math.max(0, Math.ceil((totalSeconds - elapsedSeconds) / 60.0));

        return new DeliveryTrackingResponse(order.getId(), order.getStatus().name(),
                order.getDeliveryPartnerName(), order.getDeliveryPartnerPhone(),
                restaurant.latitude(), restaurant.longitude(),
                order.getDestinationLatitude(), order.getDestinationLongitude(),
                currentLat, currentLng,
                (int) Math.round(progress * 100), etaRemaining, true);
    }

    @Transactional
    public OrderResponse cancelOrder(Long orderId, Long customerId) {
        Order order = findOrderOrThrow(orderId);
        if (!order.getCustomerId().equals(customerId)) {
            throw new AccessDeniedForResourceException("You do not own this order");
        }
        if (!CANCELLABLE_BY_CUSTOMER.contains(order.getStatus())) {
            throw new InvalidOrderStateException(
                    "Order can no longer be cancelled (current status: " + order.getStatus() + ")");
        }

        if (order.getStatus() == OrderStatus.PAYMENT_COMPLETED) {
            paymentClient.refund(orderId);
        }
        order.setStatus(OrderStatus.CANCELLED);
        return toResponse(order);
    }

    /**
     * Lets a customer rate a delivered order once. The rating is stored on
     * the order for the customer's own history, and forwarded to
     * restaurant-service (another Order -> Restaurant call) so the
     * restaurant's overall rating reflects real completed orders.
     */
    @Transactional
    public OrderResponse rateOrder(Long orderId, Long customerId, int rating, String comment) {
        Order order = findOrderOrThrow(orderId);
        if (!order.getCustomerId().equals(customerId)) {
            throw new AccessDeniedForResourceException("You do not own this order");
        }
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new InvalidOrderStateException("Only a delivered order can be rated");
        }
        if (order.getCustomerRating() != null) {
            throw new InvalidOrderStateException("This order has already been rated");
        }

        restaurantClient.submitRating(order.getRestaurantId(), new RatingRequestDto(rating));

        order.setCustomerRating(rating);
        order.setRatingComment(comment);
        return toResponse(order);
    }

    /**
     * Quick numbers for the owner's dashboard: how many orders came in
     * today, today's revenue, and lifetime totals. Deliberately counts only
     * orders the restaurant actually accepted - a rejected or cancelled
     * order was refunded and never became real revenue.
     */
    @Transactional(readOnly = true)
    public OrderSummaryResponse getRestaurantSummary(Long restaurantId, Long ownerId) {
        assertOwnsRestaurant(restaurantId, ownerId);

        Instant startOfDay = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant startOfTomorrow = startOfDay.plus(1, ChronoUnit.DAYS);

        long totalOrders = orderRepository.countByRestaurantAndStatusIn(restaurantId, REVENUE_STATUSES);
        long todayOrders = orderRepository.countByRestaurantAndStatusInAndCreatedAtBetween(
                restaurantId, REVENUE_STATUSES, startOfDay, startOfTomorrow);
        BigDecimal allTimeRevenue = orderRepository.sumRevenueByRestaurantAndStatusIn(restaurantId, REVENUE_STATUSES);
        BigDecimal todayRevenue = orderRepository.sumRevenueByRestaurantAndStatusInAndCreatedAtBetween(
                restaurantId, REVENUE_STATUSES, startOfDay, startOfTomorrow);

        return new OrderSummaryResponse(totalOrders, todayOrders, todayRevenue, allTimeRevenue);
    }

    private RestaurantDto fetchRestaurantOrThrow(Long restaurantId) {
        try {
            return restaurantClient.getRestaurant(restaurantId);
        } catch (Exception ex) {
            throw new ResourceNotFoundException("Restaurant " + restaurantId + " could not be found");
        }
    }

    private MenuItemDto fetchMenuItemOrThrow(Long restaurantId, Long menuItemId) {
        try {
            return restaurantClient.getMenuItem(restaurantId, menuItemId);
        } catch (Exception ex) {
            throw new ResourceNotFoundException("Menu item " + menuItemId + " could not be found");
        }
    }

    private void assertOwnsRestaurant(Long restaurantId, Long ownerId) {
        RestaurantDto restaurant = fetchRestaurantOrThrow(restaurantId);
        if (!restaurant.ownerId().equals(ownerId)) {
            throw new AccessDeniedForResourceException("You do not own this restaurant");
        }
    }

    private void assertCustomerOrRestaurantOwner(Order order, Long requesterId) {
        if (order.getCustomerId().equals(requesterId)) {
            return;
        }
        RestaurantDto restaurant = fetchRestaurantOrThrow(order.getRestaurantId());
        if (!restaurant.ownerId().equals(requesterId)) {
            throw new AccessDeniedForResourceException("You do not have access to this order");
        }
    }

    private Order findOrderOrThrow(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order " + id + " not found"));
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(i -> new OrderItemResponse(i.getMenuItemId(), i.getItemName(), i.getUnitPrice(),
                        i.getQuantity(), i.lineTotal()))
                .toList();
        return new OrderResponse(order.getId(), order.getCustomerId(), order.getRestaurantId(),
                order.getDeliveryAddress(), order.getTotalAmount(), order.getBaseAmount(),
                order.getSurgeMultiplier(), order.getSurgeReason(), order.getPaymentMethod(), order.getStatus(),
                order.getRejectionReason(), order.getCustomerRating(), order.getRatingComment(),
                order.getCreatedAt(), order.getUpdatedAt(), items);
    }
}
