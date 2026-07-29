package com.playsphere.media;

import com.playsphere.common.ApiResponse;
import com.playsphere.common.BusinessException;
import com.playsphere.user.CurrentUserService;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/media")
public class MediaController {
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );
    private static final Set<String> ALLOWED_PURPOSES = Set.of(
            "profiles",
            "teams",
            "turfs",
            "events",
            "reviews",
            "general"
    );

    private final MediaStorage storage;
    private final MediaAssetRepository assets;
    private final CurrentUserService current;

    public MediaController(
            MediaStorage storage,
            MediaAssetRepository assets,
            CurrentUserService current
    ) {
        this.storage = storage;
        this.assets = assets;
        this.current = current;
    }

    @PostMapping("/images")
    public ApiResponse<MediaAsset> upload(
            @RequestParam MultipartFile file,
            @RequestParam(defaultValue = "general") String purpose,
            Authentication authentication
    ) {
        if (file.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Choose an image to upload");
        }
        if (file.getSize() > 5_000_000) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Image must be under 5 MB");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Only JPG, PNG and WebP images are allowed");
        }

        String normalizedPurpose = purpose == null
                ? "general"
                : purpose.trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_PURPOSES.contains(normalizedPurpose)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Unsupported image purpose");
        }

        String userId = current.require(authentication).getId();
        StoredMedia stored = storage.upload(
                "playsphere/" + normalizedPurpose + "/" + userId,
                file
        );
        MediaAsset asset = assets.save(new MediaAsset(
                userId,
                stored,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize()
        ));
        return ApiResponse.ok("Image uploaded", asset);
    }
}
