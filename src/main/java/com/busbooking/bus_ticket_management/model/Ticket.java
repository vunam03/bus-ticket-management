package com.busbooking.bus_ticket_management.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "tickets",
        indexes = {
                @Index(name = "idx_tickets_user", columnList = "user_id"),
                @Index(name = "idx_tickets_trip", columnList = "trip_id"),
                @Index(name = "idx_tickets_seat", columnList = "seat_id")
        }
)
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_type", length = 20)
    private SeatType seatType;
    @Column(name = "seat_benefits", length = 500)
    private String benefits;
    public String getBenefits() { return benefits == null || benefits.isBlank() ? "Tiện ích chưa được ghi nhận trên vé này." : benefits; }
    public void setBenefits(String value) { benefits = value; }
    public SeatType getSeatType() { return seatType == null ? SeatType.NORMAL : seatType; }
    public void setSeatType(SeatType value) { seatType = value; }


    @Column(
            name = "ticket_code",
            nullable = false,
            unique = true,
            updatable = false,
            length = 36
    )
    private String ticketCode;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private BusTrip busTrip;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @NotNull
    @DecimalMin(value = "0.00", inclusive = false)
    @Column(
            name = "total_amount",
            nullable = false,
            precision = 14,
            scale = 2
    )
    private BigDecimal totalAmount;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethodType paymentMethod;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "ticket_status", nullable = false, length = 20)
    private TicketStatus ticketStatus = TicketStatus.PENDING;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    @Column(name = "booking_key", length = 36)
    private String bookingKey;

    public String getBookingKey() { return bookingKey; }
    public void setBookingKey(String value) { this.bookingKey = value; }

    @Column(name = "refund_amount", precision = 14, scale = 2)
    private BigDecimal refundAmount;

    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal value) { this.refundAmount = value; }

    @Column(name = "expires_at")
    private Instant expiresAt;

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant value) { this.expiresAt = value; }

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    public Instant getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(Instant value) { this.cancelledAt = value; }

    @Column(name = "cancellation_reason", length = 255)
    private String cancellationReason;

    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String value) { this.cancellationReason = value; }

    public Ticket() {
    }

    public Ticket(User user, BusTrip busTrip, Seat seat,
                  BigDecimal totalAmount,
                  PaymentMethodType paymentMethod) {
        this.user = user;
        this.busTrip = busTrip;
        this.seat = seat;
        this.totalAmount = totalAmount;
        this.paymentMethod = paymentMethod;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.ticketCode = UUID.randomUUID().toString();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getTicketCode() {
        return ticketCode;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public BusTrip getBusTrip() {
        return busTrip;
    }

    public void setBusTrip(BusTrip busTrip) {
        this.busTrip = busTrip;
    }

    public Seat getSeat() {
        return seat;
    }

    public void setSeat(Seat seat) {
        this.seat = seat;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public PaymentMethodType getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethodType paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public TicketStatus getTicketStatus() {
        return ticketStatus;
    }

    public void setTicketStatus(TicketStatus ticketStatus) {
        this.ticketStatus = ticketStatus;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }
}