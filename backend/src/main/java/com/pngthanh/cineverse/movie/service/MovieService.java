package com.pngthanh.cineverse.movie.service;

import com.pngthanh.cineverse.common.enums.MovieStatus;
import com.pngthanh.cineverse.common.exception.ApiException;
import com.pngthanh.cineverse.movie.dto.MovieRequest;
import com.pngthanh.cineverse.movie.dto.MovieResponse;
import com.pngthanh.cineverse.movie.entity.Movie;
import com.pngthanh.cineverse.movie.repository.MovieRepository;
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

    @Transactional(readOnly = true)
    public List<MovieResponse> list(MovieStatus status) {
        List<Movie> data = status == null
                ? movies.findAll()
                : movies.findAllByStatusOrderByReleaseDateDesc(status);
        return data.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public MovieResponse get(Long id) {
        return toResponse(require(id));
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
        movie.setStatus(request.status());
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
                movie.getRatingAverage(),
                movie.getReviewCount(),
                movie.getTicketsSold());
    }
}
