package com.pngthanh.cineverse.config;

import com.pngthanh.cineverse.cinema.entity.Cinema;
import com.pngthanh.cineverse.cinema.entity.Room;
import com.pngthanh.cineverse.cinema.entity.Seat;
import com.pngthanh.cineverse.cinema.repository.CinemaRepository;
import com.pngthanh.cineverse.cinema.repository.RoomRepository;
import com.pngthanh.cineverse.cinema.repository.SeatRepository;
import com.pngthanh.cineverse.common.enums.MovieStatus;
import com.pngthanh.cineverse.common.enums.Role;
import com.pngthanh.cineverse.common.enums.SeatType;
import com.pngthanh.cineverse.concession.entity.ConcessionItem;
import com.pngthanh.cineverse.concession.repository.ConcessionItemRepository;
import com.pngthanh.cineverse.movie.entity.Movie;
import com.pngthanh.cineverse.movie.repository.MovieRepository;
import com.pngthanh.cineverse.showtime.entity.Showtime;
import com.pngthanh.cineverse.showtime.entity.ShowtimeSeat;
import com.pngthanh.cineverse.showtime.repository.ShowtimeRepository;
import com.pngthanh.cineverse.showtime.repository.ShowtimeSeatRepository;
import com.pngthanh.cineverse.user.entity.User;
import com.pngthanh.cineverse.user.repository.UserRepository;
import com.pngthanh.cineverse.voucher.entity.Voucher;
import com.pngthanh.cineverse.voucher.repository.VoucherRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataInitializer implements CommandLineRunner {
    private static final int DEFAULT_ROWS = 8;
    private static final int DEFAULT_SEATS_PER_ROW = 10;
    private static final String DEMO_TRAILER = "https://www.youtube.com/watch?v=0H_mDKTRVBQ";

    private final UserRepository users;
    private final MovieRepository movies;
    private final CinemaRepository cinemas;
    private final RoomRepository rooms;
    private final SeatRepository seats;
    private final ShowtimeRepository showtimes;
    private final ShowtimeSeatRepository showtimeSeats;
    private final PasswordEncoder encoder;
    private final VoucherRepository vouchers;
    private final ConcessionItemRepository concessionItems;

    public DataInitializer(UserRepository users, MovieRepository movies, CinemaRepository cinemas,
            RoomRepository rooms, SeatRepository seats, ShowtimeRepository showtimes,
            ShowtimeSeatRepository showtimeSeats, PasswordEncoder encoder, VoucherRepository vouchers,
            ConcessionItemRepository concessionItems) {
        this.users = users;
        this.movies = movies;
        this.cinemas = cinemas;
        this.rooms = rooms;
        this.seats = seats;
        this.showtimes = showtimes;
        this.showtimeSeats = showtimeSeats;
        this.encoder = encoder;
        this.vouchers = vouchers;
        this.concessionItems = concessionItems;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedUsers();
        seedVouchers();
        seedConcessionItems();
        List<Movie> seededMovies = seedMovies();
        Room room = ensureCinemaAndRoom();
        normalizeRoomConfiguration();
        ensureSeats(room);
        normalizeExistingRoomSeats();
        if (showtimes.count() < 6) {
            seedShowtimes(seededMovies, room);
        }
    }

    private void seedUsers() {
        if (users.findByEmailIgnoreCase("admin@cineverse.vn").isEmpty()) {
            User admin = new User();
            admin.setFullName("Quản trị CineVerse");
            admin.setEmail("admin@cineverse.vn");
            admin.setPasswordHash(encoder.encode("Admin@123"));
            admin.setRole(Role.ADMIN);
            users.save(admin);
        }
        if (users.findByEmailIgnoreCase("customer@cineverse.vn").isEmpty()) {
            User customer = new User();
            customer.setFullName("Khách hàng Demo");
            customer.setEmail("customer@cineverse.vn");
            customer.setPasswordHash(encoder.encode("Customer@123"));
            users.save(customer);
        }
    }

    private void seedVouchers() {
        if (vouchers.findByCodeIgnoreCase("CINE10").isEmpty()) {
            Voucher voucher = new Voucher();
            voucher.setCode("CINE10");
            voucher.setDiscountPercent(new BigDecimal("10"));
            voucher.setMinOrderAmount(new BigDecimal("100000"));
            voucher.setMaxDiscountAmount(new BigDecimal("50000"));
            voucher.setStartsAt(LocalDateTime.now().minusDays(1));
            voucher.setExpiresAt(LocalDateTime.now().plusMonths(6));
            vouchers.save(voucher);
        }
        if (vouchers.findByCodeIgnoreCase("WELCOME20").isEmpty()) {
            Voucher voucher = new Voucher();
            voucher.setCode("WELCOME20");
            voucher.setDiscountPercent(new BigDecimal("20"));
            voucher.setMinOrderAmount(new BigDecimal("150000"));
            voucher.setMaxDiscountAmount(new BigDecimal("70000"));
            voucher.setStartsAt(LocalDateTime.now().minusDays(1));
            voucher.setExpiresAt(LocalDateTime.now().plusMonths(3));
            vouchers.save(voucher);
        }
    }

    private void seedConcessionItems() {
        upsertConcessionItem(
                "Bắp rang bơ",
                "Bắp rang vị bơ truyền thống, cỡ vừa.",
                new BigDecimal("55000"));
        upsertConcessionItem(
                "Nước ngọt",
                "Nước ngọt có ga cỡ vừa.",
                new BigDecimal("35000"));
        upsertConcessionItem(
                "Combo CineVerse",
                "1 bắp rang bơ + 2 nước ngọt, phù hợp cho hai người.",
                new BigDecimal("99000"));
    }

    private void upsertConcessionItem(String name, String description, BigDecimal price) {
        ConcessionItem item = concessionItems.findByNameIgnoreCase(name).orElseGet(ConcessionItem::new);
        item.setName(name);
        item.setDescription(description);
        item.setPrice(price);
        item.setActive(true);
        concessionItems.save(item);
    }

    private List<Movie> seedMovies() {
        List<Movie> result = new ArrayList<>();
        result.add(upsertMovie("Hành Trình Vượt Không Gian", "Một nhóm phi hành gia bước vào chuyến đi vượt giới hạn để tìm hy vọng mới cho nhân loại.", "Khoa học viễn tưởng, Phiêu lưu", 169, "T13", MovieStatus.NOW_SHOWING, "CineVerse Studio", "Diễn viên A, Diễn viên B", null, new BigDecimal("9.2"), 1840, 12450L));
        result.add(upsertMovie("Sa Mạc Đỏ", "Cuộc chiến sinh tồn và quyền lực trên một hành tinh khắc nghiệt.", "Khoa học viễn tưởng, Hành động", 156, "T16", MovieStatus.NOW_SHOWING, "Denis Demo", "Diễn viên C, Diễn viên D", null, new BigDecimal("8.8"), 1320, 9820L));
        result.add(upsertMovie("Người Nhện: Khởi Đầu Mới", "Một chương mới đầy tốc độ, lựa chọn và trách nhiệm của người hùng thành phố.", "Hành động, Phiêu lưu", 142, "T13", MovieStatus.NOW_SHOWING, "Marvel Demo", "Peter Parker, MJ", "/posters/spider-man.webp", new BigDecimal("9.4"), 2450, 15120L));
        result.add(upsertMovie("Đêm Cuối Ở Sài Gòn", "Một cuộc gặp bất ngờ kéo hai người xa lạ vào hành trình xuyên đêm.", "Tâm lý, Lãng mạn", 118, "T16", MovieStatus.NOW_SHOWING, "CineVerse Studio", "Diễn viên E, Diễn viên F", null, new BigDecimal("8.6"), 920, 7210L));
        result.add(upsertMovie("Thành Phố Mộng Mơ", "Một câu chuyện nhẹ nhàng về tuổi trẻ, âm nhạc và lựa chọn.", "Tâm lý, Âm nhạc", 128, "T13", MovieStatus.COMING_SOON, "CineVerse Studio", "Diễn viên G, Diễn viên H", null, new BigDecimal("8.9"), 760, 0L));
        result.add(upsertMovie("Thư Tình Gửi Ngoại", "Những ký ức gia đình được mở ra từ một lá thư cũ và chuyến trở về quê nhà.", "Gia đình, Tâm lý", 124, "T13", MovieStatus.COMING_SOON, "Vietnam Demo", "Diễn viên I, Diễn viên K", "/posters/thu-tinh-gui-ngoai.webp", new BigDecimal("9.1"), 1120, 0L));
        result.add(upsertMovie("Mật Mã Đại Dương", "Đội thám hiểm phát hiện tín hiệu bí ẩn dưới đáy đại dương sâu.", "Bí ẩn, Phiêu lưu", 135, "T16", MovieStatus.COMING_SOON, "CineVerse Studio", "Diễn viên L, Diễn viên M", null, new BigDecimal("8.7"), 530, 0L));
        result.add(upsertMovie("Robot Nhỏ Và Bầu Trời", "Một robot sửa chữa bé nhỏ bắt đầu chuyến phiêu lưu tìm lại bầu trời xanh.", "Hoạt hình, Gia đình", 101, "P", MovieStatus.COMING_SOON, "Animation Lab", "Lồng tiếng CineVerse", null, new BigDecimal("9.0"), 880, 0L));
        return result;
    }

    private Movie upsertMovie(String title, String description, String genres, int duration, String ageRating,
            MovieStatus status, String director, String castNames, String posterUrl, BigDecimal rating,
            int reviewCount, long ticketsSold) {
        Movie movie = movies.findByTitle(title).orElseGet(Movie::new);
        movie.setTitle(title);
        movie.setDescription(description);
        movie.setGenres(genres);
        movie.setDurationMinutes(duration);
        movie.setAgeRating(ageRating);
        movie.setReleaseDate(status == MovieStatus.COMING_SOON
                ? LocalDate.now().plusDays(30)
                : LocalDate.now().minusDays(7));
        movie.setDirector(director);
        movie.setCastNames(castNames);
        movie.setPosterUrl(posterUrl);
        movie.setTrailerUrl(DEMO_TRAILER);
        movie.setRatingAverage(rating);
        movie.setReviewCount(reviewCount);
        long currentTicketsSold = movie.getTicketsSold() == null ? 0L : movie.getTicketsSold();
        movie.setTicketsSold(Math.max(currentTicketsSold, ticketsSold));
        movie.setStatus(status);
        return movies.save(movie);
    }

    private Room ensureCinemaAndRoom() {
        Cinema cinema = cinemas.findAll().stream().findFirst().orElseGet(() -> {
            Cinema c = new Cinema();
            c.setName("CineVerse Ninh Kiều");
            c.setAddress("Ninh Kiều, Cần Thơ");
            return cinemas.save(c);
        });
        return rooms.findAll().stream().filter(r -> r.getCinema().getId().equals(cinema.getId())).findFirst().orElseGet(() -> {
            Room room = new Room();
            room.setCinema(cinema);
            room.setName("Phòng 01");
            room.setRowCount(DEFAULT_ROWS);
            room.setSeatsPerRow(DEFAULT_SEATS_PER_ROW);
            room.setWeekdayBasePrice(new BigDecimal("70000"));
            room.setWeekendBasePrice(new BigDecimal("100000"));
            room.setVipSurcharge(new BigDecimal("20000"));
            return rooms.save(room);
        });
    }

    private void normalizeRoomConfiguration() {
        for (Room room : rooms.findAll()) {
            List<Seat> roomSeats = seats.findAllByRoomIdOrderByRowIndexAscColumnIndexAsc(room.getId());
            int rowsCount = roomSeats.isEmpty()
                    ? DEFAULT_ROWS
                    : roomSeats.stream().mapToInt(Seat::getRowIndex).max().orElse(DEFAULT_ROWS - 1) + 1;
            int seatsPerRow = roomSeats.isEmpty()
                    ? DEFAULT_SEATS_PER_ROW
                    : roomSeats.stream().mapToInt(Seat::getColumnIndex).max()
                            .orElse(DEFAULT_SEATS_PER_ROW - 1) + 1;
            room.setRowCount(Math.max(6, rowsCount));
            room.setSeatsPerRow(Math.max(6, seatsPerRow));
            if (room.getWeekdayBasePrice() == null) {
                room.setWeekdayBasePrice(new BigDecimal("70000"));
            }
            if (room.getWeekendBasePrice() == null) {
                room.setWeekendBasePrice(new BigDecimal("100000"));
            }
            if (room.getVipSurcharge() == null) {
                room.setVipSurcharge(new BigDecimal("20000"));
            }
        }
    }

    private void ensureSeats(Room room) {
        List<Seat> existingSeats = seats.findAllByRoomIdOrderByRowIndexAscColumnIndexAsc(room.getId());
        if (!existingSeats.isEmpty()) {
            for (Seat seat : existingSeats) {
                SeatType expectedType = isVipSeat(
                        seat.getRowIndex(),
                        seat.getColumnIndex(),
                        room.getRowCount(),
                        room.getSeatsPerRow())
                        ? SeatType.VIP
                        : SeatType.NORMAL;
                if (seat.getType() != expectedType) {
                    seat.setType(expectedType);
                    seats.save(seat);
                }
            }
            return;
        }
        for (int rowIndex = 0; rowIndex < room.getRowCount(); rowIndex++) {
            char rowLetter = (char) ('A' + rowIndex);
            for (int seatNumber = 1; seatNumber <= room.getSeatsPerRow(); seatNumber++) {
                Seat seat = new Seat();
                seat.setRoom(room);
                seat.setSeatCode(rowLetter + String.valueOf(seatNumber));
                seat.setRowIndex(rowIndex);
                seat.setColumnIndex(seatNumber - 1);
                seat.setType(isVipSeat(
                        rowIndex,
                        seatNumber - 1,
                        room.getRowCount(),
                        room.getSeatsPerRow())
                        ? SeatType.VIP
                        : SeatType.NORMAL);
                seats.save(seat);
            }
        }
    }

    private void normalizeExistingRoomSeats() {
        for (Room room : rooms.findAll()) {
            List<Seat> roomSeats = seats.findAllByRoomIdOrderByRowIndexAscColumnIndexAsc(room.getId());
            if (roomSeats.isEmpty()) {
                continue;
            }

            int rowsCount = roomSeats.stream()
                    .mapToInt(Seat::getRowIndex)
                    .max()
                    .orElse(0) + 1;
            int seatsPerRow = roomSeats.stream()
                    .mapToInt(Seat::getColumnIndex)
                    .max()
                    .orElse(0) + 1;
            room.setRowCount(rowsCount);
            room.setSeatsPerRow(seatsPerRow);

            for (Seat seat : roomSeats) {
                SeatType expectedType = isVipSeat(
                        seat.getRowIndex(),
                        seat.getColumnIndex(),
                        rowsCount,
                        seatsPerRow)
                        ? SeatType.VIP
                        : SeatType.NORMAL;
                if (seat.getType() != expectedType) {
                    seat.setType(expectedType);
                    seats.save(seat);
                }
            }
        }
    }

    private boolean isVipSeat(
            int rowIndex,
            int columnIndex,
            int rowsCount,
            int seatsPerRow) {
        boolean hasVipCore = rowsCount >= 6 && seatsPerRow >= 6;
        boolean vipRow = rowIndex >= 2 && rowIndex < rowsCount - 2;
        boolean vipColumn = columnIndex >= 2 && columnIndex < seatsPerRow - 2;
        return hasVipCore && vipRow && vipColumn;
    }

    private void seedShowtimes(List<Movie> movieList, Room room) {
        List<Seat> roomSeats = seats.findAllByRoomIdOrderByRowIndexAscColumnIndexAsc(room.getId());
        int[] hours = {10, 13, 16, 19, 22};
        int index = 0;
        for (Movie movie : movieList.stream().filter(m -> m.getStatus() == MovieStatus.NOW_SHOWING).toList()) {
            for (int day = 1; day <= 3; day++) {
                LocalDateTime start = LocalDate.now().plusDays(day).atTime(hours[(index + day) % hours.length], 0);
                Showtime showtime = new Showtime();
                showtime.setMovie(movie);
                showtime.setRoom(room);
                showtime.setStartTime(start);
                showtime.setEndTime(start.plusMinutes(movie.getDurationMinutes()));
                boolean weekend = start.getDayOfWeek().getValue() >= 6;
                BigDecimal basePrice = weekend
                        ? room.getWeekendBasePrice()
                        : room.getWeekdayBasePrice();
                showtime.setBasePrice(basePrice);
                showtime = showtimes.save(showtime);
                for (Seat seat : roomSeats) {
                    ShowtimeSeat item = new ShowtimeSeat();
                    item.setShowtime(showtime);
                    item.setSeat(seat);
                    showtimeSeats.save(item);
                }
            }
            index++;
        }
    }
}
