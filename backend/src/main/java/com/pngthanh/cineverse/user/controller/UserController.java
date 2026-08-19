package com.pngthanh.cineverse.user.controller;

import com.pngthanh.cineverse.auth.dto.MessageResponse;
import com.pngthanh.cineverse.user.dto.ChangePasswordRequest;
import com.pngthanh.cineverse.user.dto.UpdateProfileRequest;
import com.pngthanh.cineverse.user.dto.UserProfileResponse;
import com.pngthanh.cineverse.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public UserProfileResponse profile(Authentication authentication) {
        return userService.profile(authentication.getName());
    }

    @PatchMapping
    public UserProfileResponse update(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(authentication.getName(), request);
    }

    @PostMapping("/password")
    public MessageResponse changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(authentication.getName(), request);
        return new MessageResponse("Đổi mật khẩu thành công.");
    }
}
