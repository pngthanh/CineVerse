package com.pngthanh.cineverse.booking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pngthanh.cineverse.booking.dto.SeatHoldRequest;
import com.pngthanh.cineverse.booking.dto.SeatHoldResponse;
import com.pngthanh.cineverse.cinema.entity.Seat;
import com.pngthanh.cineverse.common.enums.SeatType;
import com.pngthanh.cineverse.common.enums.ShowtimeSeatStatus;
import com.pngthanh.cineverse.common.exception.ApiException;
import com.pngthanh.cineverse.showtime.entity.Showtime;
import com.pngthanh.cineverse.showtime.entity.ShowtimeSeat;
import com.pngthanh.cineverse.showtime.repository.ShowtimeSeatRepository;
import com.pngthanh.cineverse.showtime.service.ShowtimeService;
import com.pngthanh.cineverse.user.entity.User;
import com.pngthanh.cineverse.user.service.UserService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SeatHoldServiceTest {
    private ShowtimeSeatRepository seats;
    private ShowtimeService showtimes;
    private UserService users;
    private PricingService pricing;
    private SeatHoldService service;

    @BeforeEach
    void setUp() {
        seats = mock(ShowtimeSeatRepository.class);
        showtimes = mock(ShowtimeService.class);
        users = mock(UserService.class);
        pricing = mock(PricingService.class);
        service = new SeatHoldService(seats, showtimes, users, pricing, 5);
    }

    @Test
    void availableSeatCanBeHeld() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(10L);
        when(users.requireByEmail("customer@cineverse.vn")).thenReturn(user);

        Showtime showtime = mock(Showtime.class);
        when(showtime.getId()).thenReturn(20L);
        when(showtimes.require(20L)).thenReturn(showtime);

        Seat seat = mock(Seat.class);
        when(seat.getId()).thenReturn(30L);
        when(seat.getSeatCode()).thenReturn("D5");
        when(seat.getType()).thenReturn(SeatType.VIP);

        ShowtimeSeat showtimeSeat = new ShowtimeSeat();
        showtimeSeat.setShowtime(showtime);
        showtimeSeat.setSeat(seat);
        showtimeSeat.setStatus(ShowtimeSeatStatus.AVAILABLE);

        when(seats.findForUpdate(20L, List.of(30L))).thenReturn(List.of(showtimeSeat));
        when(pricing.seatPrice(any(Showtime.class), any(ShowtimeSeat.class)))
                .thenReturn(new BigDecimal("90000"));

        SeatHoldResponse response = service.hold(
                "customer@cineverse.vn",
                new SeatHoldRequest(20L, List.of(30L)));

        assertEquals(ShowtimeSeatStatus.HELD, showtimeSeat.getStatus());
        assertEquals(10L, showtimeSeat.getHeldByUserId());
        assertNotNull(showtimeSeat.getHoldToken());
        assertNotNull(showtimeSeat.getHoldExpiresAt());
        assertEquals(new BigDecimal("90000"), response.total());
    }

    @Test
    void bookedSeatCannotBeHeldAgain() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(10L);
        when(users.requireByEmail("customer@cineverse.vn")).thenReturn(user);

        Showtime showtime = mock(Showtime.class);
        when(showtime.getId()).thenReturn(20L);
        when(showtimes.require(20L)).thenReturn(showtime);

        Seat seat = mock(Seat.class);
        when(seat.getId()).thenReturn(30L);

        ShowtimeSeat showtimeSeat = new ShowtimeSeat();
        showtimeSeat.setShowtime(showtime);
        showtimeSeat.setSeat(seat);
        showtimeSeat.setStatus(ShowtimeSeatStatus.BOOKED);
        when(seats.findForUpdate(20L, List.of(30L))).thenReturn(List.of(showtimeSeat));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.hold(
                        "customer@cineverse.vn",
                        new SeatHoldRequest(20L, List.of(30L))));

        assertEquals("SEAT_UNAVAILABLE", exception.getCode());
    }
}
