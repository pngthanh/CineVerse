package com.pngthanh.cineverse.admin.controller;

import com.pngthanh.cineverse.admin.dto.StaffAssignmentRequest;
import com.pngthanh.cineverse.admin.service.AdminService;
import com.pngthanh.cineverse.common.enums.UserStatus;
import com.pngthanh.cineverse.user.dto.UserProfileResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {
    private final AdminService adminService;

    public AdminUserController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public List<UserProfileResponse> list() {
        return adminService.listUsers();
    }

    @PatchMapping("/{id}/status")
    public UserProfileResponse updateStatus(
            @PathVariable Long id,
            @RequestParam UserStatus status) {
        return adminService.updateUserStatus(id, status);
    }

    @PatchMapping("/{id}/staff-assignment")
    public UserProfileResponse updateStaffAssignment(
            @PathVariable Long id,
            @Valid @RequestBody StaffAssignmentRequest request) {
        return adminService.updateStaffAssignment(id, request);
    }
}
