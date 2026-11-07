package com.busbooking.bus_ticket_management.service;


import com.busbooking.bus_ticket_management.dto.SecurityEventResponse;
import com.busbooking.bus_ticket_management.model.*;
import com.busbooking.bus_ticket_management.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.*;
import java.time.*;
import java.time.format.DateTimeFormatter;

@Service
public class SecurityAuditService {
    private final SecurityEventRepository events;
    private final UserRepository users;
    private final Clock clock;
    private static final DateTimeFormatter DISPLAY_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    public SecurityAuditService(SecurityEventRepository events, UserRepository users, Clock clock) {
        this.events = events;
        this.users = users;
        this.clock = clock;
    }

    @Transactional
    public void record(Long userId, SecurityEventType type) {
        var attributes = RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes instanceof ServletRequestAttributes servlet
                ? servlet.getRequest() : null;
        record(userId, type, request);
    }

    @Transactional
    public void record(Long userId, SecurityEventType type, HttpServletRequest request) {
        SecurityEvent event = new SecurityEvent();
        event.setUserId(userId);
        event.setType(type);
        event.setOccurredAt(clock.instant());
        event.setIpAddress(request == null ? "" : clean(request.getRemoteAddr(), 64));
        event.setDevice(request == null ? "" : clean(request.getHeader("User-Agent"), 180));
        events.save(event);
    }

    @Transactional
    public void recordLoginFailure(String username, boolean blocked, HttpServletRequest request) {
        users.findByUsername(UserService.normalize(username)).ifPresent(user ->
                record(user.getId(), blocked ? SecurityEventType.LOGIN_BLOCKED
                        : SecurityEventType.LOGIN_FAILED, request));
    }

    @Transactional(readOnly = true)
    public Page<SecurityEventResponse> history(Long owner, SecurityEventType type, int page) {
        Pageable pageable = PageRequest.of(Math.max(0, Math.min(page, 100_000)), 15);
        Page<SecurityEvent> result = type == null
                ? events.findByUserIdOrderByOccurredAtDescIdDesc(owner, pageable)
                : events.findByUserIdAndTypeOrderByOccurredAtDescIdDesc(owner, type, pageable);
        return result.map(event -> new SecurityEventResponse(event.getId(), event.getType().getLabel(),
                displayTime(event.getOccurredAt()), event.getIpAddress(), event.getDevice()));
    }

    public static String displayTime(Instant value) {
        return DISPLAY_TIME.format(value);
    }

    public static String clean(String value, int maxLength) {
        if (value == null || value.isBlank()) return "";
        String cleaned = value.replaceAll("[\\p{Cntrl}]", " ").strip();
        return cleaned.substring(0, Math.min(cleaned.length(), maxLength));
    }
}
