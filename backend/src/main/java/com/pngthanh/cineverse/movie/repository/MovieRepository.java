package com.pngthanh.cineverse.movie.repository;

import com.pngthanh.cineverse.common.enums.MovieStatus;
import com.pngthanh.cineverse.movie.entity.Movie;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieRepository extends JpaRepository<Movie, Long> {
    List<Movie> findAllByStatusOrderByReleaseDateDesc(MovieStatus status);
    Optional<Movie> findByTitle(String title);
}
