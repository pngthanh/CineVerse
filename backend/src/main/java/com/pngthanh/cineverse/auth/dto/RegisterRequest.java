package com.pngthanh.cineverse.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 100) String fullName,
        @NotBlank @Email String email,
        @NotBlank
        @Size(min = 4, max = 40)
        @Pattern(
                regexp = "^[A-Za-z0-9._-]+$",
                message = "Username chỉ được chứa chữ, số, dấu chấm, gạch dưới hoặc gạch ngang.")
        String username,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Size(min = 8, max = 72) String confirmPassword,
        @NotBlank
        @Pattern(regexp = "^(0|\\+84)[0-9]{9,10}$", message = "Số điện thoại không hợp lệ.")
        String phone,
        @NotBlank String provinceCode,
        @NotBlank String provinceName,
        @NotBlank String districtCode,
        @NotBlank String districtName,
        @NotBlank String wardCode,
        @NotBlank String wardName,
        @Size(max = 255) String addressDetail) {
}
