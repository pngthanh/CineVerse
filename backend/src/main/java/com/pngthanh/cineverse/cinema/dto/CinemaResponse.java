package com.pngthanh.cineverse.cinema.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CinemaResponse(
        Long id,
        String name,
        String address,
        boolean active,
        LocalDateTime closesAt,
        LocalDateTime closedAt,
        String closureReason,
        List<RoomResponse> rooms) {

    public record RoomResponse(
            Long id,
            String name,
            boolean active,
            LocalDateTime closesAt,
            LocalDateTime closedAt,
            String closureReason,
            int rows,
            int seatsPerRow,
            int seatCount,
            int vipSeatCount,
            BigDecimal weekdayBasePrice,
            BigDecimal weekendBasePrice,
            BigDecimal vipSurcharge) {
    }
}
