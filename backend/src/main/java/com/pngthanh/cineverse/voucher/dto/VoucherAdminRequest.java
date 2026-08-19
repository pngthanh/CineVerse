package com.pngthanh.cineverse.voucher.dto;

import com.pngthanh.cineverse.common.enums.VoucherAudience;
import com.pngthanh.cineverse.common.enums.VoucherDiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record VoucherAdminRequest(
        @NotBlank @Size(max = 30) String code,
        @NotBlank @Size(max = 120) String title,
        @Size(max = 500) String description,
        @NotNull VoucherDiscountType discountType,
        @NotNull @DecimalMin("0") BigDecimal discountValue,
        @NotNull @DecimalMin("0") BigDecimal minOrderAmount,
        @DecimalMin("0") BigDecimal maxDiscountAmount,
        @NotNull LocalDateTime startsAt,
        @NotNull LocalDateTime expiresAt,
        boolean active,
        boolean publicVisible,
        @NotNull VoucherAudience audience,
        Long movieId,
        Integer usageLimit,
        Integer perUserLimit,
        List<Long> assignedUserIds) {
}
