package com.playsphere.config;

import com.playsphere.user.AppUser;
import com.playsphere.user.AppUserRepository;
import com.playsphere.user.PlatformRole;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "app.seed-admin.enabled", havingValue = "true")
public class AdminBootstrap implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties properties;

    public AdminBootstrap(AppUserRepository users, PasswordEncoder passwordEncoder, AppProperties properties) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String email = properties.seedAdmin().email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmailIgnoreCase(email)) {
            log.info("Development admin seed skipped because {} already exists", email);
            return;
        }

        AppUser admin = new AppUser(
                email,
                passwordEncoder.encode(properties.seedAdmin().password()),
                "PlaySphere Admin",
                PlatformRole.ADMIN
        );
        admin.verifyEmail();
        users.save(admin);
        log.warn("Development admin account created for {}. Disable seeding and change the password outside local development.", email);
    }
}
