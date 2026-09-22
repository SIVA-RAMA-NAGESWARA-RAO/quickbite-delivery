package com.quickbite.restaurant.controller;

import com.quickbite.restaurant.dto.*;
import com.quickbite.restaurant.security.CurrentUser;
import com.quickbite.restaurant.service.RestaurantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/restaurants")
@RequiredArgsConstructor
@Tag(name = "Restaurants", description = "Catalog browsing for customers, menu management for owners")
public class RestaurantController {

    private final RestaurantService restaurantService;
    private final CurrentUser currentUser;

    @GetMapping
    @Operation(summary = "List open restaurants, optionally filtered by city")
    public ResponseEntity<List<RestaurantResponse>> list(@RequestParam(required = false) String city) {
        return ResponseEntity.ok(restaurantService.listRestaurants(city));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one restaurant with its full menu")
    public ResponseEntity<RestaurantResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(restaurantService.getRestaurant(id));
    }

    @GetMapping("/owner/me")
    @Operation(summary = "Get the restaurant owned by the signed-in user")
    public ResponseEntity<RestaurantResponse> myRestaurant() {
        return ResponseEntity.ok(restaurantService.getMyRestaurant(currentUser.id()));
    }

    @PostMapping
    @Operation(summary = "Register a new restaurant (restaurant owners only)")
    public ResponseEntity<RestaurantResponse> create(@Valid @RequestBody RestaurantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(restaurantService.createRestaurant(currentUser.id(), request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a restaurant's profile (owner only)")
    public ResponseEntity<RestaurantResponse> update(@PathVariable Long id, @Valid @RequestBody RestaurantRequest request) {
        return ResponseEntity.ok(restaurantService.updateRestaurant(id, currentUser.id(), request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Open or temporarily close a restaurant for new orders (owner only)")
    public ResponseEntity<RestaurantResponse> setStatus(@PathVariable Long id, @RequestParam boolean open) {
        return ResponseEntity.ok(restaurantService.setOpenStatus(id, currentUser.id(), open));
    }

    @PostMapping("/{id}/rating")
    @Operation(summary = "Record a customer's rating for a delivered order (called internally by order-service)")
    public ResponseEntity<RestaurantResponse> addRating(@PathVariable Long id, @Valid @RequestBody RatingRequest request) {
        return ResponseEntity.ok(restaurantService.addRating(id, request.rating()));
    }

    @PostMapping(value = "/{id}/image", consumes = "multipart/form-data")
    @Operation(summary = "Upload/replace this restaurant's cover photo (owner only)")
    public ResponseEntity<RestaurantResponse> uploadRestaurantImage(@PathVariable Long id,
                                                                     @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(restaurantService.setRestaurantImage(id, currentUser.id(), file));
    }

    @PostMapping(value = "/{id}/menu/{itemId}/image", consumes = "multipart/form-data")
    @Operation(summary = "Upload/replace a menu item's photo (owner only)")
    public ResponseEntity<MenuItemResponse> uploadMenuItemImage(@PathVariable Long id, @PathVariable Long itemId,
                                                                 @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(restaurantService.setMenuItemImage(id, itemId, currentUser.id(), file));
    }

    @GetMapping("/{id}/menu")
    @Operation(summary = "List a restaurant's menu items")
    public ResponseEntity<List<MenuItemResponse>> listMenu(@PathVariable Long id) {
        return ResponseEntity.ok(restaurantService.listMenu(id));
    }

    @GetMapping("/{id}/menu/{itemId}")
    @Operation(summary = "Get one menu item (used internally by order-service to validate price)")
    public ResponseEntity<MenuItemResponse> getMenuItem(@PathVariable Long id, @PathVariable Long itemId) {
        return ResponseEntity.ok(restaurantService.getMenuItemForOrder(id, itemId));
    }

    @PostMapping("/{id}/menu")
    @Operation(summary = "Add a menu item (owner only)")
    public ResponseEntity<MenuItemResponse> addMenuItem(@PathVariable Long id, @Valid @RequestBody MenuItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(restaurantService.addMenuItem(id, currentUser.id(), request));
    }

    @PutMapping("/{id}/menu/{itemId}")
    @Operation(summary = "Update a menu item, including toggling availability (owner only)")
    public ResponseEntity<MenuItemResponse> updateMenuItem(@PathVariable Long id, @PathVariable Long itemId,
                                                            @Valid @RequestBody MenuItemRequest request) {
        return ResponseEntity.ok(restaurantService.updateMenuItem(id, itemId, currentUser.id(), request));
    }

    @DeleteMapping("/{id}/menu/{itemId}")
    @Operation(summary = "Remove a menu item (owner only)")
    public ResponseEntity<Void> deleteMenuItem(@PathVariable Long id, @PathVariable Long itemId) {
        restaurantService.deleteMenuItem(id, itemId, currentUser.id());
        return ResponseEntity.noContent().build();
    }
}
