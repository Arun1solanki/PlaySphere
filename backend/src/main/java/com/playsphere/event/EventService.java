package com.playsphere.event;

import com.playsphere.audit.AuditService;
import com.playsphere.chat.Conversation;
import com.playsphere.chat.ConversationMember;
import com.playsphere.chat.ConversationMemberRepository;
import com.playsphere.chat.ConversationRepository;
import com.playsphere.common.BusinessException;
import com.playsphere.media.MediaOwnershipService;
import com.playsphere.notification.NotificationService;
import com.playsphere.team.Team;
import com.playsphere.team.TeamMemberRepository;
import com.playsphere.team.TeamRepository;
import com.playsphere.turf.Turf;
import com.playsphere.turf.TurfRepository;
import com.playsphere.turf.TurfSlot;
import com.playsphere.turf.TurfSlotRepository;
import com.playsphere.user.AppUserRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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
    private final TurfRepository turfs;
    private final TurfSlotRepository turfSlots;
    private final AppUserRepository users;
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
            TurfRepository turfs,
            TurfSlotRepository turfSlots,
            AppUserRepository users,
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
        this.turfs = turfs;
        this.turfSlots = turfSlots;
        this.users = users;
        this.notifications = notifications;
        this.audit = audit;
        this.conversations = conversations;
        this.conversationMembers = conversationMembers;
        this.mediaOwnership = mediaOwnership;
    }

    @Transactional(readOnly = true)
    public List<SportsEvent> discover() {
        return events.findByStatusOrderByStartAtAsc("PUBLISHED");
    }

    @Transactional(readOnly = true)
    public List<SportsEvent> mine(String organizerId) {
        return events.findByOrganizerUserIdOrderByCreatedAtDesc(organizerId);
    }

    @Transactional(readOnly = true)
    public SportsEvent require(String id) {
        return events.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Event not found"));
    }

    @Transactional
    public SportsEvent create(String organizerId, EventController.CreateEventRequest request) {
        validateScheduleAndCapacity(request);

        Turf turf = turfs.findById(request.turfId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Selected turf not found"));
        if (!"APPROVED".equals(turf.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, "Selected turf is not approved");
        }
        if (!turf.getSports().toLowerCase().contains(request.sport().trim().toLowerCase())) {
            throw new BusinessException(HttpStatus.CONFLICT, "Selected turf does not support this sport");
        }

        TurfSlot slot = turfSlots.findByIdForUpdate(request.turfSlotId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Selected turf slot not found"));
        if (!slot.getTurfId().equals(turf.getId())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Selected slot does not belong to the selected turf");
        }
        if (!"AVAILABLE".equals(slot.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, "Selected turf slot is no longer available");
        }
        if (request.startAt().isBefore(slot.getStartAt()) || request.endAt().isAfter(slot.getEndAt())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Event time must be within the selected turf slot");
        }

        String bannerUrl = mediaOwnership.requireOwnedPurpose(
                organizerId,
                request.bannerUrl(),
                "events"
        );
        SportsEvent event = events.save(new SportsEvent(
                organizerId,
                request,
                bannerUrl,
                turf.getName(),
                turf.getId(),
                slot.getId()
        ));
        slot.reserveForEvent();

        Conversation conversation = conversations.save(new Conversation(
                "EVENT",
                event.getId(),
                event.getTitle() + " event chat",
                organizerId
        ));
        conversationMembers.save(new ConversationMember(conversation.getId(), organizerId));

        notifications.send(
                turf.getOwnerUserId(),
                "EVENT_TURF_RESERVED",
                "Turf slot reserved for an event",
                event.getTitle() + " reserved " + turf.getName() + " on " + slot.getStartAt() + ".",
                "/app/turf-owner/availability"
        );
        notifications.send(
                organizerId,
                "EVENT_PUBLISHED",
                "Event published",
                event.getTitle() + " is live at " + turf.getName() + ".",
                "/app/organizer/events"
        );
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
            throw new BusinessException(HttpStatus.CONFLICT, "Started events cannot be cancelled from this workflow");
        }
        event.cancel();
        if (event.getTurfSlotId() != null) {
            turfSlots.findById(event.getTurfSlotId()).ifPresent(slot -> {
                if ("EVENT_RESERVED".equals(slot.getStatus())) slot.release();
            });
        }
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

        String resolvedTeamId = validateRegistrationType(event, userId, teamId);
        EventRegistration existing = registrations.findByEventIdAndUserId(eventId, userId).orElse(null);
        if (existing != null && !"CANCELLED".equals(existing.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, "Already registered");
        }
        if (registrations.countByEventIdAndStatus(eventId, "APPROVED") >= event.getMaxPlayers()) {
            throw new BusinessException(HttpStatus.CONFLICT, "Event is full");
        }

        boolean paid = event.getEntryFee().compareTo(BigDecimal.ZERO) == 0;
        EventRegistration registration;
        if (existing != null) {
            existing.rejoin(resolvedTeamId, paid);
            registration = existing;
        } else {
            registration = registrations.save(new EventRegistration(eventId, userId, resolvedTeamId, paid));
        }

        addEventChatMember(eventId, userId);
        notifications.send(
                event.getOrganizerUserId(),
                "EVENT_REGISTRATION",
                "New event registration",
                "A player registered for " + event.getTitle(),
                "/app/organizer/registrations"
        );
        notifications.send(
                userId,
                "EVENT_JOINED",
                "Event joined",
                "You joined " + event.getTitle() + ".",
                "/app/player/events"
        );
        return registration;
    }

    @Transactional(readOnly = true)
    public List<EventRegistration> myRegistrations(String userId) {
        return registrations.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
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
        conversations.findByConversationTypeAndReferenceId("EVENT", eventId)
                .ifPresent(conversation -> conversationMembers.deleteByConversationIdAndUserId(
                        conversation.getId(),
                        userId
                ));
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
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Match must be scheduled within the event time window");
        }
        Match match = matches.save(new Match(eventId, request));
        audit.record(organizerId, "MATCH_CREATED", "MATCH", match.getId(), match.getTitle());
        return match;
    }

    @Transactional
    public List<Match> generateFixtures(String organizerId, String eventId) {
        SportsEvent event = requireOwner(organizerId, eventId);
        if (!matches.findByEventIdOrderByScheduledAt(eventId).isEmpty()) {
            throw new BusinessException(HttpStatus.CONFLICT, "Fixtures already exist for this event");
        }

        List<String> participants = participantNames(event);
        if (participants.size() < 2) {
            throw new BusinessException(HttpStatus.CONFLICT, "At least two active registrations are required");
        }

        int matchCount = participants.size() / 2;
        long totalSeconds = Math.max(60, Duration.between(event.getStartAt(), event.getEndAt()).getSeconds());
        long stepSeconds = Math.max(60, totalSeconds / Math.max(1, matchCount));
        List<Match> generated = new ArrayList<>();
        for (int index = 0; index + 1 < participants.size(); index += 2) {
            int matchIndex = index / 2;
            Instant scheduledAt = event.getStartAt().plusSeconds(Math.min(
                    stepSeconds * matchIndex,
                    Math.max(0, totalSeconds - 60)
            ));
            generated.add(matches.save(new Match(
                    eventId,
                    "Fixture " + (matchIndex + 1),
                    participants.get(index),
                    participants.get(index + 1),
                    scheduledAt,
                    event.getVenueName()
            )));
        }
        audit.record(organizerId, "FIXTURES_GENERATED", "EVENT", eventId, String.valueOf(generated.size()));
        return generated;
    }

    @Transactional(readOnly = true)
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

    private void addEventChatMember(String eventId, String userId) {
        conversations.findByConversationTypeAndReferenceId("EVENT", eventId).ifPresent(conversation -> {
            if (!conversationMembers.existsByConversationIdAndUserId(conversation.getId(), userId)) {
                conversationMembers.save(new ConversationMember(conversation.getId(), userId));
            }
        });
    }

    private List<String> participantNames(SportsEvent event) {
        List<EventRegistration> active = registrations.findByEventIdOrderByCreatedAtDesc(event.getId()).stream()
                .filter(registration -> "APPROVED".equals(registration.getStatus()))
                .toList();

        Set<String> names = new LinkedHashSet<>();
        if ("TEAM".equals(event.getRegistrationType())) {
            active.stream()
                    .map(EventRegistration::getTeamId)
                    .filter(teamId -> teamId != null && !teamId.isBlank())
                    .map(teams::findById)
                    .flatMap(java.util.Optional::stream)
                    .map(Team::getName)
                    .forEach(names::add);
        } else {
            active.stream()
                    .map(EventRegistration::getUserId)
                    .map(users::findById)
                    .flatMap(java.util.Optional::stream)
                    .map(user -> user.getDisplayName())
                    .forEach(names::add);
        }
        return List.copyOf(names);
    }

    private String validateRegistrationType(SportsEvent event, String userId, String teamId) {
        if ("INDIVIDUAL".equals(event.getRegistrationType())) {
            if (teamId != null && !teamId.isBlank()) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "Individual events do not accept team registrations");
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
            throw new BusinessException(HttpStatus.FORBIDDEN, "Only the Team Captain can register the team");
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
