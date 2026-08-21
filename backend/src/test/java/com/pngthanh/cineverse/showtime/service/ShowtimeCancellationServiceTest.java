package com.pngthanh.cineverse.showtime.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pngthanh.cineverse.booking.entity.Booking;
import com.pngthanh.cineverse.booking.repository.BookingRepository;
import com.pngthanh.cineverse.booking.repository.BookingSeatRepository;
import com.pngthanh.cineverse.common.enums.BookingStatus;
import com.pngthanh.cineverse.common.enums.PaymentStatus;
import com.pngthanh.cineverse.payment.entity.Payment;
import com.pngthanh.cineverse.payment.repository.PaymentRepository;
import com.pngthanh.cineverse.showtime.entity.Showtime;
import com.pngthanh.cineverse.ticket.repository.TicketRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ShowtimeCancellationServiceTest {
    @Test
    void paidBookingMovesToRefundPendingWhenShowtimeIsCancelled() {
        BookingRepository bookings = mock(BookingRepository.class);
        BookingSeatRepository bookingSeats = mock(BookingSeatRepository.class);
        PaymentRepository payments = mock(PaymentRepository.class);
        TicketRepository tickets = mock(TicketRepository.class);
        ShowtimeCancellationService service = new ShowtimeCancellationService(
                bookings, bookingSeats, payments, tickets);

        Showtime showtime = new Showtime();
        showtime.setActive(true);
        Booking booking = new Booking();
        booking.setStatus(BookingStatus.CONFIRMED);
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(new BigDecimal("100000"));
        payment.setStatus(PaymentStatus.SUCCESS);

        when(bookings.findAllByShowtimeId(showtime.getId())).thenReturn(List.of(booking));
        when(bookingSeats.findAllByBookingId(booking.getId())).thenReturn(List.of());
        when(payments.findByBookingId(booking.getId())).thenReturn(Optional.of(payment));
        when(tickets.findByBookingId(booking.getId())).thenReturn(Optional.empty());

        service.cancel(showtime, "Rạp đóng", LocalDateTime.now());

        assertEquals(BookingStatus.REFUND_PENDING, booking.getStatus());
        assertEquals(PaymentStatus.REFUND_PENDING, payment.getStatus());
    }
}
