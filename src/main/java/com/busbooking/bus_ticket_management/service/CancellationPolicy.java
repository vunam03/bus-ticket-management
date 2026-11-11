package com.busbooking.bus_ticket_management.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class CancellationPolicy {

    public boolean canCancel(
            LocalDateTime departure,
            LocalDateTime now
    ) {
        return !departure.isBefore(now.plusHours(6));
    }

    public BigDecimal refund(
            BigDecimal paid,
            LocalDateTime departure,
            LocalDateTime now
    ) {
        if (!canCancel(departure, now)) {
            return BigDecimal.ZERO;
        }

        long hoursRemaining = Duration.between(now, departure).toHours();

        BigDecimal rate = hoursRemaining >= 24
                ? BigDecimal.ONE
                : new BigDecimal("0.70");

        return paid.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }
}