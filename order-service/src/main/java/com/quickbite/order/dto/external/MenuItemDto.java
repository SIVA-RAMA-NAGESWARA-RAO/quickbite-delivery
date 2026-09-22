package com.quickbite.order.dto.external;

import java.math.BigDecimal;

/** Mirrors restaurant-service's MenuItemResponse - only the fields order-service needs. */
public record MenuItemDto(
        Long id,
        Long restaurantId,
        String name,
        String description,
        BigDecimal price,
        String category,
        String imageUrl,
        boolean available
) {
}
