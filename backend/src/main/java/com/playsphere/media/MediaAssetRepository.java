package com.playsphere.media;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaAssetRepository extends JpaRepository<MediaAsset, String> {
    Optional<MediaAsset> findByOwnerUserIdAndSecureUrl(String ownerUserId, String secureUrl);
}
