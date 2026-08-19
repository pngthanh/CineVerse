package com.pngthanh.cineverse.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pngthanh.cineverse.auth.dto.AuthResponse;
import com.pngthanh.cineverse.auth.dto.GoogleCredentialRequest;
import com.pngthanh.cineverse.common.exception.ApiException;
import com.pngthanh.cineverse.user.entity.User;
import com.pngthanh.cineverse.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GoogleAuthServiceTest {
    private UserRepository users;
    private GoogleIdentityService identities;
    private AuthService authService;
    private GoogleAuthService service;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        identities = mock(GoogleIdentityService.class);
        authService = mock(AuthService.class);
        service = new GoogleAuthService(users, identities, authService);
    }

    @Test
    void googleLoginCreatesGoogleOnlyUser() {
        GoogleIdentityService.GoogleIdentity identity =
                new GoogleIdentityService.GoogleIdentity("google-sub", "member@gmail.com", "Member");
        when(identities.verify("credential")).thenReturn(identity);
        when(users.findByGoogleSubject("google-sub")).thenReturn(Optional.empty());
        when(users.findByEmailIgnoreCase("member@gmail.com")).thenReturn(Optional.empty());
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(authService.response(any(User.class))).thenReturn(mock(AuthResponse.class));

        service.login(new GoogleCredentialRequest("credential"));

        verify(users).save(any(User.class));
    }

    @Test
    void googleLoginDoesNotAutoLinkExistingEmail() {
        GoogleIdentityService.GoogleIdentity identity =
                new GoogleIdentityService.GoogleIdentity("google-sub", "member@gmail.com", "Member");
        when(identities.verify("credential")).thenReturn(identity);
        when(users.findByGoogleSubject("google-sub")).thenReturn(Optional.empty());
        when(users.findByEmailIgnoreCase("member@gmail.com")).thenReturn(Optional.of(new User()));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.login(new GoogleCredentialRequest("credential")));

        assertEquals("GOOGLE_LINK_REQUIRED", exception.getCode());
    }
}
