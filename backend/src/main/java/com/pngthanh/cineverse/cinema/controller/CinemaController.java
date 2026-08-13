package com.pngthanh.cineverse.cinema.controller;

import com.pngthanh.cineverse.cinema.dto.CinemaResponse;
import com.pngthanh.cineverse.cinema.service.CinemaService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cinemas")
public class CinemaController {
    private final CinemaService cinemaService;

    public CinemaController(CinemaService cinemaService) {
        this.cinemaService = cinemaService;
    }

    @GetMapping
    public List<CinemaResponse> list() {
        return cinemaService.list();
    }

    @GetMapping("/{id}")
    public CinemaResponse get(@PathVariable Long id) {
        return cinemaService.get(id);
    }
}
