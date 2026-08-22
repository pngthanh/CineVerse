package com.pngthanh.cineverse.payment.service;

import com.pngthanh.cineverse.booking.entity.Booking;
import com.pngthanh.cineverse.booking.entity.BookingSeat;
import com.pngthanh.cineverse.booking.repository.BookingSeatRepository;
import com.pngthanh.cineverse.booking.service.BookingService;
import com.pngthanh.cineverse.common.enums.BookingStatus;
import com.pngthanh.cineverse.common.enums.PaymentStatus;
import com.pngthanh.cineverse.common.enums.ShowtimeSeatStatus;
import com.pngthanh.cineverse.common.exception.ApiException;
import com.pngthanh.cineverse.payment.dto.PaymentResponse;
import com.pngthanh.cineverse.payment.entity.Payment;
import com.pngthanh.cineverse.payment.repository.PaymentRepository;
import com.pngthanh.cineverse.showtime.entity.ShowtimeSeat;
import com.pngthanh.cineverse.ticket.entity.Ticket;
import com.pngthanh.cineverse.ticket.repository.TicketRepository;
import com.pngthanh.cineverse.user.entity.User;
import com.pngthanh.cineverse.user.service.UserService;
import java.time.Instant;
import java.util.List;
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
    public Payment prepareVnPay(String email, Long bookingId, String transactionReference) {
        User user = users.requireByEmail(email);
        Booking booking = bookings.requireForUpdate(bookingId);
        requireBookingOwner(user, booking);

        Payment payment = payments.findByBookingId(booking.getId())
                .orElseGet(() -> createPendingPayment(booking));

        if (booking.getStatus() == BookingStatus.CONFIRMED
                && payment.getStatus() == PaymentStatus.SUCCESS) {
            return payment;
        }
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "BOOKING_NOT_PENDING",
                    "Booking không còn ở trạng thái chờ thanh toán.");
        }
        if (!booking.getExpiresAt().isAfter(Instant.now())) {
            booking.setStatus(BookingStatus.CANCELLED);
            bookings.releaseHeldSeats(booking);
            payment.setStatus(PaymentStatus.FAILED);
            payment.setResponseCode("EXPIRED");
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "BOOKING_EXPIRED",
                    "Thời gian giữ ghế đã hết.");
        }

        payment.setAmount(booking.getTotalAmount());
        payment.setProvider("VNPAY");
        payment.setMethod("VNPAY");
        payment.setStatus(PaymentStatus.PENDING);
        payment.setTransactionReference(transactionReference);
        payment.setTransactionCreatedAt(Instant.now());
        payment.setGatewayTransactionNo(null);
        payment.setBankTransactionNo(null);
        payment.setBankCode(null);
        payment.setCardType(null);
        payment.setResponseCode(null);
        payment.setTransactionStatus(null);
        payment.setCallbackSource(null);
        return payment;
    }

    @Transactional
    public PaymentResponse confirmVnPay(
            Payment payment,
            String gatewayTransactionNo,
            String bankTransactionNo,
            String bankCode,
            String cardType,
            String responseCode,
            String transactionStatus,
            String callbackSource) {
        Booking booking = bookings.requireForUpdate(payment.getBooking().getId());

        if (booking.getStatus() == BookingStatus.CONFIRMED
                && payment.getStatus() == PaymentStatus.SUCCESS) {
            return toResponse(booking, payment, ticketCode(booking));
        }
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "BOOKING_NOT_PENDING",
                    "Booking không còn ở trạng thái chờ thanh toán.");
        }

        List<BookingSeat> seats = bookingSeats.findAllByBookingId(booking.getId());
        for (BookingSeat bookingSeat : seats) {
            ShowtimeSeat showtimeSeat = bookingSeat.getShowtimeSeat();
            boolean validSeat = showtimeSeat.getStatus() == ShowtimeSeatStatus.HELD
                    && Objects.equals(showtimeSeat.getHoldToken(), booking.getHoldToken());
            if (!validSeat) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "SEAT_STATE_CHANGED",
                        "Trạng thái ghế đã thay đổi. Vui lòng liên hệ quản trị viên.");
            }
        }

        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setGatewayTransactionNo(gatewayTransactionNo);
        payment.setBankTransactionNo(bankTransactionNo);
        payment.setBankCode(bankCode);
        payment.setCardType(cardType);
        payment.setResponseCode(responseCode);
        payment.setTransactionStatus(transactionStatus);
        payment.setCallbackSource(callbackSource);
        payment.setPaidAt(Instant.now());
        booking.setStatus(BookingStatus.CONFIRMED);

        var movie = booking.getShowtime().getMovie();
        movie.setTicketsSold((movie.getTicketsSold() == null ? 0L : movie.getTicketsSold()) + seats.size());

        for (BookingSeat bookingSeat : seats) {
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

    @Transactional
    public PaymentResponse markVnPayFailed(
            Payment payment,
            String gatewayTransactionNo,
            String bankTransactionNo,
            String bankCode,
            String cardType,
            String responseCode,
            String transactionStatus,
            String callbackSource) {
        Booking booking = bookings.requireForUpdate(payment.getBooking().getId());
        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setGatewayTransactionNo(gatewayTransactionNo);
            payment.setBankTransactionNo(bankTransactionNo);
            payment.setBankCode(bankCode);
            payment.setCardType(cardType);
            payment.setResponseCode(responseCode);
            payment.setTransactionStatus(transactionStatus);
            payment.setCallbackSource(callbackSource);
            if (booking.getStatus() == BookingStatus.PENDING) {
                booking.setStatus(BookingStatus.CANCELLED);
                bookings.releaseHeldSeats(booking);
            }
        }
        return toResponse(booking, payment, ticketCode(booking));
    }

    private void requireBookingOwner(User user, Booking booking) {
        if (!Objects.equals(booking.getUser().getId(), user.getId())) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "BOOKING_FORBIDDEN",
                    "Bạn không có quyền thao tác booking này.");
        }
    }

    private Payment createPendingPayment(Booking booking) {
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(booking.getTotalAmount());
        payment.setProvider("VNPAY");
        payment.setMethod("VNPAY");
        return payments.save(payment);
    }

    private Ticket createTicket(Booking booking) {
        Ticket ticket = new Ticket();
        ticket.setBooking(booking);
        ticket.setTicketCode("CV-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase());
        ticket.setQrToken(UUID.randomUUID().toString().replace("-", ""));
        return tickets.save(ticket);
    }

    private String ticketCode(Booking booking) {
        return tickets.findByBookingId(booking.getId())
                .map(Ticket::getTicketCode)
                .orElse(null);
    }

    private PaymentResponse toResponse(Booking booking, Payment payment, String ticketCode) {
        return new PaymentResponse(
                payment.getId(),
                booking.getId(),
                booking.getStatus().name(),
                payment.getStatus().name(),
                payment.getAmount(),
                payment.getProvider(),
                payment.getMethod(),
                payment.getTransactionReference(),
                payment.getGatewayTransactionNo(),
                payment.getBankCode(),
                payment.getCardType(),
                payment.getResponseCode(),
                payment.getPaidAt(),
                ticketCode);
    }
}
