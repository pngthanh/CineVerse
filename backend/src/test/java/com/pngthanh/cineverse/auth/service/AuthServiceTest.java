package com.pngthanh.cineverse.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pngthanh.cineverse.auth.dto.LoginRequest;
import com.pngthanh.cineverse.auth.dto.RegisterRequest;
import com.pngthanh.cineverse.common.enums.UserStatus;
import com.pngthanh.cineverse.common.exception.ApiException;
import com.pngthanh.cineverse.user.entity.User;
import com.pngthanh.cineverse.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {
    private UserRepository users;
    private PasswordEncoder passwordEncoder;
    private JwtTokenService tokens;
    private AuthService service;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        tokens = mock(JwtTokenService.class);
        service = new AuthService(users, passwordEncoder, tokens);
    }

    @Test
    void registrationRejectsDuplicateUsername() {
        when(users.existsByUsernameIgnoreCase("member01")).thenReturn(true);

        ApiException exception = assertThrows(ApiException.class, () -> service.register(
                new RegisterRequest(
                        "Member",
                        "member01",
                        "Password@123",
                        "Password@123",
                        "0987654321")));

        assertEquals("USERNAME_ALREADY_EXISTS", exception.getCode());
    }

    @Test
    void registrationRejectsMismatchedConfirmation() {
        ApiException exception = assertThrows(ApiException.class, () -> service.register(
                new RegisterRequest(
                        "Member",
                        "member01",
                        "Password@123",
                        "Different@123",
                        "0987654321")));

        assertEquals("PASSWORD_CONFIRMATION_MISMATCH", exception.getCode());
    }

    @Test
    void loginAcceptsUsername() {
        User user = new User();
        user.setEmail("local+member01@local.cineverse.invalid");
        user.setUsername("member01");
        user.setPasswordHash("encoded");
        user.setStatus(UserStatus.ACTIVE);
        when(users.findByUsernameIgnoreCase("member01")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password@123", "encoded")).thenReturn(true);
        when(tokens.create(user)).thenReturn("jwt");
        when(tokens.expiresInSeconds()).thenReturn(7200L);

        service.login(new LoginRequest("member01", "Password@123"));

        verify(tokens).create(user);
    }

    @Test
    void registrationNormalizesUsername() {
        when(passwordEncoder.encode("Password@123")).thenReturn("encoded");
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tokens.create(any(User.class))).thenReturn("jwt");

        service.register(new RegisterRequest(
                "Member",
                "Member.01",
                "Password@123",
                "Password@123",
                "0987654321"));

        verify(users).existsByUsernameIgnoreCase("member.01");
    }
}
