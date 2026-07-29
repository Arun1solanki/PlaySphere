package com.playsphere.event;

import com.playsphere.audit.AuditService;
import com.playsphere.chat.Conversation;
import com.playsphere.chat.ConversationMember;
import com.playsphere.chat.ConversationMemberRepository;
import com.playsphere.chat.ConversationRepository;
import com.playsphere.common.BusinessException;
import com.playsphere.notification.NotificationService;
import com.playsphere.media.MediaOwnershipService;
import com.playsphere.team.Team;
import com.playsphere.team.TeamMemberRepository;
import com.playsphere.team.TeamRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventService {
    private static final List<String> ACTIVE_REGISTRATION_STATUSES = List.of("APPROVED", "PENDING");

    private final SportsEventRepository events;
    private final EventRegistrationRepository registrations;
    private final MatchRepository matches;
    private final TeamRepository teams;
    private final TeamMemberRepository teamMembers;
    private final NotificationService notifications;
    private final AuditService audit;
    private final ConversationRepository conversations;
    private final ConversationMemberRepository conversationMembers;
    private final MediaOwnershipService mediaOwnership;

    public EventService(
            SportsEventRepository events,
            EventRegistrationRepository registrations,
            MatchRepository matches,
            TeamRepository teams,
            TeamMemberRepository teamMembers,
            NotificationService notifications,
            AuditService audit,
            ConversationRepository conversations,
            ConversationMemberRepository conversationMembers,
            MediaOwnershipService mediaOwnership
    ) {
        this.events = events;
        this.registrations = registrations;
        this.matches = matches;
        this.teams = teams;
        this.teamMembers = teamMembers;
        this.notifications = notifications;
        this.audit = audit;
        this.conversations = conversations;
        this.conversationMembers = conversationMembers;
        this.mediaOwnership = mediaOwnership;
    }

    public List<SportsEvent> discover() {
        return events.findByStatusOrderByStartAtAsc("PUBLISHED");
    }

    public List<SportsEvent> mine(String organizerId) {
        return events.findByOrganizerUserIdOrderByCreatedAtDesc(organizerId);
    }

    public SportsEvent require(String id) {
        return events.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Event not found"));
    }

    @Transactional
    public SportsEvent create(String organizerId, EventController.CreateEventRequest request) {
        validateScheduleAndCapacity(request);
        String bannerUrl = mediaOwnership.requireOwnedPurpose(
                organizerId,
                request.bannerUrl(),
                "events"
        );
        SportsEvent event = events.save(new SportsEvent(organizerId, request, bannerUrl));
        Conversation conversation = conversations.save(new Conversation(
                "EVENT",
                event.getId(),
                event.getTitle() + " event chat",
                organizerId
        ));
        conversationMembers.save(new ConversationMember(conversation.getId(), organizerId));
        audit.record(organizerId, "EVENT_CREATED", "EVENT", event.getId(), event.getTitle());
        return event;
    }

    @Transactional
    public SportsEvent cancel(String organizerId, String eventId, String reason) {
        SportsEvent event = requireOwner(organizerId, eventId);
        if (!"PUBLISHED".equals(event.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, "Event is not active");
        }
        if (event.getStartAt().isBefore(Instant.now())) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "Started events cannot be cancelled from this workflow"
            );
        }
        event.cancel();
        registrations.findByEventIdOrderByCreatedAtDesc(eventId).stream()
                .filter(registration -> !"CANCELLED".equals(registration.getStatus()))
                .forEach(registration -> notifications.send(
                        registration.getUserId(),
                        "EVENT_CANCELLED",
                        "Event cancelled",
                        event.getTitle() + " was cancelled. " + reason,
                        "/app/player/events"
                ));
        audit.record(organizerId, "EVENT_CANCELLED", "EVENT", eventId, reason);
        return event;
    }

    @Transactional
    public EventRegistration register(String userId, String eventId, String teamId) {
        SportsEvent event = require(eventId);
        if (!"PUBLISHED".equals(event.getStatus())
                || event.getRegistrationDeadline().isBefore(Instant.now())) {
            throw new BusinessException(HttpStatus.CONFLICT, "Registration is closed");
        }
        if (registrations.existsByEventIdAndUserIdAndStatusIn(
                eventId,
                userId,
                ACTIVE_REGISTRATION_STATUSES
        )) {
            throw new BusinessException(HttpStatus.CONFLICT, "Already registered");
        }
        if (registrations.countByEventIdAndStatus(eventId, "APPROVED") >= event.getMaxPlayers()) {
            throw new BusinessException(HttpStatus.CONFLICT, "Event is full");
        }

        String resolvedTeamId = validateRegistrationType(event, userId, teamId);
        EventRegistration registration = registrations.save(new EventRegistration(
                eventId,
                userId,
                resolvedTeamId,
                event.getEntryFee().compareTo(BigDecimal.ZERO) == 0
        ));
        conversations.findByConversationTypeAndReferenceId("EVENT", eventId).ifPresent(conversation -> {
            if (!conversationMembers.existsByConversationIdAndUserId(conversation.getId(), userId)) {
                conversationMembers.save(new ConversationMember(conversation.getId(), userId));
            }
        });
        notifications.send(
                event.getOrganizerUserId(),
                "EVENT_REGISTRATION",
                "New event registration",
                "A player registered for " + event.getTitle(),
                "/app/organizer/registrations"
        );
        return registration;
    }

    public List<EventRegistration> myRegistrations(String userId) {
        return registrations.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<EventRegistration> eventRegistrations(String organizerId, String eventId) {
        requireOwner(organizerId, eventId);
        return registrations.findByEventIdOrderByCreatedAtDesc(eventId);
    }

    @Transactional
    public void leave(String userId, String eventId) {
        EventRegistration registration = registrations.findByEventIdAndUserId(eventId, userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Registration not found"));
        SportsEvent event = require(eventId);
        if (event.getStartAt().isBefore(Instant.now())) {
            throw new BusinessException(HttpStatus.CONFLICT, "Started events cannot be left");
        }
        if ("CANCELLED".equals(registration.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, "Registration is already cancelled");
        }
        registration.leave();
        notifications.send(
                event.getOrganizerUserId(),
                "EVENT_REGISTRATION_CANCELLED",
                "Participant left event",
                "A participant left " + event.getTitle(),
                "/app/organizer/registrations"
        );
    }

    @Transactional
    public Match createMatch(
            String organizerId,
            String eventId,
            EventController.CreateMatchRequest request
    ) {
        SportsEvent event = requireOwner(organizerId, eventId);
        if (request.scheduledAt().isBefore(event.getStartAt())
                || request.scheduledAt().isAfter(event.getEndAt())) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Match must be scheduled within the event time window"
            );
        }
        Match match = matches.save(new Match(eventId, request));
        audit.record(organizerId, "MATCH_CREATED", "MATCH", match.getId(), match.getTitle());
        return match;
    }

    public List<Match> matches(String eventId) {
        require(eventId);
        return matches.findByEventIdOrderByScheduledAt(eventId);
    }

    @Transactional
    public Match score(String organizerId, String matchId, int homeScore, int awayScore) {
        Match match = matches.findById(matchId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Match not found"));
        requireOwner(organizerId, match.getEventId());
        match.score(homeScore, awayScore);
        audit.record(
                organizerId,
                "MATCH_RESULT_PUBLISHED",
                "MATCH",
                match.getId(),
                homeScore + "-" + awayScore
        );
        return match;
    }

    private String validateRegistrationType(SportsEvent event, String userId, String teamId) {
        if ("INDIVIDUAL".equals(event.getRegistrationType())) {
            if (teamId != null && !teamId.isBlank()) {
                throw new BusinessException(
                        HttpStatus.BAD_REQUEST,
                        "Individual events do not accept team registrations"
                );
            }
            return null;
        }

        if (teamId == null || teamId.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "A team is required for this event");
        }
        Team team = teams.findById(teamId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Team not found"));
        if (!"ACTIVE".equals(team.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, "Team is not active");
        }
        if (!team.getCaptainUserId().equals(userId)) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "Only the Team Captain can register the team"
            );
        }
        if (!teamMembers.existsByTeamIdAndUserId(teamId, userId)) {
            throw new BusinessException(HttpStatus.CONFLICT, "Captain membership is missing");
        }
        if (!team.getSport().equalsIgnoreCase(event.getSport())) {
            throw new BusinessException(HttpStatus.CONFLICT, "Team sport does not match the event");
        }
        if (registrations.existsByEventIdAndTeamIdAndStatusIn(
                event.getId(),
                teamId,
                ACTIVE_REGISTRATION_STATUSES
        )) {
            throw new BusinessException(HttpStatus.CONFLICT, "This team is already registered");
        }
        return teamId;
    }

    private void validateScheduleAndCapacity(EventController.CreateEventRequest request) {
        if (!request.startAt().isAfter(Instant.now())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Event must start in the future");
        }
        if (!request.endAt().isAfter(request.startAt())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Event end time must be after start time");
        }
        if (!request.registrationDeadline().isBefore(request.startAt())
                || request.registrationDeadline().isBefore(Instant.now())) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Registration deadline must be in the future and before event start"
            );
        }
        if (request.maxPlayers() < request.minPlayers()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Maximum capacity is below minimum capacity");
        }
    }

    private SportsEvent requireOwner(String userId, String eventId) {
        SportsEvent event = require(eventId);
        if (!event.getOrganizerUserId().equals(userId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "You do not own this event");
        }
        return event;
    }
}
