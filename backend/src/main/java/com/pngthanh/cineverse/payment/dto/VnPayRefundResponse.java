package com.pngthanh.cineverse.payment.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record VnPayRefundResponse(
        Long bookingId,
        String bookingStatus,
        String paymentStatus,
        BigDecimal amount,
        String requestId,
        String responseCode,
        String transactionStatus,
        String refundTransactionNo,
        String message,
        Instant requestedAt,
        Instant completedAt) {
}
