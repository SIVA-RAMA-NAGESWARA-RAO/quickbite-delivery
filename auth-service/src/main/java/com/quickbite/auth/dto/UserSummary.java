package com.quickbite.auth.dto;

import com.quickbite.auth.entity.Role;

public record UserSummary(
        Long id,
        String fullName,
        String email,
        String phone,
        Role role
) {
}
