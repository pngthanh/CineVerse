package com.pngthanh.cineverse.showtime.service;

import com.pngthanh.cineverse.booking.service.PricingService;
import com.pngthanh.cineverse.cinema.entity.Room;
import com.pngthanh.cineverse.cinema.entity.Seat;
import com.pngthanh.cineverse.cinema.repository.SeatRepository;
import com.pngthanh.cineverse.cinema.service.CinemaService;
import com.pngthanh.cineverse.common.enums.ShowtimeSeatStatus;
import com.pngthanh.cineverse.common.exception.ApiException;
import com.pngthanh.cineverse.movie.entity.Movie;
import com.pngthanh.cineverse.movie.service.MovieService;
import com.pngthanh.cineverse.showtime.dto.SeatMapResponse;
import com.pngthanh.cineverse.showtime.dto.ShowtimeRequest;
import com.pngthanh.cineverse.showtime.dto.ShowtimeResponse;
import com.pngthanh.cineverse.showtime.entity.Showtime;
import com.pngthanh.cineverse.showtime.entity.ShowtimeSeat;
import com.pngthanh.cineverse.showtime.repository.ShowtimeRepository;
import com.pngthanh.cineverse.showtime.repository.ShowtimeSeatRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShowtimeService {
    private final ShowtimeRepository showtimes;
    private final ShowtimeSeatRepository showtimeSeats;
    private final SeatRepository seats;
    private final MovieService movies;
    private final CinemaService cinemas;
    private final PricingService pricing;

    public ShowtimeService(
            ShowtimeRepository showtimes,
            ShowtimeSeatRepository showtimeSeats,
            SeatRepository seats,
            MovieService movies,
            CinemaService cinemas,
            PricingService pricing) {
        this.showtimes = showtimes;
        this.showtimeSeats = showtimeSeats;
        this.seats = seats;
        this.movies = movies;
        this.cinemas = cinemas;
        this.pricing = pricing;
    }

    @Transactional(readOnly = true)
    public List<ShowtimeResponse> list(Long movieId, Long cinemaId, LocalDate date) {
        List<Showtime> data;
        if (movieId != null && cinemaId != null) {
            data = showtimes.findAllByMovieIdAndRoomCinemaIdAndActiveTrueOrderByStartTime(
                    movieId, cinemaId);
        } else if (movieId != null) {
            data = showtimes.findAllByMovieIdAndActiveTrueOrderByStartTime(movieId);
        } else if (cinemaId != null) {
            data = showtimes.findAllByRoomCinemaIdAndActiveTrueOrderByStartTime(cinemaId);
        } else {
            data = showtimes.findAllByActiveTrueOrderByStartTime();
        }

        return data.stream()
                .filter(showtime -> date == null || showtime.getStartTime().toLocalDate().equals(date))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ShowtimeResponse create(ShowtimeRequest request) {
        Movie movie = movies.require(request.movieId());
        Room room = cinemas.requireRoom(request.roomId());
        if (!room.isActive() || !room.getCinema().isActive()) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "ROOM_INACTIVE",
                    "Không thể tạo suất chiếu cho rạp hoặc phòng đang ngừng hoạt động.");
        }
        LocalDateTime endTime = request.startTime().plusMinutes(movie.getDurationMinutes());

        if (showtimes.hasConflict(room.getId(), request.startTime(), endTime, null)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "SHOWTIME_CONFLICT",
                    "Phòng chiếu đã có suất khác trong khoảng thời gian này.");
        }

        Showtime showtime = new Showtime();
        showtime.setMovie(movie);
        showtime.setRoom(room);
        showtime.setStartTime(request.startTime());
        showtime.setEndTime(endTime);
        BigDecimal basePrice = request.basePrice() == null
                ? pricing.basePrice(room, request.startTime())
                : request.basePrice();
        showtime.setBasePrice(basePrice);
        showtime = showtimes.save(showtime);

        for (Seat seat : seats.findAllByRoomIdOrderByRowIndexAscColumnIndexAsc(room.getId())) {
            if (!seat.isActive()) {
                continue;
            }
            ShowtimeSeat showtimeSeat = new ShowtimeSeat();
            showtimeSeat.setShowtime(showtime);
            showtimeSeat.setSeat(seat);
            showtimeSeat.setStatus(ShowtimeSeatStatus.AVAILABLE);
            showtimeSeats.save(showtimeSeat);
        }
        return toResponse(showtime);
    }

    @Transactional(readOnly = true)
    public SeatMapResponse seatMap(Long id) {
        Showtime showtime = require(id);
        List<SeatMapResponse.SeatItem> items = showtimeSeats
                .findAllByShowtimeIdOrderBySeatRowIndexAscSeatColumnIndexAsc(id)
                .stream()
                .map(showtimeSeat -> new SeatMapResponse.SeatItem(
                        showtimeSeat.getSeat().getId(),
                        showtimeSeat.getSeat().getSeatCode(),
                        showtimeSeat.getSeat().getType().name(),
                        showtimeSeat.getStatus().name(),
                        pricing.seatPrice(showtime, showtimeSeat),
                        showtimeSeat.getHoldExpiresAt()))
                .toList();
        return new SeatMapResponse(id, items);
    }

    @Transactional
    public void cancel(Long id) {
        Showtime showtime = require(id);
        showtime.setActive(false);
    }

    public Showtime require(Long id) {
        return showtimes.findById(id)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "SHOWTIME_NOT_FOUND",
                        "Không tìm thấy suất chiếu."));
    }

    public ShowtimeResponse toResponse(Showtime showtime) {
        return new ShowtimeResponse(
                showtime.getId(),
                showtime.getMovie().getId(),
                showtime.getMovie().getTitle(),
                showtime.getRoom().getCinema().getId(),
                showtime.getRoom().getCinema().getName(),
                showtime.getRoom().getId(),
                showtime.getRoom().getName(),
                showtime.getStartTime(),
                showtime.getEndTime(),
                showtime.getBasePrice(),
                showtime.isActive());
    }
}
