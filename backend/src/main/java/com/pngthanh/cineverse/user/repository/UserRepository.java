package com.pngthanh.cineverse.user.repository;

import com.pngthanh.cineverse.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);
    Optional<User> findByRecoveryEmailIgnoreCase(String recoveryEmail);
    Optional<User> findByUsernameIgnoreCase(String username);
    Optional<User> findByGoogleSubject(String googleSubject);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByRecoveryEmailIgnoreCase(String recoveryEmail);
    boolean existsByUsernameIgnoreCase(String username);
    boolean existsByGoogleSubject(String googleSubject);
}
