package com.playsphere.location;

import java.math.BigDecimal;

public record GeocodingResult(
        String displayName,
        BigDecimal latitude,
        BigDecimal longitude,
        String addressLine,
        String city,
        String locality
) {}
