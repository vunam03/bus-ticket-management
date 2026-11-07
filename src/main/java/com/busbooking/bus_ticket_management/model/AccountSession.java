package com.busbooking.bus_ticket_management.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Metadata for revocable sessions. Never persist the raw JSESSIONID.
 */
@Entity
@Table(name = "account_sessions", indexes = {@Index(name = "idx_session_owner_expiry", columnList = "user_id,expires_at")})
public class AccountSession {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, unique = true, length = 64)
    private String sessionHash;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private long credentialVersion;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant lastSeenAt;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(length = 64)
    private String ipAddress;

    @Column(length = 180)
    private String device;

    @Column(nullable = false)
    private boolean revoked;

    public AccountSession() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSessionHash() {
        return sessionHash;
    }

    public void setSessionHash(String sessionHash) {
        this.sessionHash = sessionHash;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public long getCredentialVersion() {
        return credentialVersion;
    }

    public void setCredentialVersion(long credentialVersion) {
        this.credentialVersion = credentialVersion;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(Instant lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public boolean getRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }


}
