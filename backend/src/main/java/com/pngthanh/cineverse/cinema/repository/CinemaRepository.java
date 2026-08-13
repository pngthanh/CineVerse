package com.pngthanh.cineverse.cinema.repository;
import com.pngthanh.cineverse.cinema.entity.Cinema;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CinemaRepository extends JpaRepository<Cinema, Long> { List<Cinema> findAllByActiveTrueOrderByName(); }
