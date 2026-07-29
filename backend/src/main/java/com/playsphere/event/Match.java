package com.playsphere.event;

import com.playsphere.common.Ids;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "matches")
public class Match {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "event_id", nullable = false, length = 36)
    private String eventId;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(name = "home_name", nullable = false, length = 140)
    private String homeName;

    @Column(name = "away_name", nullable = false, length = 140)
    private String awayName;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Column(length = 180)
    private String venue;

    @Column(name = "home_score")
    private Integer homeScore;

    @Column(name = "away_score")
    private Integer awayScore;

    @Column(nullable = false, length = 24)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Match() {}

    public Match(String eventId, EventController.CreateMatchRequest request) {
        this(eventId, request.title(), request.homeName(), request.awayName(), request.scheduledAt(), request.venue());
    }

    public Match(
            String eventId,
            String title,
            String homeName,
            String awayName,
            Instant scheduledAt,
            String venue
    ) {
        this.id = Ids.uuid();
        this.eventId = eventId;
        this.title = title;
        this.homeName = homeName;
        this.awayName = awayName;
        this.scheduledAt = scheduledAt;
        this.venue = venue;
        this.status = "SCHEDULED";
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public String getId() { return id; }
    public String getEventId() { return eventId; }
    public String getTitle() { return title; }
    public String getHomeName() { return homeName; }
    public String getAwayName() { return awayName; }
    public Instant getScheduledAt() { return scheduledAt; }
    public String getVenue() { return venue; }
    public Integer getHomeScore() { return homeScore; }
    public Integer getAwayScore() { return awayScore; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void score(int home, int away) {
        homeScore = home;
        awayScore = away;
        status = "COMPLETED";
        updatedAt = Instant.now();
    }
}
