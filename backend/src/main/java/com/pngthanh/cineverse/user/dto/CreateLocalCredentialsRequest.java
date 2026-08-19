package com.pngthanh.cineverse.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateLocalCredentialsRequest(
        @NotBlank
        @Size(min = 4, max = 40)
        @Pattern(
                regexp = "^[A-Za-z0-9._-]+$",
                message = "Username chỉ được chứa chữ, số, dấu chấm, gạch dưới hoặc gạch ngang.")
        String username,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Size(min = 8, max = 72) String confirmPassword) {
}
