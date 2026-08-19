package com.pngthanh.cineverse.voucher.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record VoucherQuoteRequest(
        @NotBlank String code,
        @NotNull @DecimalMin("0") BigDecimal subtotal,
        @NotNull Long showtimeId) {
}
