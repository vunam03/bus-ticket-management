package com.busbooking.bus_ticket_management.dto;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Khung giờ khởi hành. Khoảng giờ: [startHour, endHour). Riêng khung Tối đi qua nửa đêm (18h -> 5h).
 * <p>
 * LƯU Ý: các mốc 5 / 12 / 18 cũng được viết cứng trong câu JPQL của BusTripRepository#searchTrips.
 * Nếu đổi mốc ở đây thì phải đổi cả bên đó.
 */
public enum TimeSlot {

    MORNING("Sáng", "05:00 - 11:59", 5, 12),
    NOON("Trưa", "12:00 - 17:59", 12, 18),
    EVENING("Tối", "18:00 - 04:59", 18, 5);

    private final String label;
    private final String range;
    private final int startHour;
    private final int endHour;

    TimeSlot(String label, String range, int startHour, int endHour) {
        this.label = label;
        this.range = range;
        this.startHour = startHour;
        this.endHour = endHour;
    }

    public String getLabel() {
        return label;
    }

    public String getRange() {
        return range;
    }

    /** Giờ (0-23) có thuộc khung này không. */
    public boolean contains(int hour) {
        if (startHour < endHour) {
            return hour >= startHour && hour < endHour;
        }
        return hour >= startHour || hour < endHour; // khung qua nửa đêm
    }

    /** Xác định khung giờ của một thời điểm khởi hành. */
    public static TimeSlot of(LocalDateTime time) {
        int hour = time.getHour();
        for (TimeSlot slot : values()) {
            if (slot.contains(hour)) {
                return slot;
            }
        }
        return EVENING; // không thể xảy ra vì 3 khung phủ kín 24 giờ
    }

    public static Optional<TimeSlot> fromCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        String normalized = code.trim().toUpperCase();
        for (TimeSlot slot : values()) {
            if (slot.name().equals(normalized)) {
                return Optional.of(slot);
            }
        }
        return Optional.empty();
    }
}
