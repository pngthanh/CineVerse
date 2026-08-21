package com.pngthanh.cineverse.showtime.service;

import com.pngthanh.cineverse.booking.entity.Booking;
import com.pngthanh.cineverse.booking.entity.BookingSeat;
import com.pngthanh.cineverse.booking.repository.BookingRepository;
import com.pngthanh.cineverse.booking.repository.BookingSeatRepository;
import com.pngthanh.cineverse.common.enums.BookingStatus;
import com.pngthanh.cineverse.common.enums.PaymentStatus;
import com.pngthanh.cineverse.common.enums.ShowtimeSeatStatus;
import com.pngthanh.cineverse.common.enums.TicketStatus;
import com.pngthanh.cineverse.payment.entity.Payment;
import com.pngthanh.cineverse.payment.repository.PaymentRepository;
import com.pngthanh.cineverse.showtime.entity.Showtime;
import com.pngthanh.cineverse.ticket.repository.TicketRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShowtimeCancellationService {
    private final BookingRepository bookings;
    private final BookingSeatRepository bookingSeats;
    private final PaymentRepository payments;
    private final TicketRepository tickets;

    public ShowtimeCancellationService(
            BookingRepository bookings,
            BookingSeatRepository bookingSeats,
            PaymentRepository payments,
            TicketRepository tickets) {
        this.bookings = bookings;
        this.bookingSeats = bookingSeats;
        this.payments = payments;
        this.tickets = tickets;
    }

    @Transactional
    public void cancel(Showtime showtime, String reason, LocalDateTime cancelledAt) {
        if (!showtime.isActive()) {
            return;
        }
        showtime.setActive(false);
        showtime.setCancelledAt(cancelledAt);
        showtime.setCancellationReason(reason);

        for (Booking booking : bookings.findAllByShowtimeId(showtime.getId())) {
            cancelBooking(booking, reason);
        }
    }

    private void cancelBooking(Booking booking, String reason) {
        if (booking.getStatus() == BookingStatus.CANCELLED
                || booking.getStatus() == BookingStatus.REFUND_PENDING) {
            return;
        }

        booking.setCancellationReason(reason);
        releaseSeats(booking);

        Payment payment = payments.findByBookingId(booking.getId()).orElse(null);
        boolean paid = payment != null && payment.getStatus() == PaymentStatus.SUCCESS;
        if (paid) {
            Instant now = Instant.now();
            booking.setStatus(BookingStatus.REFUND_PENDING);
            booking.setRefundRequestedAt(now);
            payment.setStatus(PaymentStatus.REFUND_PENDING);
            payment.setRefundRequestedAt(now);
            payment.setRefundReason(reason);
        } else {
            booking.setStatus(BookingStatus.CANCELLED);
        }

        tickets.findByBookingId(booking.getId()).ifPresent(ticket -> {
            if (ticket.getStatus() != TicketStatus.USED) {
                ticket.setStatus(TicketStatus.CANCELLED);
            }
        });
    }

    private void releaseSeats(Booking booking) {
        for (BookingSeat bookingSeat : bookingSeats.findAllByBookingId(booking.getId())) {
            var showtimeSeat = bookingSeat.getShowtimeSeat();
            showtimeSeat.setStatus(ShowtimeSeatStatus.AVAILABLE);
            showtimeSeat.setHeldByUserId(null);
            showtimeSeat.setHoldToken(null);
            showtimeSeat.setHoldExpiresAt(null);
        }
    }
}
