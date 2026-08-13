package com.pngthanh.cineverse.booking.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SeatHoldResponse(
        String holdToken,
        Instant expiresAt,
        List<SeatPrice> seats,
        BigDecimal total) {

    public record SeatPrice(
            Long seatId,
            String code,
            String type,
            BigDecimal price) {
    }
}
