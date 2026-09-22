package com.quickbite.restaurant.service;

import com.quickbite.restaurant.dto.*;
import com.quickbite.restaurant.entity.MenuItem;
import com.quickbite.restaurant.entity.Restaurant;
import com.quickbite.restaurant.exception.AccessDeniedForResourceException;
import com.quickbite.restaurant.exception.ResourceNotFoundException;
import com.quickbite.restaurant.repository.MenuItemRepository;
import com.quickbite.restaurant.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;
    private final GeoLocationHelper geoLocationHelper;
    private final FileStorageService fileStorageService;

    @Transactional
    public RestaurantResponse createRestaurant(Long ownerId, RestaurantRequest request) {
        double[] coordinates = geoLocationHelper.approximateCoordinates(request.city());

        Restaurant restaurant = Restaurant.builder()
                .ownerId(ownerId)
                .name(request.name())
                .description(request.description())
                .cuisineType(request.cuisineType())
                .address(request.address())
                .city(request.city())
                .contactPhone(request.contactPhone())
                .imageUrl(request.imageUrl())
                .latitude(coordinates[0])
                .longitude(coordinates[1])
                .open(true)
                .build();

        return toResponse(restaurantRepository.save(restaurant));
    }

    @Transactional(readOnly = true)
    public List<RestaurantResponse> listRestaurants(String city) {
        List<Restaurant> restaurants = (city == null || city.isBlank())
                ? restaurantRepository.findByOpenTrue()
                : restaurantRepository.findByCityIgnoreCaseAndOpenTrue(city);
        return restaurants.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public RestaurantResponse getRestaurant(Long id) {
        return toResponse(findRestaurantOrThrow(id));
    }

    @Transactional(readOnly = true)
    public RestaurantResponse getMyRestaurant(Long ownerId) {
        Restaurant restaurant = restaurantRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("You have not registered a restaurant yet"));
        return toResponse(restaurant);
    }

    @Transactional
    public RestaurantResponse updateRestaurant(Long id, Long ownerId, RestaurantRequest request) {
        Restaurant restaurant = findRestaurantOrThrow(id);
        assertOwnership(restaurant, ownerId);

        restaurant.setName(request.name());
        restaurant.setDescription(request.description());
        restaurant.setCuisineType(request.cuisineType());
        restaurant.setAddress(request.address());
        restaurant.setCity(request.city());
        restaurant.setContactPhone(request.contactPhone());
        restaurant.setImageUrl(request.imageUrl());

        return toResponse(restaurant);
    }

    @Transactional
    public RestaurantResponse setOpenStatus(Long id, Long ownerId, boolean open) {
        Restaurant restaurant = findRestaurantOrThrow(id);
        assertOwnership(restaurant, ownerId);
        restaurant.setOpen(open);
        return toResponse(restaurant);
    }

    @Transactional
    public MenuItemResponse addMenuItem(Long restaurantId, Long ownerId, MenuItemRequest request) {
        Restaurant restaurant = findRestaurantOrThrow(restaurantId);
        assertOwnership(restaurant, ownerId);

        MenuItem item = MenuItem.builder()
                .restaurant(restaurant)
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .category(request.category())
                .imageUrl(request.imageUrl())
                .available(request.available() == null || request.available())
                .build();

        return toResponse(menuItemRepository.save(item));
    }

    @Transactional(readOnly = true)
    public List<MenuItemResponse> listMenu(Long restaurantId) {
        findRestaurantOrThrow(restaurantId);
        return menuItemRepository.findByRestaurantId(restaurantId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public MenuItemResponse updateMenuItem(Long restaurantId, Long itemId, Long ownerId, MenuItemRequest request) {
        Restaurant restaurant = findRestaurantOrThrow(restaurantId);
        assertOwnership(restaurant, ownerId);

        MenuItem item = menuItemRepository.findByIdAndRestaurantId(itemId, restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item " + itemId + " not found for this restaurant"));

        item.setName(request.name());
        item.setDescription(request.description());
        item.setPrice(request.price());
        item.setCategory(request.category());
        item.setImageUrl(request.imageUrl());
        if (request.available() != null) {
            item.setAvailable(request.available());
        }

        return toResponse(item);
    }

    @Transactional
    public void deleteMenuItem(Long restaurantId, Long itemId, Long ownerId) {
        Restaurant restaurant = findRestaurantOrThrow(restaurantId);
        assertOwnership(restaurant, ownerId);

        MenuItem item = menuItemRepository.findByIdAndRestaurantId(itemId, restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item " + itemId + " not found for this restaurant"));
        menuItemRepository.delete(item);
    }

    /** Used by order-service (via Feign) to validate price/availability at order time. */
    @Transactional(readOnly = true)
    public MenuItemResponse getMenuItemForOrder(Long restaurantId, Long itemId) {
        MenuItem item = menuItemRepository.findByIdAndRestaurantId(itemId, restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item " + itemId + " not found for restaurant " + restaurantId));
        return toResponse(item);
    }

    @Transactional
    public RestaurantResponse setRestaurantImage(Long restaurantId, Long ownerId, MultipartFile file) {
        Restaurant restaurant = findRestaurantOrThrow(restaurantId);
        assertOwnership(restaurant, ownerId);
        restaurant.setImageUrl(fileStorageService.store(file));
        return toResponse(restaurant);
    }

    @Transactional
    public MenuItemResponse setMenuItemImage(Long restaurantId, Long itemId, Long ownerId, MultipartFile file) {
        Restaurant restaurant = findRestaurantOrThrow(restaurantId);
        assertOwnership(restaurant, ownerId);
        MenuItem item = menuItemRepository.findByIdAndRestaurantId(itemId, restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item " + itemId + " not found for this restaurant"));
        item.setImageUrl(fileStorageService.store(file));
        return toResponse(item);
    }

    /**
     * Called by order-service after a customer rates a delivered order.
     * Recomputes a running average rather than storing every individual
     * rating, which keeps this service's schema simple while still giving
     * an accurate, ever-improving picture of a restaurant's quality.
     */
    @Transactional
    public RestaurantResponse addRating(Long restaurantId, int newRating) {
        Restaurant restaurant = findRestaurantOrThrow(restaurantId);

        double currentTotal = restaurant.getRating() * restaurant.getRatingCount();
        long newCount = restaurant.getRatingCount() + 1;
        double newAverage = (currentTotal + newRating) / newCount;

        restaurant.setRating(Math.round(newAverage * 10.0) / 10.0);
        restaurant.setRatingCount(newCount);

        return toResponse(restaurant);
    }

    private Restaurant findRestaurantOrThrow(Long id) {
        return restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant " + id + " not found"));
    }

    private void assertOwnership(Restaurant restaurant, Long ownerId) {
        if (!restaurant.getOwnerId().equals(ownerId)) {
            throw new AccessDeniedForResourceException("You do not own this restaurant");
        }
    }

    private RestaurantResponse toResponse(Restaurant r) {
        List<MenuItemResponse> items = r.getMenuItems() == null ? List.of()
                : r.getMenuItems().stream().map(this::toResponse).toList();
        return new RestaurantResponse(r.getId(), r.getOwnerId(), r.getName(), r.getDescription(),
                r.getCuisineType(), r.getAddress(), r.getCity(), r.getContactPhone(), r.getImageUrl(),
                r.isOpen(), r.getRating(), r.getRatingCount(), r.getLatitude(), r.getLongitude(),
                r.getCreatedAt(), items);
    }

    private MenuItemResponse toResponse(MenuItem item) {
        return new MenuItemResponse(item.getId(), item.getRestaurant().getId(), item.getName(),
                item.getDescription(), item.getPrice(), item.getCategory(), item.getImageUrl(), item.isAvailable());
    }
}
