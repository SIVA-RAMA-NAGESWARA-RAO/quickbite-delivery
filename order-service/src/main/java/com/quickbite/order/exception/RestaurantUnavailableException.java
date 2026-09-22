package com.quickbite.order.exception;

public class RestaurantUnavailableException extends RuntimeException {
    public RestaurantUnavailableException(String message) {
        super(message);
    }
}
