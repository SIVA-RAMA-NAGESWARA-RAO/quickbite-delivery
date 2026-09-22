package com.quickbite.order.entity;

public enum OrderStatus {
    /** Order captured, price validated against the live menu, payment not yet attempted. */
    CREATED,
    /** Payment gateway declined the charge; the order goes no further. */
    PAYMENT_FAILED,
    /** Payment succeeded; the order is now visible to the restaurant to accept/reject. */
    PAYMENT_COMPLETED,
    /** Restaurant has accepted and will start preparing the order. */
    ACCEPTED,
    /** Restaurant rejected the order; a paid order is refunded automatically. */
    REJECTED,
    PREPARING,
    OUT_FOR_DELIVERY,
    DELIVERED,
    /** Cancelled by the customer before the restaurant accepted it. */
    CANCELLED
}
