package com.busbooking.bus_ticket_management.repository;


import com.busbooking.bus_ticket_management.model.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;

public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, String> {
    long deleteByWindowEndsAtBeforeAndLockedUntilIsNull(Instant cutoff);
    long deleteByLockedUntilBefore(Instant cutoff);
}
