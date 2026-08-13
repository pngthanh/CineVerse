package com.pngthanh.cineverse.user.dto;

import java.time.Instant;

public record UserProfileResponse(
        Long id,
        String fullName,
        String email,
        String role,
        String status,
        Instant createdAt) {
}
