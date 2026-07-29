package com.playsphere.security;

import com.playsphere.auth.SessionController;
import com.playsphere.auth.SessionRequestMetadata;
import com.playsphere.auth.SessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final SessionService sessions;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            CustomUserDetailsService userDetailsService,
            SessionService sessions
    ) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.sessions = sessions;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        try {
            if (SecurityContextHolder.getContext().getAuthentication() == null && jwtService.isValid(token)) {
                String userId = jwtService.extractSubject(token);
                String sessionId = jwtService.extractSessionId(token);
                if (sessions.validateAccess(sessionId, userId, SessionRequestMetadata.from(request))) {
                    UserDetails principal = userDetailsService.loadUserByUsername(userId);
                    if (principal.isEnabled()) {
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                        authentication.setDetails(request.getRemoteAddr());
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        request.setAttribute(SessionController.SESSION_ID_ATTRIBUTE, sessionId);
                    }
                }
            }
        } catch (RuntimeException ignored) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
