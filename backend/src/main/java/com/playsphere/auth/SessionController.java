package com.playsphere.auth;

import com.playsphere.common.ApiResponse;
import com.playsphere.user.CurrentUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/sessions")
public class SessionController {
    public static final String SESSION_ID_ATTRIBUTE = "playsphere.sessionId";

    private final SessionService sessions;
    private final CurrentUserService currentUser;
    private final RefreshCookieService cookies;

    public SessionController(
            SessionService sessions,
            CurrentUserService currentUser,
            RefreshCookieService cookies
    ) {
        this.sessions = sessions;
        this.currentUser = currentUser;
        this.cookies = cookies;
    }

    @GetMapping
    ApiResponse<List<SessionSummary>> list(Authentication authentication, HttpServletRequest request) {
        String userId = currentUser.require(authentication).getId();
        String currentSessionId = (String) request.getAttribute(SESSION_ID_ATTRIBUTE);
        return ApiResponse.ok("Active sessions", sessions.list(userId, currentSessionId));
    }

    @DeleteMapping("/{sessionId}")
    ApiResponse<Void> revoke(
            @PathVariable String sessionId,
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String userId = currentUser.require(authentication).getId();
        sessions.revoke(userId, sessionId);
        // The target cookie is sent to this same-origin request even when the
        // selected session belongs to another tab. Expiring it prevents stale
        // per-tab refresh cookies from accumulating in the browser.
        cookies.clear(response, sessionId);
        return ApiResponse.ok("Session revoked");
    }
}
