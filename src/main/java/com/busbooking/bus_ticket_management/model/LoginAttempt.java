package com.busbooking.bus_ticket_management.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Persistent throttling key is a digest of normalized username, including unknown names.
 */
@Entity
@Table(name = "account_login_attempts", indexes = {@Index(name = "idx_attempt_expiry", columnList = "window_ends_at")})
public class LoginAttempt {

    @Id
    @Column(length = 64)
    private String usernameHash;

    @Column(nullable = false)
    private int failures;

    @Column(nullable = false)
    private Instant windowEndsAt;

    private Instant lockedUntil;

    public LoginAttempt() {
    }

    public String getUsernameHash() {
        return usernameHash;
    }

    public void setUsernameHash(String usernameHash) {
        this.usernameHash = usernameHash;
    }

    public int getFailures() {
        return failures;
    }

    public void setFailures(int failures) {
        this.failures = failures;
    }

    public Instant getWindowEndsAt() {
        return windowEndsAt;
    }

    public void setWindowEndsAt(Instant windowEndsAt) {
        this.windowEndsAt = windowEndsAt;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(Instant lockedUntil) {
        this.lockedUntil = lockedUntil;
    }


}
