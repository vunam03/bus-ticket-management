package com.busbooking.bus_ticket_management.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

//Lưu thông tin từng lần thanh toán. Một vé có thể có nhiều lần thử thanh toán.//
@Entity
@Table(
        name = "payment_transactions",
        indexes = {
                @Index(name = "idx_payment_ticket", columnList = "ticket_id"),
                @Index(name = "idx_payment_status", columnList = "status")
        }
)
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "transaction_code",
            nullable = false,
            unique = true,
            updatable = false,
            length = 36
    )
    private String transactionCode;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false, updatable = false)
    private Ticket ticket;

    @NotNull
    @DecimalMin(value = "0.00", inclusive = false)
    @Column(
            name = "amount",
            nullable = false,
            updatable = false,
            precision = 14,
            scale = 2
    )
    private BigDecimal amount;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(
            name = "payment_method",
            nullable = false,
            updatable = false,
            length = 30
    )
    private PaymentMethodType paymentMethod;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentTransactionStatus status =
            PaymentTransactionStatus.PENDING;

    // Mã tham chiếu do hệ thống thanh toán trả về, nếu có.
    @Column(name = "provider_reference", length = 100)
    private String providerReference;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Thời điểm giao dịch thành công hoặc thất bại.
    @Column(name = "completed_at")
    private Instant completedAt;

    @Version
    private Long version;

    protected PaymentTransaction() {
        // Constructor dành cho JPA.
    }

    public PaymentTransaction(Ticket ticket, BigDecimal amount,
                              PaymentMethodType paymentMethod) {
        if (ticket == null) {
            throw new IllegalArgumentException("Vé không được để trống.");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Số tiền thanh toán phải lớn hơn 0.");
        }
        if (paymentMethod == null) {
            throw new IllegalArgumentException(
                    "Phương thức thanh toán không được để trống.");
        }

        this.ticket = ticket;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.transactionCode = UUID.randomUUID().toString();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void markSuccess(String providerReference) {
        requirePending();

        String reference = providerReference == null
                ? null : providerReference.trim();

        if (reference != null && reference.length() > 100) {
            throw new IllegalArgumentException(
                    "Mã tham chiếu không được vượt quá 100 ký tự.");
        }

        this.providerReference = reference;
        this.failureReason = null;
        this.status = PaymentTransactionStatus.SUCCESS;
        this.completedAt = Instant.now();
    }

    public void markFailed(String reason) {
        requirePending();

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException(
                    "Cần cung cấp lý do thanh toán thất bại.");
        }

        String normalizedReason = reason.trim();
        if (normalizedReason.length() > 255) {
            throw new IllegalArgumentException(
                    "Lý do thất bại không được vượt quá 255 ký tự.");
        }

        this.failureReason = normalizedReason;
        this.status = PaymentTransactionStatus.FAILED;
        this.completedAt = Instant.now();
    }

    private void requirePending() {
        if (status != PaymentTransactionStatus.PENDING) {
            throw new IllegalStateException(
                    "Giao dịch đã kết thúc, không thể cập nhật kết quả.");
        }
    }

    public Long getId() {
        return id;
    }

    public String getTransactionCode() {
        return transactionCode;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public PaymentMethodType getPaymentMethod() {
        return paymentMethod;
    }

    public PaymentTransactionStatus getStatus() {
        return status;
    }

    public String getProviderReference() {
        return providerReference;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Long getVersion() {
        return version;
    }
}