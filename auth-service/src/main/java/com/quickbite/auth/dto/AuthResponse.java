package com.quickbite.auth.dto;

import com.quickbite.auth.entity.Role;

public record AuthResponse(
        String token,
        Long userId,
        String fullName,
        String email,
        Role role,
        long expiresInMs
) {
}
