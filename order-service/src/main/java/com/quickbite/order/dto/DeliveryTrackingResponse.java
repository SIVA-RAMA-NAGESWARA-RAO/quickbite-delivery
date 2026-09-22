package com.quickbite.order.dto;

public record DeliveryTrackingResponse(
        Long orderId,
        String status,
        String deliveryPartnerName,
        String deliveryPartnerPhone,
        double restaurantLat,
        double restaurantLng,
        double destinationLat,
        double destinationLng,
        double currentLat,
        double currentLng,
        int progressPercent,
        int etaMinutesRemaining,
        boolean simulated
) {
}
