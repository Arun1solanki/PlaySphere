package com.playsphere.team;

import com.playsphere.audit.AuditService;
import com.playsphere.chat.Conversation;
import com.playsphere.chat.ConversationMember;
import com.playsphere.chat.ConversationMemberRepository;
import com.playsphere.chat.ConversationRepository;
import com.playsphere.common.BusinessException;
import com.playsphere.notification.NotificationService;
import com.playsphere.media.MediaOwnershipService;
import com.playsphere.profile.UserProfile;
import com.playsphere.profile.UserProfileRepository;
import com.playsphere.user.AppUser;
import com.playsphere.user.AppUserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeamService {
    private final TeamRepository teams;
    private final TeamMemberRepository members;
    private final TeamJoinRequestRepository requests;
    private final RecruitmentPostRepository posts;
    private final RecruitmentApplicationRepository applications;
    private final NotificationService notifications;
    private final AuditService audit;
    private final ConversationRepository conversations;
    private final ConversationMemberRepository conversationMembers;
    private final AppUserRepository users;
    private final UserProfileRepository profiles;
    private final MediaOwnershipService mediaOwnership;

    public TeamService(
            TeamRepository teams,
            TeamMemberRepository members,
            TeamJoinRequestRepository requests,
            RecruitmentPostRepository posts,
            RecruitmentApplicationRepository applications,
            NotificationService notifications,
            AuditService audit,
            ConversationRepository conversations,
            ConversationMemberRepository conversationMembers,
            AppUserRepository users,
            UserProfileRepository profiles,
            MediaOwnershipService mediaOwnership
    ) {
        this.teams = teams;
        this.members = members;
        this.requests = requests;
        this.posts = posts;
        this.applications = applications;
        this.notifications = notifications;
        this.audit = audit;
        this.conversations = conversations;
        this.conversationMembers = conversationMembers;
        this.users = users;
        this.profiles = profiles;
        this.mediaOwnership = mediaOwnership;
    }

    @Transactional(readOnly = true)
    public List<TeamView> discover(String sport, String query) {
        List<Team> result = sport == null || sport.isBlank()
                ? teams.findByStatusAndVisibilityOrderByCreatedAtDesc("ACTIVE", "PUBLIC")
                : teams.findByStatusAndVisibilityAndSportIgnoreCaseOrderByCreatedAtDesc(
                        "ACTIVE",
                        "PUBLIC",
                        sport.trim()
                );
        if (query != null && !query.isBlank()) {
            String needle = query.trim().toLowerCase(Locale.ROOT);
            result = result.stream()
                    .filter(team -> contains(team.getName(), needle)
                            || contains(team.getSport(), needle)
                            || contains(team.getCity(), needle)
                            || contains(team.getLocality(), needle))
                    .toList();
        }
        return result.stream().map(team -> view(team, false)).toList();
    }

    @Transactional(readOnly = true)
    public List<TeamView> mine(String userId) {
        return members.findByUserId(userId).stream()
                .map(TeamMember::getTeamId)
                .distinct()
                .map(teams::findById)
                .flatMap(java.util.Optional::stream)
                .filter(team -> !"ARCHIVED".equals(team.getStatus()))
                .map(team -> view(team, true))
                .toList();
    }

    @Transactional(readOnly = true)
    public TeamView get(String viewerUserId, String teamId) {
        Team team = requireActive(teamId);
        boolean member = viewerUserId != null
                && members.existsByTeamIdAndUserId(teamId, viewerUserId);
        if ("PRIVATE".equals(team.getVisibility()) && !member) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "This team is private");
        }
        return view(team, member);
    }

    @Transactional
    public TeamView create(String userId, TeamController.CreateTeamRequest request) {
        AppUser creator = users.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "User not found"));
        if (!creator.isProfileCompleted()) {
            UserProfile existingProfile = profiles.findByUser_Id(userId).orElse(null);
            if (existingProfile == null) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "Complete your Player profile before creating a team");
            }
            creator.markProfileCompleted(existingProfile.getFullName());
            users.save(creator);
        }

        String name = request.name().trim();
        String city = request.city().trim();
        if (teams.existsByNameIgnoreCaseAndCityIgnoreCase(name, city)) {
            throw new BusinessException(HttpStatus.CONFLICT, "A team with this name already exists in this city");
        }

        Team team = teams.save(new Team(
                userId,
                name,
                request.sport().trim(),
                city,
                request.locality().trim(),
                request.skillLevel().trim().toUpperCase(Locale.ROOT),
                trimToNull(request.description()),
                mediaOwnership.requireOwnedPurpose(userId, request.logoUrl(), "teams"),
                request.maxMembers(),
                request.visibility().trim().toUpperCase(Locale.ROOT),
                request.joinMode().trim().toUpperCase(Locale.ROOT)
        ));
        members.save(new TeamMember(team.getId(), userId, "CAPTAIN"));

        Conversation conversation = conversations.save(new Conversation(
                "TEAM",
                team.getId(),
                team.getName() + " team chat",
                userId
        ));
        conversationMembers.save(new ConversationMember(conversation.getId(), userId));
        audit.record(userId, "TEAM_CREATED", "TEAM", team.getId(), team.getName());
        return view(team, true);
    }

    @Transactional
    public TeamView update(String captainId, String teamId, TeamController.CreateTeamRequest request) {
        Team team = requireCaptain(captainId, teamId);
        long currentMembers = members.countByTeamId(teamId);
        if (request.maxMembers() < currentMembers) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Maximum members cannot be lower than the current member count"
            );
        }
        String nextName = request.name().trim();
        String nextCity = request.city().trim();
        if ((!team.getName().equalsIgnoreCase(nextName) || !team.getCity().equalsIgnoreCase(nextCity))
                && teams.existsByNameIgnoreCaseAndCityIgnoreCase(nextName, nextCity)) {
            throw new BusinessException(HttpStatus.CONFLICT, "A team with this name already exists in this city");
        }
        team.update(
                nextName,
                request.sport().trim(),
                nextCity,
                request.locality().trim(),
                request.skillLevel().trim().toUpperCase(Locale.ROOT),
                trimToNull(request.description()),
                request.logoUrl() == null || request.logoUrl().isBlank()
                        ? team.getLogoUrl()
                        : mediaOwnership.requireOwnedPurpose(captainId, request.logoUrl(), "teams"),
                request.maxMembers(),
                request.visibility().trim().toUpperCase(Locale.ROOT),
                request.joinMode().trim().toUpperCase(Locale.ROOT)
        );
        audit.record(captainId, "TEAM_UPDATED", "TEAM", teamId, team.getName());
        return view(team, true);
    }

    @Transactional
    public void archive(String captainId, String teamId) {
        Team team = requireCaptain(captainId, teamId);
        team.archive();
        audit.record(captainId, "TEAM_ARCHIVED", "TEAM", teamId, team.getName());
    }

    @Transactional
    public TeamJoinRequestView requestJoin(String userId, String teamId, String message) {
        Team team = requireActive(teamId);
        ensureCanJoin(userId, team);

        if ("INVITE_ONLY".equals(team.getJoinMode())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "This team accepts invitations only");
        }
        if (requests.existsByTeamIdAndApplicantUserIdAndStatus(teamId, userId, "PENDING")) {
            throw new BusinessException(HttpStatus.CONFLICT, "A join request is already pending");
        }

        TeamJoinRequest request = requests.save(new TeamJoinRequest(teamId, userId, trimToNull(message)));
        if ("OPEN".equals(team.getJoinMode())) {
            addMember(team, userId);
            request.decide("APPROVED", team.getCaptainUserId());
            notifications.send(
                    userId,
                    "TEAM_JOINED",
                    "Team joined",
                    "You joined " + team.getName() + ".",
                    "/app/player/teams"
            );
            notifications.send(
                    team.getCaptainUserId(),
                    "TEAM_MEMBER_JOINED",
                    "New team member",
                    playerName(userId) + " joined " + team.getName() + ".",
                    "/app/player/teams"
            );
            audit.record(userId, "TEAM_JOINED_OPEN", "TEAM", teamId, team.getName());
            return requestView(request);
        }

        notifications.send(
                team.getCaptainUserId(),
                "TEAM_JOIN_REQUEST",
                "New team join request",
                playerName(userId) + " requested to join " + team.getName(),
                "/app/player/teams"
        );
        return requestView(request);
    }

    @Transactional(readOnly = true)
    public List<TeamJoinRequestView> requests(String captainId, String teamId) {
        Team team = requireCaptain(captainId, teamId);
        return requests.findByTeamIdOrderByCreatedAtDesc(team.getId()).stream()
                .map(this::requestView)
                .toList();
    }

    @Transactional
    public void decideRequest(String captainId, String requestId, boolean accept) {
        TeamJoinRequest request = requests.findById(requestId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Join request not found"));
        Team team = requireCaptain(captainId, request.getTeamId());
        if (!"PENDING".equals(request.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, "Request already decided");
        }

        if (accept) {
            ensureCanJoin(request.getApplicantUserId(), team);
            addMember(team, request.getApplicantUserId());
            request.decide("APPROVED", captainId);
            notifications.send(
                    request.getApplicantUserId(),
                    "TEAM_REQUEST_APPROVED",
                    "Team request approved",
                    "You joined " + team.getName(),
                    "/app/player/teams"
            );
        } else {
            request.decide("REJECTED", captainId);
            notifications.send(
                    request.getApplicantUserId(),
                    "TEAM_REQUEST_REJECTED",
                    "Team request rejected",
                    "Your request to join " + team.getName() + " was rejected",
                    "/app/player/teams"
            );
        }
        audit.record(
                captainId,
                "TEAM_JOIN_REQUEST_DECIDED",
                "TEAM_REQUEST",
                request.getId(),
                request.getStatus()
        );
    }

    @Transactional
    public void leave(String userId, String teamId) {
        Team team = requireActive(teamId);
        if (team.getCaptainUserId().equals(userId)) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "Transfer captaincy before leaving the team"
            );
        }
        requireMember(userId, teamId);
        removeMemberFromTeam(teamId, userId);
        notifications.send(
                team.getCaptainUserId(),
                "TEAM_MEMBER_LEFT",
                "Team member left",
                playerName(userId) + " left " + team.getName(),
                "/app/player/teams"
        );
        audit.record(userId, "TEAM_LEFT", "TEAM", teamId, team.getName());
    }

    @Transactional
    public void removeMember(String captainId, String teamId, String memberUserId) {
        Team team = requireCaptain(captainId, teamId);
        if (team.getCaptainUserId().equals(memberUserId)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "The Team Captain cannot be removed");
        }
        requireMember(memberUserId, teamId);
        removeMemberFromTeam(teamId, memberUserId);
        notifications.send(
                memberUserId,
                "TEAM_MEMBER_REMOVED",
                "Removed from team",
                "You were removed from " + team.getName(),
                "/app/player/teams"
        );
        audit.record(captainId, "TEAM_MEMBER_REMOVED", "TEAM", teamId, memberUserId);
    }

    @Transactional
    public TeamView transferCaptaincy(String captainId, String teamId, String nextCaptainUserId) {
        Team team = requireCaptain(captainId, teamId);
        if (captainId.equals(nextCaptainUserId)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "This player is already the Team Captain");
        }
        TeamMember nextCaptain = requireMember(nextCaptainUserId, teamId);
        TeamMember currentCaptain = requireMember(captainId, teamId);
        currentCaptain.setMemberRole("MEMBER");
        nextCaptain.setMemberRole("CAPTAIN");
        team.transferCaptaincy(nextCaptainUserId);
        notifications.send(
                nextCaptainUserId,
                "TEAM_CAPTAINCY_TRANSFERRED",
                "You are now Team Captain",
                "Captaincy of " + team.getName() + " was transferred to you.",
                "/app/player/teams"
        );
        audit.record(captainId, "TEAM_CAPTAINCY_TRANSFERRED", "TEAM", teamId, nextCaptainUserId);
        return view(team, true);
    }

    @Transactional
    public RecruitmentPost createPost(
            String captainId,
            TeamController.CreateRecruitmentPostRequest request
    ) {
        Team team = requireCaptain(captainId, request.teamId());
        ensureCapacity(team);
        if (request.applicationDeadline() != null
                && !request.applicationDeadline().isAfter(Instant.now())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Application deadline must be in the future");
        }
        long remainingCapacity = team.getMaxMembers() - members.countByTeamId(team.getId());
        if (request.playersNeeded() > remainingCapacity) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Players needed cannot exceed the team's remaining capacity"
            );
        }

        RecruitmentPost post = posts.save(new RecruitmentPost(
                team.getId(),
                captainId,
                request.title().trim(),
                team.getSport(),
                request.positionsNeeded().trim(),
                request.playersNeeded(),
                request.skillLevel().trim().toUpperCase(Locale.ROOT),
                team.getCity(),
                team.getLocality(),
                trimToNull(request.description()),
                request.applicationDeadline()
        ));
        audit.record(
                captainId,
                "RECRUITMENT_POST_CREATED",
                "RECRUITMENT_POST",
                post.getId(),
                post.getTitle()
        );
        return post;
    }

    @Transactional(readOnly = true)
    public List<RecruitmentPost> openPosts() {
        return posts.findByStatusOrderByCreatedAtDesc("OPEN").stream()
                .filter(post -> post.getApplicationDeadline() == null
                        || post.getApplicationDeadline().isAfter(Instant.now()))
                .toList();
    }

    @Transactional
    public RecruitmentApplicationView apply(String userId, String postId, String message) {
        RecruitmentPost post = posts.findById(postId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Recruitment post not found"));
        Team team = requireActive(post.getTeamId());
        if (!"OPEN".equals(post.getStatus())
                || (post.getApplicationDeadline() != null
                && post.getApplicationDeadline().isBefore(Instant.now()))) {
            throw new BusinessException(HttpStatus.CONFLICT, "Recruitment post is closed");
        }
        ensureCanJoin(userId, team);
        if (applications.existsByPostIdAndApplicantUserId(postId, userId)) {
            throw new BusinessException(HttpStatus.CONFLICT, "You already applied");
        }

        RecruitmentApplication application = applications.save(new RecruitmentApplication(
                postId,
                userId,
                trimToNull(message)
        ));
        notifications.send(
                post.getCreatedByUserId(),
                "RECRUITMENT_APPLICATION",
                "New recruitment application",
                playerName(userId) + " applied to " + post.getTitle(),
                "/app/player/need-players"
        );
        return applicationView(application);
    }

    @Transactional(readOnly = true)
    public List<RecruitmentApplicationView> postApplications(String captainId, String postId) {
        RecruitmentPost post = posts.findById(postId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Post not found"));
        requireCaptain(captainId, post.getTeamId());
        return applications.findByPostIdOrderByCreatedAtDesc(postId).stream()
                .map(this::applicationView)
                .toList();
    }

    @Transactional
    public void decideApplication(String captainId, String applicationId, boolean accept) {
        RecruitmentApplication application = applications.findById(applicationId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Application not found"));
        RecruitmentPost post = posts.findById(application.getPostId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Recruitment post not found"));
        Team team = requireCaptain(captainId, post.getTeamId());
        if (!"PENDING".equals(application.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, "Application already decided");
        }

        if (accept) {
            ensureCanJoin(application.getApplicantUserId(), team);
            addMember(team, application.getApplicantUserId());
            application.decide("APPROVED");
            post.filledOne();
            notifications.send(
                    application.getApplicantUserId(),
                    "RECRUITMENT_APPROVED",
                    "Application approved",
                    "You joined " + team.getName(),
                    "/app/player/teams"
            );
        } else {
            application.decide("REJECTED");
            notifications.send(
                    application.getApplicantUserId(),
                    "RECRUITMENT_REJECTED",
                    "Application rejected",
                    "Your application for " + post.getTitle() + " was rejected",
                    "/app/player/need-players"
            );
        }
        audit.record(
                captainId,
                "RECRUITMENT_APPLICATION_DECIDED",
                "RECRUITMENT_APPLICATION",
                application.getId(),
                application.getStatus()
        );
    }

    private void addMember(Team team, String userId) {
        ensureCapacity(team);
        if (members.existsByTeamIdAndUserId(team.getId(), userId)) {
            throw new BusinessException(HttpStatus.CONFLICT, "Player is already a team member");
        }
        members.save(new TeamMember(team.getId(), userId, "MEMBER"));
        addConversationMember("TEAM", team.getId(), userId);
    }

    private void ensureCanJoin(String userId, Team team) {
        AppUser user = users.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "User not found"));
        if (!user.isProfileCompleted()) {
            UserProfile existingProfile = profiles.findByUser_Id(userId).orElse(null);
            if (existingProfile == null) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "Complete your Player profile before joining a team");
            }
            user.markProfileCompleted(existingProfile.getFullName());
            users.save(user);
        }
        if (team.getCaptainUserId().equals(userId)
                || members.existsByTeamIdAndUserId(team.getId(), userId)) {
            throw new BusinessException(HttpStatus.CONFLICT, "You are already part of this team");
        }
        ensureCapacity(team);
    }

    private void ensureCapacity(Team team) {
        if (members.countByTeamId(team.getId()) >= team.getMaxMembers()) {
            throw new BusinessException(HttpStatus.CONFLICT, "Team is full");
        }
    }

    private void addConversationMember(String type, String referenceId, String userId) {
        conversations.findByConversationTypeAndReferenceId(type, referenceId).ifPresent(conversation -> {
            if (!conversationMembers.existsByConversationIdAndUserId(conversation.getId(), userId)) {
                conversationMembers.save(new ConversationMember(conversation.getId(), userId));
            }
        });
    }

    private void removeMemberFromTeam(String teamId, String userId) {
        members.deleteByTeamIdAndUserId(teamId, userId);
        conversations.findByConversationTypeAndReferenceId("TEAM", teamId)
                .ifPresent(conversation -> conversationMembers.deleteByConversationIdAndUserId(
                        conversation.getId(),
                        userId
                ));
    }

    private Team require(String id) {
        return teams.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Team not found"));
    }

    private Team requireActive(String id) {
        Team team = require(id);
        if (!"ACTIVE".equals(team.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, "Team is not active");
        }
        return team;
    }

    private Team requireCaptain(String userId, String teamId) {
        Team team = requireActive(teamId);
        if (!team.getCaptainUserId().equals(userId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Only the Team Captain can perform this action");
        }
        return team;
    }

    private TeamMember requireMember(String userId, String teamId) {
        return members.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Team member not found"));
    }

    private TeamView view(Team team, boolean includeMembers) {
        List<TeamMember> rows = members.findByTeamIdOrderByJoinedAtAsc(team.getId());
        List<TeamMemberView> memberViews = includeMembers
                ? rows.stream().map(this::memberView).toList()
                : List.of();
        return new TeamView(
                team.getId(),
                team.getCaptainUserId(),
                team.getName(),
                team.getSport(),
                team.getCity(),
                team.getLocality(),
                team.getSkillLevel(),
                team.getDescription(),
                team.getLogoUrl(),
                team.getMaxMembers(),
                team.getVisibility(),
                team.getJoinMode(),
                team.getStatus(),
                team.getCreatedAt(),
                team.getUpdatedAt(),
                rows.size(),
                playerSummary(team.getCaptainUserId()),
                memberViews
        );
    }

    private TeamMemberView memberView(TeamMember member) {
        return new TeamMemberView(
                member.getId(),
                member.getUserId(),
                member.getMemberRole(),
                member.getJoinedAt(),
                playerSummary(member.getUserId())
        );
    }

    private RecruitmentApplicationView applicationView(RecruitmentApplication application) {
        return new RecruitmentApplicationView(
                application.getId(),
                application.getPostId(),
                application.getApplicantUserId(),
                application.getMessage(),
                application.getStatus(),
                application.getCreatedAt(),
                application.getDecidedAt(),
                playerSummary(application.getApplicantUserId())
        );
    }

    private TeamJoinRequestView requestView(TeamJoinRequest request) {
        return new TeamJoinRequestView(
                request.getId(),
                request.getTeamId(),
                request.getApplicantUserId(),
                request.getMessage(),
                request.getStatus(),
                request.getDecidedByUserId(),
                request.getCreatedAt(),
                request.getDecidedAt(),
                playerSummary(request.getApplicantUserId())
        );
    }

    private TeamPlayerSummary playerSummary(String userId) {
        AppUser user = users.findById(userId).orElse(null);
        UserProfile profile = profiles.findByUser_Id(userId).orElse(null);
        return new TeamPlayerSummary(
                userId,
                profile != null && profile.getFullName() != null
                        ? profile.getFullName()
                        : user == null ? "Unknown player" : user.getDisplayName(),
                profile == null ? null : profile.getProfileImageUrl(),
                profile == null ? null : profile.getCity(),
                profile == null ? null : profile.getLocality(),
                profile == null ? null : profile.getSkillLevel(),
                profile == null ? null : profile.getPlayingPosition()
        );
    }

    private String playerName(String userId) {
        return playerSummary(userId).displayName();
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }
}
