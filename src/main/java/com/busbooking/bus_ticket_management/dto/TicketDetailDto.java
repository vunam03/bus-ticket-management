package com.busbooking.bus_ticket_management.dto;

import com.busbooking.bus_ticket_management.model.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

public record TicketDetailDto(
        Long id,
        String code,
        String route,
        String company,
        String passenger,
        String seat,
        SeatType seatType,
        String benefits,
        LocalDateTime departure,
        BigDecimal total,
        TicketStatus status,
        PaymentStatus paymentStatus,
        PaymentMethodType method,
        Instant createdAt,
        Instant expiresAt,
        Instant cancelledAt,
        BigDecimal refundAmount,
        BigDecimal refundPreview,
        boolean cancellable,
        boolean payable,
        String cancellationReason) {}
