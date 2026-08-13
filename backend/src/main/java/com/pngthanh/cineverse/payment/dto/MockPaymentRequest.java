package com.pngthanh.cineverse.payment.dto;

import jakarta.validation.constraints.NotNull;

public record MockPaymentRequest(
        @NotNull Long bookingId,
        @NotNull Result result) {

    public enum Result {
        SUCCESS,
        FAILED
    }
}
