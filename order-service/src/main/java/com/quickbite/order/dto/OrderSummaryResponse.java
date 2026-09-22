package com.quickbite.order.dto;

import java.math.BigDecimal;

public record OrderSummaryResponse(
        long totalOrders,
        long todayOrders,
        BigDecimal todayRevenue,
        BigDecimal allTimeRevenue
) {
}
