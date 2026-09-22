package com.quickbite.restaurant;

import com.quickbite.restaurant.dto.MenuItemRequest;
import com.quickbite.restaurant.dto.RestaurantRequest;
import com.quickbite.restaurant.entity.Restaurant;
import com.quickbite.restaurant.exception.AccessDeniedForResourceException;
import com.quickbite.restaurant.exception.ResourceNotFoundException;
import com.quickbite.restaurant.repository.MenuItemRepository;
import com.quickbite.restaurant.repository.RestaurantRepository;
import com.quickbite.restaurant.service.FileStorageService;
import com.quickbite.restaurant.service.GeoLocationHelper;
import com.quickbite.restaurant.service.RestaurantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RestaurantServiceTest {

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private MenuItemRepository menuItemRepository;

    private final GeoLocationHelper geoLocationHelper = new GeoLocationHelper();

    @Mock
    private FileStorageService fileStorageService;

    private RestaurantService restaurantService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        restaurantService = new RestaurantService(restaurantRepository, menuItemRepository, geoLocationHelper, fileStorageService);
    }

    @Test
    void createRestaurant_persistsWithOwnerId() {
        RestaurantRequest request = new RestaurantRequest("Spice Route", "Home-style curries", "Indian",
                "12 MG Road", "Bengaluru", "9998887777", null);

        when(restaurantRepository.save(any(Restaurant.class))).thenAnswer(invocation -> {
            Restaurant r = invocation.getArgument(0);
            r.setId(10L);
            r.setCreatedAt(Instant.now());
            return r;
        });

        var response = restaurantService.createRestaurant(42L, request);

        assertEquals(10L, response.id());
        assertEquals(42L, response.ownerId());
        assertEquals("Spice Route", response.name());
        assertTrue(response.open());
    }

    @Test
    void updateRestaurant_rejectsNonOwner() {
        Restaurant existing = Restaurant.builder().id(1L).ownerId(42L).name("Spice Route")
                .address("12 MG Road").city("Bengaluru").createdAt(Instant.now()).build();
        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(existing));

        RestaurantRequest request = new RestaurantRequest("New Name", null, null, "Addr", "City", null, null);

        assertThrows(AccessDeniedForResourceException.class,
                () -> restaurantService.updateRestaurant(1L, 999L, request));
    }

    @Test
    void addMenuItem_throwsWhenRestaurantMissing() {
        when(restaurantRepository.findById(5L)).thenReturn(Optional.empty());

        MenuItemRequest item = new MenuItemRequest("Butter Naan", "Soft & buttery", BigDecimal.valueOf(60), "Breads", null, true);

        assertThrows(ResourceNotFoundException.class,
                () -> restaurantService.addMenuItem(5L, 42L, item));
    }

    @Test
    void addRating_recomputesRunningAverage() {
        Restaurant existing = Restaurant.builder().id(1L).ownerId(42L).name("Spice Route")
                .address("12 MG Road").city("Bengaluru").rating(4.0).ratingCount(1L)
                .createdAt(Instant.now()).build();
        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(existing));

        var response = restaurantService.addRating(1L, 5);

        // (4.0 * 1 + 5) / 2 = 4.5
        assertEquals(4.5, response.rating());
        assertEquals(2L, response.ratingCount());
    }

    @Test
    void addRating_throwsWhenRestaurantMissing() {
        when(restaurantRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> restaurantService.addRating(99L, 5));
    }
}
