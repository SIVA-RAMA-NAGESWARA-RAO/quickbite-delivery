package com.quickbite.order.dto;

import com.quickbite.order.entity.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record PlaceOrderRequest(
        @NotNull Long restaurantId,
        @NotEmpty(message = "Order must contain at least one item") @Valid List<OrderItemRequest> items,
        @NotBlank String deliveryAddress,
        @NotNull PaymentMethod paymentMethod
) {
}
