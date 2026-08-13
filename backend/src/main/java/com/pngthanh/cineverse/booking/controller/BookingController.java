package com.pngthanh.cineverse.booking.controller;

import com.pngthanh.cineverse.booking.dto.BookingResponse;
import com.pngthanh.cineverse.booking.dto.CreateBookingRequest;
import com.pngthanh.cineverse.booking.service.BookingService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {
    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse create(
            Authentication authentication,
            @Valid @RequestBody CreateBookingRequest request) {
        return bookingService.create(authentication.getName(), request);
    }

    @GetMapping
    public List<BookingResponse> mine(Authentication authentication) {
        return bookingService.mine(authentication.getName());
    }

    @PostMapping("/{id}/cancel")
    public BookingResponse cancel(
            Authentication authentication,
            @PathVariable Long id) {
        return bookingService.cancelPending(authentication.getName(), id);
    }

    @GetMapping("/{id}")
    public BookingResponse get(
            Authentication authentication,
            @PathVariable Long id) {
        return bookingService.getMine(authentication.getName(), id);
    }
}
