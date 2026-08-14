package com.pngthanh.cineverse.showtime.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pngthanh.cineverse.booking.service.PricingService;
import com.pngthanh.cineverse.cinema.entity.Cinema;
import com.pngthanh.cineverse.cinema.entity.Room;
import com.pngthanh.cineverse.cinema.repository.SeatRepository;
import com.pngthanh.cineverse.cinema.service.CinemaService;
import com.pngthanh.cineverse.common.exception.ApiException;
import com.pngthanh.cineverse.movie.entity.Movie;
import com.pngthanh.cineverse.movie.service.MovieService;
import com.pngthanh.cineverse.showtime.dto.ShowtimeRequest;
import com.pngthanh.cineverse.showtime.repository.ShowtimeRepository;
import com.pngthanh.cineverse.showtime.repository.ShowtimeSeatRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ShowtimeServiceTest {
    private ShowtimeRepository showtimes;
    private MovieService movies;
    private CinemaService cinemas;
    private ShowtimeService service;

    @BeforeEach
    void setUp() {
        showtimes = mock(ShowtimeRepository.class);
        ShowtimeSeatRepository showtimeSeats = mock(ShowtimeSeatRepository.class);
        SeatRepository seats = mock(SeatRepository.class);
        movies = mock(MovieService.class);
        cinemas = mock(CinemaService.class);
        PricingService pricing = mock(PricingService.class);
        service = new ShowtimeService(showtimes, showtimeSeats, seats, movies, cinemas, pricing);
    }

    @Test
    void overlappingShowtimeIsRejected() {
        Movie movie = mock(Movie.class);
        when(movie.getDurationMinutes()).thenReturn(120);
        when(movies.require(1L)).thenReturn(movie);

        Cinema cinema = mock(Cinema.class);
        when(cinema.isActive()).thenReturn(true);
        Room room = mock(Room.class);
        when(room.getId()).thenReturn(2L);
        when(room.isActive()).thenReturn(true);
        when(room.getCinema()).thenReturn(cinema);
        when(cinemas.requireRoom(2L)).thenReturn(room);

        LocalDateTime start = LocalDateTime.of(2026, 8, 12, 19, 0);
        LocalDateTime end = start.plusMinutes(120);
        when(showtimes.hasConflict(2L, start, end, null)).thenReturn(true);

        ShowtimeRequest request = new ShowtimeRequest(
                1L,
                2L,
                start,
                new BigDecimal("70000"));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.create(request));

        assertEquals("SHOWTIME_CONFLICT", exception.getCode());
    }
}
