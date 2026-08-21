package com.pngthanh.cineverse.showtime.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ShowtimeResponse(
        Long id,
        Long movieId,
        String movieTitle,
        Long cinemaId,
        String cinemaName,
        Long roomId,
        String roomName,
        LocalDateTime startTime,
        LocalDateTime endTime,
        LocalDateTime salesCloseTime,
        BigDecimal basePrice,
        boolean active,
        String lifecycleStatus,
        boolean bookable,
        LocalDateTime cancelledAt,
        String cancellationReason) {
}
