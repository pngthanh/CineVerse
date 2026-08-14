package com.pngthanh.cineverse.cinema.dto;

import java.math.BigDecimal;
import java.util.List;

public record CinemaResponse(
        Long id,
        String name,
        String address,
        boolean active,
        List<RoomResponse> rooms) {

    public record RoomResponse(
            Long id,
            String name,
            boolean active,
            int rows,
            int seatsPerRow,
            int seatCount,
            int vipSeatCount,
            BigDecimal weekdayBasePrice,
            BigDecimal weekendBasePrice,
            BigDecimal vipSurcharge) {
    }
}
