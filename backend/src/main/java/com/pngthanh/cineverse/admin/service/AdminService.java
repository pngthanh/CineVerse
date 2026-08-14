package com.pngthanh.cineverse.admin.service;

import com.pngthanh.cineverse.admin.dto.DashboardResponse;
import com.pngthanh.cineverse.booking.dto.BookingResponse;
import com.pngthanh.cineverse.booking.entity.Booking;
import com.pngthanh.cineverse.booking.entity.BookingConcession;
import com.pngthanh.cineverse.booking.entity.BookingSeat;
import com.pngthanh.cineverse.booking.repository.BookingConcessionRepository;
import com.pngthanh.cineverse.booking.repository.BookingRepository;
import com.pngthanh.cineverse.booking.repository.BookingSeatRepository;
import com.pngthanh.cineverse.booking.service.BookingService;
import com.pngthanh.cineverse.cinema.repository.CinemaRepository;
import com.pngthanh.cineverse.common.enums.BookingStatus;
import com.pngthanh.cineverse.common.enums.UserStatus;
import com.pngthanh.cineverse.common.exception.ApiException;
import com.pngthanh.cineverse.movie.repository.MovieRepository;
import com.pngthanh.cineverse.user.dto.UserProfileResponse;
import com.pngthanh.cineverse.user.entity.User;
import com.pngthanh.cineverse.user.repository.UserRepository;
import com.pngthanh.cineverse.user.service.UserService;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {
    private final UserRepository users;
    private final BookingRepository bookings;
    private final BookingSeatRepository bookingSeats;
    private final BookingConcessionRepository bookingConcessions;
    private final MovieRepository movies;
    private final CinemaRepository cinemas;
    private final BookingService bookingService;
    private final UserService userService;

    public AdminService(
            UserRepository users,
            BookingRepository bookings,
            BookingSeatRepository bookingSeats,
            BookingConcessionRepository bookingConcessions,
            MovieRepository movies,
            CinemaRepository cinemas,
            BookingService bookingService,
            UserService userService) {
        this.users = users;
        this.bookings = bookings;
        this.bookingSeats = bookingSeats;
        this.bookingConcessions = bookingConcessions;
        this.movies = movies;
        this.cinemas = cinemas;
        this.bookingService = bookingService;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public DashboardResponse dashboard() {
        List<Booking> realizedBookings = bookings.findAll().stream()
                .filter(this::isRealized)
                .toList();

        BigDecimal grossSeatRevenue = BigDecimal.ZERO;
        BigDecimal concessionRevenue = BigDecimal.ZERO;
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal netRevenue = BigDecimal.ZERO;
        long ticketsSold = 0;

        Map<Long, CinemaAccumulator> cinemaStats = new LinkedHashMap<>();
        Map<Long, MovieAccumulator> movieStats = new LinkedHashMap<>();
        Map<Long, ConcessionAccumulator> concessionStats = new LinkedHashMap<>();

        cinemas.findAllByOrderByName().forEach(cinema -> cinemaStats.put(
                cinema.getId(),
                new CinemaAccumulator(cinema.getId(), cinema.getName())));
        movies.findAll().forEach(movie -> movieStats.put(
                movie.getId(),
                new MovieAccumulator(movie.getId(), movie.getTitle(), movie.getPosterUrl())));

        for (Booking booking : realizedBookings) {
            List<BookingSeat> seatItems = bookingSeats.findAllByBookingId(booking.getId());
            List<BookingConcession> concessionItems = bookingConcessions
                    .findAllByBookingIdOrderByIdAsc(booking.getId());

            BigDecimal seatRevenue = seatItems.stream()
                    .map(BookingSeat::getPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal bookingConcessionRevenue = concessionItems.stream()
                    .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal bookingDiscount = booking.getDiscountAmount() == null
                    ? BigDecimal.ZERO
                    : booking.getDiscountAmount();
            BigDecimal bookingNet = booking.getTotalAmount() == null
                    ? seatRevenue.add(bookingConcessionRevenue).subtract(bookingDiscount)
                    : booking.getTotalAmount();

            long bookingTickets = seatItems.size();
            ticketsSold += bookingTickets;
            grossSeatRevenue = grossSeatRevenue.add(seatRevenue);
            concessionRevenue = concessionRevenue.add(bookingConcessionRevenue);
            discountAmount = discountAmount.add(bookingDiscount);
            netRevenue = netRevenue.add(bookingNet);

            var cinema = booking.getShowtime().getRoom().getCinema();
            CinemaAccumulator cinemaAccumulator = cinemaStats.computeIfAbsent(
                    cinema.getId(),
                    id -> new CinemaAccumulator(cinema.getId(), cinema.getName()));
            cinemaAccumulator.add(
                    bookingTickets,
                    seatRevenue,
                    bookingConcessionRevenue,
                    bookingDiscount,
                    bookingNet);

            var movie = booking.getShowtime().getMovie();
            MovieAccumulator movieAccumulator = movieStats.computeIfAbsent(
                    movie.getId(),
                    id -> new MovieAccumulator(movie.getId(), movie.getTitle(), movie.getPosterUrl()));
            movieAccumulator.add(bookingTickets, seatRevenue);
            cinemaAccumulator.addMovie(
                    movie.getId(),
                    movie.getTitle(),
                    movie.getPosterUrl(),
                    bookingTickets,
                    seatRevenue);

            for (BookingConcession item : concessionItems) {
                var concession = item.getConcessionItem();
                ConcessionAccumulator concessionAccumulator = concessionStats.computeIfAbsent(
                        concession.getId(),
                        id -> new ConcessionAccumulator(concession.getId(), concession.getName()));
                concessionAccumulator.add(item.getQuantity(), item.getUnitPrice());
                cinemaAccumulator.addConcession(
                        concession.getId(),
                        concession.getName(),
                        item.getQuantity(),
                        item.getUnitPrice());
            }
        }

        List<DashboardResponse.CinemaRevenue> cinemaRows = cinemaStats.values().stream()
                .map(CinemaAccumulator::toResponse)
                .sorted(Comparator.comparing(DashboardResponse.CinemaRevenue::netRevenue).reversed())
                .toList();
        List<DashboardResponse.MovieRevenue> movieRows = movieStats.values().stream()
                .map(MovieAccumulator::toResponse)
                .sorted(Comparator.comparingLong(DashboardResponse.MovieRevenue::ticketsSold).reversed())
                .toList();
        List<DashboardResponse.ConcessionRevenue> concessionRows = concessionStats.values().stream()
                .map(ConcessionAccumulator::toResponse)
                .sorted(Comparator.comparing(DashboardResponse.ConcessionRevenue::revenue).reversed())
                .toList();

        return new DashboardResponse(
                users.count(),
                bookings.count(),
                realizedBookings.size(),
                movies.count(),
                cinemas.count(),
                ticketsSold,
                grossSeatRevenue,
                concessionRevenue,
                discountAmount,
                netRevenue,
                cinemaRows,
                movieRows,
                concessionRows);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> listBookings() {
        return bookings.findAll().stream()
                .map(bookingService::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BookingResponse getBooking(Long id) {
        Booking booking = bookingService.require(id);
        return bookingService.toResponse(booking);
    }

    @Transactional(readOnly = true)
    public List<UserProfileResponse> listUsers() {
        return users.findAll().stream()
                .map(userService::toResponse)
                .toList();
    }

    @Transactional
    public UserProfileResponse updateUserStatus(Long id, UserStatus status) {
        User user = users.findById(id)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "USER_NOT_FOUND",
                        "Không tìm thấy người dùng."));
        user.setStatus(status);
        return userService.toResponse(user);
    }

    private boolean isRealized(Booking booking) {
        return booking.getStatus() == BookingStatus.CONFIRMED
                || booking.getStatus() == BookingStatus.COMPLETED;
    }

    private static final class CinemaAccumulator {
        private final Long id;
        private final String name;
        private long bookings;
        private long ticketsSold;
        private BigDecimal seatRevenue = BigDecimal.ZERO;
        private BigDecimal concessionRevenue = BigDecimal.ZERO;
        private BigDecimal discountAmount = BigDecimal.ZERO;
        private BigDecimal netRevenue = BigDecimal.ZERO;
        private final Map<Long, MovieAccumulator> movies = new LinkedHashMap<>();
        private final Map<Long, ConcessionAccumulator> concessions = new LinkedHashMap<>();

        CinemaAccumulator(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        void add(
                long ticketCount,
                BigDecimal seats,
                BigDecimal concessions,
                BigDecimal discount,
                BigDecimal net) {
            bookings++;
            ticketsSold += ticketCount;
            seatRevenue = seatRevenue.add(seats);
            concessionRevenue = concessionRevenue.add(concessions);
            discountAmount = discountAmount.add(discount);
            netRevenue = netRevenue.add(net);
        }

        void addMovie(
                Long movieId,
                String title,
                String posterUrl,
                long ticketCount,
                BigDecimal revenue) {
            MovieAccumulator movie = movies.computeIfAbsent(
                    movieId,
                    id -> new MovieAccumulator(movieId, title, posterUrl));
            movie.add(ticketCount, revenue);
        }

        void addConcession(
                Long itemId,
                String itemName,
                int quantity,
                BigDecimal unitPrice) {
            ConcessionAccumulator item = concessions.computeIfAbsent(
                    itemId,
                    id -> new ConcessionAccumulator(itemId, itemName));
            item.add(quantity, unitPrice);
        }

        DashboardResponse.CinemaRevenue toResponse() {
            return new DashboardResponse.CinemaRevenue(
                    id,
                    name,
                    bookings,
                    ticketsSold,
                    seatRevenue,
                    concessionRevenue,
                    discountAmount,
                    netRevenue,
                    movies.values().stream()
                            .map(MovieAccumulator::toResponse)
                            .sorted(Comparator.comparingLong(
                                    DashboardResponse.MovieRevenue::ticketsSold).reversed())
                            .toList(),
                    concessions.values().stream()
                            .map(ConcessionAccumulator::toResponse)
                            .sorted(Comparator.comparing(
                                    DashboardResponse.ConcessionRevenue::revenue).reversed())
                            .toList());
        }
    }

    private static final class MovieAccumulator {
        private final Long id;
        private final String title;
        private final String posterUrl;
        private long bookings;
        private long ticketsSold;
        private BigDecimal ticketRevenue = BigDecimal.ZERO;

        MovieAccumulator(Long id, String title, String posterUrl) {
            this.id = id;
            this.title = title;
            this.posterUrl = posterUrl;
        }

        void add(long ticketCount, BigDecimal revenue) {
            bookings++;
            ticketsSold += ticketCount;
            ticketRevenue = ticketRevenue.add(revenue);
        }

        DashboardResponse.MovieRevenue toResponse() {
            return new DashboardResponse.MovieRevenue(
                    id,
                    title,
                    posterUrl,
                    bookings,
                    ticketsSold,
                    ticketRevenue);
        }
    }

    private static final class ConcessionAccumulator {
        private final Long id;
        private final String name;
        private long quantity;
        private BigDecimal revenue = BigDecimal.ZERO;

        ConcessionAccumulator(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        void add(int itemQuantity, BigDecimal unitPrice) {
            quantity += itemQuantity;
            revenue = revenue.add(unitPrice.multiply(BigDecimal.valueOf(itemQuantity)));
        }

        DashboardResponse.ConcessionRevenue toResponse() {
            return new DashboardResponse.ConcessionRevenue(id, name, quantity, revenue);
        }
    }
}
