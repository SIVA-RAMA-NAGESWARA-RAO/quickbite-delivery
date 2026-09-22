package com.quickbite.order.dto.external;

/** Mirrors auth-service's UserSummary - just enough to send a notification email. */
public record UserSummaryDto(
        Long id,
        String fullName,
        String email,
        String phone,
        String role
) {
}
