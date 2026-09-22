package com.quickbite.payment.dto;

import com.quickbite.payment.entity.PaymentMethod;
import com.quickbite.payment.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        Long id,
        Long orderId,
        Long customerId,
        BigDecimal amount,
        PaymentMethod method,
        PaymentStatus status,
        String transactionRef,
        String failureReason,
        Instant createdAt
) {
}
