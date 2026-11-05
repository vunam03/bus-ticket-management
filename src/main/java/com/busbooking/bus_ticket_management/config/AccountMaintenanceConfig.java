package com.busbooking.bus_ticket_management.config;


import com.busbooking.bus_ticket_management.repository.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;

@Configuration
@EnableScheduling
public class AccountMaintenanceConfig {
    private final SecurityEventRepository events;
    private final AccountSessionRepository sessions;
    private final LoginAttemptRepository attempts;
    private final Clock clock;

    public AccountMaintenanceConfig(SecurityEventRepository events, AccountSessionRepository sessions,
                                    LoginAttemptRepository attempts, Clock clock) {
        this.events = events;
        this.sessions = sessions;
        this.attempts = attempts;
        this.clock = clock;
    }

    /** Retain account audit data for 90 days; expire old authentication metadata daily. */
    @Scheduled(cron = "0 15 3 * * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void cleanExpiredMetadata() {
        Instant yesterday = clock.instant().minus(Duration.ofDays(1));
        events.deleteByOccurredAtBefore(clock.instant().minus(Duration.ofDays(90)));
        sessions.deleteByExpiresAtBefore(yesterday);
        attempts.deleteByWindowEndsAtBeforeAndLockedUntilIsNull(yesterday);
        attempts.deleteByLockedUntilBefore(yesterday);
    }
}
