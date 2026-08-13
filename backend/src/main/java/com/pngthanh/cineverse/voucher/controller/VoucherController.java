package com.pngthanh.cineverse.voucher.controller;

import com.pngthanh.cineverse.voucher.dto.VoucherQuoteRequest;
import com.pngthanh.cineverse.voucher.dto.VoucherQuoteResponse;
import com.pngthanh.cineverse.voucher.service.VoucherService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vouchers")
public class VoucherController {
    private final VoucherService voucherService;

    public VoucherController(VoucherService voucherService) {
        this.voucherService = voucherService;
    }

    @PostMapping("/quote")
    public VoucherQuoteResponse quote(@Valid @RequestBody VoucherQuoteRequest request) {
        return voucherService.quote(request.code(), request.subtotal());
    }
}
