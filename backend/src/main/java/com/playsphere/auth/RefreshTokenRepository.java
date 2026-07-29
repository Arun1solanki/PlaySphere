package com.playsphere.auth;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    Optional<RefreshToken> findByIdAndUser_Id(String id, String userId);
    List<RefreshToken> findAllByUser_IdAndRevokedAtIsNullOrderByLastUsedAtDesc(String userId);
}
