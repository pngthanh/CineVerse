package com.pngthanh.cineverse.movie.dto;

import java.time.LocalDate;

public record MovieResponse(
        Long id,
        String title,
        String description,
        String genres,
        Integer durationMinutes,
        LocalDate releaseDate,
        LocalDate endDate,
        String director,
        String castNames,
        String ageRating,
        String posterUrl,
        String backdropUrl,
        String trailerUrl,
        String status,
        Long ticketsSold) {
}
