package com.playsphere.media;

import com.playsphere.common.BusinessException;
import com.playsphere.common.Ids;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@ConditionalOnProperty(prefix = "app.storage", name = "provider", havingValue = "local", matchIfMissing = true)
public class LocalMediaStorage implements MediaStorage {
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp"
    );

    private final Path root = Path.of("uploads").toAbsolutePath().normalize();

    @Override
    public StoredMedia upload(String folder, MultipartFile file) {
        try {
            String extension = EXTENSIONS.get(file.getContentType());
            if (extension == null) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "Unsupported image type");
            }
            String publicId = folder + "/" + Ids.uuid() + extension;
            Path target = root.resolve(publicId).normalize();
            if (!target.startsWith(root)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "Invalid upload path");
            }
            Files.createDirectories(target.getParent());
            file.transferTo(target);
            return new StoredMedia(
                    "LOCAL",
                    publicId,
                    "/uploads/" + publicId.replace('\\', '/')
            );
        } catch (IOException exception) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to store image");
        }
    }

    @Override
    public void delete(String publicId) {
        try {
            Path target = root.resolve(publicId).normalize();
            if (target.startsWith(root)) Files.deleteIfExists(target);
        } catch (IOException ignored) {
            // A missing local file should not break the owning business operation.
        }
    }
}
