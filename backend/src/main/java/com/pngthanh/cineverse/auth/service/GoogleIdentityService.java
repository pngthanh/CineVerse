package com.pngthanh.cineverse.auth.service;

import com.pngthanh.cineverse.common.exception.ApiException;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;

@Service
public class GoogleIdentityService {
    private static final String GOOGLE_JWK_SET_URI = "https://www.googleapis.com/oauth2/v3/certs";
    private static final Set<String> GOOGLE_ISSUERS = Set.of(
            "accounts.google.com",
            "https://accounts.google.com");

    private final String clientId;
    private final NimbusJwtDecoder decoder;

    public GoogleIdentityService(@Value("${app.google.client-id:}") String clientId) {
        this.clientId = clientId == null ? "" : clientId.trim();
        this.decoder = NimbusJwtDecoder.withJwkSetUri(GOOGLE_JWK_SET_URI).build();
        this.decoder.setJwtValidator(validator());
    }

    public GoogleIdentity verify(String credential) {
        if (clientId.isBlank()) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "GOOGLE_AUTH_NOT_CONFIGURED",
                    "Đăng nhập Google chưa được cấu hình trên máy chủ.");
        }
        try {
            Jwt jwt = decoder.decode(credential);
            String subject = jwt.getSubject();
            String email = jwt.getClaimAsString("email");
            String name = jwt.getClaimAsString("name");
            Boolean emailVerified = jwt.getClaim("email_verified");
            if (subject == null || subject.isBlank() || email == null || email.isBlank()
                    || !Boolean.TRUE.equals(emailVerified)) {
                throw invalidCredential();
            }
            return new GoogleIdentity(subject, email.trim().toLowerCase(), name);
        } catch (JwtException ex) {
            throw invalidCredential();
        }
    }

    private OAuth2TokenValidator<Jwt> validator() {
        return jwt -> {
            OAuth2Error error = validate(jwt);
            return error == null
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(error);
        };
    }

    private OAuth2Error validate(Jwt jwt) {
        Instant expiresAt = jwt.getExpiresAt();
        if (expiresAt == null || !expiresAt.isAfter(Instant.now())) {
            return error("google_token_expired");
        }
        String issuer = jwt.getIssuer() == null ? null : jwt.getIssuer().toString();
        if (!GOOGLE_ISSUERS.contains(issuer)) {
            return error("google_issuer_invalid");
        }
        List<String> audience = jwt.getAudience();
        if (audience == null || !audience.contains(clientId)) {
            return error("google_audience_invalid");
        }
        return null;
    }

    private OAuth2Error error(String code) {
        return new OAuth2Error(code, "Google ID token is invalid.", null);
    }

    private ApiException invalidCredential() {
        return new ApiException(
                HttpStatus.UNAUTHORIZED,
                "GOOGLE_CREDENTIAL_INVALID",
                "Không thể xác minh tài khoản Google. Vui lòng thử lại.");
    }

    public record GoogleIdentity(String subject, String email, String fullName) {
    }
}
