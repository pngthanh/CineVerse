package com.pngthanh.cineverse.voucher.dto;

import java.math.BigDecimal;

public record VoucherQuoteResponse(
        String code,
        String discountType,
        BigDecimal discountValue,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal totalAmount) {
}
