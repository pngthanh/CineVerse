package com.pngthanh.cineverse.movie.dto;

import com.pngthanh.cineverse.common.enums.MovieStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record MovieRequest(
        @NotBlank @Size(max = 160) String title,
        @NotBlank @Size(max = 2000) String description,
        @NotBlank String genres,
        @NotNull @Min(1) Integer durationMinutes,
        LocalDate releaseDate,
        LocalDate endDate,
        String director,
        String castNames,
        String ageRating,
        String posterUrl,
        String backdropUrl,
        String trailerUrl,
        @NotNull MovieStatus status) {
}
