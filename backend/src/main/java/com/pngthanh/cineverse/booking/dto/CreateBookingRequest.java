package com.pngthanh.cineverse.booking.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateBookingRequest(
        @NotBlank String holdToken,
        String voucherCode,
        List<@Valid ConcessionSelection> concessions) {

    public record ConcessionSelection(
            @NotNull Long itemId,
            @Min(1) @Max(10) int quantity) {
    }
}
