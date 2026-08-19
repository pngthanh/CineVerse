package com.pngthanh.cineverse.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pngthanh.cineverse.auth.dto.ForgotPasswordRequest;
import com.pngthanh.cineverse.auth.dto.ResetPasswordRequest;
import com.pngthanh.cineverse.auth.entity.PasswordResetToken;
import com.pngthanh.cineverse.auth.repository.PasswordResetTokenRepository;
import com.pngthanh.cineverse.common.exception.ApiException;
import com.pngthanh.cineverse.user.entity.User;
import com.pngthanh.cineverse.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class PasswordResetServiceTest {
    private UserRepository users;
    private PasswordResetTokenRepository tokens;
    private PasswordResetMailService mailService;
    private PasswordEncoder encoder;
    private PasswordResetService service;
    private final Instant now = Instant.parse("2026-08-16T00:00:00Z");

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        tokens = mock(PasswordResetTokenRepository.class);
        mailService = mock(PasswordResetMailService.class);
        encoder = mock(PasswordEncoder.class);
        service = new PasswordResetService(
                users,
                tokens,
                mailService,
                encoder,
                Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    void forgotPasswordDoesNotRevealUnknownEmail() {
        service.request(new ForgotPasswordRequest("unknown@cineverse.vn"));

        verify(users).findByEmailIgnoreCase("unknown@cineverse.vn");
    }

    @Test
    void forgotPasswordCreatesTokenAndSendsMail() {
        User user = new User();
        user.setFullName("Member");
        user.setEmail("member@cineverse.vn");
        when(users.findByEmailIgnoreCase("member@cineverse.vn")).thenReturn(Optional.of(user));
        when(tokens.save(any(PasswordResetToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.request(new ForgotPasswordRequest("member@cineverse.vn"));

        verify(tokens).deleteAllByUser(user);
        verify(mailService).sendResetLink(anyString(), anyString(), anyString());
    }

    @Test
    void resetRejectsMismatchedPassword() {
        ApiException exception = assertThrows(ApiException.class, () -> service.reset(
                new ResetPasswordRequest("token", "Password@123", "Different@123")));

        assertEquals("PASSWORD_CONFIRMATION_MISMATCH", exception.getCode());
    }
}
