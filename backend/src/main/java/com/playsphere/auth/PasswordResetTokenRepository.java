package com.playsphere.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, String> {
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);
    Optional<PasswordResetToken> findTopByUser_IdOrderByCreatedAtDesc(String userId);
    void deleteAllByUser_Id(String userId);
}
