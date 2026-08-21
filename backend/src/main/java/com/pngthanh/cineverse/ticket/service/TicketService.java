package com.pngthanh.cineverse.ticket.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;
import com.pngthanh.cineverse.booking.entity.Booking;
import com.pngthanh.cineverse.booking.repository.BookingSeatRepository;
import com.pngthanh.cineverse.common.enums.BookingStatus;
import com.pngthanh.cineverse.common.enums.PaymentStatus;
import com.pngthanh.cineverse.common.enums.Role;
import com.pngthanh.cineverse.common.enums.TicketStatus;
import com.pngthanh.cineverse.common.exception.ApiException;
import com.pngthanh.cineverse.payment.repository.PaymentRepository;
import com.pngthanh.cineverse.ticket.dto.StaffTicketResponse;
import com.pngthanh.cineverse.ticket.entity.Ticket;
import com.pngthanh.cineverse.ticket.repository.TicketRepository;
import com.pngthanh.cineverse.user.entity.User;
import com.pngthanh.cineverse.user.service.UserService;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class TicketService {
    private static final String QR_PREFIX = "CINEVERSE:TICKET:";

    private final TicketRepository tickets;
    private final BookingSeatRepository bookingSeats;
    private final PaymentRepository payments;
    private final UserService users;
    private final Clock clock;
    private final long opensBeforeMinutes;
    private final long closesAfterStartMinutes;

    public TicketService(
            TicketRepository tickets,
            BookingSeatRepository bookingSeats,
            PaymentRepository payments,
            UserService users,
            Clock clock,
            @Value("${app.check-in.opens-before-minutes:30}") long opensBeforeMinutes,
            @Value("${app.check-in.closes-after-start-minutes:30}") long closesAfterStartMinutes) {
        this.tickets = tickets;
        this.bookingSeats = bookingSeats;
        this.payments = payments;
        this.users = users;
        this.clock = clock;
        this.opensBeforeMinutes = opensBeforeMinutes;
        this.closesAfterStartMinutes = closesAfterStartMinutes;
    }

    @Transactional(readOnly = true)
    public StaffTicketResponse lookupManual(String principal, String rawTicketCode) {
        User staff = requireStaff(principal);
        Ticket ticket = tickets.findByTicketCodeIgnoreCase(normalizeCode(rawTicketCode))
                .orElseThrow(this::ticketNotFound);
        return staffResponse(ticket, staff);
    }

    @Transactional(readOnly = true)
    public StaffTicketResponse lookupQr(String principal, MultipartFile file) {
        User staff = requireStaff(principal);
        QrIdentity identity = parseQr(decodeQr(file));
        Ticket ticket = tickets.findByTicketCodeIgnoreCase(identity.ticketCode())
                .orElseThrow(this::ticketNotFound);
        ensureQrToken(ticket);
        if (!Objects.equals(ticket.getQrToken(), identity.token())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "TICKET_QR_INVALID",
                    "Mã QR không hợp lệ hoặc đã bị chỉnh sửa.");
        }
        return staffResponse(ticket, staff);
    }

    @Transactional
    public StaffTicketResponse checkIn(String principal, String rawTicketCode) {
        User staff = requireStaff(principal);
        Ticket ticket = tickets.findByTicketCodeIgnoreCase(normalizeCode(rawTicketCode))
                .orElseThrow(this::ticketNotFound);
        Validation validation = validate(ticket, staff);
        if (!validation.canCheckIn()) {
            throw new ApiException(HttpStatus.CONFLICT, validation.code(), validation.message());
        }
        ticket.setStatus(TicketStatus.USED);
        ticket.setCheckedInAt(Instant.now(clock));
        ticket.setCheckedInBy(staff);
        return staffResponse(ticket, staff);
    }

    @Transactional
    public byte[] qrPng(String principal, String rawTicketCode) {
        User user = users.requireByEmail(principal);
        Ticket ticket = requireOwnedTicket(user, rawTicketCode);
        ensureQrToken(ticket);
        return createQrPng(qrPayload(ticket), 360);
    }

    @Transactional
    public byte[] downloadableTicket(String principal, String rawTicketCode) {
        User user = users.requireByEmail(principal);
        Ticket ticket = requireOwnedTicket(user, rawTicketCode);
        ensureQrToken(ticket);
        return createTicketImage(ticket);
    }

    private Ticket requireOwnedTicket(User user, String rawTicketCode) {
        Ticket ticket = tickets.findByTicketCodeIgnoreCase(normalizeCode(rawTicketCode))
                .orElseThrow(this::ticketNotFound);
        if (user.getRole() != Role.ADMIN
                && !Objects.equals(ticket.getBooking().getUser().getId(), user.getId())) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "TICKET_FORBIDDEN",
                    "Bạn không có quyền truy cập vé này.");
        }
        return ticket;
    }

    private User requireStaff(String principal) {
        User staff = users.requireByEmail(principal);
        if (staff.getRole() != Role.STAFF) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "STAFF_REQUIRED",
                    "Chức năng này chỉ dành cho nhân viên rạp.");
        }
        if (staff.getAssignedCinema() == null) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "STAFF_CINEMA_REQUIRED",
                    "Tài khoản nhân viên chưa được phân công rạp.");
        }
        return staff;
    }

    private StaffTicketResponse staffResponse(Ticket ticket, User staff) {
        Booking booking = ticket.getBooking();
        var showtime = booking.getShowtime();
        var cinema = showtime.getRoom().getCinema();
        Validation validation = validate(ticket, staff);
        List<String> seats = bookingSeats.findAllByBookingId(booking.getId()).stream()
                .map(item -> item.getShowtimeSeat().getSeat().getSeatCode())
                .toList();
        String paymentStatus = payments.findByBookingId(booking.getId())
                .map(payment -> payment.getStatus().name())
                .orElse(null);
        return new StaffTicketResponse(
                ticket.getTicketCode(),
                ticket.getStatus().name(),
                booking.getStatus().name(),
                paymentStatus,
                showtime.getMovie().getTitle(),
                cinema.getId(),
                cinema.getName(),
                showtime.getRoom().getName(),
                showtime.getStartTime(),
                seats,
                booking.getUser().getFullName(),
                staff.getAssignedCinema().getId(),
                staff.getAssignedCinema().getName(),
                Objects.equals(cinema.getId(), staff.getAssignedCinema().getId()),
                validation.canCheckIn(),
                validation.message(),
                ticket.getCheckedInAt(),
                ticket.getCheckedInBy() == null ? null : ticket.getCheckedInBy().getFullName());
    }

    private Validation validate(Ticket ticket, User staff) {
        Booking booking = ticket.getBooking();
        var cinema = booking.getShowtime().getRoom().getCinema();
        if (!Objects.equals(cinema.getId(), staff.getAssignedCinema().getId())) {
            return new Validation(
                    false,
                    "TICKET_WRONG_CINEMA",
                    "Vé thuộc " + cinema.getName() + ", không thuộc rạp "
                            + staff.getAssignedCinema().getName() + " của bạn.");
        }
        if (ticket.getStatus() == TicketStatus.USED) {
            return new Validation(false, "TICKET_ALREADY_USED", "Vé đã được sử dụng trước đó.");
        }
        if (ticket.getStatus() != TicketStatus.CONFIRMED) {
            return new Validation(false, "TICKET_NOT_ACTIVE", "Vé không còn hiệu lực.");
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED
                && booking.getStatus() != BookingStatus.COMPLETED) {
            return new Validation(false, "BOOKING_NOT_CONFIRMED", "Booking của vé chưa được xác nhận.");
        }
        boolean paid = payments.findByBookingId(booking.getId())
                .map(payment -> payment.getStatus() == PaymentStatus.SUCCESS)
                .orElse(false);
        if (!paid) {
            return new Validation(false, "PAYMENT_NOT_SUCCESS", "Vé chưa có thanh toán thành công.");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime start = booking.getShowtime().getStartTime();
        LocalDateTime opensAt = start.minusMinutes(opensBeforeMinutes);
        LocalDateTime closesAt = start.plusMinutes(closesAfterStartMinutes);
        if (now.isBefore(opensAt)) {
            return new Validation(
                    false,
                    "CHECKIN_TOO_EARLY",
                    "Chưa đến giờ check-in. Vé được mở check-in trước suất chiếu "
                            + opensBeforeMinutes + " phút.");
        }
        if (now.isAfter(closesAt)) {
            return new Validation(false, "CHECKIN_CLOSED", "Đã quá thời gian check-in của suất chiếu.");
        }
        return new Validation(true, "OK", "Vé hợp lệ và sẵn sàng check-in.");
    }

    private void ensureQrToken(Ticket ticket) {
        if (ticket.getQrToken() == null || ticket.getQrToken().isBlank()) {
            ticket.setQrToken(UUID.randomUUID().toString().replace("-", ""));
        }
    }

    private String qrPayload(Ticket ticket) {
        return QR_PREFIX + ticket.getTicketCode() + ":" + ticket.getQrToken();
    }

    private QrIdentity parseQr(String value) {
        if (value == null || !value.startsWith(QR_PREFIX)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "TICKET_QR_INVALID", "Ảnh không chứa QR vé CineVerse hợp lệ.");
        }
        String[] parts = value.substring(QR_PREFIX.length()).split(":", 2);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "TICKET_QR_INVALID", "QR vé CineVerse không đúng định dạng.");
        }
        return new QrIdentity(normalizeCode(parts[0]), parts[1]);
    }

    private String decodeQr(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "QR_IMAGE_REQUIRED", "Hãy chọn ảnh có mã QR của vé.");
        }
        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "QR_IMAGE_INVALID", "Không thể đọc file ảnh đã tải lên.");
            }
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
            return new MultiFormatReader().decode(bitmap).getText();
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "QR_NOT_FOUND", "Không tìm thấy mã QR hợp lệ trong ảnh.");
        }
    }

    private byte[] createQrPng(String content, int size) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            return output.toByteArray();
        } catch (WriterException | IOException ex) {
            throw new IllegalStateException("Không thể tạo mã QR vé.", ex);
        }
    }

    private byte[] createTicketImage(Ticket ticket) {
        try {
            Booking booking = ticket.getBooking();
            var showtime = booking.getShowtime();
            BufferedImage qr = ImageIO.read(new java.io.ByteArrayInputStream(createQrPng(qrPayload(ticket), 360)));
            BufferedImage image = new BufferedImage(1200, 620, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(new Color(11, 14, 20));
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.setColor(new Color(245, 70, 70));
            graphics.setFont(new Font("SansSerif", Font.BOLD, 34));
            graphics.drawString("CineVerse", 60, 70);
            graphics.setColor(Color.WHITE);
            graphics.setFont(new Font("SansSerif", Font.BOLD, 30));
            graphics.drawString(showtime.getMovie().getTitle(), 60, 135);
            graphics.setFont(new Font("SansSerif", Font.PLAIN, 23));
            graphics.drawString("Rạp: " + showtime.getRoom().getCinema().getName(), 60, 190);
            graphics.drawString("Phòng: " + showtime.getRoom().getName(), 60, 230);
            graphics.drawString("Suất: " + showtime.getStartTime(), 60, 270);
            String seatText = bookingSeats.findAllByBookingId(booking.getId()).stream()
                    .map(item -> item.getShowtimeSeat().getSeat().getSeatCode())
                    .reduce((left, right) -> left + ", " + right)
                    .orElse("-");
            graphics.drawString("Ghế: " + seatText, 60, 310);
            graphics.drawString("Khách: " + booking.getUser().getFullName(), 60, 350);
            graphics.setFont(new Font("Monospaced", Font.BOLD, 25));
            graphics.drawString("Mã vé: " + ticket.getTicketCode(), 60, 405);
            graphics.setStroke(new BasicStroke(2));
            graphics.setColor(new Color(45, 52, 64));
            graphics.drawLine(60, 450, 760, 450);
            graphics.setFont(new Font("SansSerif", Font.PLAIN, 18));
            graphics.setColor(new Color(180, 186, 198));
            graphics.drawString("Đưa mã QR này cho nhân viên tại đúng rạp để check-in.", 60, 495);
            graphics.drawImage(qr, 790, 120, 340, 340, null);
            graphics.dispose();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Không thể tạo ảnh vé.", ex);
        }
    }

    private String normalizeCode(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private ApiException ticketNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "TICKET_NOT_FOUND", "Không tìm thấy vé.");
    }

    private record Validation(boolean canCheckIn, String code, String message) {
    }

    private record QrIdentity(String ticketCode, String token) {
    }
}
