package com.pngthanh.cineverse.payment.service;

import com.pngthanh.cineverse.common.exception.ApiException;
import com.pngthanh.cineverse.payment.dto.PaymentResponse;
import com.pngthanh.cineverse.payment.dto.VnPayCreatePaymentResponse;
import com.pngthanh.cineverse.payment.entity.Payment;
import com.pngthanh.cineverse.payment.repository.PaymentRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VnPayService {
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter VNPAY_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final VnPayConfig config;
    private final PaymentRepository payments;
    private final PaymentService paymentService;

    public VnPayService(VnPayConfig config, PaymentRepository payments, PaymentService paymentService) {
        this.config = config;
        this.payments = payments;
        this.paymentService = paymentService;
    }

    @Transactional
    public VnPayCreatePaymentResponse createPayment(
            String email,
            Long bookingId,
            HttpServletRequest request) {
        requireConfigured();
        String reference = nextReference(bookingId);
        Payment payment = paymentService.prepareVnPay(email, bookingId, reference);

        if (payment.getStatus().name().equals("SUCCESS")) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "PAYMENT_ALREADY_SUCCESS",
                    "Booking này đã được thanh toán thành công.");
        }

        ZonedDateTime now = ZonedDateTime.now(VIETNAM_ZONE);
        Instant bookingExpiry = payment.getBooking().getExpiresAt();
        ZonedDateTime expiresAt = bookingExpiry.atZone(VIETNAM_ZONE);

        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", config.getTmnCode());
        params.put("vnp_Amount", payment.getAmount().multiply(BigDecimal.valueOf(100)).toBigIntegerExact().toString());
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", reference);
        params.put("vnp_OrderInfo", "Thanh toan ve CineVerse " + payment.getBooking().getBookingCode());
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", config.getReturnUrl());
        params.put("vnp_IpAddr", clientIp(request));
        params.put("vnp_CreateDate", now.format(VNPAY_TIME));
        params.put("vnp_ExpireDate", expiresAt.format(VNPAY_TIME));

        String signedData = VnPaySigner.buildSignedData(params);
        String secureHash = VnPaySigner.hmacSha512(config.getHashSecret(), signedData);
        String paymentUrl = config.getPayUrl() + "?" + signedData + "&vnp_SecureHash=" + secureHash;
        return new VnPayCreatePaymentResponse(bookingId, reference, paymentUrl);
    }

    @Transactional
    public CallbackResult processCallback(Map<String, String> input, String source) {
        requireConfigured();
        String receivedHash = input.get("vnp_SecureHash");
        if (receivedHash == null || receivedHash.isBlank()) {
            return CallbackResult.invalid("97", "Missing checksum");
        }

        Map<String, String> signedValues = new LinkedHashMap<>(input);
        signedValues.remove("vnp_SecureHash");
        signedValues.remove("vnp_SecureHashType");
        String expectedHash = VnPaySigner.hmacSha512(
                config.getHashSecret(),
                VnPaySigner.buildSignedData(signedValues));
        if (!expectedHash.equalsIgnoreCase(receivedHash)) {
            return CallbackResult.invalid("97", "Invalid checksum");
        }

        String reference = input.get("vnp_TxnRef");
        if (reference == null || reference.isBlank()) {
            return CallbackResult.invalid("01", "Missing order reference");
        }
        Payment payment = payments.findByTransactionReferenceForUpdate(reference).orElse(null);
        if (payment == null) {
            return CallbackResult.invalid("01", "Order not found");
        }
        if (!config.getTmnCode().equals(input.get("vnp_TmnCode"))) {
            return CallbackResult.invalid("97", "Invalid terminal");
        }
        if (!amountMatches(payment, input.get("vnp_Amount"))) {
            return CallbackResult.invalid("04", "Invalid amount");
        }

        String responseCode = input.get("vnp_ResponseCode");
        String transactionStatus = input.get("vnp_TransactionStatus");
        boolean success = "00".equals(responseCode) && "00".equals(transactionStatus);
        PaymentResponse response;
        if (success) {
            response = paymentService.confirmVnPay(
                    payment,
                    input.get("vnp_TransactionNo"),
                    input.get("vnp_BankTranNo"),
                    input.get("vnp_BankCode"),
                    input.get("vnp_CardType"),
                    responseCode,
                    transactionStatus,
                    source);
        } else {
            response = paymentService.markVnPayFailed(
                    payment,
                    input.get("vnp_TransactionNo"),
                    input.get("vnp_BankTranNo"),
                    input.get("vnp_BankCode"),
                    input.get("vnp_CardType"),
                    responseCode,
                    transactionStatus,
                    source);
        }
        return CallbackResult.valid(success, response.bookingId(), response.paymentStatus(), responseCode);
    }

    private void requireConfigured() {
        if (!config.isConfigured()) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "VNPAY_NOT_CONFIGURED",
                    "VNPAY Sandbox chưa được cấu hình trên backend.");
        }
    }

    private boolean amountMatches(Payment payment, String rawAmount) {
        if (rawAmount == null || rawAmount.isBlank()) {
            return false;
        }
        try {
            BigDecimal callbackAmount = new BigDecimal(rawAmount).divide(BigDecimal.valueOf(100));
            return callbackAmount.compareTo(payment.getAmount()) == 0;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private String nextReference(Long bookingId) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        return "CV" + bookingId + "-" + System.currentTimeMillis() + "-" + suffix;
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        String value = forwarded == null || forwarded.isBlank()
                ? request.getRemoteAddr()
                : forwarded.split(",")[0].trim();
        if ("0:0:0:0:0:0:0:1".equals(value) || "::1".equals(value)) {
            return "127.0.0.1";
        }
        return value;
    }

    public record CallbackResult(
            boolean valid,
            boolean success,
            Long bookingId,
            String paymentStatus,
            String responseCode,
            String rspCode,
            String message) {
        static CallbackResult valid(boolean success, Long bookingId, String paymentStatus, String responseCode) {
            return new CallbackResult(true, success, bookingId, paymentStatus, responseCode, "00", "Confirm Success");
        }

        static CallbackResult invalid(String rspCode, String message) {
            return new CallbackResult(false, false, null, null, null, rspCode, message);
        }
    }
}
