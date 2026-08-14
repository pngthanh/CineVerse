package com.pngthanh.cineverse.cinema.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record RoomRequest(
        @NotBlank String name,
        @Min(6) @Max(26) int rows,
        @Min(6) @Max(30) int seatsPerRow,
        @NotNull @DecimalMin("0") BigDecimal weekdayBasePrice,
        @NotNull @DecimalMin("0") BigDecimal weekendBasePrice,
        @NotNull @DecimalMin("0") BigDecimal vipSurcharge,
        boolean active) {
}
