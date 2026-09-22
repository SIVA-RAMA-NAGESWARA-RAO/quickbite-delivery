package com.quickbite.order.controller;

import com.quickbite.order.dto.OrderResponse;
import com.quickbite.order.dto.OrderSummaryResponse;
import com.quickbite.order.dto.DeliveryTrackingResponse;
import com.quickbite.order.dto.PlaceOrderRequest;
import com.quickbite.order.dto.RateOrderRequest;
import com.quickbite.order.dto.RejectOrderRequest;
import com.quickbite.order.entity.OrderStatus;
import com.quickbite.order.security.CurrentUser;
import com.quickbite.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order placement, tracking and the restaurant accept/reject workflow")
public class OrderController {

    private final OrderService orderService;
    private final CurrentUser currentUser;

    @PostMapping
    @Operation(summary = "Place a new order and attempt payment (customers)")
    public ResponseEntity<OrderResponse> placeOrder(@Valid @RequestBody PlaceOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.placeOrder(currentUser.id(), request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one order (the customer who placed it or the owning restaurant)")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrder(id, currentUser.id()));
    }

    @GetMapping("/customer/me")
    @Operation(summary = "List the signed-in customer's order history")
    public ResponseEntity<List<OrderResponse>> myOrders() {
        return ResponseEntity.ok(orderService.listCustomerOrders(currentUser.id()));
    }

    @GetMapping("/restaurant/{restaurantId}")
    @Operation(summary = "List incoming orders for a restaurant (owner only), optionally filtered by status")
    public ResponseEntity<List<OrderResponse>> restaurantOrders(@PathVariable Long restaurantId,
                                                                 @RequestParam(required = false) OrderStatus status) {
        return ResponseEntity.ok(orderService.listRestaurantOrders(restaurantId, currentUser.id(), status));
    }

    @GetMapping("/restaurant/{restaurantId}/summary")
    @Operation(summary = "Today's and all-time order/revenue counters for the owner's dashboard")
    public ResponseEntity<OrderSummaryResponse> restaurantSummary(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(orderService.getRestaurantSummary(restaurantId, currentUser.id()));
    }

    @PutMapping("/{id}/accept")
    @Operation(summary = "Accept a paid order (restaurant owner)")
    public ResponseEntity<OrderResponse> accept(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.acceptOrder(id, currentUser.id()));
    }

    @PutMapping("/{id}/reject")
    @Operation(summary = "Reject a paid order; automatically refunds the customer (restaurant owner)")
    public ResponseEntity<OrderResponse> reject(@PathVariable Long id, @RequestBody(required = false) RejectOrderRequest body) {
        String reason = body == null ? null : body.reason();
        return ResponseEntity.ok(orderService.rejectOrder(id, currentUser.id(), reason));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Advance an accepted order through PREPARING -> OUT_FOR_DELIVERY -> DELIVERED (restaurant owner)")
    public ResponseEntity<OrderResponse> advanceStatus(@PathVariable Long id, @RequestParam OrderStatus target) {
        return ResponseEntity.ok(orderService.advanceStatus(id, currentUser.id(), target));
    }

    @GetMapping("/{id}/tracking")
    @Operation(summary = "Live (simulated) delivery position, available once the order is out for delivery")
    public ResponseEntity<DeliveryTrackingResponse> tracking(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getTracking(id, currentUser.id()));
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "Cancel an order before the restaurant accepts it (customer); refunds if already paid")
    public ResponseEntity<OrderResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.cancelOrder(id, currentUser.id()));
    }

    @PostMapping("/{id}/rating")
    @Operation(summary = "Rate a delivered order once (customer); updates the restaurant's overall rating")
    public ResponseEntity<OrderResponse> rate(@PathVariable Long id, @Valid @RequestBody RateOrderRequest request) {
        return ResponseEntity.ok(orderService.rateOrder(id, currentUser.id(), request.rating(), request.comment()));
    }
}
