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
import com.pngthanh.cineverse.common.enums.VoucherAudience;
import com.pngthanh.cineverse.common.enums.VoucherDiscountType;
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
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataInitializer implements CommandLineRunner {
    private static final String DATASET_VERSION = "rich-demo-2026-08-v1";
    private static final String DEMO_TRAILER = "https://www.youtube.com/watch?v=0H_mDKTRVBQ";

    private final UserRepository users;
    private final MovieRepository movies;
    private final CinemaRepository cinemas;
    private final RoomRepository rooms;
    private final SeatRepository seats;
    private final ShowtimeRepository showtimes;
    private final ShowtimeSeatRepository showtimeSeats;
    private final VoucherRepository vouchers;
    private final ConcessionItemRepository concessionItems;
    private final PasswordEncoder encoder;
    private final JdbcTemplate jdbc;
    private final Clock clock;

    public DataInitializer(
            UserRepository users,
            MovieRepository movies,
            CinemaRepository cinemas,
            RoomRepository rooms,
            SeatRepository seats,
            ShowtimeRepository showtimes,
            ShowtimeSeatRepository showtimeSeats,
            VoucherRepository vouchers,
            ConcessionItemRepository concessionItems,
            PasswordEncoder encoder,
            JdbcTemplate jdbc,
            Clock clock) {
        this.users = users;
        this.movies = movies;
        this.cinemas = cinemas;
        this.rooms = rooms;
        this.seats = seats;
        this.showtimes = showtimes;
        this.showtimeSeats = showtimeSeats;
        this.vouchers = vouchers;
        this.concessionItems = concessionItems;
        this.encoder = encoder;
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (resetDemoDataOnce()) {
            seedUsers();
            seedVouchers();
            seedConcessions();
            List<Movie> movieList = seedMovies();
            List<Room> roomList = seedCinemasAndRooms();
            seedShowtimes(movieList, roomList);
            return;
        }
        ensureDemoUsernames();
    }

    private boolean resetDemoDataOnce() {
        jdbc.execute("create table if not exists app_seed_state (seed_key varchar(100) primary key, seed_value varchar(200) not null)");
        List<String> versions = jdbc.query(
                "select seed_value from app_seed_state where seed_key = 'dataset_version'",
                (rs, rowNum) -> rs.getString(1));
        if (!versions.isEmpty() && DATASET_VERSION.equals(versions.get(0))) {
            return false;
        }
        jdbc.execute("""
                truncate table
                    booking_concessions,
                    booking_seats,
                    tickets,
                    payments,
                    bookings,
                    showtime_seats,
                    showtimes,
                    seats,
                    rooms,
                    cinemas,
                    movies,
                    saved_vouchers,
                    voucher_assignments,
                    vouchers,
                    concession_items,
                    users
                restart identity cascade
                """);
        jdbc.update(
                "insert into app_seed_state(seed_key, seed_value) values ('dataset_version', ?) "
                        + "on conflict (seed_key) do update set seed_value = excluded.seed_value",
                DATASET_VERSION);
        return true;
    }

    private void ensureDemoUsernames() {
        users.findByEmailIgnoreCase("admin@cineverse.vn").ifPresent(user -> {
            if (user.getUsername() == null || user.getUsername().isBlank()) {
                user.setUsername("admin");
            }
        });
        users.findByEmailIgnoreCase("customer@cineverse.vn").ifPresent(user -> {
            if (user.getUsername() == null || user.getUsername().isBlank()) {
                user.setUsername("customer");
            }
        });
    }

    private void seedUsers() {
        User admin = new User();
        admin.setFullName("Quản trị CineVerse");
        admin.setEmail("admin@cineverse.vn");
        admin.setUsername("admin");
        admin.setPasswordHash(encoder.encode("Admin@123"));
        admin.setRole(Role.ADMIN);
        users.save(admin);

        User customer = new User();
        customer.setFullName("Khách hàng Demo");
        customer.setEmail("customer@cineverse.vn");
        customer.setUsername("customer");
        customer.setPasswordHash(encoder.encode("Customer@123"));
        users.save(customer);
    }

    private void seedVouchers() {
        createVoucher("CINE10", "10", "100000", "50000", -2, 180);
        createVoucher("WELCOME20", "20", "150000", "70000", -2, 90);
        createVoucher("WEEKEND15", "15", "200000", "80000", -2, 60);
    }

    private void createVoucher(
            String code,
            String percent,
            String minOrder,
            String maxDiscount,
            int startOffsetDays,
            int expiryOffsetDays) {
        Voucher voucher = new Voucher();
        BigDecimal percentValue = new BigDecimal(percent);
        voucher.setCode(code);
        voucher.setTitle("Ưu đãi " + code);
        voucher.setDescription("Voucher demo dành cho khách hàng CineVerse.");
        voucher.setDiscountType(VoucherDiscountType.PERCENT);
        voucher.setDiscountValue(percentValue);
        voucher.setDiscountPercent(percentValue);
        voucher.setMinOrderAmount(new BigDecimal(minOrder));
        voucher.setMaxDiscountAmount(new BigDecimal(maxDiscount));
        voucher.setStartsAt(LocalDateTime.now(clock).plusDays(startOffsetDays));
        voucher.setExpiresAt(LocalDateTime.now(clock).plusDays(expiryOffsetDays));
        voucher.setAudience(VoucherAudience.ALL);
        voucher.setPublicVisible(true);
        voucher.setPerUserLimit(1);
        vouchers.save(voucher);
    }

    private void seedConcessions() {
        createConcession("Bắp rang bơ", "Bắp rang vị bơ truyền thống, cỡ vừa.", "55000");
        createConcession("Bắp caramel", "Bắp rang phủ caramel, cỡ vừa.", "65000");
        createConcession("Nước ngọt", "Nước ngọt có ga cỡ vừa.", "35000");
        createConcession("Nước suối", "Nước suối 500 ml.", "25000");
        createConcession("Combo CineVerse", "1 bắp rang bơ + 2 nước ngọt.", "99000");
        createConcession("Combo Couple", "1 bắp caramel lớn + 2 nước ngọt lớn.", "129000");
    }

    private void createConcession(String name, String description, String price) {
        ConcessionItem item = new ConcessionItem();
        item.setName(name);
        item.setDescription(description);
        item.setPrice(new BigDecimal(price));
        item.setActive(true);
        concessionItems.save(item);
    }

    private List<Movie> seedMovies() {
        LocalDate today = LocalDate.now(clock);
        List<Movie> result = new ArrayList<>();
        result.add(createMovie("Người Nhện: Khởi Đầu Mới", "Một người hùng trẻ phải cân bằng trách nhiệm, gia đình và những lựa chọn thay đổi cả thành phố.", "Hành động, Phiêu lưu", 142, "T13", MovieStatus.NOW_SHOWING, "CineVerse Studio", "Peter, MJ, Miles", "/posters/demo/spiderman.jpg", today.minusDays(18), today.plusDays(24)));
        result.add(createMovie("Đại Chiến Titan: Hồi Kết", "Trận chiến cuối cùng mở ra khi những bí mật sau bức tường được phơi bày.", "Hoạt hình, Hành động", 148, "T16", MovieStatus.NOW_SHOWING, "Animation Studio", "Dàn lồng tiếng CineVerse", "/posters/demo/poster_aot.jpg", today.minusDays(12), today.plusDays(20)));
        result.add(createMovie("Hành Trình Của Moana", "Một chuyến đi vượt đại dương để tìm lại niềm tin và kết nối với cội nguồn.", "Hoạt hình, Gia đình", 112, "P", MovieStatus.NOW_SHOWING, "Ocean Studio", "Dàn lồng tiếng CineVerse", "/posters/demo/poster_hanh_trinh_cua_moana_.jpg", today.minusDays(8), today.plusDays(30)));
        result.add(createMovie("Ngày Tàn Của Phố Oak", "Một khu phố bình yên bị cuốn vào chuỗi sự kiện bí ẩn trong một đêm mất điện.", "Kinh dị, Bí ẩn", 126, "T18", MovieStatus.NOW_SHOWING, "Night Lab", "Diễn viên A, Diễn viên B", "/posters/demo/poster_ngay_tan_cua_pho_oak_2.jpg", today.minusDays(6), today.plusDays(21)));
        result.add(createMovie("Uma Musume: Kỷ Nguyên Mới", "Những vận động viên trẻ theo đuổi giấc mơ chiến thắng trên đường đua lớn nhất.", "Hoạt hình, Thể thao", 108, "P", MovieStatus.NOW_SHOWING, "Derby Pictures", "Dàn lồng tiếng CineVerse", "/posters/demo/poster_umamusume_pretty_derby_khoi_dau_ky_nguyen_moi_5.jpg", today.minusDays(4), today.plusDays(26)));
        result.add(createMovie("Kamen Rider Agito", "Một người hùng thức tỉnh sức mạnh mới để đối đầu mối đe dọa bí ẩn.", "Hành động, Khoa học viễn tưởng", 119, "T13", MovieStatus.NOW_SHOWING, "Hero Works", "Diễn viên C, Diễn viên D", "/posters/demo/agito_adaptation_main_website_470_x_700-.jpg", today.minusDays(15), today.plusDays(14)));
        result.add(createMovie("Shin: Huyền Thoại Trở Lại", "Một hành trình mới bắt đầu khi người hùng cũ buộc phải quay lại chiến trường.", "Hành động, Chính kịch", 133, "T16", MovieStatus.NOW_SHOWING, "Shin Pictures", "Diễn viên E, Diễn viên F", "/posters/demo/470wx700h-shin.jpg", today.minusDays(9), today.plusDays(18)));
        result.add(createMovie("Đội Cứu Hộ Thành Phố", "Những người bạn nhỏ cùng nhau giải cứu thành phố khỏi một sự cố lớn.", "Hoạt hình, Gia đình", 96, "P", MovieStatus.NOW_SHOWING, "Family Studio", "Dàn lồng tiếng CineVerse", "/posters/demo/470x700-paw.jpg", today.minusDays(3), today.plusDays(35)));
        result.add(createMovie("Cánh Đồng Ngược Gió", "Một người trẻ trở về quê nhà và tìm thấy câu trả lời cho những điều từng bỏ lỡ.", "Tâm lý, Gia đình", 121, "T13", MovieStatus.NOW_SHOWING, "Lotus Film", "Diễn viên G, Diễn viên H", "/posters/demo/c_n_ng_ng_ng_o_poster_social.jpg", today.minusDays(10), today.plusDays(20)));
        result.add(createMovie("Chuyến Tàu 03512", "Những hành khách xa lạ bị mắc kẹt trên chuyến tàu đêm đầy bí mật.", "Bí ẩn, Giật gân", 128, "T16", MovieStatus.NOW_SHOWING, "Railway Films", "Diễn viên I, Diễn viên K", "/posters/demo/HO00003512.jpg", today.minusDays(5), today.plusDays(25)));
        result.add(createMovie("Mật Mã 03531", "Một nhóm chuyên gia lần theo những ký hiệu dẫn tới âm mưu bị che giấu nhiều năm.", "Trinh thám, Hành động", 137, "T16", MovieStatus.NOW_SHOWING, "Cipher Studio", "Diễn viên L, Diễn viên M", "/posters/demo/HO00003531.jpg", today.minusDays(7), today.plusDays(19)));
        result.add(createMovie("Bầu Trời Sau Cơn Mưa", "Hai con người gặp nhau giữa những ngày khó khăn và cùng tìm lại hy vọng.", "Tình cảm, Tâm lý", 116, "T13", MovieStatus.NOW_SHOWING, "Blue Sky Film", "Diễn viên N, Diễn viên O", "/posters/demo/HO00003539.jpg", today.minusDays(2), today.plusDays(32)));
        result.add(createMovie("Ký Ức Mùa Hạ", "Một nhóm bạn trở lại nơi họ từng lớn lên và đối diện những ký ức chưa khép lại.", "Tâm lý, Thanh xuân", 124, "T13", MovieStatus.COMING_SOON, "Summer House", "Diễn viên P, Diễn viên Q", "/posters/demo/HO00003569.jpg", today.plusDays(5), today.plusDays(40)));
        result.add(createMovie("Hành Tinh Lạ", "Tín hiệu từ một hành tinh xa xôi kéo đoàn thám hiểm vào cuộc phiêu lưu chưa từng có.", "Khoa học viễn tưởng, Phiêu lưu", 151, "T13", MovieStatus.COMING_SOON, "Nova Studio", "Diễn viên R, Diễn viên S", "/posters/demo/HO00003591.jpg", today.plusDays(8), today.plusDays(45)));
        result.add(createMovie("Thành Phố Không Ngủ", "Một đêm duy nhất kết nối nhiều số phận trong thành phố rực sáng.", "Tâm lý, Tội phạm", 132, "T16", MovieStatus.COMING_SOON, "Metro Films", "Diễn viên T, Diễn viên U", "/posters/demo/HO00003623.jpg", today.plusDays(12), today.plusDays(48)));
        result.add(createMovie("Khi Chúng Ta Gặp Lại", "Một cuộc hội ngộ bất ngờ sau nhiều năm khiến mọi lựa chọn cũ được nhìn lại.", "Tình cảm, Tâm lý", 118, "T13", MovieStatus.COMING_SOON, "Heart Studio", "Diễn viên V, Diễn viên W", "/posters/demo/kijsada-teaser_poster-700x1000.jpg", today.plusDays(15), today.plusDays(50)));
        result.add(createMovie("Odyssey: Dấu Chân Hoang Dã", "Một hành trình băng qua vùng đất rộng lớn để đưa một sinh vật quý hiếm trở về nhà.", "Phiêu lưu, Gia đình", 127, "P", MovieStatus.COMING_SOON, "Odyssey Works", "Diễn viên X, Diễn viên Y", "/posters/demo/ody_horseposter_470x700.jpg", today.plusDays(18), today.plusDays(55)));
        result.add(createMovie("Ánh Sáng Cuối Đường", "Một cuộc chạy đua với thời gian khi cả thành phố mất liên lạc.", "Hành động, Giật gân", 139, "T16", MovieStatus.COMING_SOON, "Signal Pictures", "Diễn viên Z, Diễn viên AA", "/posters/demo/poster hien thi tren web (1)-1783996727902.avif", today.plusDays(20), today.plusDays(60)));
        return result;
    }

    private Movie createMovie(
            String title,
            String description,
            String genres,
            int duration,
            String ageRating,
            MovieStatus status,
            String director,
            String castNames,
            String posterUrl,
            LocalDate releaseDate,
            LocalDate endDate) {
        Movie movie = new Movie();
        movie.setTitle(title);
        movie.setDescription(description);
        movie.setGenres(genres);
        movie.setDurationMinutes(duration);
        movie.setAgeRating(ageRating);
        movie.setStatus(status);
        movie.setDirector(director);
        movie.setCastNames(castNames);
        movie.setPosterUrl(posterUrl);
        movie.setBackdropUrl(posterUrl);
        movie.setTrailerUrl(DEMO_TRAILER);
        movie.setReleaseDate(releaseDate);
        movie.setEndDate(endDate);
        movie.setTicketsSold(0L);
        return movies.save(movie);
    }

    private List<Room> seedCinemasAndRooms() {
        List<Room> result = new ArrayList<>();
        Cinema ninhKieu = createCinema("CineVerse Ninh Kiều", "12 Đại lộ Hòa Bình, Ninh Kiều, Cần Thơ");
        Cinema caiRang = createCinema("CineVerse Cái Răng", "88 Võ Nguyên Giáp, Cái Răng, Cần Thơ");
        Cinema binhThuy = createCinema("CineVerse Bình Thủy", "120 Cách Mạng Tháng Tám, Bình Thủy, Cần Thơ");
        Cinema oMon = createCinema("CineVerse Ô Môn", "35 Châu Văn Liêm, Ô Môn, Cần Thơ");
        Cinema thotNot = createCinema("CineVerse Thốt Nốt", "210 Quốc lộ 91, Thốt Nốt, Cần Thơ");

        result.add(createRoom(ninhKieu, "IMAX 01", 10, 14, "90000", "120000", "30000"));
        result.add(createRoom(ninhKieu, "Cinema 02", 8, 10, "70000", "100000", "20000"));
        result.add(createRoom(caiRang, "Premium 01", 9, 12, "80000", "110000", "25000"));
        result.add(createRoom(caiRang, "Cinema 02", 7, 10, "65000", "90000", "20000"));
        result.add(createRoom(binhThuy, "Cinema 01", 8, 12, "70000", "95000", "20000"));
        result.add(createRoom(binhThuy, "Mini 02", 6, 8, "60000", "80000", "15000"));
        result.add(createRoom(oMon, "Cinema 01", 8, 10, "65000", "90000", "20000"));
        result.add(createRoom(oMon, "Mini 02", 6, 6, "55000", "75000", "15000"));
        result.add(createRoom(thotNot, "Cinema 01", 7, 10, "60000", "85000", "18000"));
        result.add(createRoom(thotNot, "Cinema 02", 6, 8, "60000", "85000", "18000"));
        return result;
    }

    private Cinema createCinema(String name, String address) {
        Cinema cinema = new Cinema();
        cinema.setName(name);
        cinema.setAddress(address);
        cinema.setActive(true);
        return cinemas.save(cinema);
    }

    private Room createRoom(
            Cinema cinema,
            String name,
            int rowCount,
            int seatsPerRow,
            String weekday,
            String weekend,
            String vipSurcharge) {
        Room room = new Room();
        room.setCinema(cinema);
        room.setName(name);
        room.setActive(true);
        room.setRowCount(rowCount);
        room.setSeatsPerRow(seatsPerRow);
        room.setWeekdayBasePrice(new BigDecimal(weekday));
        room.setWeekendBasePrice(new BigDecimal(weekend));
        room.setVipSurcharge(new BigDecimal(vipSurcharge));
        room = rooms.save(room);
        createSeats(room);
        return room;
    }

    private void createSeats(Room room) {
        for (int row = 0; row < room.getRowCount(); row++) {
            for (int column = 0; column < room.getSeatsPerRow(); column++) {
                Seat seat = new Seat();
                seat.setRoom(room);
                seat.setRowIndex(row);
                seat.setColumnIndex(column);
                seat.setSeatCode(String.valueOf((char) ('A' + row)) + (column + 1));
                seat.setType(isVipSeat(row, column, room.getRowCount(), room.getSeatsPerRow())
                        ? SeatType.VIP
                        : SeatType.NORMAL);
                seat.setActive(true);
                seats.save(seat);
            }
        }
    }

    private boolean isVipSeat(int rowIndex, int columnIndex, int rowsCount, int seatsPerRow) {
        return rowsCount >= 6
                && seatsPerRow >= 6
                && rowIndex >= 2
                && rowIndex < rowsCount - 2
                && columnIndex >= 2
                && columnIndex < seatsPerRow - 2;
    }

    private void seedShowtimes(List<Movie> movieList, List<Room> roomList) {
        LocalDateTime now = LocalDateTime.now(clock).withSecond(0).withNano(0);
        List<Movie> activeMovies = movieList.stream()
                .filter(movie -> movie.getStatus() == MovieStatus.NOW_SHOWING)
                .toList();

        int movieIndex = 0;
        int roomIndex = 0;
        for (int dayOffset = -2; dayOffset <= 4; dayOffset++) {
            int[] hours = {9, 12, 15, 18, 21};
            for (int hour : hours) {
                Room room = roomList.get(roomIndex++ % roomList.size());
                Movie movie = activeMovies.get(movieIndex++ % activeMovies.size());
                LocalDateTime start = now.toLocalDate().plusDays(dayOffset).atTime(hour, (roomIndex % 2) * 15);
                createShowtime(movie, room, start, true);
            }
        }

        createShowtime(activeMovies.get(0), roomList.get(0), now.minusMinutes(10), true);
        createShowtime(activeMovies.get(1), roomList.get(1), now.minusMinutes(45), true);
        createShowtime(activeMovies.get(2), roomList.get(2), now.plusMinutes(40), true);
        createShowtime(activeMovies.get(3), roomList.get(3), now.plusHours(2), true);
        createShowtime(activeMovies.get(4), roomList.get(4), now.plusHours(5), false);
    }

    private void createShowtime(Movie movie, Room room, LocalDateTime start, boolean active) {
        LocalDateTime end = start.plusMinutes(movie.getDurationMinutes());
        if (showtimes.hasConflict(room.getId(), start, end, null)) {
            return;
        }
        Showtime showtime = new Showtime();
        showtime.setMovie(movie);
        showtime.setRoom(room);
        showtime.setStartTime(start);
        showtime.setEndTime(end);
        boolean weekend = start.getDayOfWeek().getValue() >= 6;
        showtime.setBasePrice(weekend ? room.getWeekendBasePrice() : room.getWeekdayBasePrice());
        showtime.setActive(active);
        showtime = showtimes.save(showtime);

        for (Seat seat : seats.findAllByRoomIdOrderByRowIndexAscColumnIndexAsc(room.getId())) {
            ShowtimeSeat showtimeSeat = new ShowtimeSeat();
            showtimeSeat.setShowtime(showtime);
            showtimeSeat.setSeat(seat);
            showtimeSeats.save(showtimeSeat);
        }
    }
}
