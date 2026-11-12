package com.myapp.aqdeeha.modules.catalog.entity;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class ShopOpenNowTest {

    @ParameterizedTest(name = "switch={0} {1}-{2} at {3} -> {4}")
    @CsvSource({
            // same-day hours
            "true, 09:00, 23:00, 08:59, false",
            "true, 09:00, 23:00, 09:00, true",
            "true, 09:00, 23:00, 22:59, true",
            "true, 09:00, 23:00, 23:00, false",
            // past midnight
            "true, 18:00, 02:00, 17:59, false",
            "true, 18:00, 02:00, 23:30, true",
            "true, 18:00, 02:00, 01:59, true",
            "true, 18:00, 02:00, 02:00, false",
            // 24h
            "true, 00:00, 00:00, 13:00, true",
            // manual switch off wins
            "false, 00:00, 00:00, 13:00, false",
            "false, 09:00, 23:00, 12:00, false",
    })
    void openNow(boolean open, LocalTime opens, LocalTime closes, LocalTime now, boolean expected) {
        assertThat(Shop.isOpenAt(open, opens, closes, now)).isEqualTo(expected);
    }
}
