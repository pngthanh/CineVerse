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
import com.pngthanh.cineverse.showtime.repository.ShowtimeRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CinemaService {
    private static final int MIN_ROWS = 6;
    private static final int MIN_SEATS_PER_ROW = 6;

    private final CinemaRepository cinemas;
    private final RoomRepository rooms;
    private final SeatRepository seats;
    private final ShowtimeRepository showtimes;

    public CinemaService(
            CinemaRepository cinemas,
            RoomRepository rooms,
            SeatRepository seats,
            ShowtimeRepository showtimes) {
        this.cinemas = cinemas;
        this.rooms = rooms;
        this.seats = seats;
        this.showtimes = showtimes;
    }

    @Transactional(readOnly = true)
    public List<CinemaResponse> list() {
        return cinemas.findAllByActiveTrueOrderByName().stream()
                .map(cinema -> toResponse(cinema, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CinemaResponse> listAdmin() {
        return cinemas.findAllByOrderByName().stream()
                .map(cinema -> toResponse(cinema, true))
                .toList();
    }

    @Transactional(readOnly = true)
    public CinemaResponse get(Long id) {
        Cinema cinema = requireCinema(id);
        if (!cinema.isActive()) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "CINEMA_NOT_FOUND",
                    "Không tìm thấy rạp.");
        }
        return toResponse(cinema, false);
    }

    @Transactional
    public CinemaResponse create(CinemaRequest request) {
        Cinema cinema = new Cinema();
        cinema.setName(request.name().trim());
        cinema.setAddress(request.address().trim());
        cinema.setActive(request.active());
        return toResponse(cinemas.save(cinema), true);
    }

    @Transactional
    public CinemaResponse update(Long id, CinemaRequest request) {
        Cinema cinema = requireCinema(id);
        cinema.setName(request.name().trim());
        cinema.setAddress(request.address().trim());
        cinema.setActive(request.active());
        return toResponse(cinema, true);
    }

    @Transactional
    public void deactivateCinema(Long id) {
        Cinema cinema = requireCinema(id);
        if (showtimes.existsByRoomCinemaIdAndActiveTrueAndStartTimeAfter(
                id,
                LocalDateTime.now())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "CINEMA_HAS_FUTURE_SHOWTIMES",
                    "Rạp đang có suất chiếu tương lai. Hãy hủy hoặc chuyển các suất đó trước.");
        }
        cinema.setActive(false);
        for (Room room : rooms.findAllByCinemaIdOrderByName(id)) {
            room.setActive(false);
        }
        showtimes.findAllByRoomCinemaIdAndActiveTrueOrderByStartTime(id)
                .forEach(showtime -> showtime.setActive(false));
    }

    @Transactional
    public CinemaResponse.RoomResponse createRoom(Long cinemaId, RoomRequest request) {
        Cinema cinema = requireCinema(cinemaId);
        if (!cinema.isActive()) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "CINEMA_INACTIVE",
                    "Không thể thêm phòng vào rạp đang ngừng hoạt động.");
        }
        validateRoomRequest(request);
        if (rooms.existsByCinemaIdAndNameIgnoreCase(cinemaId, request.name().trim())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "ROOM_NAME_EXISTS",
                    "Tên phòng đã tồn tại trong rạp này.");
        }

        Room room = new Room();
        room.setCinema(cinema);
        applyRoomRequest(room, request);
        room = rooms.save(room);
        createSeats(room, request.rows(), request.seatsPerRow());
        return toRoomResponse(room);
    }

    @Transactional
    public CinemaResponse.RoomResponse updateRoom(Long roomId, RoomRequest request) {
        Room room = requireRoom(roomId);
        validateRoomRequest(request);
        if (rooms.existsByCinemaIdAndNameIgnoreCaseAndIdNot(
                room.getCinema().getId(),
                request.name().trim(),
                roomId)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "ROOM_NAME_EXISTS",
                    "Tên phòng đã tồn tại trong rạp này.");
        }
        if (request.active() && !room.getCinema().isActive()) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "CINEMA_INACTIVE",
                    "Hãy kích hoạt rạp trước khi kích hoạt lại phòng.");
        }

        int currentRows = resolveRows(room);
        int currentSeatsPerRow = resolveSeatsPerRow(room);
        boolean layoutChanged = currentRows != request.rows()
                || currentSeatsPerRow != request.seatsPerRow();
        if (layoutChanged && showtimes.existsByRoomId(roomId)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "ROOM_LAYOUT_LOCKED",
                    "Phòng đã có lịch chiếu nên không thể đổi số hàng/ghế. Bạn vẫn có thể sửa tên và giá.");
        }

        applyRoomRequest(room, request);
        if (layoutChanged) {
            seats.deleteAllByRoomId(roomId);
            seats.flush();
            createSeats(room, request.rows(), request.seatsPerRow());
        } else {
            normalizeSeatTypes(room, request.rows(), request.seatsPerRow());
        }
        return toRoomResponse(room);
    }

    @Transactional
    public void deactivateRoom(Long roomId) {
        Room room = requireRoom(roomId);
        if (showtimes.existsByRoomIdAndActiveTrueAndStartTimeAfter(
                roomId,
                LocalDateTime.now())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "ROOM_HAS_FUTURE_SHOWTIMES",
                    "Phòng đang có suất chiếu tương lai. Hãy hủy các suất đó trước.");
        }
        room.setActive(false);
        showtimes.findAllByRoomIdAndActiveTrueOrderByStartTime(roomId)
                .forEach(showtime -> showtime.setActive(false));
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
        return toResponse(cinema, true);
    }

    private CinemaResponse toResponse(Cinema cinema, boolean includeInactiveRooms) {
        List<Room> roomData = includeInactiveRooms
                ? rooms.findAllByCinemaIdOrderByName(cinema.getId())
                : rooms.findAllByCinemaIdAndActiveTrueOrderByName(cinema.getId());
        List<CinemaResponse.RoomResponse> roomResponses = roomData.stream()
                .map(this::toRoomResponse)
                .toList();

        return new CinemaResponse(
                cinema.getId(),
                cinema.getName(),
                cinema.getAddress(),
                cinema.isActive(),
                roomResponses);
    }

    private void validateRoomRequest(RoomRequest request) {
        if (request.rows() < MIN_ROWS || request.seatsPerRow() < MIN_SEATS_PER_ROW) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "ROOM_TOO_SMALL",
                    "Phòng phải có ít nhất 6 hàng và mỗi hàng ít nhất 6 ghế.");
        }
    }

    private void applyRoomRequest(Room room, RoomRequest request) {
        room.setName(request.name().trim());
        room.setRowCount(request.rows());
        room.setSeatsPerRow(request.seatsPerRow());
        room.setWeekdayBasePrice(request.weekdayBasePrice());
        room.setWeekendBasePrice(request.weekendBasePrice());
        room.setVipSurcharge(request.vipSurcharge());
        room.setActive(request.active());
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
                seat.setType(resolveSeatType(
                        rowIndex,
                        rowsCount,
                        seatNumber - 1,
                        seatsPerRow));
                seats.save(seat);
            }
        }
    }

    private void normalizeSeatTypes(Room room, int rowsCount, int seatsPerRow) {
        for (Seat seat : seats.findAllByRoomIdOrderByRowIndexAscColumnIndexAsc(room.getId())) {
            SeatType expected = resolveSeatType(
                    seat.getRowIndex(),
                    rowsCount,
                    seat.getColumnIndex(),
                    seatsPerRow);
            if (seat.getType() != expected) {
                seat.setType(expected);
            }
        }
    }

    private SeatType resolveSeatType(
            int rowIndex,
            int rowsCount,
            int columnIndex,
            int seatsPerRow) {
        boolean vipRow = rowIndex >= 2 && rowIndex < rowsCount - 2;
        boolean vipColumn = columnIndex >= 2 && columnIndex < seatsPerRow - 2;
        return vipRow && vipColumn ? SeatType.VIP : SeatType.NORMAL;
    }

    private int resolveRows(Room room) {
        if (room.getRowCount() != null) {
            return room.getRowCount();
        }
        return seats.findAllByRoomIdOrderByRowIndexAscColumnIndexAsc(room.getId()).stream()
                .mapToInt(Seat::getRowIndex)
                .max()
                .orElse(MIN_ROWS - 1) + 1;
    }

    private int resolveSeatsPerRow(Room room) {
        if (room.getSeatsPerRow() != null) {
            return room.getSeatsPerRow();
        }
        return seats.findAllByRoomIdOrderByRowIndexAscColumnIndexAsc(room.getId()).stream()
                .mapToInt(Seat::getColumnIndex)
                .max()
                .orElse(MIN_SEATS_PER_ROW - 1) + 1;
    }

    private CinemaResponse.RoomResponse toRoomResponse(Room room) {
        List<Seat> roomSeats = seats.findAllByRoomIdOrderByRowIndexAscColumnIndexAsc(room.getId());
        int rowsCount = room.getRowCount() == null ? resolveRows(room) : room.getRowCount();
        int seatsPerRow = room.getSeatsPerRow() == null
                ? resolveSeatsPerRow(room)
                : room.getSeatsPerRow();
        int vipCount = (int) roomSeats.stream()
                .filter(seat -> seat.getType() == SeatType.VIP)
                .count();
        return new CinemaResponse.RoomResponse(
                room.getId(),
                room.getName(),
                room.isActive(),
                rowsCount,
                seatsPerRow,
                roomSeats.size(),
                vipCount,
                room.getWeekdayBasePrice(),
                room.getWeekendBasePrice(),
                room.getVipSurcharge());
    }
}
