package com.pngthanh.cineverse.auth.service;

import com.pngthanh.cineverse.auth.dto.AuthResponse;
import com.pngthanh.cineverse.auth.dto.LoginRequest;
import com.pngthanh.cineverse.auth.dto.RegisterRequest;
import com.pngthanh.cineverse.common.enums.Role;
import com.pngthanh.cineverse.common.enums.UserStatus;
import com.pngthanh.cineverse.common.exception.ApiException;
import com.pngthanh.cineverse.user.entity.User;
import com.pngthanh.cineverse.user.repository.UserRepository;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private static final String LOCAL_EMAIL_SUFFIX = "@local.cineverse.invalid";

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService tokens;

    public AuthService(
            UserRepository users,
            PasswordEncoder passwordEncoder,
            JwtTokenService tokens) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = normalizeUsername(request.username());
        if (users.existsByUsernameIgnoreCase(username)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "USERNAME_ALREADY_EXISTS",
                    "Username đã được sử dụng.");
        }
        if (!request.password().equals(request.confirmPassword())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "PASSWORD_CONFIRMATION_MISMATCH",
                    "Mật khẩu xác nhận không khớp.");
        }

        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(localIdentityEmail(username));
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setPhone(request.phone().trim());
        user.setRole(Role.CUSTOMER);
        user.setStatus(UserStatus.ACTIVE);
        return response(users.save(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String identifier = request.identifier().trim();
        User user;
        if (identifier.contains("@")) {
            String email = normalizeEmail(identifier);
            user = users.findByRecoveryEmailIgnoreCase(email)
                    .orElseGet(() -> users.findByEmailIgnoreCase(email).orElse(null));
        } else {
            user = users.findByUsernameIgnoreCase(normalizeUsername(identifier)).orElse(null);
        }
        if (user == null || user.getStatus() != UserStatus.ACTIVE || !validPassword(user, request.password())) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "INVALID_CREDENTIALS",
                    "Username/email hoặc mật khẩu không đúng.");
        }
        return response(user);
    }

    private boolean validPassword(User user, String rawPassword) {
        return user.getPasswordHash() != null
                && !user.getPasswordHash().isBlank()
                && passwordEncoder.matches(rawPassword, user.getPasswordHash());
    }

    public AuthResponse response(User user) {
        return new AuthResponse(
                tokens.create(user),
                "Bearer",
                tokens.expiresInSeconds(),
                new AuthResponse.UserSummary(
                        user.getId(),
                        user.getFullName(),
                        publicEmail(user),
                        user.getUsername(),
                        user.hasLocalCredentials(),
                        user.hasGoogleAccount(),
                        user.getGoogleEmail(),
                        user.getPhone(),
                        user.getProvinceCode(),
                        user.getProvinceName(),
                        user.getDistrictCode(),
                        user.getDistrictName(),
                        user.getWardCode(),
                        user.getWardName(),
                        user.getAddressDetail(),
                        user.getRole().name(),
                        user.getStatus().name(),
                        user.getCreatedAt()));
    }

    private String publicEmail(User user) {
        if (user.getRecoveryEmail() != null && !user.getRecoveryEmail().isBlank()) {
            return user.getRecoveryEmail();
        }
        if (user.hasGoogleAccount()) {
            return user.getGoogleEmail();
        }
        return null;
    }

    private String localIdentityEmail(String username) {
        return "local+" + username + LOCAL_EMAIL_SUFFIX;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }
}
