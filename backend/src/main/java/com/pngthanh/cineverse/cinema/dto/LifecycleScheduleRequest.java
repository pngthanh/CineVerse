package com.pngthanh.cineverse.cinema.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record LifecycleScheduleRequest(
        @NotNull LocalDateTime closesAt,
        @NotBlank String reason) {
}
