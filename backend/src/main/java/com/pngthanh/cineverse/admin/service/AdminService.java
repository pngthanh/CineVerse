package com.pngthanh.cineverse.admin.service;

import com.pngthanh.cineverse.admin.dto.DashboardResponse;
import com.pngthanh.cineverse.booking.dto.BookingResponse;
import com.pngthanh.cineverse.booking.entity.Booking;
import com.pngthanh.cineverse.booking.repository.BookingRepository;
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
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {
    private final UserRepository users;
    private final BookingRepository bookings;
    private final MovieRepository movies;
    private final CinemaRepository cinemas;
    private final BookingService bookingService;
    private final UserService userService;

    public AdminService(
            UserRepository users,
            BookingRepository bookings,
            MovieRepository movies,
            CinemaRepository cinemas,
            BookingService bookingService,
            UserService userService) {
        this.users = users;
        this.bookings = bookings;
        this.movies = movies;
        this.cinemas = cinemas;
        this.bookingService = bookingService;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public DashboardResponse dashboard() {
        return new DashboardResponse(
                users.count(),
                bookings.count(),
                bookings.countByStatus(BookingStatus.CONFIRMED),
                movies.count(),
                cinemas.count());
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
}
