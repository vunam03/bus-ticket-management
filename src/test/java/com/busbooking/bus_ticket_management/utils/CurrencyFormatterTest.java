package com.busbooking.bus_ticket_management.utils;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrencyFormatterTest {

    @Test
    void formatsWithDotSeparatorAndSuffix() {
        assertEquals("350.000đ", CurrencyFormatter.format(new BigDecimal("350000")));
        assertEquals("1.250.000đ", CurrencyFormatter.format(1250000));
        assertEquals("0đ", CurrencyFormatter.format(BigDecimal.ZERO));
    }

    @Test
    void nullAmountShowsContact() {
        assertEquals("Liên hệ", CurrencyFormatter.format((BigDecimal) null));
    }

    @Test
    void rangeCollapsesWhenEqual() {
        assertEquals("100.000đ", CurrencyFormatter.formatRange(new BigDecimal("100000"), new BigDecimal("100000")));
        assertEquals("100.000đ - 200.000đ", CurrencyFormatter.formatRange(new BigDecimal("100000"), new BigDecimal("200000")));
    }

    @Test
    void compactForm() {
        assertEquals("350K", CurrencyFormatter.compact(new BigDecimal("350000")));
        assertEquals("1,2tr", CurrencyFormatter.compact(new BigDecimal("1200000")));
    }

    @Test
    void parseReadsDigitsOnly() {
        assertEquals(350000, CurrencyFormatter.parse("350.000đ").orElseThrow().intValue());
        assertTrue(CurrencyFormatter.parse("abc").isEmpty());
        assertTrue(CurrencyFormatter.parse(null).isEmpty());
    }

    @Test
    void toBigDecimalAcceptsAnyNumber() {
        assertEquals(0, CurrencyFormatter.toBigDecimal(150000L).compareTo(new BigDecimal("150000")));
        assertEquals(0, CurrencyFormatter.toBigDecimal(new BigDecimal("12.5")).compareTo(new BigDecimal("12.5")));
        assertEquals(0, CurrencyFormatter.toBigDecimal(null).compareTo(BigDecimal.ZERO));
    }
}
