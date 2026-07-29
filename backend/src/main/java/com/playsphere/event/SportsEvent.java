package com.playsphere.event;

import com.playsphere.common.Ids;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "events")
public class SportsEvent {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "organizer_user_id", nullable = false, length = 36)
    private String organizerUserId;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(length = 1200)
    private String description;

    @Column(nullable = false, length = 80)
    private String sport;

    @Column(name = "event_type", nullable = false, length = 32)
    private String eventType;

    @Column(name = "registration_type", nullable = false, length = 24)
    private String registrationType;

    @Column(nullable = false, length = 80)
    private String city;

    @Column(nullable = false, length = 80)
    private String locality;

    @Column(name = "venue_name", length = 180)
    private String venueName;

    @Column(name = "turf_id", length = 36)
    private String turfId;

    @Column(name = "turf_slot_id", length = 36)
    private String turfSlotId;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Column(name = "registration_deadline", nullable = false)
    private Instant registrationDeadline;

    @Column(name = "min_players", nullable = false)
    private int minPlayers;

    @Column(name = "max_players", nullable = false)
    private int maxPlayers;

    @Column(name = "entry_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal entryFee;

    @Column(name = "banner_url", length = 700)
    private String bannerUrl;

    @Column(length = 1500)
    private String rules;

    @Column(nullable = false, length = 24)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SportsEvent() {}

    public SportsEvent(
            String organizerUserId,
            EventController.CreateEventRequest request,
            String validatedBannerUrl,
            String venueName,
            String turfId,
            String turfSlotId
    ) {
        this.id = Ids.uuid();
        this.organizerUserId = organizerUserId;
        this.title = request.title().trim();
        this.description = trim(request.description());
        this.sport = request.sport().trim();
        this.eventType = request.eventType().trim().toUpperCase();
        this.registrationType = request.registrationType().trim().toUpperCase();
        this.city = request.city().trim();
        this.locality = request.locality().trim();
        this.venueName = trim(venueName);
        this.turfId = trim(turfId);
        this.turfSlotId = trim(turfSlotId);
        this.startAt = request.startAt();
        this.endAt = request.endAt();
        this.registrationDeadline = request.registrationDeadline();
        this.minPlayers = request.minPlayers();
        this.maxPlayers = request.maxPlayers();
        this.entryFee = request.entryFee();
        this.bannerUrl = validatedBannerUrl;
        this.rules = trim(request.rules());
        this.status = "PUBLISHED";
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    private static String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public String getId() { return id; }
    public String getOrganizerUserId() { return organizerUserId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getSport() { return sport; }
    public String getEventType() { return eventType; }
    public String getRegistrationType() { return registrationType; }
    public String getCity() { return city; }
    public String getLocality() { return locality; }
    public String getVenueName() { return venueName; }
    public String getTurfId() { return turfId; }
    public String getTurfSlotId() { return turfSlotId; }
    public Instant getStartAt() { return startAt; }
    public Instant getEndAt() { return endAt; }
    public Instant getRegistrationDeadline() { return registrationDeadline; }
    public int getMinPlayers() { return minPlayers; }
    public int getMaxPlayers() { return maxPlayers; }
    public BigDecimal getEntryFee() { return entryFee; }
    public String getBannerUrl() { return bannerUrl; }
    public String getRules() { return rules; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void cancel() {
        status = "CANCELLED";
        updatedAt = Instant.now();
    }
}
