package com.pngthanh.cineverse.auth.service;

import com.pngthanh.cineverse.auth.dto.AuthResponse;
import com.pngthanh.cineverse.auth.dto.GoogleCredentialRequest;
import com.pngthanh.cineverse.common.enums.Role;
import com.pngthanh.cineverse.common.enums.UserStatus;
import com.pngthanh.cineverse.common.exception.ApiException;
import com.pngthanh.cineverse.user.entity.User;
import com.pngthanh.cineverse.user.repository.UserRepository;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoogleAuthService {
    private final UserRepository users;
    private final GoogleIdentityService googleIdentityService;
    private final AuthService authService;

    public GoogleAuthService(
            UserRepository users,
            GoogleIdentityService googleIdentityService,
            AuthService authService) {
        this.users = users;
        this.googleIdentityService = googleIdentityService;
        this.authService = authService;
    }

    @Transactional
    public AuthResponse login(GoogleCredentialRequest request) {
        GoogleIdentityService.GoogleIdentity identity =
                googleIdentityService.verify(request.credential());

        Optional<User> linked = users.findByGoogleSubject(identity.subject());
        if (linked.isPresent()) {
            User user = linked.get();
            requireActive(user);
            user.setGoogleEmail(identity.email());
            return authService.response(user);
        }

        User emailOwner = users.findByEmailIgnoreCase(identity.email()).orElse(null);
        if (emailOwner != null) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "GOOGLE_LINK_REQUIRED",
                    "Email này đã có tài khoản CineVerse. Hãy đăng nhập bằng tài khoản hiện tại rồi liên kết Google trong Tài khoản của tôi.");
        }

        User user = new User();
        user.setFullName(normalizeName(identity.fullName(), identity.email()));
        user.setEmail(identity.email());
        user.setGoogleSubject(identity.subject());
        user.setGoogleEmail(identity.email());
        user.setRole(Role.CUSTOMER);
        user.setStatus(UserStatus.ACTIVE);
        return authService.response(users.save(user));
    }

    private void requireActive(User user) {
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "ACCOUNT_NOT_ACTIVE",
                    "Tài khoản CineVerse hiện không hoạt động.");
        }
    }

    private String normalizeName(String name, String email) {
        if (name != null && !name.isBlank()) {
            return name.trim();
        }
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : "CineVerse Member";
    }
}
