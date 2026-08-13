package com.pngthanh.cineverse.cinema.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record RoomRequest(
        @NotBlank String name,
        @Min(1) @Max(26) int rows,
        @Min(2) @Max(30) int seatsPerRow) {
}
