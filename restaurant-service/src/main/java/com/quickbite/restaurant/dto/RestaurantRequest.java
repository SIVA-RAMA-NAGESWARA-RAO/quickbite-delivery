package com.quickbite.restaurant.dto;

import jakarta.validation.constraints.NotBlank;

public record RestaurantRequest(
        @NotBlank String name,
        String description,
        String cuisineType,
        @NotBlank String address,
        @NotBlank String city,
        String contactPhone,
        String imageUrl
) {
}
