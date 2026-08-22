package com.pngthanh.cineverse.admin.controller;

import com.pngthanh.cineverse.admin.service.AdminService;
import com.pngthanh.cineverse.booking.dto.BookingResponse;
import com.pngthanh.cineverse.payment.dto.VnPayRefundResponse;
import com.pngthanh.cineverse.payment.service.VnPayRefundService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/bookings")
public class AdminBookingController {
    private final AdminService adminService;
    private final VnPayRefundService refundService;

    public AdminBookingController(AdminService adminService, VnPayRefundService refundService) {
        this.adminService = adminService;
        this.refundService = refundService;
    }

    @GetMapping
    public List<BookingResponse> list() {
        return adminService.listBookings();
    }

    @GetMapping("/{id}")
    public BookingResponse get(@PathVariable Long id) {
        return adminService.getBooking(id);
    }
    @PostMapping("/{id}/refund")
    public VnPayRefundResponse refund(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest request) {
        return refundService.refund(id, authentication.getName(), request);
    }
}
