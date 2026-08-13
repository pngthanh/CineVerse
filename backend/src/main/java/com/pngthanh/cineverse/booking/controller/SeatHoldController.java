package com.pngthanh.cineverse.booking.controller;

import com.pngthanh.cineverse.booking.dto.SeatHoldRequest;
import com.pngthanh.cineverse.booking.dto.SeatHoldResponse;
import com.pngthanh.cineverse.booking.service.SeatHoldService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/seat-holds")
public class SeatHoldController {
    private final SeatHoldService seatHoldService;

    public SeatHoldController(SeatHoldService seatHoldService) {
        this.seatHoldService = seatHoldService;
    }

    @PostMapping
    public SeatHoldResponse hold(
            Authentication authentication,
            @Valid @RequestBody SeatHoldRequest request) {
        return seatHoldService.hold(authentication.getName(), request);
    }
}
