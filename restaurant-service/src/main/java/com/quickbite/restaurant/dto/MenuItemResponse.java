package com.quickbite.restaurant.dto;

import java.math.BigDecimal;

public record MenuItemResponse(
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
