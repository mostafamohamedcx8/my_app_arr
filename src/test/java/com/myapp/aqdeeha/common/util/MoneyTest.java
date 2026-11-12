package com.myapp.aqdeeha.common.util;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MoneyTest {

    record Fee(@EgpAmount BigDecimal amount) {
    }

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void alwaysTwoDecimals() {
        assertThat(Money.egp("1.5")).isEqualByComparingTo("1.50").hasToString("1.50");
        assertThat(Money.egp("15")).hasToString("15.00");
        assertThat(Money.egp("2.005")).hasToString("2.01"); // half-up
        assertThat(Money.fromPiasters(150)).hasToString("1.50");
        assertThat(Money.toPiasters(new BigDecimal("1.50"))).isEqualTo(150);
        assertThat(Money.ZERO).hasToString("0.00");
    }

    @Test
    void jsonShowsEgpWithPiasters() {
        String json = JsonMapper.builder().build().writeValueAsString(new Fee(Money.egp("1.5")));
        assertThat(json).isEqualTo("{\"amount\":1.50}");
    }

    @Test
    void egpAmountValidation() {
        assertThat(validator.validate(new Fee(new BigDecimal("15.50")))).isEmpty();
        assertThat(validator.validate(new Fee(new BigDecimal("0")))).isEmpty();
        assertThat(validator.validate(new Fee(new BigDecimal("1.505")))).hasSize(1);   // 3 decimals
        assertThat(validator.validate(new Fee(new BigDecimal("-1.00")))).hasSize(1);   // negative
        assertThat(validator.validate(new Fee(new BigDecimal("100000000.00")))).hasSize(1); // > DECIMAL(10,2)
    }
}
