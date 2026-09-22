package com.quickbite.order.dto.external;

/** Mirrors restaurant-service's RestaurantResponse - only the fields order-service needs. */
public record RestaurantDto(
        Long id,
        Long ownerId,
        String name,
        String city,
        boolean open,
        Double latitude,
        Double longitude
) {
}
