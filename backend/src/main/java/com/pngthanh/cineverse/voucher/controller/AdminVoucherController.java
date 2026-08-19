package com.pngthanh.cineverse.voucher.controller;

import com.pngthanh.cineverse.voucher.dto.VoucherAdminRequest;
import com.pngthanh.cineverse.voucher.dto.VoucherResponse;
import com.pngthanh.cineverse.voucher.service.VoucherService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/vouchers")
public class AdminVoucherController {
    private final VoucherService voucherService;

    public AdminVoucherController(VoucherService voucherService) {
        this.voucherService = voucherService;
    }

    @GetMapping
    public List<VoucherResponse> list() {
        return voucherService.adminList();
    }

    @PostMapping
    public VoucherResponse create(@Valid @RequestBody VoucherAdminRequest request) {
        return voucherService.create(request);
    }

    @PutMapping("/{id}")
    public VoucherResponse update(
            @PathVariable Long id,
            @Valid @RequestBody VoucherAdminRequest request) {
        return voucherService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public VoucherResponse deactivate(@PathVariable Long id) {
        return voucherService.deactivate(id);
    }
}
