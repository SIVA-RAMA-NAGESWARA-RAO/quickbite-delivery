package com.quickbite.order.client;

import com.quickbite.order.config.FeignClientConfig;
import com.quickbite.order.dto.external.MenuItemDto;
import com.quickbite.order.dto.external.RatingRequestDto;
import com.quickbite.order.dto.external.RestaurantDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Talks to restaurant-service by its Eureka service id ("restaurant-service")
 * rather than a hard-coded host:port. Spring Cloud LoadBalancer picks a
 * healthy instance from the registry on every call, which is what gives us
 * horizontal scaling without any client-side configuration changes.
 */
@FeignClient(name = "restaurant-service", configuration = FeignClientConfig.class)
public interface RestaurantClient {

    @GetMapping("/restaurants/{restaurantId}")
    RestaurantDto getRestaurant(@PathVariable("restaurantId") Long restaurantId);

    @GetMapping("/restaurants/{restaurantId}/menu/{itemId}")
    MenuItemDto getMenuItem(@PathVariable("restaurantId") Long restaurantId, @PathVariable("itemId") Long itemId);

    @PostMapping("/restaurants/{restaurantId}/rating")
    RestaurantDto submitRating(@PathVariable("restaurantId") Long restaurantId, @RequestBody RatingRequestDto request);
}
