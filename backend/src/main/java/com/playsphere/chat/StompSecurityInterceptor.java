package com.playsphere.chat;

import com.playsphere.auth.SessionService;
import com.playsphere.security.CustomUserDetailsService;
import com.playsphere.security.JwtService;
import java.security.Principal;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class StompSecurityInterceptor implements ChannelInterceptor {
    private final JwtService jwtService;
    private final SessionService sessions;
    private final CustomUserDetailsService userDetailsService;
    private final ConversationMemberRepository members;

    public StompSecurityInterceptor(JwtService jwtService, SessionService sessions,
                                    CustomUserDetailsService userDetailsService,
                                    ConversationMemberRepository members) {
        this.jwtService = jwtService;
        this.sessions = sessions;
        this.userDetailsService = userDetailsService;
        this.members = members;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String header = accessor.getFirstNativeHeader("Authorization");
            if (header == null || !header.startsWith("Bearer ")) {
                throw new AccessDeniedException("WebSocket authentication required");
            }
            String token = header.substring(7);
            if (!jwtService.isValid(token)) throw new AccessDeniedException("Invalid access token");
            String userId = jwtService.extractSubject(token);
            String sessionId = jwtService.extractSessionId(token);
            if (!sessions.validateAccess(sessionId, userId)) throw new AccessDeniedException("Session expired");
            UserDetails principal = userDetailsService.loadUserByUsername(userId);
            if (!principal.isEnabled()) throw new AccessDeniedException("Account disabled");
            accessor.setUser(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        }
        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            Principal principal = accessor.getUser();
            String destination = accessor.getDestination();
            if (principal == null || destination == null || !destination.startsWith("/topic/conversations/")) {
                throw new AccessDeniedException("Invalid subscription");
            }
            String conversationId = destination.substring("/topic/conversations/".length());
            if (!members.existsByConversationIdAndUserId(conversationId, principal.getName())) {
                throw new AccessDeniedException("Conversation membership required");
            }
        }
        return message;
    }
}
