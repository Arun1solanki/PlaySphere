package com.playsphere.auth;

import com.playsphere.config.AppProperties;
import com.playsphere.user.AppUser;
import com.playsphere.user.PlatformRole;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class SessionPolicy {
    private final AppProperties properties;

    public SessionPolicy(AppProperties properties) {
        this.properties = properties;
    }

    public Duration idleTimeout(AppUser user) {
        long minutes = user.getRoles().stream()
                .mapToLong(this::minutesFor)
                .min()
                .orElse(properties.session().playerIdleMinutes());
        return Duration.ofMinutes(minutes);
    }

    private long minutesFor(PlatformRole role) {
        return switch (role) {
            case PLAYER -> properties.session().playerIdleMinutes();
            case ORGANIZER -> properties.session().organizerIdleMinutes();
            case TURF_OWNER -> properties.session().turfOwnerIdleMinutes();
            case ADMIN -> properties.session().adminIdleMinutes();
            case SUPER_ADMIN -> properties.session().superAdminIdleMinutes();
        };
    }
}
