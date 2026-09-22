package com.quickbite.order.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.List;

/**
 * QuickBite doesn't integrate a real fleet-tracking system, so once an order
 * goes OUT_FOR_DELIVERY we simulate one: a delivery partner is "assigned",
 * given a plausible ETA, and a destination point near the restaurant stands
 * in for the customer's real address (geocoding free-text addresses would
 * need a paid mapping API and a network call this project doesn't depend
 * on). OrderService reads this back to interpolate a moving position on the
 * frontend's tracking map - simulated, but a real, working piece of
 * engineering rather than a hard-coded static screen.
 */
@Component
public class DeliverySimulator {

    private static final List<String> PARTNER_NAMES = List.of(
            "Arjun Kumar", "Priya Sharma", "Ravi Teja", "Sneha Reddy",
            "Vikram Singh", "Anjali Nair", "Karthik Iyer", "Divya Menon"
    );

    private static final double DESTINATION_JITTER_DEGREES = 0.03; // roughly ±3 km from the restaurant

    private final SecureRandom random = new SecureRandom();

    public record DeliveryAssignment(String partnerName, String partnerPhone, int etaMinutes,
                                      double destinationLat, double destinationLng) {
    }

    public DeliveryAssignment assign(double restaurantLat, double restaurantLng) {
        String name = PARTNER_NAMES.get(random.nextInt(PARTNER_NAMES.size()));
        String phone = "9" + (100000000 + random.nextInt(900000000));
        int etaMinutes = 10 + random.nextInt(11); // 10–20 minutes

        double destLat = restaurantLat + (random.nextDouble() - 0.5) * 2 * DESTINATION_JITTER_DEGREES;
        double destLng = restaurantLng + (random.nextDouble() - 0.5) * 2 * DESTINATION_JITTER_DEGREES;

        return new DeliveryAssignment(name, phone, etaMinutes, destLat, destLng);
    }
}
