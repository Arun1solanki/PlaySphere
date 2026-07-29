package com.playsphere.review;

import com.playsphere.common.ApiResponse;
import com.playsphere.user.CurrentUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    private final ReviewService service;
    private final CurrentUserService current;

    public ReviewController(ReviewService service, CurrentUserService current) {
        this.service = service;
        this.current = current;
    }

    public record CreateReview(
            @Pattern(regexp = "TURF|EVENT") String targetType,
            @NotBlank String targetId,
            @Min(1) @Max(5) int rating,
            @Size(max = 1000) String comment
    ) {}

    public record Moderate(
            @Pattern(regexp = "PUBLISHED|HIDDEN|REMOVED|UNDER_REVIEW") String status
    ) {}

    @GetMapping
    public ApiResponse<List<Review>> list(
            @RequestParam String targetType,
            @RequestParam String targetId
    ) {
        return ApiResponse.ok("Reviews", service.list(targetType, targetId));
    }

    @PostMapping
    public ApiResponse<Review> create(
            @Valid @RequestBody CreateReview request,
            Authentication authentication
    ) {
        return ApiResponse.ok(
                "Review published",
                service.create(current.require(authentication).getId(), request)
        );
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<Review> moderate(
            @PathVariable String id,
            @Valid @RequestBody Moderate request
    ) {
        return ApiResponse.ok("Review moderated", service.moderate(id, request.status()));
    }
}
