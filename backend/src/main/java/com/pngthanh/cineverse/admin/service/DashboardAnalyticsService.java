package com.pngthanh.cineverse.admin.service;

import com.pngthanh.cineverse.admin.dto.AdvancedDashboardResponse;
import com.pngthanh.cineverse.booking.entity.Booking;
import com.pngthanh.cineverse.booking.entity.BookingConcession;
import com.pngthanh.cineverse.booking.entity.BookingSeat;
import com.pngthanh.cineverse.booking.repository.BookingConcessionRepository;
import com.pngthanh.cineverse.booking.repository.BookingRepository;
import com.pngthanh.cineverse.booking.repository.BookingSeatRepository;
import com.pngthanh.cineverse.common.enums.BookingStatus;
import com.pngthanh.cineverse.payment.entity.Payment;
import com.pngthanh.cineverse.payment.repository.PaymentRepository;
import com.pngthanh.cineverse.ticket.entity.Ticket;
import com.pngthanh.cineverse.ticket.repository.TicketRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardAnalyticsService {
    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");

    private final BookingRepository bookings;
    private final BookingSeatRepository bookingSeats;
    private final BookingConcessionRepository bookingConcessions;
    private final PaymentRepository payments;
    private final TicketRepository tickets;

    public DashboardAnalyticsService(
            BookingRepository bookings,
            BookingSeatRepository bookingSeats,
            BookingConcessionRepository bookingConcessions,
            PaymentRepository payments,
            TicketRepository tickets) {
        this.bookings = bookings;
        this.bookingSeats = bookingSeats;
        this.bookingConcessions = bookingConcessions;
        this.payments = payments;
        this.tickets = tickets;
    }

    @Transactional(readOnly = true)
    public AdvancedDashboardResponse dashboard(
            LocalDate from,
            LocalDate to,
            Long cinemaId,
            Long movieId) {
        LocalDate effectiveFrom = from;
        LocalDate effectiveTo = to;
        if (effectiveFrom != null && effectiveTo != null && effectiveFrom.isAfter(effectiveTo)) {
            LocalDate swap = effectiveFrom;
            effectiveFrom = effectiveTo;
            effectiveTo = swap;
        }

        final LocalDate start = effectiveFrom;
        final LocalDate end = effectiveTo;
        List<Booking> realized = bookings.findAll().stream()
                .filter(this::isRealized)
                .filter(booking -> inDateRange(booking.getCreatedAt(), start, end))
                .filter(booking -> cinemaId == null
                        || cinemaId.equals(booking.getShowtime().getRoom().getCinema().getId()))
                .filter(booking -> movieId == null
                        || movieId.equals(booking.getShowtime().getMovie().getId()))
                .toList();

        Map<Long, Payment> paymentByBooking = payments.findAll().stream()
                .filter(payment -> payment.getBooking() != null)
                .collect(Collectors.toMap(
                        payment -> payment.getBooking().getId(),
                        Function.identity(),
                        (left, right) -> left));
        Map<Long, Ticket> ticketByBooking = tickets.findAll().stream()
                .filter(ticket -> ticket.getBooking() != null)
                .collect(Collectors.toMap(
                        ticket -> ticket.getBooking().getId(),
                        Function.identity(),
                        (left, right) -> left));

        BigDecimal seatRevenue = BigDecimal.ZERO;
        BigDecimal concessionRevenue = BigDecimal.ZERO;
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal netRevenue = BigDecimal.ZERO;
        long ticketsSold = 0;
        long checkedInTickets = 0;

        Map<String, TrendAccumulator> trend = new LinkedHashMap<>();
        Map<Long, NamedAccumulator> cinemaStats = new LinkedHashMap<>();
        Map<Long, MovieAccumulator> movieStats = new LinkedHashMap<>();
        Map<Long, ItemAccumulator> concessionStats = new LinkedHashMap<>();
        Map<String, VoucherAccumulator> voucherStats = new LinkedHashMap<>();
        Map<String, PaymentAccumulator> paymentStats = new LinkedHashMap<>();
        boolean monthly = start != null && end != null
                && ChronoUnit.DAYS.between(start, end) > 62;

        for (Booking booking : realized) {
            List<BookingSeat> seats = bookingSeats.findAllByBookingId(booking.getId());
            List<BookingConcession> concessions = bookingConcessions
                    .findAllByBookingIdOrderByIdAsc(booking.getId());
            BigDecimal bookingSeatRevenue = seats.stream()
                    .map(BookingSeat::getPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal bookingConcessionRevenue = concessions.stream()
                    .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal bookingDiscount = value(booking.getDiscountAmount());
            BigDecimal bookingNet = booking.getTotalAmount() == null
                    ? bookingSeatRevenue.add(bookingConcessionRevenue).subtract(bookingDiscount)
                    : booking.getTotalAmount();
            long seatCount = seats.size();

            seatRevenue = seatRevenue.add(bookingSeatRevenue);
            concessionRevenue = concessionRevenue.add(bookingConcessionRevenue);
            discountAmount = discountAmount.add(bookingDiscount);
            netRevenue = netRevenue.add(bookingNet);
            ticketsSold += seatCount;
            Ticket ticket = ticketByBooking.get(booking.getId());
            if (ticket != null && ticket.getCheckedInAt() != null) {
                checkedInTickets += seatCount;
            }

            String period = periodLabel(booking.getCreatedAt(), monthly);
            trend.computeIfAbsent(period, ignored -> new TrendAccumulator())
                    .add(bookingNet, seatCount);

            var cinema = booking.getShowtime().getRoom().getCinema();
            cinemaStats.computeIfAbsent(cinema.getId(), ignored -> new NamedAccumulator(cinema.getName()))
                    .add(bookingNet, seatCount);

            var movie = booking.getShowtime().getMovie();
            movieStats.computeIfAbsent(
                    movie.getId(),
                    ignored -> new MovieAccumulator(movie.getTitle(), movie.getPosterUrl()))
                    .add(bookingNet, seatCount);

            for (BookingConcession item : concessions) {
                var concession = item.getConcessionItem();
                concessionStats.computeIfAbsent(
                        concession.getId(),
                        ignored -> new ItemAccumulator(concession.getName()))
                        .add(item.getQuantity(), item.getUnitPrice());
            }

            if (booking.getVoucherCode() != null && !booking.getVoucherCode().isBlank()) {
                voucherStats.computeIfAbsent(booking.getVoucherCode(), ignored -> new VoucherAccumulator())
                        .add(bookingDiscount);
            }

            Payment payment = paymentByBooking.get(booking.getId());
            String method = payment == null || payment.getMethod() == null || payment.getMethod().isBlank()
                    ? "Không xác định"
                    : payment.getMethod();
            paymentStats.computeIfAbsent(method, ignored -> new PaymentAccumulator())
                    .add(bookingNet);
        }

        return new AdvancedDashboardResponse(
                realized.size(),
                ticketsSold,
                checkedInTickets,
                seatRevenue,
                concessionRevenue,
                discountAmount,
                netRevenue,
                trend.entrySet().stream()
                        .map(entry -> new AdvancedDashboardResponse.TrendPoint(
                                entry.getKey(), entry.getValue().revenue, entry.getValue().tickets))
                        .toList(),
                cinemaStats.entrySet().stream()
                        .map(entry -> new AdvancedDashboardResponse.CinemaRow(
                                entry.getKey(), entry.getValue().name, entry.getValue().bookings,
                                entry.getValue().tickets, entry.getValue().revenue))
                        .sorted(Comparator.comparing(AdvancedDashboardResponse.CinemaRow::revenue).reversed())
                        .toList(),
                movieStats.entrySet().stream()
                        .map(entry -> new AdvancedDashboardResponse.MovieRow(
                                entry.getKey(), entry.getValue().title, entry.getValue().posterUrl,
                                entry.getValue().bookings, entry.getValue().tickets, entry.getValue().revenue))
                        .sorted(Comparator.comparingLong(AdvancedDashboardResponse.MovieRow::tickets).reversed())
                        .toList(),
                concessionStats.entrySet().stream()
                        .map(entry -> new AdvancedDashboardResponse.ConcessionRow(
                                entry.getKey(), entry.getValue().name,
                                entry.getValue().quantity, entry.getValue().revenue))
                        .sorted(Comparator.comparing(AdvancedDashboardResponse.ConcessionRow::revenue).reversed())
                        .toList(),
                voucherStats.entrySet().stream()
                        .map(entry -> new AdvancedDashboardResponse.VoucherRow(
                                entry.getKey(), entry.getValue().uses, entry.getValue().discount))
                        .sorted(Comparator.comparingLong(AdvancedDashboardResponse.VoucherRow::uses).reversed())
                        .toList(),
                paymentStats.entrySet().stream()
                        .map(entry -> new AdvancedDashboardResponse.PaymentMethodRow(
                                entry.getKey(), entry.getValue().transactions, entry.getValue().revenue))
                        .sorted(Comparator.comparing(AdvancedDashboardResponse.PaymentMethodRow::revenue).reversed())
                        .toList());
    }

    private boolean isRealized(Booking booking) {
        return booking.getStatus() == BookingStatus.CONFIRMED
                || booking.getStatus() == BookingStatus.COMPLETED;
    }

    private boolean inDateRange(Instant instant, LocalDate from, LocalDate to) {
        LocalDate date = instant.atZone(VIETNAM).toLocalDate();
        return (from == null || !date.isBefore(from)) && (to == null || !date.isAfter(to));
    }

    private String periodLabel(Instant instant, boolean monthly) {
        LocalDate date = instant.atZone(VIETNAM).toLocalDate();
        return monthly ? YearMonth.from(date).toString() : date.toString();
    }

    private BigDecimal value(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private static final class TrendAccumulator {
        private BigDecimal revenue = BigDecimal.ZERO;
        private long tickets;
        void add(BigDecimal amount, long count) {
            revenue = revenue.add(amount);
            tickets += count;
        }
    }

    private static final class NamedAccumulator {
        private final String name;
        private long bookings;
        private long tickets;
        private BigDecimal revenue = BigDecimal.ZERO;
        NamedAccumulator(String name) {
            this.name = name;
        }
        void add(BigDecimal amount, long count) {
            bookings++;
            tickets += count;
            revenue = revenue.add(amount);
        }
    }

    private static final class MovieAccumulator {
        private final String title;
        private final String posterUrl;
        private long bookings;
        private long tickets;
        private BigDecimal revenue = BigDecimal.ZERO;
        MovieAccumulator(String title, String posterUrl) {
            this.title = title;
            this.posterUrl = posterUrl;
        }
        void add(BigDecimal amount, long count) {
            bookings++;
            tickets += count;
            revenue = revenue.add(amount);
        }
    }

    private static final class ItemAccumulator {
        private final String name;
        private long quantity;
        private BigDecimal revenue = BigDecimal.ZERO;
        ItemAccumulator(String name) {
            this.name = name;
        }
        void add(int count, BigDecimal unitPrice) {
            quantity += count;
            revenue = revenue.add(unitPrice.multiply(BigDecimal.valueOf(count)));
        }
    }

    private static final class VoucherAccumulator {
        private long uses;
        private BigDecimal discount = BigDecimal.ZERO;
        void add(BigDecimal amount) {
            uses++;
            discount = discount.add(amount);
        }
    }

    private static final class PaymentAccumulator {
        private long transactions;
        private BigDecimal revenue = BigDecimal.ZERO;
        void add(BigDecimal amount) {
            transactions++;
            revenue = revenue.add(amount);
        }
    }
}
