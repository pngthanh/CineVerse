package com.pngthanh.cineverse.admin.controller;

import com.pngthanh.cineverse.admin.service.AdminService;
import com.pngthanh.cineverse.booking.dto.BookingResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/bookings")
public class AdminBookingController {
    private final AdminService adminService;

    public AdminBookingController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public List<BookingResponse> list() {
        return adminService.listBookings();
    }

    @GetMapping("/{id}")
    public BookingResponse get(@PathVariable Long id) {
        return adminService.getBooking(id);
    }
}
