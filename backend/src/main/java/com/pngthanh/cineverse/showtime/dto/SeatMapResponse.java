package com.pngthanh.cineverse.showtime.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SeatMapResponse(Long showtimeId, List<SeatItem> seats) {
    public record SeatItem(
            Long seatId,
            String code,
            String type,
            String status,
            BigDecimal price,
            Instant holdExpiresAt) {
    }
}
