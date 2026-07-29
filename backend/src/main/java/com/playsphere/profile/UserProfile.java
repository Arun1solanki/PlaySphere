package com.playsphere.profile;

import com.playsphere.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_profiles")
public class UserProfile {
    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private AppUser user;

    @Column(name = "full_name", nullable = false, length = 80)
    private String fullName;

    @Column(name = "phone_number", nullable = false, length = 13)
    private String phoneNumber;

    @Column(nullable = false, length = 80)
    private String city;

    @Column(nullable = false, length = 80)
    private String locality;

    @Column(name = "location_description", length = 180)
    private String locationDescription;

    @Column(length = 500)
    private String bio;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Column(name = "preferred_sports", length = 300)
    private String preferredSports;

    @Column(name = "skill_level", length = 32)
    private String skillLevel;

    @Column(name = "playing_position", length = 80)
    private String playingPosition;

    @Column(name = "availability_summary", length = 180)
    private String availabilitySummary;

    @Column(name = "organization_name", length = 140)
    private String organizationName;

    @Column(name = "business_name", length = 140)
    private String businessName;

    @Column(nullable = false)
    private boolean discoverable;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserProfile() {}

    public UserProfile(AppUser user) {
        this.id = UUID.randomUUID().toString();
        this.user = user;
        this.discoverable = true;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (id == null) id = UUID.randomUUID().toString();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }

    public void update(
            String fullName,
            String phoneNumber,
            String city,
            String locality,
            String locationDescription,
            String bio,
            String profileImageUrl,
            String preferredSports,
            String skillLevel,
            String playingPosition,
            String availabilitySummary,
            String organizationName,
            String businessName,
            boolean discoverable
    ) {
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.city = city;
        this.locality = locality;
        this.locationDescription = locationDescription;
        this.bio = bio;
        this.profileImageUrl = profileImageUrl;
        this.preferredSports = preferredSports;
        this.skillLevel = skillLevel;
        this.playingPosition = playingPosition;
        this.availabilitySummary = availabilitySummary;
        this.organizationName = organizationName;
        this.businessName = businessName;
        this.discoverable = discoverable;
    }

    public String getId() { return id; }
    public AppUser getUser() { return user; }
    public String getFullName() { return fullName; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getCity() { return city; }
    public String getLocality() { return locality; }
    public String getLocationDescription() { return locationDescription; }
    public String getBio() { return bio; }
    public String getProfileImageUrl() { return profileImageUrl; }
    public String getPreferredSports() { return preferredSports; }
    public String getSkillLevel() { return skillLevel; }
    public String getPlayingPosition() { return playingPosition; }
    public String getAvailabilitySummary() { return availabilitySummary; }
    public String getOrganizationName() { return organizationName; }
    public String getBusinessName() { return businessName; }
    public boolean isDiscoverable() { return discoverable; }
}
