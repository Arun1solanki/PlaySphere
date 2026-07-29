package com.playsphere.event;

import com.playsphere.common.ApiResponse;
import com.playsphere.user.CurrentUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class EventController {
    private final EventService service;
    private final CurrentUserService current;

    public EventController(EventService service, CurrentUserService current) {
        this.service = service;
        this.current = current;
    }

    public record CreateEventRequest(
            @NotBlank @Size(max = 160) String title,
            @Size(max = 1200) String description,
            @NotBlank String sport,
            @NotBlank String eventType,
            @Pattern(regexp = "INDIVIDUAL|TEAM") String registrationType,
            @NotBlank String city,
            @NotBlank String locality,
            @NotBlank String turfId,
            @NotBlank String turfSlotId,
            @NotNull Instant startAt,
            @NotNull Instant endAt,
            @NotNull Instant registrationDeadline,
            @Min(1) int minPlayers,
            @Min(5) int maxPlayers,
            @NotNull @DecimalMin("0.00") BigDecimal entryFee,
            String bannerUrl,
            @Size(max = 1500) String rules
    ) {}

    public record RegisterRequest(String teamId) {}
    public record CancelEventRequest(@NotBlank @Size(max = 500) String reason) {}
    public record CreateMatchRequest(
            @NotBlank String title,
            @NotBlank String homeName,
            @NotBlank String awayName,
            @NotNull Instant scheduledAt,
            String venue
    ) {}
    public record ScoreRequest(@Min(0) int homeScore, @Min(0) int awayScore) {}

    @GetMapping("/events")
    public ApiResponse<List<SportsEvent>> discover() {
        return ApiResponse.ok("Events", service.discover());
    }

    @GetMapping("/events/mine")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ApiResponse<List<SportsEvent>> mine(Authentication authentication) {
        return ApiResponse.ok("My events", service.mine(current.require(authentication).getId()));
    }

    @PostMapping("/events")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ApiResponse<SportsEvent> create(
            @Valid @RequestBody CreateEventRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(
                "Event published and turf slot reserved",
                service.create(current.require(authentication).getId(), request)
        );
    }

    @PatchMapping("/events/{id}/cancel")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ApiResponse<SportsEvent> cancel(
            @PathVariable String id,
            @Valid @RequestBody CancelEventRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(
                "Event cancelled",
                service.cancel(current.require(authentication).getId(), id, request.reason())
        );
    }

    @PostMapping("/events/{id}/registrations")
    @PreAuthorize("hasRole('PLAYER')")
    public ApiResponse<EventRegistration> register(
            @PathVariable String id,
            @RequestBody(required = false) RegisterRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(
                "Registration created",
                service.register(
                        current.require(authentication).getId(),
                        id,
                        request == null ? null : request.teamId()
                )
        );
    }

    @DeleteMapping("/events/{id}/registrations/mine")
    @PreAuthorize("hasRole('PLAYER')")
    public ApiResponse<Void> leave(@PathVariable String id, Authentication authentication) {
        service.leave(current.require(authentication).getId(), id);
        return ApiResponse.ok("You left the event");
    }

    @GetMapping("/event-registrations/mine")
    @PreAuthorize("hasRole('PLAYER')")
    public ApiResponse<List<EventRegistration>> myRegistrations(Authentication authentication) {
        return ApiResponse.ok(
                "My registrations",
                service.myRegistrations(current.require(authentication).getId())
        );
    }

    @GetMapping("/events/{id}/registrations")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ApiResponse<List<EventRegistration>> registrations(
            @PathVariable String id,
            Authentication authentication
    ) {
        return ApiResponse.ok(
                "Registrations",
                service.eventRegistrations(current.require(authentication).getId(), id)
        );
    }

    @GetMapping("/events/{id}/matches")
    public ApiResponse<List<Match>> matches(@PathVariable String id) {
        return ApiResponse.ok("Matches", service.matches(id));
    }

    @PostMapping("/events/{id}/matches")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ApiResponse<Match> createMatch(
            @PathVariable String id,
            @Valid @RequestBody CreateMatchRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(
                "Match scheduled",
                service.createMatch(current.require(authentication).getId(), id, request)
        );
    }

    @PostMapping("/events/{id}/matches/generate")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ApiResponse<List<Match>> generateFixtures(
            @PathVariable String id,
            Authentication authentication
    ) {
        return ApiResponse.ok(
                "Fixtures generated",
                service.generateFixtures(current.require(authentication).getId(), id)
        );
    }

    @PatchMapping("/matches/{id}/score")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ApiResponse<Match> score(
            @PathVariable String id,
            @Valid @RequestBody ScoreRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(
                "Result published",
                service.score(
                        current.require(authentication).getId(),
                        id,
                        request.homeScore(),
                        request.awayScore()
                )
        );
    }
}
