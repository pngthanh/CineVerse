package com.pngthanh.cineverse.auth.service;

import com.pngthanh.cineverse.auth.dto.ForgotPasswordRequest;
import com.pngthanh.cineverse.auth.dto.ResetPasswordRequest;
import com.pngthanh.cineverse.auth.entity.PasswordResetToken;
import com.pngthanh.cineverse.auth.repository.PasswordResetTokenRepository;
import com.pngthanh.cineverse.common.exception.ApiException;
import com.pngthanh.cineverse.user.entity.User;
import com.pngthanh.cineverse.user.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetService {
    private static final int TOKEN_BYTES = 32;
    private static final long TOKEN_MINUTES = 30;

    private final UserRepository users;
    private final PasswordResetTokenRepository resetTokens;
    private final PasswordResetMailService mailService;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public PasswordResetService(
            UserRepository users,
            PasswordResetTokenRepository resetTokens,
            PasswordResetMailService mailService,
            PasswordEncoder passwordEncoder,
            Clock clock) {
        this.users = users;
        this.resetTokens = resetTokens;
        this.mailService = mailService;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional
    public void request(ForgotPasswordRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        User user = users.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) {
            return;
        }

        resetTokens.deleteAllByUser(user);
        String rawToken = createRawToken();
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash(hash(rawToken));
        token.setExpiresAt(Instant.now(clock).plus(TOKEN_MINUTES, ChronoUnit.MINUTES));
        resetTokens.save(token);
        mailService.sendResetLink(user.getEmail(), user.getFullName(), rawToken);
    }

    @Transactional
    public void reset(ResetPasswordRequest request) {
        requireMatchingPasswords(request.newPassword(), request.confirmPassword());
        PasswordResetToken token = resetTokens.findByTokenHash(hash(request.token()))
                .orElseThrow(this::invalidToken);
        Instant now = Instant.now(clock);
        if (token.getUsedAt() != null || !token.getExpiresAt().isAfter(now)) {
            throw invalidToken();
        }

        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        token.setUsedAt(now);
    }

    private void requireMatchingPasswords(String password, String confirmation) {
        if (!password.equals(confirmation)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "PASSWORD_CONFIRMATION_MISMATCH",
                    "Mật khẩu xác nhận không khớp.");
        }
    }

    private String createRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] value = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(value);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available.", ex);
        }
    }

    private ApiException invalidToken() {
        return new ApiException(
                HttpStatus.BAD_REQUEST,
                "INVALID_RESET_TOKEN",
                "Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn.");
    }
}
