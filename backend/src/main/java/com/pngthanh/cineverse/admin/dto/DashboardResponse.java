package com.pngthanh.cineverse.admin.dto;

public record DashboardResponse(
        long totalUsers,
        long totalBookings,
        long confirmedBookings,
        long totalMovies,
        long totalCinemas) {
}
