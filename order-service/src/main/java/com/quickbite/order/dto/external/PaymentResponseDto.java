package com.quickbite.order.dto.external;

import java.math.BigDecimal;

public record PaymentResponseDto(
        Long id,
        Long orderId,
        Long customerId,
        BigDecimal amount,
        String method,
        String status,
        String transactionRef,
        String failureReason,
        String createdAt
) {
}
