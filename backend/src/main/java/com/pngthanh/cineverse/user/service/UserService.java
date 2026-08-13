package com.pngthanh.cineverse.user.service;

import com.pngthanh.cineverse.common.exception.ApiException;
import com.pngthanh.cineverse.user.dto.UpdateProfileRequest;
import com.pngthanh.cineverse.user.dto.UserProfileResponse;
import com.pngthanh.cineverse.user.entity.User;
import com.pngthanh.cineverse.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository users;

    public UserService(UserRepository users) {
        this.users = users;
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

    public UserProfileResponse toResponse(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().name(),
                user.getStatus().name(),
                user.getCreatedAt());
    }
}
