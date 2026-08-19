package com.pngthanh.cineverse.voucher.service;

import com.pngthanh.cineverse.booking.repository.BookingRepository;
import com.pngthanh.cineverse.common.enums.BookingStatus;
import com.pngthanh.cineverse.common.enums.VoucherAudience;
import com.pngthanh.cineverse.common.enums.VoucherDiscountType;
import com.pngthanh.cineverse.common.exception.ApiException;
import com.pngthanh.cineverse.movie.entity.Movie;
import com.pngthanh.cineverse.movie.repository.MovieRepository;
import com.pngthanh.cineverse.showtime.entity.Showtime;
import com.pngthanh.cineverse.showtime.repository.ShowtimeRepository;
import com.pngthanh.cineverse.user.entity.User;
import com.pngthanh.cineverse.user.repository.UserRepository;
import com.pngthanh.cineverse.voucher.dto.VoucherAdminRequest;
import com.pngthanh.cineverse.voucher.dto.VoucherQuoteResponse;
import com.pngthanh.cineverse.voucher.dto.VoucherResponse;
import com.pngthanh.cineverse.voucher.entity.SavedVoucher;
import com.pngthanh.cineverse.voucher.entity.Voucher;
import com.pngthanh.cineverse.voucher.entity.VoucherAssignment;
import com.pngthanh.cineverse.voucher.repository.SavedVoucherRepository;
import com.pngthanh.cineverse.voucher.repository.VoucherAssignmentRepository;
import com.pngthanh.cineverse.voucher.repository.VoucherRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VoucherService {
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final VoucherRepository vouchers;
    private final SavedVoucherRepository savedVouchers;
    private final VoucherAssignmentRepository assignments;
    private final UserRepository users;
    private final MovieRepository movies;
    private final ShowtimeRepository showtimes;
    private final BookingRepository bookings;
    private final Clock clock;

    public VoucherService(
            VoucherRepository vouchers,
            SavedVoucherRepository savedVouchers,
            VoucherAssignmentRepository assignments,
            UserRepository users,
            MovieRepository movies,
            ShowtimeRepository showtimes,
            BookingRepository bookings,
            Clock clock) {
        this.vouchers = vouchers;
        this.savedVouchers = savedVouchers;
        this.assignments = assignments;
        this.users = users;
        this.movies = movies;
        this.showtimes = showtimes;
        this.bookings = bookings;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<VoucherResponse> publicVouchers(String principal) {
        User user = principal == null ? null : findUser(principal);
        LocalDateTime now = LocalDateTime.now(clock);
        return vouchers.findAllByOrderByExpiresAtDesc().stream()
                .filter(Voucher::isActive)
                .filter(Voucher::isPublicVisible)
                .filter(voucher -> voucher.getAudience() == VoucherAudience.ALL)
                .filter(voucher -> !now.isBefore(voucher.getStartsAt()) && now.isBefore(voucher.getExpiresAt()))
                .map(voucher -> response(voucher, user))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VoucherResponse> saved(String principal) {
        User user = requireUser(principal);
        return savedVouchers.findAllByUserIdOrderBySavedAtDesc(user.getId()).stream()
                .map(SavedVoucher::getVoucher)
                .map(voucher -> response(voucher, user))
                .toList();
    }

    @Transactional
    public VoucherResponse save(String principal, Long voucherId) {
        User user = requireUser(principal);
        Voucher voucher = requireVoucher(voucherId);
        ensureCanSee(voucher, user);
        if (!savedVouchers.existsByUserIdAndVoucherId(user.getId(), voucher.getId())) {
            SavedVoucher saved = new SavedVoucher();
            saved.setUser(user);
            saved.setVoucher(voucher);
            savedVouchers.save(saved);
        }
        return response(voucher, user);
    }

    @Transactional
    public void unsave(String principal, Long voucherId) {
        User user = requireUser(principal);
        savedVouchers.deleteByUserIdAndVoucherId(user.getId(), voucherId);
    }

    @Transactional(readOnly = true)
    public VoucherQuoteResponse quote(
            String principal,
            String rawCode,
            BigDecimal subtotal,
            Long showtimeId) {
        User user = requireUser(principal);
        Showtime showtime = showtimes.findById(showtimeId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "SHOWTIME_NOT_FOUND",
                        "Không tìm thấy suất chiếu."));
        Voucher voucher = requireUsable(rawCode, subtotal, user, showtime.getMovie());
        BigDecimal discount = calculateDiscount(voucher, subtotal);
        return new VoucherQuoteResponse(
                voucher.getCode(),
                voucher.getDiscountType().name(),
                voucher.getDiscountValue(),
                subtotal,
                discount,
                subtotal.subtract(discount));
    }

    @Transactional(readOnly = true)
    public AppliedVoucher apply(
            String rawCode,
            BigDecimal subtotal,
            User user,
            Movie movie) {
        if (rawCode == null || rawCode.isBlank()) {
            return new AppliedVoucher(null, BigDecimal.ZERO, subtotal);
        }
        Voucher voucher = requireUsable(rawCode, subtotal, user, movie);
        BigDecimal discount = calculateDiscount(voucher, subtotal);
        return new AppliedVoucher(voucher.getCode(), discount, subtotal.subtract(discount));
    }

    @Transactional(readOnly = true)
    public List<VoucherResponse> adminList() {
        return vouchers.findAllByOrderByExpiresAtDesc().stream()
                .map(voucher -> response(voucher, null))
                .toList();
    }

    @Transactional
    public VoucherResponse create(VoucherAdminRequest request) {
        String code = normalizeCode(request.code());
        if (vouchers.existsByCodeIgnoreCase(code)) {
            throw new ApiException(HttpStatus.CONFLICT, "VOUCHER_CODE_EXISTS", "Mã voucher đã tồn tại.");
        }
        Voucher voucher = new Voucher();
        applyAdminRequest(voucher, request, code);
        Voucher saved = vouchers.save(voucher);
        replaceAssignments(saved, request.assignedUserIds());
        return response(saved, null);
    }

    @Transactional
    public VoucherResponse update(Long id, VoucherAdminRequest request) {
        Voucher voucher = requireVoucher(id);
        String code = normalizeCode(request.code());
        Voucher existing = vouchers.findByCodeIgnoreCase(code).orElse(null);
        if (existing != null && !existing.getId().equals(id)) {
            throw new ApiException(HttpStatus.CONFLICT, "VOUCHER_CODE_EXISTS", "Mã voucher đã tồn tại.");
        }
        applyAdminRequest(voucher, request, code);
        replaceAssignments(voucher, request.assignedUserIds());
        return response(voucher, null);
    }

    @Transactional
    public VoucherResponse deactivate(Long id) {
        Voucher voucher = requireVoucher(id);
        voucher.setActive(false);
        return response(voucher, null);
    }

    private void applyAdminRequest(Voucher voucher, VoucherAdminRequest request, String code) {
        if (!request.expiresAt().isAfter(request.startsAt())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VOUCHER_DATE_INVALID",
                    "Thời gian hết hạn phải sau thời gian bắt đầu.");
        }
        if (request.discountType() == VoucherDiscountType.PERCENT
                && request.discountValue().compareTo(ONE_HUNDRED) > 0) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VOUCHER_PERCENT_INVALID",
                    "Mức giảm phần trăm không được vượt quá 100%.");
        }
        Movie movie = request.movieId() == null ? null : movies.findById(request.movieId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "MOVIE_NOT_FOUND",
                        "Không tìm thấy phim áp dụng."));
        voucher.setCode(code);
        voucher.setTitle(request.title().trim());
        voucher.setDescription(clean(request.description()));
        voucher.setDiscountType(request.discountType());
        voucher.setDiscountValue(request.discountValue());
        voucher.setDiscountPercent(request.discountType() == VoucherDiscountType.PERCENT
                ? request.discountValue()
                : BigDecimal.ZERO);
        voucher.setFixedDiscountAmount(request.discountType() == VoucherDiscountType.FIXED
                ? request.discountValue()
                : null);
        voucher.setMinOrderAmount(request.minOrderAmount());
        voucher.setMaxDiscountAmount(request.maxDiscountAmount());
        voucher.setStartsAt(request.startsAt());
        voucher.setExpiresAt(request.expiresAt());
        voucher.setActive(request.active());
        voucher.setPublicVisible(request.publicVisible());
        voucher.setAudience(request.audience());
        voucher.setMovie(movie);
        voucher.setUsageLimit(normalizeLimit(request.usageLimit()));
        voucher.setPerUserLimit(normalizeLimit(request.perUserLimit()));
    }

    private void replaceAssignments(Voucher voucher, List<Long> userIds) {
        assignments.deleteAllByVoucherId(voucher.getId());
        if (voucher.getAudience() != VoucherAudience.SELECTED_USERS || userIds == null) {
            return;
        }
        userIds.stream().distinct().forEach(userId -> {
            User user = users.findById(userId)
                    .orElseThrow(() -> new ApiException(
                            HttpStatus.NOT_FOUND,
                            "USER_NOT_FOUND",
                            "Không tìm thấy người dùng được chọn."));
            VoucherAssignment assignment = new VoucherAssignment();
            assignment.setVoucher(voucher);
            assignment.setUser(user);
            assignments.save(assignment);
        });
    }

    private Voucher requireUsable(String rawCode, BigDecimal subtotal, User user, Movie movie) {
        Voucher voucher = vouchers.findByCodeIgnoreCase(normalizeCode(rawCode))
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "VOUCHER_NOT_FOUND",
                        "Mã ưu đãi không tồn tại."));
        LocalDateTime now = LocalDateTime.now(clock);
        if (!voucher.isActive() || now.isBefore(voucher.getStartsAt()) || !now.isBefore(voucher.getExpiresAt())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "VOUCHER_NOT_ACTIVE",
                    "Mã ưu đãi chưa có hiệu lực hoặc đã hết hạn.");
        }
        ensureEligible(voucher, user);
        if (voucher.getMovie() != null && (movie == null || !voucher.getMovie().getId().equals(movie.getId()))) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "VOUCHER_MOVIE_NOT_ELIGIBLE",
                    "Mã ưu đãi không áp dụng cho phim này.");
        }
        if (subtotal.compareTo(voucher.getMinOrderAmount()) < 0) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "VOUCHER_MIN_ORDER",
                    "Đơn hàng chưa đạt giá trị tối thiểu để dùng mã này.");
        }
        if (voucher.getUsageLimit() != null
                && bookings.countByVoucherCodeIgnoreCaseAndStatusNot(voucher.getCode(), BookingStatus.CANCELLED)
                >= voucher.getUsageLimit()) {
            throw new ApiException(HttpStatus.CONFLICT, "VOUCHER_USAGE_LIMIT", "Mã ưu đãi đã hết lượt sử dụng.");
        }
        if (voucher.getPerUserLimit() != null
                && bookings.countByUserIdAndVoucherCodeIgnoreCaseAndStatusNot(
                        user.getId(), voucher.getCode(), BookingStatus.CANCELLED) >= voucher.getPerUserLimit()) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "VOUCHER_USER_LIMIT",
                    "Bạn đã sử dụng hết số lượt cho mã ưu đãi này.");
        }
        return voucher;
    }

    private void ensureCanSee(Voucher voucher, User user) {
        if (!voucher.isActive()) {
            throw new ApiException(HttpStatus.CONFLICT, "VOUCHER_NOT_ACTIVE", "Mã ưu đãi không còn hoạt động.");
        }
        if (voucher.getAudience() == VoucherAudience.ALL && voucher.isPublicVisible()) {
            return;
        }
        ensureEligible(voucher, user);
    }

    private void ensureEligible(Voucher voucher, User user) {
        if (voucher.getAudience() == VoucherAudience.ALL) {
            return;
        }
        if (!assignments.existsByVoucherIdAndUserId(voucher.getId(), user.getId())) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "VOUCHER_NOT_ASSIGNED",
                    "Mã ưu đãi này không được cấp cho tài khoản của bạn.");
        }
    }

    private BigDecimal calculateDiscount(Voucher voucher, BigDecimal subtotal) {
        BigDecimal discount;
        if (voucher.getDiscountType() == VoucherDiscountType.FIXED) {
            discount = voucher.getDiscountValue();
        } else {
            discount = subtotal
                    .multiply(voucher.getDiscountValue())
                    .divide(ONE_HUNDRED, 0, RoundingMode.DOWN);
        }
        if (voucher.getMaxDiscountAmount() != null) {
            discount = discount.min(voucher.getMaxDiscountAmount());
        }
        return discount.min(subtotal).max(BigDecimal.ZERO);
    }

    private VoucherResponse response(Voucher voucher, User user) {
        List<Long> assignedUserIds = assignments.findAllByVoucherId(voucher.getId()).stream()
                .map(assignment -> assignment.getUser().getId())
                .toList();
        boolean isSaved = user != null && savedVouchers.existsByUserIdAndVoucherId(user.getId(), voucher.getId());
        boolean eligible = user == null
                ? voucher.getAudience() == VoucherAudience.ALL
                : voucher.getAudience() == VoucherAudience.ALL
                        || assignments.existsByVoucherIdAndUserId(voucher.getId(), user.getId());
        return new VoucherResponse(
                voucher.getId(),
                voucher.getCode(),
                voucher.getTitle(),
                voucher.getDescription(),
                voucher.getDiscountType().name(),
                voucher.getDiscountValue(),
                voucher.getMinOrderAmount(),
                voucher.getMaxDiscountAmount(),
                voucher.getStartsAt(),
                voucher.getExpiresAt(),
                voucher.isActive(),
                voucher.isPublicVisible(),
                voucher.getAudience().name(),
                voucher.getMovie() == null ? null : voucher.getMovie().getId(),
                voucher.getMovie() == null ? null : voucher.getMovie().getTitle(),
                voucher.getUsageLimit(),
                voucher.getPerUserLimit(),
                assignedUserIds,
                isSaved,
                eligible);
    }

    private User requireUser(String principal) {
        User user = findUser(principal);
        if (user == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "Không tìm thấy người dùng.");
        }
        return user;
    }

    private User findUser(String principal) {
        return principal == null ? null : users.findByEmailIgnoreCase(principal).orElse(null);
    }

    private Voucher requireVoucher(Long id) {
        return vouchers.findById(id)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "VOUCHER_NOT_FOUND",
                        "Không tìm thấy voucher."));
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private Integer normalizeLimit(Integer value) {
        return value == null || value <= 0 ? null : value;
    }

    private String clean(String value) {
    if (value == null) {
        return null;
    }
    String cleaned = value.trim();
    return cleaned.isEmpty() ? null : cleaned;
    }

    public record AppliedVoucher(String code, BigDecimal discountAmount, BigDecimal totalAmount) {
    }
}
