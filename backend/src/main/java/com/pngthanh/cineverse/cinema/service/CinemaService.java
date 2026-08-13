package com.pngthanh.cineverse.cinema.service;

import com.pngthanh.cineverse.cinema.dto.CinemaRequest;
import com.pngthanh.cineverse.cinema.dto.CinemaResponse;
import com.pngthanh.cineverse.cinema.dto.RoomRequest;
import com.pngthanh.cineverse.cinema.entity.Cinema;
import com.pngthanh.cineverse.cinema.entity.Room;
import com.pngthanh.cineverse.cinema.entity.Seat;
import com.pngthanh.cineverse.cinema.repository.CinemaRepository;
import com.pngthanh.cineverse.cinema.repository.RoomRepository;
import com.pngthanh.cineverse.cinema.repository.SeatRepository;
import com.pngthanh.cineverse.common.enums.SeatType;
import com.pngthanh.cineverse.common.exception.ApiException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CinemaService {
    private final CinemaRepository cinemas;
    private final RoomRepository rooms;
    private final SeatRepository seats;

    public CinemaService(
            CinemaRepository cinemas,
            RoomRepository rooms,
            SeatRepository seats) {
        this.cinemas = cinemas;
        this.rooms = rooms;
        this.seats = seats;
    }

    @Transactional(readOnly = true)
    public List<CinemaResponse> list() {
        return cinemas.findAllByActiveTrueOrderByName().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CinemaResponse get(Long id) {
        return toResponse(requireCinema(id));
    }

    @Transactional
    public CinemaResponse create(CinemaRequest request) {
        Cinema cinema = new Cinema();
        cinema.setName(request.name());
        cinema.setAddress(request.address());
        cinema.setActive(request.active());
        return toResponse(cinemas.save(cinema));
    }

    @Transactional
    public CinemaResponse update(Long id, CinemaRequest request) {
        Cinema cinema = requireCinema(id);
        cinema.setName(request.name());
        cinema.setAddress(request.address());
        cinema.setActive(request.active());
        return toResponse(cinema);
    }

    @Transactional
    public CinemaResponse.RoomResponse createRoom(Long cinemaId, RoomRequest request) {
        Cinema cinema = requireCinema(cinemaId);
        Room room = new Room();
        room.setCinema(cinema);
        room.setName(request.name());
        room = rooms.save(room);

        createSeats(room, request.rows(), request.seatsPerRow());
        int seatCount = request.rows() * request.seatsPerRow();
        return new CinemaResponse.RoomResponse(
                room.getId(),
                room.getName(),
                room.isActive(),
                seatCount);
    }

    public Cinema requireCinema(Long id) {
        return cinemas.findById(id)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "CINEMA_NOT_FOUND",
                        "Không tìm thấy rạp."));
    }

    public Room requireRoom(Long id) {
        return rooms.findById(id)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "ROOM_NOT_FOUND",
                        "Không tìm thấy phòng chiếu."));
    }

    public CinemaResponse toResponse(Cinema cinema) {
        List<CinemaResponse.RoomResponse> roomResponses = rooms
                .findAllByCinemaIdOrderByName(cinema.getId()).stream()
                .map(this::toRoomResponse)
                .toList();

        return new CinemaResponse(
                cinema.getId(),
                cinema.getName(),
                cinema.getAddress(),
                cinema.isActive(),
                roomResponses);
    }

    private void createSeats(Room room, int rowsCount, int seatsPerRow) {
        for (int rowIndex = 0; rowIndex < rowsCount; rowIndex++) {
            char rowLetter = (char) ('A' + rowIndex);
            for (int seatNumber = 1; seatNumber <= seatsPerRow; seatNumber++) {
                Seat seat = new Seat();
                seat.setRoom(room);
                seat.setSeatCode(rowLetter + String.valueOf(seatNumber));
                seat.setRowIndex(rowIndex);
                seat.setColumnIndex(seatNumber - 1);
                seat.setType(resolveSeatType(rowIndex, rowsCount, seatNumber - 1, seatsPerRow));
                seats.save(seat);
            }
        }
    }

    private SeatType resolveSeatType(
            int rowIndex,
            int rowsCount,
            int columnIndex,
            int seatsPerRow) {
        boolean hasVipCore = rowsCount >= 5 && seatsPerRow >= 5;
        boolean vipRow = rowIndex >= 2 && rowIndex < rowsCount - 2;
        boolean vipColumn = columnIndex >= 2 && columnIndex < seatsPerRow - 2;
        return hasVipCore && vipRow && vipColumn ? SeatType.VIP : SeatType.NORMAL;
    }

    private CinemaResponse.RoomResponse toRoomResponse(Room room) {
        int seatCount = seats.findAllByRoomIdOrderByRowIndexAscColumnIndexAsc(room.getId()).size();
        return new CinemaResponse.RoomResponse(
                room.getId(),
                room.getName(),
                room.isActive(),
                seatCount);
    }
}
