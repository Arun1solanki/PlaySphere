package com.playsphere.event;

import com.playsphere.common.Ids;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "event_registrations")
public class EventRegistration {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "event_id", nullable = false, length = 36)
    private String eventId;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "team_id", length = 36)
    private String teamId;

    @Column(nullable = false, length = 24)
    private String status;

    @Column(name = "payment_status", nullable = false, length = 24)
    private String paymentStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    protected EventRegistration() {}

    public EventRegistration(String eventId, String userId, String teamId, boolean paid) {
        this.id = Ids.uuid();
        this.eventId = eventId;
        this.userId = userId;
        this.teamId = teamId;
        this.status = "APPROVED";
        this.paymentStatus = paid ? "SUCCESS" : "PENDING";
        this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public String getEventId() { return eventId; }
    public String getUserId() { return userId; }
    public String getTeamId() { return teamId; }
    public String getStatus() { return status; }
    public String getPaymentStatus() { return paymentStatus; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getDecidedAt() { return decidedAt; }

    public void paid() { paymentStatus = "SUCCESS"; }

    public void leave() {
        status = "CANCELLED";
        decidedAt = Instant.now();
    }

    public void rejoin(String nextTeamId, boolean freeEntry) {
        boolean alreadyPaid = "SUCCESS".equals(paymentStatus);
        teamId = nextTeamId;
        status = "APPROVED";
        paymentStatus = freeEntry || alreadyPaid ? "SUCCESS" : "PENDING";
        decidedAt = null;
    }
}
