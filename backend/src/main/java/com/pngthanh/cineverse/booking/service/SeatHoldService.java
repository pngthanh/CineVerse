package com.pngthanh.cineverse.booking.service;

import com.pngthanh.cineverse.booking.dto.SeatHoldRequest;
import com.pngthanh.cineverse.booking.dto.SeatHoldResponse;
import com.pngthanh.cineverse.common.enums.ShowtimeSeatStatus;
import com.pngthanh.cineverse.common.exception.ApiException;
import com.pngthanh.cineverse.showtime.entity.Showtime;
import com.pngthanh.cineverse.showtime.entity.ShowtimeSeat;
import com.pngthanh.cineverse.showtime.repository.ShowtimeSeatRepository;
import com.pngthanh.cineverse.showtime.service.ShowtimeService;
import com.pngthanh.cineverse.user.entity.User;
import com.pngthanh.cineverse.user.service.UserService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SeatHoldService {
    private final ShowtimeSeatRepository seats;
    private final ShowtimeService showtimes;
    private final UserService users;
    private final PricingService pricing;
    private final long holdMinutes;

    public SeatHoldService(
            ShowtimeSeatRepository seats,
            ShowtimeService showtimes,
            UserService users,
            PricingService pricing,
            @Value("${app.booking.seat-hold-minutes}") long holdMinutes) {
        this.seats = seats;
        this.showtimes = showtimes;
        this.users = users;
        this.pricing = pricing;
        this.holdMinutes = holdMinutes;
    }

    @Transactional
    public SeatHoldResponse hold(String email, SeatHoldRequest request) {
        User user = users.requireByEmail(email);
        Showtime showtime = showtimes.require(request.showtimeId());
        List<Long> uniqueIds = request.seatIds().stream().distinct().sorted().toList();
        List<ShowtimeSeat> lockedSeats = seats.findForUpdate(showtime.getId(), uniqueIds);

        if (lockedSeats.size() != uniqueIds.size()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_SEATS",
                    "Có ghế không thuộc suất chiếu này.");
        }

        Instant now = Instant.now();
        releaseExpiredLockedSeats(lockedSeats, now);
        ensureSeatsAvailableForUser(lockedSeats, user.getId());

        String holdToken = UUID.randomUUID().toString();
        Instant expiresAt = now.plus(holdMinutes, ChronoUnit.MINUTES);
        List<SeatHoldResponse.SeatPrice> responseSeats = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (ShowtimeSeat seat : lockedSeats) {
            seat.setStatus(ShowtimeSeatStatus.HELD);
            seat.setHeldByUserId(user.getId());
            seat.setHoldToken(holdToken);
            seat.setHoldExpiresAt(expiresAt);

            BigDecimal price = pricing.seatPrice(showtime, seat);
            total = total.add(price);
            responseSeats.add(new SeatHoldResponse.SeatPrice(
                    seat.getSeat().getId(),
                    seat.getSeat().getSeatCode(),
                    seat.getSeat().getType().name(),
                    price));
        }

        return new SeatHoldResponse(holdToken, expiresAt, responseSeats, total);
    }

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void releaseExpiredHolds() {
        seats.releaseExpired(ShowtimeSeatStatus.AVAILABLE, ShowtimeSeatStatus.HELD, Instant.now());
    }

    public void release(ShowtimeSeat seat) {
        seat.setStatus(ShowtimeSeatStatus.AVAILABLE);
        seat.setHeldByUserId(null);
        seat.setHoldToken(null);
        seat.setHoldExpiresAt(null);
    }

    private void releaseExpiredLockedSeats(List<ShowtimeSeat> lockedSeats, Instant now) {
        for (ShowtimeSeat seat : lockedSeats) {
            boolean expired = seat.getStatus() == ShowtimeSeatStatus.HELD
                    && seat.getHoldExpiresAt() != null
                    && seat.getHoldExpiresAt().isBefore(now);
            if (expired) {
                // Ghế hết thời gian giữ phải được mở lại trước khi kiểm tra xung đột.
                release(seat);
            }
        }
    }

    private void ensureSeatsAvailableForUser(List<ShowtimeSeat> lockedSeats, Long userId) {
        for (ShowtimeSeat seat : lockedSeats) {
            boolean heldByCurrentUser = seat.getStatus() == ShowtimeSeatStatus.HELD
                    && Objects.equals(seat.getHeldByUserId(), userId);
            boolean unavailable = seat.getStatus() != ShowtimeSeatStatus.AVAILABLE
                    && !heldByCurrentUser;
            if (unavailable) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "SEAT_UNAVAILABLE",
                        "Một hoặc nhiều ghế vừa được người khác giữ/đặt.");
            }
        }
    }
}
