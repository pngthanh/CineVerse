package com.pngthanh.cineverse.payment.dto;

public record VnPayCreatePaymentResponse(
        Long bookingId,
        String transactionReference,
        String paymentUrl) {
}
