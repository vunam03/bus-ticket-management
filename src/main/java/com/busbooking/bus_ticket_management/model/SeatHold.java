package com.busbooking.bus_ticket_management.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

/**
 * Represents a temporary hold placed on a seat for a specific trip.
 * A hold expires after a configured duration (default 10 minutes) and is
 * automatically released by scheduled cleanup or on-demand validation.
 */
@Entity
@Table(
    name = "seat_holds",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_seat_holds_seat", columnNames = {"seat_id"})
    },
    indexes = {
        @Index(name = "idx_seat_holds_trip_holder", columnList = "trip_id, holder_key"),
        @Index(name = "idx_seat_holds_expires", columnList = "hold_expires_at")
    }
)
public class SeatHold {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private BusTrip busTrip;

    /**
     * Identifies the customer holding the seat. This can be an authenticated
     * user id or an anonymous session id, allowing guests to select seats.
     */
    @Column(name = "holder_key", nullable = false, length = 120)
    private String holderKey;

    @Column(name = "hold_expires_at", nullable = false)
    private LocalDateTime holdExpiresAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public SeatHold() {
    }

    public SeatHold(Seat seat, BusTrip busTrip, String holderKey, LocalDateTime holdExpiresAt, LocalDateTime createdAt) {
        this.seat = seat;
        this.busTrip = busTrip;
        this.holderKey = holderKey;
        this.holdExpiresAt = holdExpiresAt;
        this.createdAt = createdAt;
    }

    public boolean isExpired(LocalDateTime now) {
        return holdExpiresAt != null && !holdExpiresAt.isAfter(now);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Seat getSeat() {
        return seat;
    }

    public void setSeat(Seat seat) {
        this.seat = seat;
    }

    public BusTrip getBusTrip() {
        return busTrip;
    }

    public void setBusTrip(BusTrip busTrip) {
        this.busTrip = busTrip;
    }

    public String getHolderKey() {
        return holderKey;
    }

    public void setHolderKey(String holderKey) {
        this.holderKey = holderKey;
    }

    public LocalDateTime getHoldExpiresAt() {
        return holdExpiresAt;
    }

    public void setHoldExpiresAt(LocalDateTime holdExpiresAt) {
        this.holdExpiresAt = holdExpiresAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
