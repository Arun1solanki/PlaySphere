package com.playsphere.location;

import com.playsphere.common.ApiResponse;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/locations")
public class LocationController {
    private final NominatimGeocodingService geocoding;

    public LocationController(NominatimGeocodingService geocoding) {
        this.geocoding = geocoding;
    }

    @GetMapping("/search")
    public ApiResponse<List<GeocodingResult>> search(@RequestParam(name = "q") String query) {
        return ApiResponse.ok("Map results", geocoding.search(query));
    }

    @GetMapping("/reverse")
    public ApiResponse<GeocodingResult> reverse(
            @RequestParam BigDecimal latitude,
            @RequestParam BigDecimal longitude
    ) {
        return ApiResponse.ok("Map location", geocoding.reverse(latitude, longitude));
    }
}
