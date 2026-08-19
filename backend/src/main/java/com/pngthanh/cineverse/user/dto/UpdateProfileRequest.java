package com.pngthanh.cineverse.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank @Size(max = 100) String fullName,
        @NotBlank
        @Pattern(regexp = "^(0|\\+84)[0-9]{9,10}$", message = "Số điện thoại không hợp lệ.")
        String phone,
        @Email @Size(max = 150) String email,
        @Size(max = 20) String provinceCode,
        @Size(max = 100) String provinceName,
        @Size(max = 20) String districtCode,
        @Size(max = 100) String districtName,
        @Size(max = 20) String wardCode,
        @Size(max = 100) String wardName,
        @Size(max = 255) String addressDetail) {
}
