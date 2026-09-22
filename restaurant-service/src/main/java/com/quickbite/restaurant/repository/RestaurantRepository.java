package com.quickbite.restaurant.repository;

import com.quickbite.restaurant.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {
    List<Restaurant> findByCityIgnoreCaseAndOpenTrue(String city);
    List<Restaurant> findByOpenTrue();
    Optional<Restaurant> findByOwnerId(Long ownerId);
    boolean existsByOwnerId(Long ownerId);
}
