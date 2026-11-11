package com.busbooking.bus_ticket_management.utils;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DateUtilsTest {

    @Test
    void formatsDuration() {
        assertEquals("6 giờ 30 phút", DateUtils.formatDuration(390));
        assertEquals("2 giờ", DateUtils.formatDuration(120));
        assertEquals("45 phút", DateUtils.formatDuration(45));
        assertEquals("—", DateUtils.formatDuration(-1));
    }

    @Test
    void durationBetweenTwoTimes() {
        LocalDateTime from = LocalDateTime.of(2026, 11, 8, 22, 0);
        assertEquals("8 giờ 30 phút", DateUtils.formatDuration(from, from.plusMinutes(510)));
    }

    @Test
    void vietnameseDayNames() {
        assertEquals("Chủ nhật", DateUtils.dayOfWeekName(LocalDate.of(2026, 11, 8)));
        assertEquals("Thứ Hai", DateUtils.dayOfWeekName(LocalDate.of(2026, 11, 9)));
        assertEquals("T2", DateUtils.shortDayName(LocalDate.of(2026, 11, 9)));
        assertEquals("CN", DateUtils.shortDayName(LocalDate.of(2026, 11, 8)));
        assertEquals("Thứ Hai, 09/11/2026", DateUtils.fullDateLabel(LocalDate.of(2026, 11, 9)));
        assertEquals("Mọi ngày", DateUtils.fullDateLabel(null));
    }

    @Test
    void relativeLabels() {
        LocalDate today = LocalDate.of(2026, 11, 7);
        assertEquals("Hôm nay", DateUtils.relativeDayLabel(today, today));
        assertEquals("Ngày mai", DateUtils.relativeDayLabel(today.plusDays(1), today));
        assertEquals("", DateUtils.relativeDayLabel(today.plusDays(2), today));
    }

    @Test
    void parsesBothDateFormats() {
        assertEquals(LocalDate.of(2026, 11, 9), DateUtils.parseDate("2026-11-09").orElseThrow());
        assertEquals(LocalDate.of(2026, 11, 9), DateUtils.parseDate("09/11/2026").orElseThrow());
        assertTrue(DateUtils.parseDate("không phải ngày").isEmpty());
        assertTrue(DateUtils.parseDate("").isEmpty());
    }

    @Test
    void convertsNativeQueryDateValues() {
        LocalDate expected = LocalDate.of(2026, 11, 8);
        assertEquals(expected, DateUtils.toLocalDate(java.sql.Date.valueOf(expected)));
        assertEquals(expected, DateUtils.toLocalDate(expected));
        assertEquals(expected, DateUtils.toLocalDate(expected.atTime(10, 0)));
        assertEquals(expected, DateUtils.toLocalDate("2026-11-08"));
        assertThrows(IllegalArgumentException.class, () -> DateUtils.toLocalDate(42));
    }
}
