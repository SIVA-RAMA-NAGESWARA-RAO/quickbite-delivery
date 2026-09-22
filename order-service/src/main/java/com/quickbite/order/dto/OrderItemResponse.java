package com.quickbite.order.dto;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long menuItemId,
        String itemName,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal lineTotal
) {
}
