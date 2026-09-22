package com.quickbite.order.client;
import com.quickbite.order.dto.external.MenuItemDto;
import com.quickbite.order.dto.external.RatingRequestDto;
import com.quickbite.order.dto.external.RestaurantDto;
import com.quickbite.restaurant.service.RestaurantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
@Component @RequiredArgsConstructor
public class RestaurantClient {
  private final RestaurantService restaurantService;
  public RestaurantDto getRestaurant(Long id){var r=restaurantService.getRestaurant(id);return new RestaurantDto(r.id(),r.ownerId(),r.name(),r.city(),r.open(),r.latitude(),r.longitude());}
  public MenuItemDto getMenuItem(Long restaurantId,Long itemId){var m=restaurantService.getMenuItemForOrder(restaurantId,itemId);return new MenuItemDto(m.id(),m.restaurantId(),m.name(),m.description(),m.price(),m.category(),m.imageUrl(),m.available());}
  public RestaurantDto submitRating(Long id,RatingRequestDto request){var r=restaurantService.addRating(id,request.rating());return new RestaurantDto(r.id(),r.ownerId(),r.name(),r.city(),r.open(),r.latitude(),r.longitude());}
}