package com.quickbite.order.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * QuickBite is a *demand-driven* platform - pricing should respond to real
 * demand, not stay flat around the clock. This applies a transparent surge
 * multiplier when either (a) it's a known peak dining window, or (b) the
 * target restaurant already has a backlog of unfulfilled orders right now.
 * Both the trigger and the exact multiplier are returned to the caller so
 * the customer always sees *why* their total went up, never a silent markup.
 *
 * Takes a {@link Clock} rather than calling LocalTime.now() directly so
 * tests can pin the "current time" instead of becoming flaky depending on
 * when they happen to run.
 */
@Component
public class SurgePricingService {

    private static final LocalTime LUNCH_START = LocalTime.of(12, 0);
    private static final LocalTime LUNCH_END = LocalTime.of(15, 0);
    private static final LocalTime DINNER_START = LocalTime.of(19, 0);
    private static final LocalTime DINNER_END = LocalTime.of(22, 30);

    private static final BigDecimal PEAK_HOUR_MULTIPLIER = new BigDecimal("1.15");
    private static final BigDecimal HIGH_DEMAND_MULTIPLIER = new BigDecimal("1.10");
    private static final long HIGH_DEMAND_ORDER_THRESHOLD = 5;

    private final Clock clock;

    public SurgePricingService(Clock clock) {
        this.clock = clock;
    }

    public record SurgeResult(BigDecimal baseAmount, BigDecimal finalAmount, BigDecimal multiplier, String reason) {
    }

    public SurgeResult apply(BigDecimal baseAmount, long restaurantPendingOrders) {
        List<String> reasons = new ArrayList<>();
        BigDecimal multiplier = BigDecimal.ONE;

        if (isPeakDiningHour(LocalTime.now(clock))) {
            multiplier = multiplier.multiply(PEAK_HOUR_MULTIPLIER);
            reasons.add("peak dining hours");
        }
        if (restaurantPendingOrders >= HIGH_DEMAND_ORDER_THRESHOLD) {
            multiplier = multiplier.multiply(HIGH_DEMAND_MULTIPLIER);
            reasons.add("high demand at this restaurant right now");
        }

        BigDecimal finalAmount = baseAmount.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
        String reason = reasons.isEmpty() ? null : capitalize(String.join(" and ", reasons));
        return new SurgeResult(baseAmount, finalAmount, multiplier.setScale(2, RoundingMode.HALF_UP), reason);
    }

    private boolean isPeakDiningHour(LocalTime now) {
        return (!now.isBefore(LUNCH_START) && now.isBefore(LUNCH_END))
                || (!now.isBefore(DINNER_START) && now.isBefore(DINNER_END));
    }

    private String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}

