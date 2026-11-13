package com.busbooking.bus_ticket_management.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BusTypeTest {

    @Test
    void normalizeKeyRemovesAccentsAndSeparators() {
        assertEquals("giuong_nam", BusType.normalizeKey("Giường nằm"));
        assertEquals("giuong_nam", BusType.normalizeKey("  GIUONG_NAM "));
        assertEquals("ghe_ngoi", BusType.normalizeKey("Ghế ngồi"));
        assertEquals("limousine", BusType.normalizeKey("Limousine"));
        assertEquals("", BusType.normalizeKey(null));
        assertEquals("d", BusType.normalizeKey("Đ"));
    }

    @Test
    void recognisesEveryReasonableSpelling() {
        assertEquals(BusType.GIUONG_NAM, BusType.fromCode("Giường nằm").orElseThrow());
        assertEquals(BusType.GIUONG_NAM, BusType.fromCode("giuong nam").orElseThrow());
        assertEquals(BusType.GIUONG_NAM, BusType.fromCode("GIUONG_NAM").orElseThrow());
        assertEquals(BusType.GHE_NGOI, BusType.fromCode("ghế ngồi").orElseThrow());
        assertEquals(BusType.LIMOUSINE, BusType.fromCode("LIMOUSINE").orElseThrow());
    }

    @Test
    void unknownOrBlankValuesAreNotRecognised() {
        assertTrue(BusType.fromCode("Xe buýt").isEmpty());
        assertTrue(BusType.fromCode("").isEmpty());
        assertTrue(BusType.fromCode(null).isEmpty());
    }

    @Test
    void labelFallsBackToRawValue() {
        assertEquals("Giường nằm", BusType.labelOf("giuong_nam"));
        assertEquals("Xe buýt", BusType.labelOf("Xe buýt"));
        assertEquals("Khác", BusType.labelOf(null));
    }

    @Test
    void databaseValuesAreLowerCaseAndCoverCommonSpellings() {
        assertTrue(BusType.GIUONG_NAM.getDatabaseValues().contains("giường nằm"));
        assertTrue(BusType.GIUONG_NAM.getDatabaseValues().contains("giuong nam"));
        assertTrue(BusType.GIUONG_NAM.getDatabaseValues().contains("giuong_nam"));
        assertTrue(BusType.LIMOUSINE.getDatabaseValues().contains("limousine"));
        for (String value : BusType.GHE_NGOI.getDatabaseValues()) {
            assertEquals(value.toLowerCase(), value);
        }
        assertFalse(BusType.LIMOUSINE.getDatabaseValues().contains("Limousine"));
    }
}
