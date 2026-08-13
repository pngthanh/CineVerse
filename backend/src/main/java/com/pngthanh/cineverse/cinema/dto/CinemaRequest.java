package com.pngthanh.cineverse.cinema.dto;

import jakarta.validation.constraints.NotBlank;

public record CinemaRequest(
        @NotBlank String name,
        @NotBlank String address,
        boolean active) {
}
