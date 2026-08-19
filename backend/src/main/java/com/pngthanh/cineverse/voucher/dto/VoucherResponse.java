package com.pngthanh.cineverse.voucher.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record VoucherResponse(
        Long id,
        String code,
        String title,
        String description,
        String discountType,
        BigDecimal discountValue,
        BigDecimal minOrderAmount,
        BigDecimal maxDiscountAmount,
        LocalDateTime startsAt,
        LocalDateTime expiresAt,
        boolean active,
        boolean publicVisible,
        String audience,
        Long movieId,
        String movieTitle,
        Integer usageLimit,
        Integer perUserLimit,
        List<Long> assignedUserIds,
        boolean saved,
        boolean eligible) {
}
