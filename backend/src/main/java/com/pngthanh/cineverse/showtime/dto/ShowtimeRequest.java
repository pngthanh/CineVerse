package com.pngthanh.cineverse.showtime.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ShowtimeRequest(
        @NotNull Long movieId,
        @NotNull Long roomId,
        @NotNull LocalDateTime startTime,
        @DecimalMin("0") BigDecimal basePrice) {
}
