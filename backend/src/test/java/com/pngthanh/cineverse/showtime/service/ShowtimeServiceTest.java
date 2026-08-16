package com.pngthanh.cineverse.showtime.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pngthanh.cineverse.booking.service.PricingService;
import com.pngthanh.cineverse.cinema.entity.Cinema;
import com.pngthanh.cineverse.cinema.entity.Room;
import com.pngthanh.cineverse.cinema.repository.SeatRepository;
import com.pngthanh.cineverse.cinema.service.CinemaService;
import com.pngthanh.cineverse.common.enums.ShowtimeLifecycleStatus;
import com.pngthanh.cineverse.common.exception.ApiException;
import com.pngthanh.cineverse.movie.entity.Movie;
import com.pngthanh.cineverse.movie.service.MovieService;
import com.pngthanh.cineverse.showtime.dto.ShowtimeRequest;
import com.pngthanh.cineverse.showtime.entity.Showtime;
import com.pngthanh.cineverse.showtime.repository.ShowtimeRepository;
import com.pngthanh.cineverse.showtime.repository.ShowtimeSeatRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ShowtimeServiceTest {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Instant NOW = Instant.parse("2026-08-15T12:00:00Z");

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
        Clock clock = Clock.fixed(NOW, ZONE);
        service = new ShowtimeService(
                showtimes,
                showtimeSeats,
                seats,
                movies,
                cinemas,
                pricing,
                clock,
                20);
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

        LocalDateTime start = LocalDateTime.ofInstant(NOW, ZONE).plusDays(2);
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

    @Test
    void showtimeWithinGracePeriodIsStillBookable() {
        LocalDateTime now = LocalDateTime.ofInstant(NOW, ZONE);
        Showtime showtime = showtime(now.minusMinutes(10), now.plusMinutes(100), true);

        assertTrue(service.isBookable(showtime));
        assertEquals(ShowtimeLifecycleStatus.NOW_PLAYING, service.lifecycleStatus(showtime));
    }

    @Test
    void showtimeAfterGracePeriodIsNotBookable() {
        LocalDateTime now = LocalDateTime.ofInstant(NOW, ZONE);
        Showtime showtime = showtime(now.minusMinutes(21), now.plusMinutes(99), true);

        assertFalse(service.isBookable(showtime));
        assertEquals(ShowtimeLifecycleStatus.NOW_PLAYING, service.lifecycleStatus(showtime));
    }

    @Test
    void cancelledShowtimeIsNeverBookable() {
        LocalDateTime now = LocalDateTime.ofInstant(NOW, ZONE);
        Showtime showtime = showtime(now.plusHours(2), now.plusHours(4), false);

        assertFalse(service.isBookable(showtime));
        assertEquals(ShowtimeLifecycleStatus.CANCELLED, service.lifecycleStatus(showtime));
    }

    private Showtime showtime(LocalDateTime start, LocalDateTime end, boolean active) {
        Showtime showtime = new Showtime();
        showtime.setStartTime(start);
        showtime.setEndTime(end);
        showtime.setActive(active);
        return showtime;
    }
}
