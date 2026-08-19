package com.pngthanh.cineverse.auth.repository;

import com.pngthanh.cineverse.auth.entity.PasswordResetToken;
import com.pngthanh.cineverse.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);
    void deleteAllByUser(User user);
}
