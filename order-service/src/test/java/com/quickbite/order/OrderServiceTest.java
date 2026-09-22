package com.quickbite.order;

import com.quickbite.order.client.PaymentClient;
import com.quickbite.order.client.RestaurantClient;
import com.quickbite.order.dto.*;
import com.quickbite.order.dto.external.MenuItemDto;
import com.quickbite.order.dto.external.PaymentResponseDto;
import com.quickbite.order.dto.external.RestaurantDto;
import com.quickbite.order.entity.Order;
import com.quickbite.order.entity.OrderStatus;
import com.quickbite.order.entity.PaymentMethod;
import com.quickbite.order.exception.AccessDeniedForResourceException;
import com.quickbite.order.exception.InvalidOrderStateException;
import com.quickbite.order.exception.RestaurantUnavailableException;
import com.quickbite.order.repository.OrderRepository;
import com.quickbite.order.service.OrderService;
import com.quickbite.order.service.PaymentGatewayClient;
import com.quickbite.order.service.DeliverySimulator;
import com.quickbite.order.service.NotificationService;
import com.quickbite.order.service.SurgePricingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private RestaurantClient restaurantClient;
    @Mock private PaymentClient paymentClient;
    @Mock private PaymentGatewayClient paymentGatewayClient;
    @Mock private DeliverySimulator deliverySimulator;
    @Mock private NotificationService notificationService;

    // Fixed at 10:00 AM UTC - deliberately outside both peak windows
    // (12:00-15:00 and 19:00-22:30) so surge pricing never kicks in and
    // order totals in these tests are exact, regardless of what time the
    // test suite actually runs.
    private final SurgePricingService surgePricingService =
            new SurgePricingService(Clock.fixed(Instant.parse("2026-01-14T10:00:00Z"), ZoneOffset.UTC));

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        orderService = new OrderService(orderRepository, restaurantClient, paymentClient, paymentGatewayClient,
                surgePricingService, deliverySimulator, notificationService);
    }

    private RestaurantDto openRestaurant() {
        return new RestaurantDto(1L, 55L, "Spice Route", "Bengaluru", true, 17.4, 78.5);
    }

    @Test
    void placeOrder_marksPaymentCompleted_whenGatewayApproves() {
        PlaceOrderRequest request = new PlaceOrderRequest(1L,
                List.of(new OrderItemRequest(100L, 2)), "221B Baker Street", PaymentMethod.UPI);

        when(restaurantClient.getRestaurant(1L)).thenReturn(openRestaurant());
        when(restaurantClient.getMenuItem(1L, 100L))
                .thenReturn(new MenuItemDto(100L, 1L, "Butter Naan", "desc", BigDecimal.valueOf(60), "Breads", null, true));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(500L);
            o.setCreatedAt(Instant.now());
            o.setUpdatedAt(Instant.now());
            return o;
        });
        when(paymentGatewayClient.charge(any())).thenReturn(
                new PaymentResponseDto(1L, 500L, 9L, BigDecimal.valueOf(120), "UPI", "SUCCESS", "TXN-1", null, null));

        OrderResponse response = orderService.placeOrder(9L, request);

        assertEquals(OrderStatus.PAYMENT_COMPLETED, response.status());
        assertEquals(0, BigDecimal.valueOf(120).compareTo(response.totalAmount()));
    }

    @Test
    void placeOrder_marksPaymentFailed_whenGatewayDeclines() {
        PlaceOrderRequest request = new PlaceOrderRequest(1L,
                List.of(new OrderItemRequest(100L, 1)), "221B Baker Street", PaymentMethod.CARD);

        when(restaurantClient.getRestaurant(1L)).thenReturn(openRestaurant());
        when(restaurantClient.getMenuItem(1L, 100L))
                .thenReturn(new MenuItemDto(100L, 1L, "Butter Naan", "desc", BigDecimal.valueOf(60), "Breads", null, true));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(501L);
            o.setCreatedAt(Instant.now());
            o.setUpdatedAt(Instant.now());
            return o;
        });
        when(paymentGatewayClient.charge(any())).thenReturn(
                new PaymentResponseDto(null, 501L, 9L, BigDecimal.valueOf(60), "CARD", "FAILED", null, "Card declined", null));

        OrderResponse response = orderService.placeOrder(9L, request);

        assertEquals(OrderStatus.PAYMENT_FAILED, response.status());
    }

    @Test
    void placeOrder_rejectsClosedRestaurant() {
        PlaceOrderRequest request = new PlaceOrderRequest(1L,
                List.of(new OrderItemRequest(100L, 1)), "Addr", PaymentMethod.UPI);
        when(restaurantClient.getRestaurant(1L)).thenReturn(new RestaurantDto(1L, 55L, "Spice Route", "Bengaluru", false, 17.4, 78.5));

        assertThrows(RestaurantUnavailableException.class, () -> orderService.placeOrder(9L, request));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void acceptOrder_rejectsNonOwner() {
        Order order = Order.builder().id(10L).customerId(9L).restaurantId(1L)
                .status(OrderStatus.PAYMENT_COMPLETED).totalAmount(BigDecimal.TEN).build();
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(restaurantClient.getRestaurant(1L)).thenReturn(openRestaurant()); // ownerId 55

        assertThrows(AccessDeniedForResourceException.class, () -> orderService.acceptOrder(10L, 999L));
    }

    @Test
    void acceptOrder_rejectsWhenNotYetPaid() {
        Order order = Order.builder().id(11L).customerId(9L).restaurantId(1L)
                .status(OrderStatus.CREATED).totalAmount(BigDecimal.TEN).build();
        when(orderRepository.findById(11L)).thenReturn(Optional.of(order));
        when(restaurantClient.getRestaurant(1L)).thenReturn(openRestaurant());

        assertThrows(InvalidOrderStateException.class, () -> orderService.acceptOrder(11L, 55L));
    }

    @Test
    void rejectOrder_triggersRefund_forPaidOrder() {
        Order order = Order.builder().id(12L).customerId(9L).restaurantId(1L)
                .status(OrderStatus.PAYMENT_COMPLETED).totalAmount(BigDecimal.TEN).build();
        when(orderRepository.findById(12L)).thenReturn(Optional.of(order));
        when(restaurantClient.getRestaurant(1L)).thenReturn(openRestaurant());

        orderService.rejectOrder(12L, 55L, "Out of stock");

        verify(paymentClient).refund(12L);
        assertEquals(OrderStatus.REJECTED, order.getStatus());
    }

    @Test
    void cancelOrder_rejectsAfterAcceptance() {
        Order order = Order.builder().id(13L).customerId(9L).restaurantId(1L)
                .status(OrderStatus.ACCEPTED).totalAmount(BigDecimal.TEN).build();
        when(orderRepository.findById(13L)).thenReturn(Optional.of(order));

        assertThrows(InvalidOrderStateException.class, () -> orderService.cancelOrder(13L, 9L));
    }

    @Test
    void rateOrder_succeeds_forDeliveredUnratedOrder() {
        Order order = Order.builder().id(14L).customerId(9L).restaurantId(1L)
                .status(OrderStatus.DELIVERED).totalAmount(BigDecimal.TEN).build();
        when(orderRepository.findById(14L)).thenReturn(Optional.of(order));

        var response = orderService.rateOrder(14L, 9L, 5, "Great food!");

        assertEquals(5, response.customerRating());
        verify(restaurantClient).submitRating(eq(1L), any());
    }

    @Test
    void rateOrder_rejectsWhenNotDelivered() {
        Order order = Order.builder().id(15L).customerId(9L).restaurantId(1L)
                .status(OrderStatus.PREPARING).totalAmount(BigDecimal.TEN).build();
        when(orderRepository.findById(15L)).thenReturn(Optional.of(order));

        assertThrows(InvalidOrderStateException.class, () -> orderService.rateOrder(15L, 9L, 5, null));
    }

    @Test
    void rateOrder_rejectsSecondRating() {
        Order order = Order.builder().id(16L).customerId(9L).restaurantId(1L)
                .status(OrderStatus.DELIVERED).totalAmount(BigDecimal.TEN).customerRating(4).build();
        when(orderRepository.findById(16L)).thenReturn(Optional.of(order));

        assertThrows(InvalidOrderStateException.class, () -> orderService.rateOrder(16L, 9L, 5, null));
    }
}
