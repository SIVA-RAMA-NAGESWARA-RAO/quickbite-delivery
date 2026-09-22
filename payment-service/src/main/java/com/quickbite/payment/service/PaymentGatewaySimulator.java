package com.quickbite.payment.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * Stands in for a real payment gateway (Razorpay, Stripe, etc). No card data
 * is ever collected here - the "gateway" simply approves or declines a
 * transaction so the rest of the platform (order acceptance, notifications,
 * refunds) can be built and demoed end to end without a merchant account.
 * Swap this class out for a real client when moving to production.
 */
@Component
public class PaymentGatewaySimulator {

    private static final double SUCCESS_RATE = 0.92;
    private final SecureRandom random = new SecureRandom();

    public GatewayResult charge(double amount) {
        String ref = "TXN-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        boolean approved = random.nextDouble() < SUCCESS_RATE;
        String reason = approved ? null : "Card declined by issuing bank (simulated)";
        return new GatewayResult(approved, ref, reason);
    }

    public record GatewayResult(boolean approved, String transactionRef, String failureReason) {
    }
}
