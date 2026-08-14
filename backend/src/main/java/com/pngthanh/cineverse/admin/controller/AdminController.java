package com.pngthanh.cineverse.admin.controller;

import com.pngthanh.cineverse.admin.dto.DashboardResponse;
import com.pngthanh.cineverse.admin.service.AdminService;
import com.pngthanh.cineverse.cinema.dto.CinemaRequest;
import com.pngthanh.cineverse.cinema.dto.CinemaResponse;
import com.pngthanh.cineverse.cinema.dto.RoomRequest;
import com.pngthanh.cineverse.cinema.service.CinemaService;
import com.pngthanh.cineverse.movie.dto.MovieRequest;
import com.pngthanh.cineverse.movie.dto.MovieResponse;
import com.pngthanh.cineverse.movie.service.MovieService;
import com.pngthanh.cineverse.showtime.dto.ShowtimeRequest;
import com.pngthanh.cineverse.showtime.dto.ShowtimeResponse;
import com.pngthanh.cineverse.showtime.service.ShowtimeService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService admin;
    private final MovieService movies;
    private final CinemaService cinemas;
    private final ShowtimeService showtimes;

    public AdminController(
            AdminService admin,
            MovieService movies,
            CinemaService cinemas,
            ShowtimeService showtimes) {
        this.admin = admin;
        this.movies = movies;
        this.cinemas = cinemas;
        this.showtimes = showtimes;
    }

    @GetMapping("/dashboard")
    public DashboardResponse dashboard() {
        return admin.dashboard();
    }

    @GetMapping("/movies")
    public List<MovieResponse> movies() {
        return movies.list(null);
    }

    @PostMapping("/movies")
    @ResponseStatus(HttpStatus.CREATED)
    public MovieResponse createMovie(@Valid @RequestBody MovieRequest request) {
        return movies.create(request);
    }

    @PutMapping("/movies/{id}")
    public MovieResponse updateMovie(
            @PathVariable Long id,
            @Valid @RequestBody MovieRequest request) {
        return movies.update(id, request);
    }

    @DeleteMapping("/movies/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateMovie(@PathVariable Long id) {
        movies.deactivate(id);
    }


    @GetMapping("/cinemas")
    public List<CinemaResponse> cinemas() {
        return cinemas.listAdmin();
    }

    @PostMapping("/cinemas")
    @ResponseStatus(HttpStatus.CREATED)
    public CinemaResponse createCinema(@Valid @RequestBody CinemaRequest request) {
        return cinemas.create(request);
    }

    @PutMapping("/cinemas/{id}")
    public CinemaResponse updateCinema(
            @PathVariable Long id,
            @Valid @RequestBody CinemaRequest request) {
        return cinemas.update(id, request);
    }

    @DeleteMapping("/cinemas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateCinema(@PathVariable Long id) {
        cinemas.deactivateCinema(id);
    }

    @PostMapping("/cinemas/{id}/rooms")
    @ResponseStatus(HttpStatus.CREATED)
    public CinemaResponse.RoomResponse createRoom(
            @PathVariable Long id,
            @Valid @RequestBody RoomRequest request) {
        return cinemas.createRoom(id, request);
    }

    @PutMapping("/rooms/{id}")
    public CinemaResponse.RoomResponse updateRoom(
            @PathVariable Long id,
            @Valid @RequestBody RoomRequest request) {
        return cinemas.updateRoom(id, request);
    }

    @DeleteMapping("/rooms/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateRoom(@PathVariable Long id) {
        cinemas.deactivateRoom(id);
    }

    @PostMapping("/showtimes")
    @ResponseStatus(HttpStatus.CREATED)
    public ShowtimeResponse createShowtime(@Valid @RequestBody ShowtimeRequest request) {
        return showtimes.create(request);
    }

    @DeleteMapping("/showtimes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelShowtime(@PathVariable Long id) {
        showtimes.cancel(id);
    }
}
