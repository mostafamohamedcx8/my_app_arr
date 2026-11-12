package com.myapp.aqdeeha.common.util;

import com.myapp.aqdeeha.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PhoneNumbersTest {

    @ParameterizedTest
    @ValueSource(strings = {"01012345678", "+201012345678", "201012345678", "00201012345678",
            "010 1234 5678", "010-1234-5678", "٠١٠١٢٣٤٥٦٧٨"})
    void normalizesEgyptianMobiles(String raw) {
        assertEquals("+201012345678", PhoneNumbers.normalize(raw));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "123", "01312345678", "0101234567", "010123456789", "+441012345678", "abc"})
    void rejectsInvalidNumbers(String raw) {
        assertThrows(BusinessException.class, () -> PhoneNumbers.normalize(raw));
    }

    @Test
    void acceptsAllEgyptianPrefixes() {
        for (String p : new String[]{"010", "011", "012", "015"}) {
            assertEquals("+20" + p.substring(1) + "12345678", PhoneNumbers.normalize(p + "12345678"));
        }
    }
}
