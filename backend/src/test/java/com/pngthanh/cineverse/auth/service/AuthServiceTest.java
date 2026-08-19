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

        service = new AuthService(
                users,
                passwordEncoder,
                tokens);
    }

    @Test
    void registrationRejectsDuplicateUsername() {
        when(users.existsByUsernameIgnoreCase("member01"))
                .thenReturn(true);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.register(
                        registerRequest(
                                "member01",
                                "Password@123",
                                "Password@123")));

        assertEquals(
                "USERNAME_ALREADY_EXISTS",
                exception.getCode());
    }

    @Test
    void registrationRejectsMismatchedConfirmation() {
        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.register(
                        registerRequest(
                                "member01",
                                "Password@123",
                                "Different@123")));

        assertEquals(
                "PASSWORD_CONFIRMATION_MISMATCH",
                exception.getCode());
    }

    @Test
    void loginAcceptsUsername() {
        User user = new User();

        user.setEmail("member@cineverse.vn");
        user.setUsername("member01");
        user.setPasswordHash("encoded");
        user.setStatus(UserStatus.ACTIVE);

        when(users.findByUsernameIgnoreCase("member01"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "Password@123",
                "encoded"))
                .thenReturn(true);

        when(tokens.create(user))
                .thenReturn("jwt");

        when(tokens.expiresInSeconds())
                .thenReturn(7200L);

        service.login(
                new LoginRequest(
                        "member01",
                        "Password@123"));

        verify(tokens).create(user);
    }

    @Test
    void registrationNormalizesUsername() {
        when(passwordEncoder.encode("Password@123"))
                .thenReturn("encoded");

        when(users.save(any(User.class)))
                .thenAnswer(
                        invocation ->
                                invocation.getArgument(0));

        when(tokens.create(any(User.class)))
                .thenReturn("jwt");

        service.register(
                new RegisterRequest(
                        "Member",
                        "MEMBER@CINEVERSE.VN",
                        "Member.01",
                        "Password@123",
                        "Password@123",
                        "0912345678",
                        "92",
                        "Thành phố Cần Thơ",
                        "916",
                        "Quận Ninh Kiều",
                        "31117",
                        "Phường An Khánh",
                        "123 Nguyễn Văn Cừ"));

        verify(users)
                .existsByUsernameIgnoreCase("member.01");
    }

    private RegisterRequest registerRequest(
            String username,
            String password,
            String confirmPassword) {

        return new RegisterRequest(
                "Member",
                "member@cineverse.vn",
                username,
                password,
                confirmPassword,
                "0912345678",
                "92",
                "Thành phố Cần Thơ",
                "916",
                "Quận Ninh Kiều",
                "31117",
                "Phường An Khánh",
                "123 Nguyễn Văn Cừ");
    }
}