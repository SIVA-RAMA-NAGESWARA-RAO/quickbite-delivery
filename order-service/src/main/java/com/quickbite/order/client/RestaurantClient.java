package com.quickbite.order.client;

import com.quickbite.order.dto.external.MenuItemDto;
import com.quickbite.order.dto.external.RatingRequestDto;
import com.quickbite.order.dto.external.RestaurantDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "restaurant-service")
public interface RestaurantClient {

    @GetMapping("/restaurants/{id}")
    RestaurantDto getRestaurant(@PathVariable("id") Long id);

    @GetMapping("/restaurants/{id}/menu/{itemId}")
    MenuItemDto getMenuItem(@PathVariable("id") Long restaurantId,
                            @PathVariable("itemId") Long itemId);

    @PostMapping("/restaurants/{id}/rating")
    RestaurantDto submitRating(@PathVariable("id") Long id,
                               @RequestBody RatingRequestDto request);
}
