package com.pngthanh.cineverse.user.dto;

import java.time.Instant;

public record UserProfileResponse(
        Long id,
        String fullName,
        String email,
        String username,
        boolean localCredentials,
        boolean googleLinked,
        String googleEmail,
        String phone,
        String provinceCode,
        String provinceName,
        String districtCode,
        String districtName,
        String wardCode,
        String wardName,
        String addressDetail,
        String role,
        Long assignedCinemaId,
        String assignedCinemaName,
        String status,
        Instant createdAt) {
}
