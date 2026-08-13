package com.pngthanh.cineverse.voucher.service;

import com.pngthanh.cineverse.common.exception.ApiException;
import com.pngthanh.cineverse.voucher.dto.VoucherQuoteResponse;
import com.pngthanh.cineverse.voucher.entity.Voucher;
import com.pngthanh.cineverse.voucher.repository.VoucherRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VoucherService {
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private final VoucherRepository vouchers;

    public VoucherService(VoucherRepository vouchers) {
        this.vouchers = vouchers;
    }

    @Transactional(readOnly = true)
    public VoucherQuoteResponse quote(String rawCode, BigDecimal subtotal) {
        Voucher voucher = requireUsable(rawCode, subtotal);
        BigDecimal discount = calculateDiscount(voucher, subtotal);
        return new VoucherQuoteResponse(
                voucher.getCode(),
                voucher.getDiscountPercent(),
                subtotal,
                discount,
                subtotal.subtract(discount));
    }

    @Transactional(readOnly = true)
    public AppliedVoucher apply(String rawCode, BigDecimal subtotal) {
        if (rawCode == null || rawCode.isBlank()) {
            return new AppliedVoucher(null, BigDecimal.ZERO, subtotal);
        }
        Voucher voucher = requireUsable(rawCode, subtotal);
        BigDecimal discount = calculateDiscount(voucher, subtotal);
        return new AppliedVoucher(voucher.getCode(), discount, subtotal.subtract(discount));
    }

    private Voucher requireUsable(String rawCode, BigDecimal subtotal) {
        String code = rawCode.trim().toUpperCase(Locale.ROOT);
        Voucher voucher = vouchers.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "VOUCHER_NOT_FOUND",
                        "Mã ưu đãi không tồn tại."));
        LocalDateTime now = LocalDateTime.now();
        if (!voucher.isActive() || now.isBefore(voucher.getStartsAt()) || now.isAfter(voucher.getExpiresAt())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "VOUCHER_NOT_ACTIVE",
                    "Mã ưu đãi chưa có hiệu lực hoặc đã hết hạn.");
        }
        if (subtotal.compareTo(voucher.getMinOrderAmount()) < 0) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "VOUCHER_MIN_ORDER",
                    "Đơn hàng chưa đạt giá trị tối thiểu để dùng mã này.");
        }
        return voucher;
    }

    private BigDecimal calculateDiscount(Voucher voucher, BigDecimal subtotal) {
        BigDecimal discount = subtotal
                .multiply(voucher.getDiscountPercent())
                .divide(ONE_HUNDRED, 0, RoundingMode.DOWN);
        if (voucher.getMaxDiscountAmount() != null) {
            discount = discount.min(voucher.getMaxDiscountAmount());
        }
        return discount.min(subtotal);
    }

    public record AppliedVoucher(String code, BigDecimal discountAmount, BigDecimal totalAmount) {
    }
}
