package com.busbooking.bus_ticket_management.service;


import com.busbooking.bus_ticket_management.dto.AccountSessionResponse;
import com.busbooking.bus_ticket_management.model.*;
import com.busbooking.bus_ticket_management.repository.*;
import com.busbooking.bus_ticket_management.security.AccountPrincipal;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.*;
import java.util.*;

@Service
@Transactional
public class AccountSessionService {
    private final AccountSessionRepository sessions;
    private final UserRepository users;
    private final SecurityAuditService audit;
    private final Clock clock;
    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    public AccountSessionService(AccountSessionRepository sessions, UserRepository users,
                                 SecurityAuditService audit, Clock clock) {
        this.sessions = sessions;
        this.users = users;
        this.audit = audit;
        this.clock = clock;
    }

    public void register(AccountPrincipal principal, HttpServletRequest request) {
        HttpSession browserSession = request.getSession();
        AccountSession session = new AccountSession();
        session.setId(UUID.randomUUID().toString());
        session.setSessionHash(hash(browserSession));
        session.setUserId(principal.getId());
        session.setCredentialVersion(principal.getCredentialVersion());
        session.setCreatedAt(clock.instant());
        session.setLastSeenAt(clock.instant());
        session.setExpiresAt(expiry(browserSession));
        session.setIpAddress(SecurityAuditService.clean(request.getRemoteAddr(), 64));
        session.setDevice(SecurityAuditService.clean(request.getHeader("User-Agent"), 180));
        sessions.saveAndFlush(session);
    }

    public boolean touch(AccountPrincipal principal, HttpSession browserSession) {
        if (browserSession == null) return false;
        var found = sessions.findBySessionHashAndUserId(hash(browserSession), principal.getId());
        if (found.isEmpty()) return false;
        AccountSession session = found.get();
        if (session.getRevoked() || session.getCredentialVersion() != principal.getCredentialVersion()) {
            return false;
        }
        return sessions.touchActive(session.getId(), clock.instant(), expiry(browserSession)) == 1;
    }

    @Transactional(readOnly = true)
    public List<AccountSessionResponse> active(AccountPrincipal principal, HttpSession current) {
        String currentHash = hash(current);
        return sessions.findByUserIdAndCredentialVersionAndRevokedFalseAndExpiresAtAfterOrderByLastSeenAtDesc(
                        principal.getId(), principal.getCredentialVersion(), clock.instant())
                .stream().map(session -> new AccountSessionResponse(session.getId(),
                        SecurityAuditService.displayTime(session.getCreatedAt()),
                        SecurityAuditService.displayTime(session.getLastSeenAt()),
                        SecurityAuditService.displayTime(session.getExpiresAt()),
                        session.getIpAddress(), session.getDevice(), session.getSessionHash().equals(currentHash)))
                .toList();
    }

    /** Ownership is part of the query, not a hidden form field. */
    public boolean revoke(Long owner, String managementId, HttpSession current) {
        AccountSession target = sessions.findByIdAndUserId(managementId, owner)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy phiên."));
        if (!target.getRevoked()) {
            target.setRevoked(true);
            sessions.saveAndFlush(target);
            audit.record(owner, SecurityEventType.SESSION_REVOKED);
        }
        return target.getSessionHash().equals(hash(current));
    }

    public void revokeOthers(Long owner, HttpSession current) {
        sessions.revokeOtherSessions(owner, hash(current));
        audit.record(owner, SecurityEventType.OTHER_SESSIONS_REVOKED);
    }

    public void revokeAll(Long owner) {
        User user = users.findLockedById(owner).orElseThrow();
        entityManager.refresh(user, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        user.setCredentialVersion(user.getCredentialVersion() + 1);
        users.saveAndFlush(user);
        sessions.revokeAllForUser(owner);
        audit.record(owner, SecurityEventType.ALL_SESSIONS_REVOKED);
    }

    public void logout(Long owner, HttpSession current) {
        if (current != null) {
            sessions.findBySessionHashAndUserId(hash(current), owner).ifPresent(session -> {
                session.setRevoked(true);
                sessions.save(session);
            });
        }
    }

    private Instant expiry(HttpSession session) {
        int seconds = session.getMaxInactiveInterval();
        return clock.instant().plusSeconds(seconds > 0 ? seconds : 1800);
    }

    private String hash(HttpSession session) {
        return session == null ? "" : AuthService.digest(session.getId());
    }
}
