package com.busbooking.bus_ticket_management.security;


import com.busbooking.bus_ticket_management.model.SecurityEventType;
import com.busbooking.bus_ticket_management.service.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import org.springframework.security.core.*;
import org.springframework.security.web.authentication.*;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class AccountAuthenticationHandlers implements AuthenticationSuccessHandler,
        AuthenticationFailureHandler, LogoutHandler {
    private final AccountSessionService sessions;
    private final SecurityAuditService audit;
    private final LoginAttemptService attempts;

    public AccountAuthenticationHandlers(AccountSessionService sessions, SecurityAuditService audit,
                                         LoginAttemptService attempts) {
        this.sessions = sessions;
        this.audit = audit;
        this.attempts = attempts;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        AccountPrincipal principal = (AccountPrincipal) authentication.getPrincipal();
        sessions.register(principal, request);
        audit.record(principal.getId(), SecurityEventType.LOGIN_SUCCESS, request);
        // Do not retain stale login exceptions or saved request parameters in the session.
        request.getSession().removeAttribute("SPRING_SECURITY_LAST_EXCEPTION");
        request.getSession().removeAttribute("SPRING_SECURITY_SAVED_REQUEST");
        response.sendRedirect(request.getContextPath() + "/user/profile");
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String username = request.getParameter("username");
        long remaining = attempts.remainingSeconds(username);
        audit.recordLoginFailure(username, remaining > 0, request);
        String suffix = remaining > 0 ? "?retrySeconds=" + remaining : "?error";
        response.sendRedirect(request.getContextPath() + "/auth/login" + suffix);
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof AccountPrincipal principal) {
            sessions.logout(principal.getId(), request.getSession(false));
            audit.record(principal.getId(), SecurityEventType.LOGOUT, request);
        }
    }
}
