package com.busbooking.bus_ticket_management.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Immutable through the application: no passwords, tokens or session identifiers.
 */
@Entity
@Table(name = "account_security_events", indexes = {@Index(name = "idx_security_owner_time", columnList = "user_id,occurred_at")})
public class SecurityEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 40)
    private SecurityEventType type;

    @Column(nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(length = 64, updatable = false)
    private String ipAddress;

    @Column(length = 180, updatable = false)
    private String device;

    public SecurityEvent() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public SecurityEventType getType() {
        return type;
    }

    public void setType(SecurityEventType type) {
        this.type = type;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
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


}
