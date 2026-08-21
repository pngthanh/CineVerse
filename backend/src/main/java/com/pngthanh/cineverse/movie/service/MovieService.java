package com.pngthanh.cineverse.movie.service;

import com.pngthanh.cineverse.common.enums.MovieStatus;
import com.pngthanh.cineverse.common.exception.ApiException;
import com.pngthanh.cineverse.movie.dto.MovieRequest;
import com.pngthanh.cineverse.movie.dto.MovieResponse;
import com.pngthanh.cineverse.movie.entity.Movie;
import com.pngthanh.cineverse.movie.repository.MovieRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MovieService {
    private final MovieRepository movies;

    public MovieService(MovieRepository movies) {
        this.movies = movies;
    }

    @Transactional
    public List<MovieResponse> list(MovieStatus status) {
        syncLifecycle();
        List<Movie> data = status == null
                ? movies.findAll()
                : movies.findAllByStatusOrderByReleaseDateDesc(status);
        return data.stream().map(this::toResponse).toList();
    }

    @Transactional
    public List<MovieResponse> listPublic(MovieStatus status) {
        syncLifecycle();
        return movies.findAll().stream()
                .filter(movie -> movie.getStatus() == MovieStatus.NOW_SHOWING
                        || movie.getStatus() == MovieStatus.COMING_SOON)
                .filter(movie -> status == null || movie.getStatus() == status)
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public MovieResponse get(Long id) {
        return toResponse(require(id));
    }

    @Transactional
    public MovieResponse getPublic(Long id) {
        syncLifecycle();
        Movie movie = require(id);
        if (movie.getStatus() == MovieStatus.ENDED || movie.getStatus() == MovieStatus.INACTIVE) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "MOVIE_NOT_AVAILABLE",
                    "Phim không còn hiển thị trên hệ thống đặt vé.");
        }
        return toResponse(movie);
    }

    @Transactional
    public MovieResponse create(MovieRequest request) {
        Movie movie = new Movie();
        apply(movie, request);
        return toResponse(movies.save(movie));
    }

    @Transactional
    public MovieResponse update(Long id, MovieRequest request) {
        Movie movie = require(id);
        apply(movie, request);
        return toResponse(movie);
    }

    @Transactional
    public void deactivate(Long id) {
        Movie movie = require(id);
        movie.setStatus(MovieStatus.INACTIVE);
    }

    public Movie require(Long id) {
        return movies.findById(id)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "MOVIE_NOT_FOUND",
                        "Không tìm thấy phim."));
    }

    private void apply(Movie movie, MovieRequest request) {
        validateDates(request);
        movie.setTitle(request.title());
        movie.setDescription(request.description());
        movie.setGenres(request.genres());
        movie.setDurationMinutes(request.durationMinutes());
        movie.setReleaseDate(request.releaseDate());
        movie.setEndDate(request.endDate());
        movie.setDirector(request.director());
        movie.setCastNames(request.castNames());
        movie.setAgeRating(request.ageRating());
        movie.setPosterUrl(request.posterUrl());
        movie.setBackdropUrl(request.backdropUrl());
        movie.setTrailerUrl(request.trailerUrl());
        movie.setStatus(request.status() == MovieStatus.INACTIVE
                ? MovieStatus.INACTIVE
                : resolveLifecycle(movie, LocalDate.now()));
    }

    @Transactional
    public void syncLifecycle() {
        LocalDate today = LocalDate.now();
        for (Movie movie : movies.findAll()) {
            if (movie.getStatus() != MovieStatus.INACTIVE) {
                movie.setStatus(resolveLifecycle(movie, today));
            }
        }
    }

    public boolean isShowingOn(Movie movie, LocalDate date) {
        if (movie.getStatus() == MovieStatus.INACTIVE) {
            return false;
        }
        if (movie.getReleaseDate() != null && date.isBefore(movie.getReleaseDate())) {
            return false;
        }
        return movie.getEndDate() == null || !date.isAfter(movie.getEndDate());
    }

    private MovieStatus resolveLifecycle(Movie movie, LocalDate today) {
        if (movie.getReleaseDate() != null && today.isBefore(movie.getReleaseDate())) {
            return MovieStatus.COMING_SOON;
        }
        if (movie.getEndDate() != null && today.isAfter(movie.getEndDate())) {
            return MovieStatus.ENDED;
        }
        return MovieStatus.NOW_SHOWING;
    }

    private void validateDates(MovieRequest request) {
        if (request.releaseDate() != null && request.endDate() != null
                && request.endDate().isBefore(request.releaseDate())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "MOVIE_INVALID_DATE_RANGE",
                    "Ngày kết thúc chiếu không được trước ngày bắt đầu chiếu.");
        }
    }

    private MovieResponse toResponse(Movie movie) {
        return new MovieResponse(
                movie.getId(),
                movie.getTitle(),
                movie.getDescription(),
                movie.getGenres(),
                movie.getDurationMinutes(),
                movie.getReleaseDate(),
                movie.getEndDate(),
                movie.getDirector(),
                movie.getCastNames(),
                movie.getAgeRating(),
                movie.getPosterUrl(),
                movie.getBackdropUrl(),
                movie.getTrailerUrl(),
                movie.getStatus().name(),
                movie.getTicketsSold());
    }
}
