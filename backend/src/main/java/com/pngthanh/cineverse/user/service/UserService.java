package com.pngthanh.cineverse.user.service;

import com.pngthanh.cineverse.common.exception.ApiException;
import com.pngthanh.cineverse.user.dto.ChangePasswordRequest;
import com.pngthanh.cineverse.user.dto.UpdateProfileRequest;
import com.pngthanh.cineverse.user.dto.UserProfileResponse;
import com.pngthanh.cineverse.user.entity.User;
import com.pngthanh.cineverse.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public User requireByEmail(String email) {
        return users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "USER_NOT_FOUND",
                        "Không tìm thấy người dùng."));
    }

    @Transactional(readOnly = true)
    public UserProfileResponse profile(String email) {
        return toResponse(requireByEmail(email));
    }

    @Transactional
    public UserProfileResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = requireByEmail(email);
        user.setFullName(request.fullName().trim());
        return toResponse(user);
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = requireByEmail(email);
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "PASSWORD_CONFIRMATION_MISMATCH",
                    "Mật khẩu mới và xác nhận mật khẩu không khớp.");
        }
        if (!user.hasLocalCredentials()
                || !passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "CURRENT_PASSWORD_INVALID",
                    "Mật khẩu hiện tại không đúng.");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "PASSWORD_NOT_CHANGED",
                    "Mật khẩu mới phải khác mật khẩu hiện tại.");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    }

    public UserProfileResponse toResponse(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getUsername(),
                user.hasLocalCredentials(),
                user.getRole().name(),
                user.getStatus().name(),
                user.getCreatedAt());
    }
}
