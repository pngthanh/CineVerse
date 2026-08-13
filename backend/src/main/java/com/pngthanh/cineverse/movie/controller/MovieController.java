package com.pngthanh.cineverse.movie.controller;

import com.pngthanh.cineverse.common.enums.MovieStatus;
import com.pngthanh.cineverse.movie.dto.MovieResponse;
import com.pngthanh.cineverse.movie.service.MovieService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/movies")
public class MovieController {
    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @GetMapping
    public List<MovieResponse> list(@RequestParam(required = false) MovieStatus status) {
        return movieService.list(status);
    }

    @GetMapping("/{id}")
    public MovieResponse get(@PathVariable Long id) {
        return movieService.get(id);
    }
}
