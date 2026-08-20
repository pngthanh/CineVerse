package com.pngthanh.cineverse.payment.dto;

import jakarta.validation.constraints.NotNull;

public record VnPayCreatePaymentRequest(@NotNull Long bookingId) {
}
