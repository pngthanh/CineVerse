package com.pngthanh.cineverse.booking.service;

import com.pngthanh.cineverse.booking.dto.BookingResponse;
import com.pngthanh.cineverse.booking.dto.CreateBookingRequest;
import com.pngthanh.cineverse.booking.entity.Booking;
import com.pngthanh.cineverse.booking.entity.BookingConcession;
import com.pngthanh.cineverse.booking.entity.BookingSeat;
import com.pngthanh.cineverse.booking.repository.BookingConcessionRepository;
import com.pngthanh.cineverse.booking.repository.BookingRepository;
import com.pngthanh.cineverse.booking.repository.BookingSeatRepository;
import com.pngthanh.cineverse.common.enums.BookingStatus;
import com.pngthanh.cineverse.common.enums.ShowtimeSeatStatus;
import com.pngthanh.cineverse.common.exception.ApiException;
import com.pngthanh.cineverse.concession.service.ConcessionService;
import com.pngthanh.cineverse.payment.repository.PaymentRepository;
import com.pngthanh.cineverse.showtime.entity.ShowtimeSeat;
import com.pngthanh.cineverse.showtime.repository.ShowtimeSeatRepository;
import com.pngthanh.cineverse.ticket.repository.TicketRepository;
import com.pngthanh.cineverse.user.entity.User;
import com.pngthanh.cineverse.user.service.UserService;
import com.pngthanh.cineverse.voucher.service.VoucherService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {
    private final BookingRepository bookings;
    private final BookingSeatRepository bookingSeats;
    private final BookingConcessionRepository bookingConcessions;
    private final ShowtimeSeatRepository showtimeSeats;
    private final PaymentRepository payments;
    private final TicketRepository tickets;
    private final UserService users;
    private final PricingService pricing;
    private final VoucherService vouchers;
    private final ConcessionService concessions;

    public BookingService(
            BookingRepository bookings,
            BookingSeatRepository bookingSeats,
            BookingConcessionRepository bookingConcessions,
            ShowtimeSeatRepository showtimeSeats,
            PaymentRepository payments,
            TicketRepository tickets,
            UserService users,
            PricingService pricing,
            VoucherService vouchers,
            ConcessionService concessions) {
        this.bookings = bookings;
        this.bookingSeats = bookingSeats;
        this.bookingConcessions = bookingConcessions;
        this.showtimeSeats = showtimeSeats;
        this.payments = payments;
        this.tickets = tickets;
        this.users = users;
        this.pricing = pricing;
        this.vouchers = vouchers;
        this.concessions = concessions;
    }

    @Transactional
    public BookingResponse create(String email, CreateBookingRequest request) {
        User user = users.requireByEmail(email);

        Booking existing = bookings.findByHoldToken(request.holdToken()).orElse(null);
        if (existing != null) {
            if (!Objects.equals(existing.getUser().getId(), user.getId())) {
                throw new ApiException(
                        HttpStatus.FORBIDDEN,
                        "HOLD_FORBIDDEN",
                        "Phiên giữ ghế không thuộc tài khoản hiện tại.");
            }
            return toResponse(existing);
        }

        List<ShowtimeSeat> heldSeats = showtimeSeats.findByHoldTokenForUpdate(request.holdToken());

        // Kiểm tra lại sau khi lấy lock để request trùng không tạo hai booking.
        existing = bookings.findByHoldToken(request.holdToken()).orElse(null);
        if (existing != null) {
            if (!Objects.equals(existing.getUser().getId(), user.getId())) {
                throw new ApiException(
                        HttpStatus.FORBIDDEN,
                        "HOLD_FORBIDDEN",
                        "Phiên giữ ghế không thuộc tài khoản hiện tại.");
            }
            return toResponse(existing);
        }

        if (heldSeats.isEmpty()) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "HOLD_NOT_FOUND",
                    "Phiên giữ ghế không còn hiệu lực.");
        }

        Instant now = Instant.now();
        for (ShowtimeSeat seat : heldSeats) {
            boolean validHold = seat.getStatus() == ShowtimeSeatStatus.HELD
                    && Objects.equals(seat.getHeldByUserId(), user.getId())
                    && seat.getHoldExpiresAt() != null
                    && seat.getHoldExpiresAt().isAfter(now);
            if (!validHold) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "HOLD_EXPIRED",
                        "Phiên giữ ghế đã hết hạn.");
            }
        }

        var showtime = heldSeats.getFirst().getShowtime();

        Booking booking = new Booking();
        booking.setBookingCode(nextCode());
        booking.setUser(user);
        booking.setShowtime(showtime);
        booking.setHoldToken(request.holdToken());
        booking.setExpiresAt(heldSeats.getFirst().getHoldExpiresAt());

        BigDecimal seatTotal = heldSeats.stream()
                .map(seat -> pricing.seatPrice(showtime, seat))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        ConcessionService.ConcessionQuote concessionQuote = concessions.quote(request.concessions());
        BigDecimal subtotal = seatTotal.add(concessionQuote.totalAmount());

        booking.setConcessionAmount(concessionQuote.totalAmount());
        booking.setSubtotalAmount(subtotal);
        VoucherService.AppliedVoucher appliedVoucher = vouchers.apply(request.voucherCode(), subtotal);
        booking.setVoucherCode(appliedVoucher.code());
        booking.setDiscountAmount(appliedVoucher.discountAmount());
        booking.setTotalAmount(appliedVoucher.totalAmount());
        Booking savedBooking = bookings.save(booking);

        for (ShowtimeSeat seat : heldSeats) {
            BookingSeat bookingSeat = new BookingSeat();
            bookingSeat.setBooking(savedBooking);
            bookingSeat.setShowtimeSeat(seat);
            bookingSeat.setPrice(pricing.seatPrice(showtime, seat));
            bookingSeats.save(bookingSeat);
        }

        for (ConcessionService.SelectedConcession selected : concessionQuote.selections()) {
            BookingConcession bookingConcession = new BookingConcession();
            bookingConcession.setBooking(savedBooking);
            bookingConcession.setConcessionItem(selected.item());
            bookingConcession.setQuantity(selected.quantity());
            bookingConcession.setUnitPrice(selected.item().getPrice());
            bookingConcessions.save(bookingConcession);
        }
        return toResponse(savedBooking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> mine(String email) {
        User user = users.requireByEmail(email);
        return bookings.findAllByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BookingResponse getMine(String email, Long id) {
        User user = users.requireByEmail(email);
        Booking booking = bookings.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "BOOKING_NOT_FOUND",
                        "Không tìm thấy booking."));
        return toResponse(booking);
    }

    @Transactional
    public BookingResponse cancelPending(String email, Long id) {
        User user = users.requireByEmail(email);
        Booking booking = bookings.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "BOOKING_NOT_FOUND",
                        "Không tìm thấy booking."));
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "BOOKING_NOT_PENDING",
                    "Chỉ có thể hủy booking đang chờ thanh toán.");
        }
        booking.setStatus(BookingStatus.CANCELLED);
        releaseHeldSeats(booking);
        return toResponse(booking);
    }

    public Booking require(Long id) {
        return bookings.findById(id)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "BOOKING_NOT_FOUND",
                        "Không tìm thấy booking."));
    }

    public Booking requireForUpdate(Long id) {
        return bookings.findByIdForUpdate(id)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "BOOKING_NOT_FOUND",
                        "Không tìm thấy booking."));
    }

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void cancelExpired() {
        Instant now = Instant.now();
        for (Booking candidate : bookings.findAllByStatusAndExpiresAtBefore(
                BookingStatus.PENDING,
                now)) {
            Booking booking = requireForUpdate(candidate.getId());
            if (booking.getStatus() == BookingStatus.PENDING
                    && booking.getExpiresAt().isBefore(now)) {
                booking.setStatus(BookingStatus.CANCELLED);
                releaseHeldSeats(booking);
            }
        }
    }

    public BookingResponse toResponse(Booking booking) {
        var showtime = booking.getShowtime();
        List<BookingResponse.SeatInfo> seatInfo = bookingSeats.findAllByBookingId(booking.getId())
                .stream()
                .map(bookingSeat -> new BookingResponse.SeatInfo(
                        bookingSeat.getShowtimeSeat().getSeat().getId(),
                        bookingSeat.getShowtimeSeat().getSeat().getSeatCode(),
                        bookingSeat.getShowtimeSeat().getSeat().getType().name(),
                        bookingSeat.getPrice()))
                .toList();
        BigDecimal seatAmount = seatInfo.stream()
                .map(BookingResponse.SeatInfo::price)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<BookingResponse.ConcessionInfo> concessionInfo = bookingConcessions
                .findAllByBookingIdOrderByIdAsc(booking.getId())
                .stream()
                .map(item -> new BookingResponse.ConcessionInfo(
                        item.getConcessionItem().getId(),
                        item.getConcessionItem().getName(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()))))
                .toList();
        BigDecimal concessionAmount = booking.getConcessionAmount() == null
                ? BigDecimal.ZERO
                : booking.getConcessionAmount();
        BigDecimal subtotalAmount = booking.getSubtotalAmount() == null
                ? seatAmount.add(concessionAmount)
                : booking.getSubtotalAmount();
        String paymentStatus = payments.findByBookingId(booking.getId())
                .map(payment -> payment.getStatus().name())
                .orElse(null);
        var ticket = tickets.findByBookingId(booking.getId()).orElse(null);

        return new BookingResponse(
                booking.getId(),
                booking.getBookingCode(),
                booking.getStatus().name(),
                booking.getCreatedAt(),
                booking.getExpiresAt(),
                seatAmount,
                concessionAmount,
                subtotalAmount,
                booking.getDiscountAmount() == null ? BigDecimal.ZERO : booking.getDiscountAmount(),
                booking.getTotalAmount(),
                booking.getVoucherCode(),
                new BookingResponse.UserInfo(
                        booking.getUser().getId(),
                        booking.getUser().getFullName(),
                        booking.getUser().getEmail(),
                        booking.getUser().getRole().name(),
                        booking.getUser().getStatus().name(),
                        booking.getUser().getCreatedAt()),
                new BookingResponse.ShowtimeInfo(
                        showtime.getId(),
                        showtime.getMovie().getTitle(),
                        showtime.getRoom().getCinema().getName(),
                        showtime.getRoom().getName(),
                        showtime.getStartTime()),
                seatInfo,
                concessionInfo,
                paymentStatus,
                ticket == null ? null : ticket.getTicketCode(),
                ticket == null ? null : ticket.getStatus().name());
    }

    public void releaseHeldSeats(Booking booking) {
        for (BookingSeat bookingSeat : bookingSeats.findAllByBookingId(booking.getId())) {
            ShowtimeSeat showtimeSeat = bookingSeat.getShowtimeSeat();
            if (showtimeSeat.getStatus() == ShowtimeSeatStatus.HELD
                    && Objects.equals(showtimeSeat.getHoldToken(), booking.getHoldToken())) {
                showtimeSeat.setStatus(ShowtimeSeatStatus.AVAILABLE);
                showtimeSeat.setHeldByUserId(null);
                showtimeSeat.setHoldToken(null);
                showtimeSeat.setHoldExpiresAt(null);
            }
        }
    }

    private String nextCode() {
        return "CVB-"
                + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-"
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
