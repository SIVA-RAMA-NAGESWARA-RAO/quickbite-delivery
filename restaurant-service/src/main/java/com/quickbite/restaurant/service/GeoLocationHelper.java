package com.quickbite.restaurant.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Map;

/**
 * QuickBite doesn't integrate a geocoding API (that needs a paid key and an
 * external network call), so instead of asking restaurant owners to look up
 * their own latitude/longitude, we approximate a location from the city
 * name they already typed and nudge it with a small random offset - enough
 * for the delivery-tracking map to place restaurants at sensible, spread-out
 * points without ever showing two restaurants stacked on the exact same pin.
 */
@Component
public class GeoLocationHelper {

    private static final Map<String, double[]> CITY_CENTERS = Map.ofEntries(
            Map.entry("hyderabad", new double[]{17.3850, 78.4867}),
            Map.entry("bengaluru", new double[]{12.9716, 77.5946}),
            Map.entry("bangalore", new double[]{12.9716, 77.5946}),
            Map.entry("mumbai", new double[]{19.0760, 72.8777}),
            Map.entry("delhi", new double[]{28.7041, 77.1025}),
            Map.entry("chennai", new double[]{13.0827, 80.2707}),
            Map.entry("pune", new double[]{18.5204, 73.8567}),
            Map.entry("kolkata", new double[]{22.5726, 88.3639}),
            Map.entry("ahmedabad", new double[]{23.0225, 72.5714}),
            Map.entry("nellore", new double[]{14.4426, 79.9865})
    );

    private static final double[] DEFAULT_CENTER = {20.5937, 78.9629}; // geographic center of India
    private static final double JITTER_DEGREES = 0.02; // roughly ±2 km

    private final SecureRandom random = new SecureRandom();

    public double[] approximateCoordinates(String city) {
        double[] center = city == null
                ? DEFAULT_CENTER
                : CITY_CENTERS.getOrDefault(city.trim().toLowerCase(), DEFAULT_CENTER);

        double lat = center[0] + (random.nextDouble() - 0.5) * 2 * JITTER_DEGREES;
        double lng = center[1] + (random.nextDouble() - 0.5) * 2 * JITTER_DEGREES;
        return new double[]{lat, lng};
    }
}
