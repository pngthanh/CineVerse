package com.pngthanh.cineverse.auth.controller;

import com.pngthanh.cineverse.auth.dto.AuthResponse;
import com.pngthanh.cineverse.auth.dto.ForgotPasswordRequest;
import com.pngthanh.cineverse.auth.dto.LoginRequest;
import com.pngthanh.cineverse.auth.dto.MessageResponse;
import com.pngthanh.cineverse.auth.dto.RegisterRequest;
import com.pngthanh.cineverse.auth.dto.ResetPasswordRequest;
import com.pngthanh.cineverse.auth.service.AuthService;
import com.pngthanh.cineverse.auth.service.PasswordResetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthService authService, PasswordResetService passwordResetService) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/forgot-password")
    public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.request(request);
        return new MessageResponse(
                "Nếu email tồn tại trong hệ thống, CineVerse đã gửi liên kết đặt lại mật khẩu.");
    }

    @PostMapping("/reset-password")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.reset(request);
        return new MessageResponse("Đặt lại mật khẩu thành công. Bạn có thể đăng nhập bằng mật khẩu mới.");
    }
}
