package com.pngthanh.cineverse.admin.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResponse(
        long totalUsers,
        long totalBookings,
        long confirmedBookings,
        long totalMovies,
        long totalCinemas,
        long ticketsSold,
        BigDecimal grossSeatRevenue,
        BigDecimal concessionRevenue,
        BigDecimal discountAmount,
        BigDecimal netRevenue,
        List<CinemaRevenue> cinemas,
        List<MovieRevenue> movies,
        List<ConcessionRevenue> concessions) {

    public record CinemaRevenue(
            Long cinemaId,
            String cinemaName,
            long bookings,
            long ticketsSold,
            BigDecimal seatRevenue,
            BigDecimal concessionRevenue,
            BigDecimal discountAmount,
            BigDecimal netRevenue,
            List<MovieRevenue> movies,
            List<ConcessionRevenue> concessions) {
    }

    public record MovieRevenue(
            Long movieId,
            String movieTitle,
            String posterUrl,
            long bookings,
            long ticketsSold,
            BigDecimal ticketRevenue) {
    }

    public record ConcessionRevenue(
            Long itemId,
            String itemName,
            long quantity,
            BigDecimal revenue) {
    }
}
