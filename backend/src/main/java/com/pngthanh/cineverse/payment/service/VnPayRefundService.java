package com.pngthanh.cineverse.payment.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import com.pngthanh.cineverse.booking.entity.Booking;
import com.pngthanh.cineverse.booking.service.BookingService;
import com.pngthanh.cineverse.common.enums.BookingStatus;
import com.pngthanh.cineverse.common.enums.PaymentStatus;
import com.pngthanh.cineverse.common.exception.ApiException;
import com.pngthanh.cineverse.payment.dto.VnPayRefundResponse;
import com.pngthanh.cineverse.payment.entity.Payment;
import com.pngthanh.cineverse.payment.repository.PaymentRepository;

import jakarta.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

@Service
public class VnPayRefundService {

    private static final DateTimeFormatter VNPAY_TIME =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private static final ZoneId VIETNAM =
            ZoneId.of("Asia/Ho_Chi_Minh");

    private final PaymentRepository payments;
    private final BookingService bookings;
    private final VnPayConfig config;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    @Autowired
    public VnPayRefundService(
            PaymentRepository payments,
            BookingService bookings,
            VnPayConfig config,
            ObjectMapper objectMapper) {

        this(
                payments,
                bookings,
                config,
                objectMapper,
                RestClient.builder());
    }

    VnPayRefundService(
            PaymentRepository payments,
            BookingService bookings,
            VnPayConfig config,
            ObjectMapper objectMapper,
            RestClient.Builder restClientBuilder) {

        this.payments = payments;
        this.bookings = bookings;
        this.config = config;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder.build();
    }

    @Transactional
    public VnPayRefundResponse refund(
            Long bookingId,
            String adminName,
            HttpServletRequest request) {

        requireConfigured();

        Booking booking = bookings.requireForUpdate(bookingId);

        Payment payment = payments.findByBookingId(bookingId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "PAYMENT_NOT_FOUND",
                        "Không tìm thấy giao dịch thanh toán."));

        if (booking.getStatus() != BookingStatus.REFUND_PENDING) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "BOOKING_NOT_REFUND_PENDING",
                    "Booking không ở trạng thái chờ hoàn tiền.");
        }

        if (payment.getStatus() != PaymentStatus.REFUND_PENDING
                && payment.getStatus() != PaymentStatus.REFUND_FAILED) {

            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "PAYMENT_NOT_REFUNDABLE",
                    "Giao dịch không ở trạng thái có thể hoàn tiền.");
        }

        if (payment.getTransactionReference() == null
                || payment.getGatewayTransactionNo() == null) {

            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "PAYMENT_REFERENCE_MISSING",
                    "Giao dịch thiếu mã tham chiếu VNPAY để hoàn tiền.");
        }

        Instant now = Instant.now();

        String requestId = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 32);

        String createDate =
                VNPAY_TIME.format(now.atZone(VIETNAM));

        Instant originalCreatedAt =
                payment.getTransactionCreatedAt() == null
                        ? payment.getCreatedAt()
                        : payment.getTransactionCreatedAt();

        String transactionDate =
                VNPAY_TIME.format(originalCreatedAt.atZone(VIETNAM));

        String amount = payment.getAmount()
                .multiply(BigDecimal.valueOf(100))
                .toBigIntegerExact()
                .toString();

        String ip = clientIp(request);
        String createBy = safeCreateBy(adminName);
        String orderInfo =
                "Hoan tien booking " + booking.getBookingCode();

        Map<String, String> payload = new LinkedHashMap<>();

        payload.put("vnp_RequestId", requestId);
        payload.put("vnp_Version", "2.1.0");
        payload.put("vnp_Command", "refund");
        payload.put("vnp_TmnCode", config.getTmnCode());
        payload.put("vnp_TransactionType", "02");
        payload.put("vnp_TxnRef", payment.getTransactionReference());
        payload.put("vnp_Amount", amount);
        payload.put("vnp_TransactionNo", payment.getGatewayTransactionNo());
        payload.put("vnp_TransactionDate", transactionDate);
        payload.put("vnp_CreateBy", createBy);
        payload.put("vnp_CreateDate", createDate);
        payload.put("vnp_IpAddr", ip);
        payload.put("vnp_OrderInfo", orderInfo);

        String signed = String.join(
                "|",
                requestId,
                "2.1.0",
                "refund",
                config.getTmnCode(),
                "02",
                payment.getTransactionReference(),
                amount,
                payment.getGatewayTransactionNo(),
                transactionDate,
                createBy,
                createDate,
                ip,
                orderInfo);

        payload.put(
                "vnp_SecureHash",
                VnPaySigner.hmacSha512(
                        config.getHashSecret(),
                        signed));

        payment.setRefundRequestId(requestId);
        payment.setRefundRequestedAt(now);
        payment.setRefundResponseCode(null);
        payment.setRefundTransactionStatus(null);
        payment.setRefundTransactionNo(null);
        payment.setRefundMessage(null);
        payment.setRefundCompletedAt(null);

        try {

            String raw = restClient.post()
                    .uri(config.getApiUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            JsonNode response =
                    objectMapper.readTree(raw == null ? "{}" : raw);

            String responseCode =
                    text(response, "vnp_ResponseCode");

            String transactionStatus =
                    text(response, "vnp_TransactionStatus");

            String refundTransactionNo =
                    text(response, "vnp_TransactionNo");

            String message =
                    text(response, "vnp_Message");

            payment.setRefundResponseCode(responseCode);
            payment.setRefundTransactionStatus(transactionStatus);
            payment.setRefundTransactionNo(refundTransactionNo);
            payment.setRefundMessage(message);

            if ("00".equals(responseCode)
                    && "00".equals(transactionStatus)) {

                payment.setStatus(PaymentStatus.REFUNDED);
                payment.setRefundCompletedAt(Instant.now());
                booking.setStatus(BookingStatus.CANCELLED);

            } else if ("00".equals(responseCode)
                    || "94".equals(responseCode)
                    || "05".equals(transactionStatus)
                    || "06".equals(transactionStatus)) {

                payment.setStatus(PaymentStatus.REFUND_PENDING);

            } else {

                payment.setStatus(PaymentStatus.REFUND_FAILED);
            }

            return response(booking, payment);

        } catch (Exception ex) {

            payment.setStatus(PaymentStatus.REFUND_FAILED);

            payment.setRefundMessage(
                    "Không kết nối được VNPAY Sandbox: "
                            + ex.getClass().getSimpleName());

            return response(booking, payment);
        }
    }

    private VnPayRefundResponse response(
            Booking booking,
            Payment payment) {

        return new VnPayRefundResponse(
                booking.getId(),
                booking.getStatus().name(),
                payment.getStatus().name(),
                payment.getAmount(),
                payment.getRefundRequestId(),
                payment.getRefundResponseCode(),
                payment.getRefundTransactionStatus(),
                payment.getRefundTransactionNo(),
                payment.getRefundMessage(),
                payment.getRefundRequestedAt(),
                payment.getRefundCompletedAt());
    }

    private void requireConfigured() {

        if (!config.isConfigured()) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "VNPAY_NOT_CONFIGURED",
                    "VNPAY Sandbox chưa được cấu hình trên backend.");
        }
    }

    private String text(JsonNode node, String key) {

        JsonNode value = node.get(key);

        return value == null || value.isNull()
                ? null
                : value.asText();
    }

    private String safeCreateBy(String value) {

        String cleaned = value == null
                ? "admin"
                : value.replaceAll(
                        "[^A-Za-z0-9@._-]",
                        "_");

        return cleaned.isBlank()
                ? "admin"
                : cleaned.substring(
                        0,
                        Math.min(cleaned.length(), 100));
    }

    private String clientIp(HttpServletRequest request) {

        String forwarded =
                request.getHeader("X-Forwarded-For");

        String value =
                forwarded == null || forwarded.isBlank()
                        ? request.getRemoteAddr()
                        : forwarded.split(",")[0].trim();

        return "0:0:0:0:0:0:0:1".equals(value)
                        || "::1".equals(value)
                ? "127.0.0.1"
                : value;
    }
}
