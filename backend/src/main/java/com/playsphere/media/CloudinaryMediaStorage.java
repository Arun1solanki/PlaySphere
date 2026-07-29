package com.playsphere.media;

import tools.jackson.databind.JsonNode;
import com.playsphere.common.BusinessException;
import com.playsphere.common.Ids;
import com.playsphere.config.AppProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

@Component
@ConditionalOnProperty(prefix = "app.storage", name = "provider", havingValue = "cloudinary")
public class CloudinaryMediaStorage implements MediaStorage {
    private final AppProperties.Storage.Cloudinary settings;
    private final RestClient client;

    public CloudinaryMediaStorage(AppProperties properties, RestClient.Builder builder) {
        this.settings = properties.storage().cloudinary();
        if (settings.cloudName().isBlank()
                || settings.apiKey().isBlank()
                || settings.apiSecret().isBlank()) {
            throw new IllegalStateException("Cloudinary credentials are missing");
        }
        this.client = builder
                .baseUrl("https://api.cloudinary.com/v1_1/" + settings.cloudName())
                .build();
    }

    @Override
    public StoredMedia upload(String folder, MultipartFile file) {
        try {
            long timestamp = Instant.now().getEpochSecond();
            String publicId = folder + "/" + Ids.uuid();
            String signature = sha1(
                    "public_id=" + publicId + "&timestamp=" + timestamp + settings.apiSecret()
            );

            var body = new LinkedMultiValueMap<String, Object>();
            body.add("api_key", settings.apiKey());
            body.add("timestamp", String.valueOf(timestamp));
            body.add("public_id", publicId);
            body.add("signature", signature);
            body.add("file", new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            });

            JsonNode response = client.post()
                    .uri("/image/upload")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null || response.path("secure_url").asString().isBlank()) {
                throw new IllegalStateException("Cloudinary returned an empty upload response");
            }
            return new StoredMedia(
                    "CLOUDINARY",
                    response.path("public_id").asString(),
                    response.path("secure_url").asString()
            );
        } catch (Exception exception) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "Cloudinary upload failed");
        }
    }

    @Override
    public void delete(String publicId) {
        try {
            long timestamp = Instant.now().getEpochSecond();
            String signature = sha1(
                    "invalidate=true&public_id=" + publicId + "&timestamp=" + timestamp + settings.apiSecret()
            );
            var body = new LinkedMultiValueMap<String, String>();
            body.add("api_key", settings.apiKey());
            body.add("timestamp", String.valueOf(timestamp));
            body.add("public_id", publicId);
            body.add("invalidate", "true");
            body.add("signature", signature);

            client.post()
                    .uri("/image/destroy")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception exception) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "Cloudinary deletion failed");
        }
    }

    private static String sha1(String value) throws Exception {
        return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-1")
                        .digest(value.getBytes(StandardCharsets.UTF_8))
        );
    }
}
