package com.pngthanh.cineverse.user.service;

import com.pngthanh.cineverse.auth.service.GoogleIdentityService;
import com.pngthanh.cineverse.common.exception.ApiException;
import com.pngthanh.cineverse.user.dto.ChangePasswordRequest;
import com.pngthanh.cineverse.user.dto.CreateLocalCredentialsRequest;
import com.pngthanh.cineverse.user.dto.UpdateProfileRequest;
import com.pngthanh.cineverse.user.dto.UserProfileResponse;
import com.pngthanh.cineverse.user.entity.User;
import com.pngthanh.cineverse.user.repository.UserRepository;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final GoogleIdentityService googleIdentityService;

    public UserService(
            UserRepository users,
            PasswordEncoder passwordEncoder,
            GoogleIdentityService googleIdentityService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.googleIdentityService = googleIdentityService;
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
        String recoveryEmail = normalizeEmail(request.email());
        if (recoveryEmail != null) {
            User recoveryOwner = users.findByRecoveryEmailIgnoreCase(recoveryEmail).orElse(null);
            if (recoveryOwner != null && !recoveryOwner.getId().equals(user.getId())) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "EMAIL_ALREADY_EXISTS",
                        "Email này đã được sử dụng bởi tài khoản khác.");
            }
            User primaryOwner = users.findByEmailIgnoreCase(recoveryEmail).orElse(null);
            if (primaryOwner != null && !primaryOwner.getId().equals(user.getId())) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "EMAIL_ALREADY_EXISTS",
                        "Email này đã được sử dụng bởi tài khoản khác.");
            }
        }

        user.setFullName(request.fullName().trim());
        user.setPhone(request.phone().trim());
        user.setRecoveryEmail(recoveryEmail);
        user.setProvinceCode(normalizeNullable(request.provinceCode()));
        user.setProvinceName(normalizeNullable(request.provinceName()));
        user.setDistrictCode(normalizeNullable(request.districtCode()));
        user.setDistrictName(normalizeNullable(request.districtName()));
        user.setWardCode(normalizeNullable(request.wardCode()));
        user.setWardName(normalizeNullable(request.wardName()));
        user.setAddressDetail(normalizeNullable(request.addressDetail()));
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

    @Transactional
    public UserProfileResponse createLocalCredentials(
            String email,
            CreateLocalCredentialsRequest request) {
        User user = requireByEmail(email);
        if (user.hasLocalCredentials()) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "LOCAL_CREDENTIALS_ALREADY_EXIST",
                    "Tài khoản đã có username và mật khẩu CineVerse.");
        }
        if (!request.password().equals(request.confirmPassword())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "PASSWORD_CONFIRMATION_MISMATCH",
                    "Mật khẩu xác nhận không khớp.");
        }
        String username = request.username().trim().toLowerCase(Locale.ROOT);
        if (users.existsByUsernameIgnoreCase(username)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "USERNAME_ALREADY_EXISTS",
                    "Username đã được sử dụng.");
        }
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        return toResponse(user);
    }

    @Transactional
    public UserProfileResponse linkGoogle(String email, String credential) {
        User user = requireByEmail(email);
        GoogleIdentityService.GoogleIdentity identity = googleIdentityService.verify(credential);

        User subjectOwner = users.findByGoogleSubject(identity.subject()).orElse(null);
        if (subjectOwner != null && !subjectOwner.getId().equals(user.getId())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "GOOGLE_ACCOUNT_ALREADY_LINKED",
                    "Tài khoản Google này đã liên kết với một tài khoản CineVerse khác.");
        }

        User emailOwner = users.findByEmailIgnoreCase(identity.email()).orElse(null);
        if (emailOwner != null && !emailOwner.getId().equals(user.getId())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "GOOGLE_EMAIL_ALREADY_USED",
                    "Email Google này đang thuộc một tài khoản CineVerse khác.");
        }
        User recoveryOwner = users.findByRecoveryEmailIgnoreCase(identity.email()).orElse(null);
        if (recoveryOwner != null && !recoveryOwner.getId().equals(user.getId())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "GOOGLE_EMAIL_ALREADY_USED",
                    "Email Google này đang thuộc một tài khoản CineVerse khác.");
        }

        user.setGoogleSubject(identity.subject());
        user.setGoogleEmail(identity.email());
        return toResponse(user);
    }

    @Transactional
    public UserProfileResponse unlinkGoogle(String email) {
        User user = requireByEmail(email);
        if (!user.hasGoogleAccount()) {
            return toResponse(user);
        }
        if (!user.hasLocalCredentials()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "LOGIN_METHOD_REQUIRED",
                    "Hãy tạo username và mật khẩu CineVerse trước khi ngắt liên kết Google.");
        }
        user.setGoogleSubject(null);
        user.setGoogleEmail(null);
        return toResponse(user);
    }

    public UserProfileResponse toResponse(User user) {
        return new UserProfileResponse(
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
                user.getCreatedAt());
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

    private String normalizeEmail(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
