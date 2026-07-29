package com.playsphere.team;

import com.playsphere.common.Ids;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "teams")
public class Team {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "captain_user_id", nullable = false, length = 36)
    private String captainUserId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 80)
    private String sport;

    @Column(nullable = false, length = 80)
    private String city;

    @Column(nullable = false, length = 80)
    private String locality;

    @Column(name = "skill_level", nullable = false, length = 32)
    private String skillLevel;

    @Column(length = 600)
    private String description;

    @Column(name = "logo_url", length = 700)
    private String logoUrl;

    @Column(name = "max_members", nullable = false)
    private int maxMembers;

    @Column(nullable = false, length = 20)
    private String visibility;

    @Column(name = "join_mode", nullable = false, length = 24)
    private String joinMode;

    @Column(nullable = false, length = 24)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Team() {}

    public Team(
            String captainUserId,
            String name,
            String sport,
            String city,
            String locality,
            String skillLevel,
            String description,
            String logoUrl,
            int maxMembers,
            String visibility,
            String joinMode
    ) {
        this.id = Ids.uuid();
        this.captainUserId = captainUserId;
        this.name = name;
        this.sport = sport;
        this.city = city;
        this.locality = locality;
        this.skillLevel = skillLevel;
        this.description = description;
        this.logoUrl = logoUrl;
        this.maxMembers = maxMembers;
        this.visibility = visibility;
        this.joinMode = joinMode;
        this.status = "ACTIVE";
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public String getId() { return id; }
    public String getCaptainUserId() { return captainUserId; }
    public String getName() { return name; }
    public String getSport() { return sport; }
    public String getCity() { return city; }
    public String getLocality() { return locality; }
    public String getSkillLevel() { return skillLevel; }
    public String getDescription() { return description; }
    public String getLogoUrl() { return logoUrl; }
    public int getMaxMembers() { return maxMembers; }
    public String getVisibility() { return visibility; }
    public String getJoinMode() { return joinMode; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void update(
            String name,
            String sport,
            String city,
            String locality,
            String skillLevel,
            String description,
            String logoUrl,
            int maxMembers,
            String visibility,
            String joinMode
    ) {
        this.name = name;
        this.sport = sport;
        this.city = city;
        this.locality = locality;
        this.skillLevel = skillLevel;
        this.description = description;
        this.logoUrl = logoUrl;
        this.maxMembers = maxMembers;
        this.visibility = visibility;
        this.joinMode = joinMode;
        this.updatedAt = Instant.now();
    }

    public void transferCaptaincy(String nextCaptainUserId) {
        this.captainUserId = nextCaptainUserId;
        this.updatedAt = Instant.now();
    }

    public void archive() {
        this.status = "ARCHIVED";
        this.updatedAt = Instant.now();
    }
}
