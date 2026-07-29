package com.playsphere.location;

import com.playsphere.common.BusinessException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Service
public class NominatimGeocodingService {
    private static final Object RATE_LOCK = new Object();
    private static Instant lastRemoteRequest = Instant.EPOCH;

    private final RestClient client;
    private final JsonMapper jsonMapper;
    private final boolean enabled;
    private final Map<String, List<GeocodingResult>> cache = new ConcurrentHashMap<>();

    public NominatimGeocodingService(
            RestClient.Builder builder,
            JsonMapper jsonMapper,
            @Value("${app.maps.provider:nominatim}") String provider,
            @Value("${app.maps.nominatim.user-agent:PlaySphere-Local-Development/0.1}") String userAgent
    ) {
        this.client = builder
                .baseUrl("https://nominatim.openstreetmap.org")
                .defaultHeader(HttpHeaders.USER_AGENT, userAgent)
                .defaultHeader(HttpHeaders.ACCEPT_LANGUAGE, "en-IN,en;q=0.9")
                .build();
        this.jsonMapper = jsonMapper;
        this.enabled = "nominatim".equalsIgnoreCase(provider);
    }

    public List<GeocodingResult> search(String rawQuery) {
        if (!enabled) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "Map search is disabled");
        }
        String query = rawQuery == null ? "" : rawQuery.trim();
        if (query.length() < 3) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Enter at least 3 characters to search the map");
        }
        if (query.length() > 180) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Map search is too long");
        }

        String key = query.toLowerCase(Locale.ROOT);
        List<GeocodingResult> cached = cache.get(key);
        if (cached != null) return cached;

        throttlePublicService();
        try {
            String body = client.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search")
                            .queryParam("q", query)
                            .queryParam("format", "jsonv2")
                            .queryParam("addressdetails", 1)
                            .queryParam("limit", 5)
                            .queryParam("countrycodes", "in")
                            .build())
                    .retrieve()
                    .body(String.class);

            JsonNode root = jsonMapper.readTree(body == null ? "[]" : body);
            List<GeocodingResult> results = new ArrayList<>();
            for (JsonNode node : root) {
                BigDecimal latitude = decimal(node.path("lat").asString(""));
                BigDecimal longitude = decimal(node.path("lon").asString(""));
                if (latitude == null || longitude == null) continue;

                results.add(toResult(node, latitude, longitude));
            }
            List<GeocodingResult> immutable = List.copyOf(results);
            cache.put(key, immutable);
            return immutable;
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(
                    HttpStatus.BAD_GATEWAY,
                    "Map search is temporarily unavailable. Try again or use your current location."
            );
        }
    }


    public GeocodingResult reverse(BigDecimal latitude, BigDecimal longitude) {
        if (!enabled) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "Map search is disabled");
        }
        if (latitude == null || longitude == null
                || latitude.compareTo(BigDecimal.valueOf(-90)) < 0
                || latitude.compareTo(BigDecimal.valueOf(90)) > 0
                || longitude.compareTo(BigDecimal.valueOf(-180)) < 0
                || longitude.compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Invalid map coordinates");
        }

        String key = "reverse:" + latitude.stripTrailingZeros().toPlainString()
                + ":" + longitude.stripTrailingZeros().toPlainString();
        List<GeocodingResult> cached = cache.get(key);
        if (cached != null && !cached.isEmpty()) return cached.getFirst();

        throttlePublicService();
        try {
            String body = client.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/reverse")
                            .queryParam("lat", latitude.toPlainString())
                            .queryParam("lon", longitude.toPlainString())
                            .queryParam("format", "jsonv2")
                            .queryParam("addressdetails", 1)
                            .build())
                    .retrieve()
                    .body(String.class);

            JsonNode node = jsonMapper.readTree(body == null ? "{}" : body);
            GeocodingResult result = toResult(node, latitude, longitude);
            cache.put(key, List.of(result));
            return result;
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(
                    HttpStatus.BAD_GATEWAY,
                    "Unable to identify this map location. Search for the address instead."
            );
        }
    }

    private static void throttlePublicService() {
        synchronized (RATE_LOCK) {
            long remaining = 1_000 - Duration.between(lastRemoteRequest, Instant.now()).toMillis();
            if (remaining > 0) {
                try {
                    Thread.sleep(remaining);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "Map search was interrupted");
                }
            }
            lastRemoteRequest = Instant.now();
        }
    }


    private static GeocodingResult toResult(
            JsonNode node,
            BigDecimal latitude,
            BigDecimal longitude
    ) {
        JsonNode address = node.path("address");
        String city = firstText(address, "city", "town", "village", "municipality", "county");
        String locality = firstText(address, "suburb", "neighbourhood", "city_district", "quarter", "hamlet");
        String road = firstText(address, "road", "pedestrian", "residential", "footway");
        String houseNumber = firstText(address, "house_number");
        String addressLine = joinNonBlank(houseNumber, road);
        String displayName = node.path("display_name").asString("Selected map location");
        if (addressLine == null || addressLine.isBlank()) addressLine = displayName;

        return new GeocodingResult(
                displayName,
                latitude,
                longitude,
                addressLine,
                city == null ? "" : city,
                locality == null ? "" : locality
        );
    }

    private static BigDecimal decimal(String value) {
        try {
            return new BigDecimal(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String firstText(JsonNode node, String... names) {
        for (String name : names) {
            String value = node.path(name).asString("").trim();
            if (!value.isBlank()) return value;
        }
        return null;
    }

    private static String joinNonBlank(String first, String second) {
        boolean hasFirst = first != null && !first.isBlank();
        boolean hasSecond = second != null && !second.isBlank();
        if (!hasFirst && !hasSecond) return null;
        if (!hasFirst) return second;
        if (!hasSecond) return first;
        return first + " " + second;
    }
}
