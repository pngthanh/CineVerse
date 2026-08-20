package com.pngthanh.cineverse.payment.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        Long paymentId,
        Long bookingId,
        String bookingStatus,
        String paymentStatus,
        BigDecimal amount,
        String provider,
        String method,
        String transactionReference,
        String gatewayTransactionNo,
        String bankCode,
        String cardType,
        String responseCode,
        Instant paidAt,
        String ticketCode) {
}
