package com.pngthanh.cineverse.showtime.controller;

import com.pngthanh.cineverse.showtime.dto.SeatMapResponse;
import com.pngthanh.cineverse.showtime.dto.ShowtimeResponse;
import com.pngthanh.cineverse.showtime.service.ShowtimeService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/showtimes")
public class ShowtimeController {
    private final ShowtimeService service;

    public ShowtimeController(ShowtimeService service) {
        this.service = service;
    }

    @GetMapping
    public List<ShowtimeResponse> list(
            @RequestParam(required = false) Long movieId,
            @RequestParam(required = false) Long cinemaId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.list(movieId, cinemaId, date);
    }

    @GetMapping("/{id}/seats")
    public SeatMapResponse seats(@PathVariable Long id) {
        return service.seatMap(id);
    }
}
