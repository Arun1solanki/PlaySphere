package com.playsphere.config;

import com.playsphere.profile.UserProfile;
import com.playsphere.profile.UserProfileRepository;
import com.playsphere.user.AccountStatus;
import com.playsphere.user.AppUser;
import com.playsphere.user.AppUserRepository;
import com.playsphere.user.PlatformRole;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "app.seed-demo.enabled", havingValue = "true")
public class DemoAccountsBootstrap implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DemoAccountsBootstrap.class);

    private final AppUserRepository users;
    private final UserProfileRepository profiles;
    private final PasswordEncoder passwordEncoder;
    private final String demoPassword;

    public DemoAccountsBootstrap(
            AppUserRepository users,
            UserProfileRepository profiles,
            PasswordEncoder passwordEncoder,
            @Value("${app.seed-demo.password:Demo@12345}") String demoPassword
    ) {
        this.users = users;
        this.profiles = profiles;
        this.passwordEncoder = passwordEncoder;
        this.demoPassword = demoPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<DemoAccount> accounts = List.of(
                new DemoAccount(
                        "player@playsphere.local", "Demo Player", PlatformRole.PLAYER,
                        "+919876543210", "Navi Mumbai", "Vashi",
                        "Football, Cricket", "INTERMEDIATE", "Midfielder",
                        "Weekday evenings and weekends", null, null
                ),
                new DemoAccount(
                        "organizer@playsphere.local", "Demo Organizer", PlatformRole.ORGANIZER,
                        "+919876543211", "Navi Mumbai", "Nerul",
                        null, null, null, null,
                        "PlaySphere Sports Events", null
                ),
                new DemoAccount(
                        "owner@playsphere.local", "Demo Turf Owner", PlatformRole.TURF_OWNER,
                        "+919876543212", "Navi Mumbai", "Kharghar",
                        null, null, null, null,
                        null, "PlaySphere Arena"
                ),
                new DemoAccount(
                        "admin@playsphere.local", "Demo Admin", PlatformRole.ADMIN,
                        "+919876543213", "Navi Mumbai", "Belapur",
                        null, null, null, null,
                        null, null
                ),
                new DemoAccount(
                        "superadmin@playsphere.local", "Demo Super Admin", PlatformRole.SUPER_ADMIN,
                        "+919876543214", "Navi Mumbai", "Belapur",
                        null, null, null, null,
                        null, null
                )
        );

        accounts.forEach(this::seedAccount);
        log.warn("Development demo accounts are enabled. Disable APP_SEED_DEMO_ACCOUNTS outside local development.");
    }

    private void seedAccount(DemoAccount demo) {
        String email = demo.email().toLowerCase(Locale.ROOT);
        AppUser user = users.findByEmailIgnoreCase(email)
                .orElseGet(() -> new AppUser(
                        email,
                        passwordEncoder.encode(demoPassword),
                        demo.displayName(),
                        demo.role()
                ));

        // Demo accounts are intentionally refreshed so their documented credentials remain usable locally.
        user.setPasswordHash(passwordEncoder.encode(demoPassword));
        user.verifyEmail();
        user.setStatus(AccountStatus.ACTIVE);
        user.markProfileCompleted(demo.displayName());
        users.save(user);

        UserProfile profile = profiles.findByUser_Id(user.getId())
                .orElseGet(() -> new UserProfile(user));
        profile.update(
                demo.displayName(),
                demo.phoneNumber(),
                demo.city(),
                demo.locality(),
                demo.locality() + ", " + demo.city(),
                "Local development demo account for the " + demo.role() + " role.",
                null,
                demo.preferredSports(),
                demo.skillLevel(),
                demo.playingPosition(),
                demo.availabilitySummary(),
                demo.organizationName(),
                demo.businessName(),
                true
        );
        profiles.save(profile);
        log.info("Demo {} account ready: {}", demo.role(), email);
    }

    private record DemoAccount(
            String email,
            String displayName,
            PlatformRole role,
            String phoneNumber,
            String city,
            String locality,
            String preferredSports,
            String skillLevel,
            String playingPosition,
            String availabilitySummary,
            String organizationName,
            String businessName
    ) {}
}
