package com.playsphere.security;

import com.playsphere.user.AccountStatus;
import com.playsphere.user.AppUser;
import com.playsphere.user.AppUserRepository;
import java.util.stream.Collectors;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final AppUserRepository users;

    public CustomUserDetailsService(AppUserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String idOrEmail) throws UsernameNotFoundException {
        AppUser user = users.findById(idOrEmail)
                .or(() -> users.findByEmailIgnoreCase(idOrEmail))
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        var authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .collect(Collectors.toSet());

        boolean enabled = user.getStatus() == AccountStatus.ACTIVE && user.isEmailVerified();
        return User.withUsername(user.getId())
                .password(user.getPasswordHash())
                .authorities(authorities)
                .disabled(!enabled)
                .build();
    }
}
