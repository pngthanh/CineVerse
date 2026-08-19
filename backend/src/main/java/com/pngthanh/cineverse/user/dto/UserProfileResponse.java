package com.pngthanh.cineverse.user.dto;

import java.time.Instant;

public record UserProfileResponse(
        Long id,
        String fullName,
        String email,
        String username,
        boolean localCredentials,
        String role,
        String status,
        Instant createdAt) {
}
