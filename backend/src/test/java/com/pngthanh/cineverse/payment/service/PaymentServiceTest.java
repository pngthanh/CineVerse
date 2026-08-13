package com.pngthanh.cineverse.payment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pngthanh.cineverse.booking.entity.Booking;
import com.pngthanh.cineverse.booking.entity.BookingSeat;
import com.pngthanh.cineverse.booking.repository.BookingSeatRepository;
import com.pngthanh.cineverse.booking.service.BookingService;
import com.pngthanh.cineverse.common.enums.BookingStatus;
import com.pngthanh.cineverse.common.enums.PaymentStatus;
import com.pngthanh.cineverse.common.enums.ShowtimeSeatStatus;
import com.pngthanh.cineverse.payment.dto.MockPaymentRequest;
import com.pngthanh.cineverse.payment.dto.PaymentResponse;
import com.pngthanh.cineverse.payment.entity.Payment;
import com.pngthanh.cineverse.payment.repository.PaymentRepository;
import com.pngthanh.cineverse.movie.entity.Movie;
import com.pngthanh.cineverse.showtime.entity.Showtime;
import com.pngthanh.cineverse.showtime.entity.ShowtimeSeat;
import com.pngthanh.cineverse.ticket.entity.Ticket;
import com.pngthanh.cineverse.ticket.repository.TicketRepository;
import com.pngthanh.cineverse.user.entity.User;
import com.pngthanh.cineverse.user.service.UserService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaymentServiceTest {
    private PaymentRepository payments;
    private TicketRepository tickets;
    private BookingService bookings;
    private BookingSeatRepository bookingSeats;
    private UserService users;
    private PaymentService service;

    @BeforeEach
    void setUp() {
        payments = mock(PaymentRepository.class);
        tickets = mock(TicketRepository.class);
        bookings = mock(BookingService.class);
        bookingSeats = mock(BookingSeatRepository.class);
        users = mock(UserService.class);
        service = new PaymentService(payments, tickets, bookings, bookingSeats, users);
    }

    @Test
    void successfulPaymentConfirmsBookingBooksSeatsAndCreatesTicket() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(10L);
        when(users.requireByEmail("customer@cineverse.vn")).thenReturn(user);

        Booking booking = mock(Booking.class);
        when(booking.getId()).thenReturn(20L);
        when(booking.getUser()).thenReturn(user);
        when(booking.getStatus()).thenReturn(BookingStatus.PENDING);
        when(booking.getExpiresAt()).thenReturn(Instant.now().plusSeconds(120));
        when(booking.getHoldToken()).thenReturn("hold-token");
        when(booking.getTotalAmount()).thenReturn(new BigDecimal("180000"));
        Movie movie = mock(Movie.class);
        when(movie.getTicketsSold()).thenReturn(120L);
        Showtime showtime = mock(Showtime.class);
        when(showtime.getMovie()).thenReturn(movie);
        when(booking.getShowtime()).thenReturn(showtime);
        when(bookings.requireForUpdate(20L)).thenReturn(booking);

        when(payments.findByBookingId(20L)).thenReturn(Optional.empty());
        when(payments.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShowtimeSeat showtimeSeat = mock(ShowtimeSeat.class);
        when(showtimeSeat.getStatus()).thenReturn(ShowtimeSeatStatus.HELD);
        when(showtimeSeat.getHoldToken()).thenReturn("hold-token");
        BookingSeat bookingSeat = mock(BookingSeat.class);
        when(bookingSeat.getShowtimeSeat()).thenReturn(showtimeSeat);
        when(bookingSeats.findAllByBookingId(20L)).thenReturn(List.of(bookingSeat));

        when(tickets.findByBookingId(20L)).thenReturn(Optional.empty());
        when(tickets.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = service.mock(
                "customer@cineverse.vn",
                new MockPaymentRequest(20L, MockPaymentRequest.Result.SUCCESS));

        assertEquals(PaymentStatus.SUCCESS.name(), response.paymentStatus());
        assertNotNull(response.ticketCode());
        verify(booking).setStatus(BookingStatus.CONFIRMED);
        verify(showtimeSeat).setStatus(ShowtimeSeatStatus.BOOKED);
        verify(showtimeSeat).setHeldByUserId(null);
        verify(showtimeSeat).setHoldToken(null);
        verify(showtimeSeat).setHoldExpiresAt(null);
        verify(movie).setTicketsSold(121L);
        verify(tickets).save(any(Ticket.class));
    }

    @Test
    void failedPaymentCancelsBookingAndReleasesHeldSeats() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(10L);
        when(users.requireByEmail("customer@cineverse.vn")).thenReturn(user);

        Booking booking = mock(Booking.class);
        when(booking.getId()).thenReturn(20L);
        when(booking.getUser()).thenReturn(user);
        when(booking.getStatus()).thenReturn(BookingStatus.PENDING);
        when(booking.getExpiresAt()).thenReturn(Instant.now().plusSeconds(120));
        when(booking.getTotalAmount()).thenReturn(new BigDecimal("180000"));
        when(bookings.requireForUpdate(20L)).thenReturn(booking);

        when(payments.findByBookingId(20L)).thenReturn(Optional.empty());
        when(payments.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = service.mock(
                "customer@cineverse.vn",
                new MockPaymentRequest(20L, MockPaymentRequest.Result.FAILED));

        assertEquals(PaymentStatus.FAILED.name(), response.paymentStatus());
        verify(booking).setStatus(BookingStatus.CANCELLED);
        verify(bookings).releaseHeldSeats(booking);
        verify(tickets, never()).save(any(Ticket.class));
    }
}
