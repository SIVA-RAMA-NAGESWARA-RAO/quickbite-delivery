package com.quickbite.restaurant.dto;

import java.time.Instant;
import java.util.List;

public record RestaurantResponse(
        Long id,
        Long ownerId,
        String name,
        String description,
        String cuisineType,
        String address,
        String city,
        String contactPhone,
        String imageUrl,
        boolean open,
        Double rating,
        Long ratingCount,
        Double latitude,
        Double longitude,
        Instant createdAt,
        List<MenuItemResponse> menuItems
) {
}
