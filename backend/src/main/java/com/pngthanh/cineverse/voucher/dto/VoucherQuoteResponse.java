package com.pngthanh.cineverse.voucher.dto;

import java.math.BigDecimal;

public record VoucherQuoteResponse(
        String code,
        BigDecimal discountPercent,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal totalAmount) {
}
