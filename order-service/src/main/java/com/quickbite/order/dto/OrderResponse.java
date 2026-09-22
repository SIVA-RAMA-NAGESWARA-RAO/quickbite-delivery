package com.quickbite.order.dto;

import com.quickbite.order.entity.OrderStatus;
import com.quickbite.order.entity.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        Long customerId,
        Long restaurantId,
        String deliveryAddress,
        BigDecimal totalAmount,
        BigDecimal baseAmount,
        BigDecimal surgeMultiplier,
        String surgeReason,
        PaymentMethod paymentMethod,
        OrderStatus status,
        String rejectionReason,
        Integer customerRating,
        String ratingComment,
        Instant createdAt,
        Instant updatedAt,
        List<OrderItemResponse> items
) {
}
