package com.myapp.aqdeeha.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Money convention (see CLAUDE.md): all amounts are <b>EGP</b> with exactly <b>2 decimals</b>
 * (piasters). {@code 1.50} = 1 EGP and 50 piasters.
 * <ul>
 *   <li>Java: {@link BigDecimal}, always created/normalized through {@link #egp}.</li>
 *   <li>DB: {@code DECIMAL(10,2)}.</li>
 *   <li>JSON: a number with 2 decimals, e.g. {@code "deliveryFee": 15.00}.</li>
 *   <li>Request fields: annotate with {@code @EgpAmount}.</li>
 * </ul>
 * Never use {@code double}/{@code float} for money.
 */
public final class Money {

    public static final int SCALE = 2;
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(SCALE, RoundingMode.UNNECESSARY);

    private Money() {
    }

    /** Normalizes to 2 decimals (half-up). {@code egp(new BigDecimal("1.5"))} → {@code 1.50}. */
    public static BigDecimal egp(BigDecimal amount) {
        return amount == null ? null : amount.setScale(SCALE, RoundingMode.HALF_UP);
    }

    /** {@code egp("15")} → {@code 15.00}. */
    public static BigDecimal egp(String amount) {
        return amount == null ? null : egp(new BigDecimal(amount));
    }

    /** From whole piasters: {@code fromPiasters(150)} → {@code 1.50}. */
    public static BigDecimal fromPiasters(long piasters) {
        return BigDecimal.valueOf(piasters, SCALE);
    }

    /** To whole piasters: {@code toPiasters(1.50)} → {@code 150}. */
    public static long toPiasters(BigDecimal amount) {
        return egp(amount).movePointRight(SCALE).longValueExact();
    }
}
