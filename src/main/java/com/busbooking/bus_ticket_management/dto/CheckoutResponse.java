package com.busbooking.bus_ticket_management.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CheckoutResponse(
        Long tripId,
        String route,
        String company,
        LocalDateTime departure,
        List<SeatLine> seats,
        BigDecimal total,
        LocalDateTime holdUntil) {
    public record SeatLine(Long id, String number, BigDecimal amount, com.busbooking.bus_ticket_management.model.SeatType seatType, String benefits) {}
}
