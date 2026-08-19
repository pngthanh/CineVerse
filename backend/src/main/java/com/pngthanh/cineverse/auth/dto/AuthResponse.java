package com.pngthanh.cineverse.auth.dto;

import java.time.Instant;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        UserSummary user) {

    public record UserSummary(
            Long id,
            String fullName,
            String email,
            String username,
            boolean localCredentials,
            String role,
            String status,
            Instant createdAt) {
    }
}
