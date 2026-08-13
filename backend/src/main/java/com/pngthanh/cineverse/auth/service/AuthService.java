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
        String email = normalizeEmail(request.email());
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "EMAIL_ALREADY_EXISTS",
                    "Email đã được sử dụng.");
        }

        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.CUSTOMER);
        user.setStatus(UserStatus.ACTIVE);
        return response(users.save(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = users.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .orElseThrow(() -> new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "INVALID_CREDENTIALS",
                        "Email hoặc mật khẩu không đúng."));

        boolean invalidAccount = user.getStatus() != UserStatus.ACTIVE;
        boolean invalidPassword = !passwordEncoder.matches(request.password(), user.getPasswordHash());
        if (invalidAccount || invalidPassword) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "INVALID_CREDENTIALS",
                    "Email hoặc mật khẩu không đúng.");
        }
        return response(user);
    }

    private AuthResponse response(User user) {
        return new AuthResponse(
                tokens.create(user),
                "Bearer",
                tokens.expiresInSeconds(),
                new AuthResponse.UserSummary(
                        user.getId(),
                        user.getFullName(),
                        user.getEmail(),
                        user.getRole().name(),
                        user.getStatus().name(),
                        user.getCreatedAt()));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
