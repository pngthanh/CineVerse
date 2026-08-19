package com.pngthanh.cineverse.voucher.controller;

import com.pngthanh.cineverse.voucher.dto.VoucherQuoteRequest;
import com.pngthanh.cineverse.voucher.dto.VoucherQuoteResponse;
import com.pngthanh.cineverse.voucher.dto.VoucherResponse;
import com.pngthanh.cineverse.voucher.service.VoucherService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vouchers")
public class VoucherController {
    private final VoucherService voucherService;

    public VoucherController(VoucherService voucherService) {
        this.voucherService = voucherService;
    }

    @GetMapping("/public")
    public List<VoucherResponse> publicVouchers(Principal principal) {
        return voucherService.publicVouchers(principal == null ? null : principal.getName());
    }

    @PostMapping("/quote")
    public VoucherQuoteResponse quote(
            Principal principal,
            @Valid @RequestBody VoucherQuoteRequest request) {
        return voucherService.quote(
                principal.getName(),
                request.code(),
                request.subtotal(),
                request.showtimeId());
    }

    @GetMapping("/saved")
    public List<VoucherResponse> saved(Principal principal) {
        return voucherService.saved(principal.getName());
    }

    @PostMapping("/{id}/save")
    public VoucherResponse save(Principal principal, @PathVariable Long id) {
        return voucherService.save(principal.getName(), id);
    }

    @DeleteMapping("/{id}/save")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsave(Principal principal, @PathVariable Long id) {
        voucherService.unsave(principal.getName(), id);
    }
}
