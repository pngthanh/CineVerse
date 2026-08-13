package com.pngthanh.cineverse.payment.service;

import com.pngthanh.cineverse.booking.entity.Booking;
import com.pngthanh.cineverse.booking.entity.BookingSeat;
import com.pngthanh.cineverse.booking.repository.BookingSeatRepository;
import com.pngthanh.cineverse.booking.service.BookingService;
import com.pngthanh.cineverse.common.enums.BookingStatus;
import com.pngthanh.cineverse.common.enums.PaymentStatus;
import com.pngthanh.cineverse.common.enums.ShowtimeSeatStatus;
import com.pngthanh.cineverse.common.exception.ApiException;
import com.pngthanh.cineverse.payment.dto.MockPaymentRequest;
import com.pngthanh.cineverse.payment.dto.PaymentResponse;
import com.pngthanh.cineverse.payment.entity.Payment;
import com.pngthanh.cineverse.payment.repository.PaymentRepository;
import com.pngthanh.cineverse.showtime.entity.ShowtimeSeat;
import com.pngthanh.cineverse.ticket.entity.Ticket;
import com.pngthanh.cineverse.ticket.repository.TicketRepository;
import com.pngthanh.cineverse.user.entity.User;
import com.pngthanh.cineverse.user.service.UserService;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {
    private final PaymentRepository payments;
    private final TicketRepository tickets;
    private final BookingService bookings;
    private final BookingSeatRepository bookingSeats;
    private final UserService users;

    public PaymentService(
            PaymentRepository payments,
            TicketRepository tickets,
            BookingService bookings,
            BookingSeatRepository bookingSeats,
            UserService users) {
        this.payments = payments;
        this.tickets = tickets;
        this.bookings = bookings;
        this.bookingSeats = bookingSeats;
        this.users = users;
    }

    @Transactional
    public PaymentResponse mock(String email, MockPaymentRequest request) {
        User user = users.requireByEmail(email);
        Booking booking = bookings.requireForUpdate(request.bookingId());

        if (!Objects.equals(booking.getUser().getId(), user.getId())) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "BOOKING_FORBIDDEN",
                    "Bạn không có quyền thao tác booking này.");
        }

        Payment payment = payments.findByBookingId(booking.getId())
                .orElseGet(() -> createPendingPayment(booking));

        // Request lặp lại sau khi thanh toán thành công phải trả lại cùng kết quả.
        if (booking.getStatus() == BookingStatus.CONFIRMED
                && payment.getStatus() == PaymentStatus.SUCCESS) {
            String ticketCode = tickets.findByBookingId(booking.getId())
                    .map(Ticket::getTicketCode)
                    .orElse(null);
            return toResponse(booking, payment, ticketCode);
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "BOOKING_NOT_PENDING",
                    "Booking không còn ở trạng thái chờ thanh toán.");
        }

        if (booking.getExpiresAt().isBefore(Instant.now())) {
            booking.setStatus(BookingStatus.CANCELLED);
            bookings.releaseHeldSeats(booking);
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "BOOKING_EXPIRED",
                    "Thời gian giữ ghế đã hết.");
        }

        if (request.result() == MockPaymentRequest.Result.SUCCESS) {
            return confirmPayment(booking, payment);
        }
        return failPayment(booking, payment);
    }

    private PaymentResponse confirmPayment(Booking booking, Payment payment) {
        for (BookingSeat bookingSeat : bookingSeats.findAllByBookingId(booking.getId())) {
            ShowtimeSeat showtimeSeat = bookingSeat.getShowtimeSeat();
            boolean validSeat = showtimeSeat.getStatus() == ShowtimeSeatStatus.HELD
                    && Objects.equals(showtimeSeat.getHoldToken(), booking.getHoldToken());
            if (!validSeat) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "SEAT_STATE_CHANGED",
                        "Trạng thái ghế đã thay đổi. Vui lòng đặt lại.");
            }
        }

        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(Instant.now());
        booking.setStatus(BookingStatus.CONFIRMED);
        var movie = booking.getShowtime().getMovie();
        movie.setTicketsSold((movie.getTicketsSold() == null ? 0L : movie.getTicketsSold())
                + bookingSeats.findAllByBookingId(booking.getId()).size());

        for (BookingSeat bookingSeat : bookingSeats.findAllByBookingId(booking.getId())) {
            ShowtimeSeat showtimeSeat = bookingSeat.getShowtimeSeat();
            showtimeSeat.setStatus(ShowtimeSeatStatus.BOOKED);
            showtimeSeat.setHeldByUserId(null);
            showtimeSeat.setHoldToken(null);
            showtimeSeat.setHoldExpiresAt(null);
        }

        Ticket ticket = tickets.findByBookingId(booking.getId())
                .orElseGet(() -> createTicket(booking));
        return toResponse(booking, payment, ticket.getTicketCode());
    }

    private PaymentResponse failPayment(Booking booking, Payment payment) {
        payment.setStatus(PaymentStatus.FAILED);
        booking.setStatus(BookingStatus.CANCELLED);
        bookings.releaseHeldSeats(booking);
        return toResponse(booking, payment, null);
    }

    private Payment createPendingPayment(Booking booking) {
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(booking.getTotalAmount());
        return payments.save(payment);
    }

    private Ticket createTicket(Booking booking) {
        Ticket ticket = new Ticket();
        ticket.setBooking(booking);
        ticket.setTicketCode("CV-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase());
        return tickets.save(ticket);
    }

    private PaymentResponse toResponse(Booking booking, Payment payment, String ticketCode) {
        return new PaymentResponse(
                payment.getId(),
                booking.getId(),
                booking.getStatus().name(),
                payment.getStatus().name(),
                payment.getAmount(),
                payment.getPaidAt(),
                ticketCode);
    }
}
