package com.pngthanh.cineverse.admin.dto;

import com.pngthanh.cineverse.common.enums.Role;
import jakarta.validation.constraints.NotNull;

public record StaffAssignmentRequest(
        @NotNull Role role,
        Long cinemaId) {
}
