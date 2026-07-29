package com.playsphere.media;

import com.playsphere.common.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class MediaOwnershipService {
    private final MediaAssetRepository assets;

    public MediaOwnershipService(MediaAssetRepository assets) {
        this.assets = assets;
    }

    public String requireOwnedPurpose(String ownerUserId, String secureUrl, String purpose) {
        if (secureUrl == null || secureUrl.isBlank()) return null;
        MediaAsset asset = assets.findByOwnerUserIdAndSecureUrl(ownerUserId, secureUrl.trim())
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.BAD_REQUEST,
                        "Upload the image through PlaySphere before using it"
                ));
        String marker = "/" + purpose + "/";
        if (!asset.getPublicId().contains(marker)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "This image was uploaded for a different purpose");
        }
        return asset.getSecureUrl();
    }
}
