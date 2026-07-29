package com.playsphere.team;

import com.playsphere.common.ApiResponse;
import com.playsphere.user.CurrentUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TeamController {
    private final TeamService service;
    private final CurrentUserService current;

    public TeamController(TeamService service, CurrentUserService current) {
        this.service = service;
        this.current = current;
    }

    public record CreateTeamRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Size(max = 80) String sport,
            @NotBlank @Size(max = 80) String city,
            @NotBlank @Size(max = 80) String locality,
            @NotBlank @Size(max = 32) String skillLevel,
            @Size(max = 600) String description,
            @Size(max = 700) String logoUrl,
            @Min(2) @Max(100) int maxMembers,
            @NotBlank @Pattern(regexp = "PUBLIC|PRIVATE") String visibility,
            @NotBlank @Pattern(regexp = "OPEN|REQUEST_APPROVAL|INVITE_ONLY") String joinMode
    ) {}

    public record MessageRequest(@Size(max = 400) String message) {}
    public record DecisionRequest(boolean accept) {}
    public record TransferCaptainRequest(@NotBlank String nextCaptainUserId) {}

    public record CreateRecruitmentPostRequest(
            @NotBlank String teamId,
            @NotBlank @Size(max = 140) String title,
            @NotBlank @Size(max = 300) String positionsNeeded,
            @Min(1) @Max(50) int playersNeeded,
            @NotBlank @Size(max = 32) String skillLevel,
            @Size(max = 700) String description,
            Instant applicationDeadline
    ) {}

    @GetMapping("/teams")
    public ApiResponse<List<TeamView>> discover(
            @RequestParam(required = false) String sport,
            @RequestParam(required = false, name = "q") String query
    ) {
        return ApiResponse.ok("Teams", service.discover(sport, query));
    }

    @GetMapping("/teams/{id}")
    public ApiResponse<TeamView> get(@PathVariable String id, Authentication authentication) {
        String viewerId = authentication == null ? null : current.require(authentication).getId();
        return ApiResponse.ok("Team", service.get(viewerId, id));
    }

    @GetMapping("/teams/mine")
    @PreAuthorize("hasRole('PLAYER')")
    public ApiResponse<List<TeamView>> mine(Authentication authentication) {
        return ApiResponse.ok("My teams", service.mine(current.require(authentication).getId()));
    }

    @PostMapping("/teams")
    @PreAuthorize("hasRole('PLAYER')")
    public ApiResponse<TeamView> create(
            @Valid @RequestBody CreateTeamRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(
                "Team created",
                service.create(current.require(authentication).getId(), request)
        );
    }

    @PutMapping("/teams/{id}")
    @PreAuthorize("hasRole('PLAYER')")
    public ApiResponse<TeamView> update(
            @PathVariable String id,
            @Valid @RequestBody CreateTeamRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(
                "Team updated",
                service.update(current.require(authentication).getId(), id, request)
        );
    }

    @DeleteMapping("/teams/{id}")
    @PreAuthorize("hasRole('PLAYER')")
    public ApiResponse<Void> archive(@PathVariable String id, Authentication authentication) {
        service.archive(current.require(authentication).getId(), id);
        return ApiResponse.ok("Team archived");
    }

    @PostMapping("/teams/{id}/join-requests")
    @PreAuthorize("hasRole('PLAYER')")
    public ApiResponse<TeamJoinRequestView> request(
            @PathVariable String id,
            @RequestBody(required = false) MessageRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(
                "Join request submitted",
                service.requestJoin(
                        current.require(authentication).getId(),
                        id,
                        request == null ? null : request.message()
                )
        );
    }

    @GetMapping("/teams/{id}/join-requests")
    @PreAuthorize("hasRole('PLAYER')")
    public ApiResponse<List<TeamJoinRequestView>> requests(
            @PathVariable String id,
            Authentication authentication
    ) {
        return ApiResponse.ok(
                "Join requests",
                service.requests(current.require(authentication).getId(), id)
        );
    }

    @PatchMapping("/team-join-requests/{id}")
    @PreAuthorize("hasRole('PLAYER')")
    public ApiResponse<Void> decide(
            @PathVariable String id,
            @RequestBody DecisionRequest request,
            Authentication authentication
    ) {
        service.decideRequest(current.require(authentication).getId(), id, request.accept());
        return ApiResponse.ok("Request updated");
    }

    @DeleteMapping("/teams/{teamId}/members/me")
    @PreAuthorize("hasRole('PLAYER')")
    public ApiResponse<Void> leave(
            @PathVariable String teamId,
            Authentication authentication
    ) {
        service.leave(current.require(authentication).getId(), teamId);
        return ApiResponse.ok("You left the team");
    }

    @DeleteMapping("/teams/{teamId}/members/{userId}")
    @PreAuthorize("hasRole('PLAYER')")
    public ApiResponse<Void> removeMember(
            @PathVariable String teamId,
            @PathVariable String userId,
            Authentication authentication
    ) {
        service.removeMember(current.require(authentication).getId(), teamId, userId);
        return ApiResponse.ok("Team member removed");
    }

    @PatchMapping("/teams/{teamId}/captain")
    @PreAuthorize("hasRole('PLAYER')")
    public ApiResponse<TeamView> transferCaptaincy(
            @PathVariable String teamId,
            @Valid @RequestBody TransferCaptainRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(
                "Captaincy transferred",
                service.transferCaptaincy(
                        current.require(authentication).getId(),
                        teamId,
                        request.nextCaptainUserId()
                )
        );
    }

    @GetMapping("/recruitment-posts")
    public ApiResponse<List<RecruitmentPost>> posts() {
        return ApiResponse.ok("Open recruitment posts", service.openPosts());
    }

    @PostMapping("/recruitment-posts")
    @PreAuthorize("hasRole('PLAYER')")
    public ApiResponse<RecruitmentPost> createPost(
            @Valid @RequestBody CreateRecruitmentPostRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(
                "Recruitment post published",
                service.createPost(current.require(authentication).getId(), request)
        );
    }

    @PostMapping("/recruitment-posts/{id}/applications")
    @PreAuthorize("hasRole('PLAYER')")
    public ApiResponse<RecruitmentApplicationView> apply(
            @PathVariable String id,
            @RequestBody(required = false) MessageRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(
                "Application sent",
                service.apply(
                        current.require(authentication).getId(),
                        id,
                        request == null ? null : request.message()
                )
        );
    }

    @GetMapping("/recruitment-posts/{id}/applications")
    @PreAuthorize("hasRole('PLAYER')")
    public ApiResponse<List<RecruitmentApplicationView>> applications(
            @PathVariable String id,
            Authentication authentication
    ) {
        return ApiResponse.ok(
                "Applications",
                service.postApplications(current.require(authentication).getId(), id)
        );
    }

    @PatchMapping("/recruitment-applications/{id}")
    @PreAuthorize("hasRole('PLAYER')")
    public ApiResponse<Void> decideApplication(
            @PathVariable String id,
            @RequestBody DecisionRequest request,
            Authentication authentication
    ) {
        service.decideApplication(current.require(authentication).getId(), id, request.accept());
        return ApiResponse.ok("Application updated");
    }
}
