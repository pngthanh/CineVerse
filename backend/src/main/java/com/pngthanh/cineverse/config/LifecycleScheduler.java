package com.pngthanh.cineverse.config;

import com.pngthanh.cineverse.cinema.service.CinemaService;
import com.pngthanh.cineverse.movie.service.MovieService;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LifecycleScheduler {
    private final CinemaService cinemas;
    private final MovieService movies;
    private final Clock clock;

    public LifecycleScheduler(CinemaService cinemas, MovieService movies, Clock clock) {
        this.cinemas = cinemas;
        this.movies = movies;
        this.clock = clock;
    }

    @Scheduled(fixedDelay = 60_000)
    public void synchronizeLifecycle() {
        cinemas.processDueClosures(LocalDateTime.now(clock));
        movies.syncLifecycle();
    }
}
