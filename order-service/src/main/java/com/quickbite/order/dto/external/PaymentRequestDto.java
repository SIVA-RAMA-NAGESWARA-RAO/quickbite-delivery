package com.quickbite.order.dto.external;

import java.math.BigDecimal;

public record PaymentRequestDto(
        Long orderId,
        Long customerId,
        BigDecimal amount,
        String method
) {
}
