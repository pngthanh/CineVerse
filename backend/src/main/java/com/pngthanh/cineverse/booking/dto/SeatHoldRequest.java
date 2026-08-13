package com.pngthanh.cineverse.booking.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SeatHoldRequest(
        @NotNull Long showtimeId,
        @NotEmpty @Size(max = 8) List<Long> seatIds) {
}
