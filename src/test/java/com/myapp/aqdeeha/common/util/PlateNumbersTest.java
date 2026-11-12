package com.myapp.aqdeeha.common.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlateNumbersTest {

    @ParameterizedTest
    @ValueSource(strings = {"س ع د ١٢٣", "سعد١٢٣", "سعد 123", "1234 ب ج", "ABC1234", "أ ب ج د ١ ٢ ٣ ٤", "ب١ج٢", "123", "سعد",
            "  س ع د  ١٢٣  "})
    void accepts(String plate) {
        assertTrue(plate.matches(PlateNumbers.REGEX), plate);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "سعد١٢٣٤٥٦", "ABCDE12345", "س-ع-د", "ABC_123", "سعد/123", "١٢٣.", "أ ب ج د ١ ٢ ٣ ٤ ٥"})
    void rejects(String plate) {
        assertFalse(plate.matches(PlateNumbers.REGEX), plate);
    }

    @Test
    void normalizeCollapsesSpaces() {
        assertEquals("س ع د ١٢٣", PlateNumbers.normalize("  س  ع د   ١٢٣ "));
    }
}
