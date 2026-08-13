package com.pngthanh.cineverse.movie.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MovieResponse(
        Long id,
        String title,
        String description,
        String genres,
        Integer durationMinutes,
        LocalDate releaseDate,
        String director,
        String castNames,
        String ageRating,
        String posterUrl,
        String backdropUrl,
        String trailerUrl,
        String status,
        BigDecimal ratingAverage,
        Integer reviewCount,
        Long ticketsSold) {
}
