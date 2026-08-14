package com.pngthanh.cineverse.cinema.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pngthanh.cineverse.cinema.dto.RoomRequest;
import com.pngthanh.cineverse.cinema.entity.Cinema;
import com.pngthanh.cineverse.cinema.entity.Room;
import com.pngthanh.cineverse.cinema.entity.Seat;
import com.pngthanh.cineverse.cinema.repository.CinemaRepository;
import com.pngthanh.cineverse.cinema.repository.RoomRepository;
import com.pngthanh.cineverse.cinema.repository.SeatRepository;
import com.pngthanh.cineverse.common.enums.SeatType;
import com.pngthanh.cineverse.common.exception.ApiException;
import com.pngthanh.cineverse.showtime.repository.ShowtimeRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CinemaServiceTest {
    private CinemaRepository cinemas;
    private RoomRepository rooms;
    private SeatRepository seats;
    private CinemaService service;

    @BeforeEach
    void setUp() {
        cinemas = mock(CinemaRepository.class);
        rooms = mock(RoomRepository.class);
        seats = mock(SeatRepository.class);
        ShowtimeRepository showtimes = mock(ShowtimeRepository.class);
        service = new CinemaService(cinemas, rooms, seats, showtimes);
    }

    @Test
    void roomMustBeAtLeastSixBySix() {
        Cinema cinema = activeCinema();
        when(cinemas.findById(1L)).thenReturn(Optional.of(cinema));

        RoomRequest request = new RoomRequest(
                "Phòng nhỏ",
                5,
                6,
                new BigDecimal("70000"),
                new BigDecimal("100000"),
                new BigDecimal("20000"),
                true);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.createRoom(1L, request));

        assertEquals("ROOM_TOO_SMALL", exception.getCode());
    }

    @Test
    void sixBySixRoomCreatesFourVipSeatsInTheCenter() {
        Cinema cinema = activeCinema();
        when(cinemas.findById(1L)).thenReturn(Optional.of(cinema));
        when(rooms.existsByCinemaIdAndNameIgnoreCase(1L, "Phòng 06")).thenReturn(false);
        when(rooms.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(seats.findAllByRoomIdOrderByRowIndexAscColumnIndexAsc(null))
                .thenAnswer(invocation -> List.of());

        List<Seat> savedSeats = new ArrayList<>();
        when(seats.save(any(Seat.class))).thenAnswer(invocation -> {
            Seat seat = invocation.getArgument(0);
            savedSeats.add(seat);
            return seat;
        });

        RoomRequest request = new RoomRequest(
                "Phòng 06",
                6,
                6,
                new BigDecimal("70000"),
                new BigDecimal("100000"),
                new BigDecimal("25000"),
                true);
        service.createRoom(1L, request);

        assertEquals(36, savedSeats.size());
        assertEquals(4, savedSeats.stream().filter(seat -> seat.getType() == SeatType.VIP).count());
    }

    private Cinema activeCinema() {
        Cinema cinema = new Cinema();
        cinema.setName("CineVerse Test");
        cinema.setAddress("Cần Thơ");
        cinema.setActive(true);
        return cinema;
    }
}
