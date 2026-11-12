package com.myapp.aqdeeha.modules.order.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.util.List;

public final class QuoteDtos {

    private QuoteDtos() {
    }

    /**
     * Price quote before creating an order.
     * <ul>
     *   <li>{@code type=shops}: {@code shopIds} (1..max_shops_per_order) + dropoff.</li>
     *   <li>{@code type=anything}: dropoff, and optionally a pickup point (both pickupLat and pickupLng).</li>
     * </ul>
     */
    public record QuoteRequest(
            @NotBlank(message = "validation.required")
            @Pattern(regexp = "^(shops|anything)$", message = "validation.order_type")
            String type,

            List<@NotNull(message = "validation.required") Long> shopIds,

            @NotNull(message = "validation.required")
            @DecimalMin(value = "-90", message = "validation.latitude")
            @DecimalMax(value = "90", message = "validation.latitude")
            BigDecimal dropoffLat,

            @NotNull(message = "validation.required")
            @DecimalMin(value = "-180", message = "validation.longitude")
            @DecimalMax(value = "180", message = "validation.longitude")
            BigDecimal dropoffLng,

            @DecimalMin(value = "-90", message = "validation.latitude")
            @DecimalMax(value = "90", message = "validation.latitude")
            BigDecimal pickupLat,

            @DecimalMin(value = "-180", message = "validation.longitude")
            @DecimalMax(value = "180", message = "validation.longitude")
            BigDecimal pickupLng) {
    }

    /**
     * @param priceType      fixed (shops: deliveryFee = minFee = maxFee) | customer (anything: pick a fee in range)
     * @param deliveryFee    shops only
     * @param distanceKm     road km pickup → dropoff (anything with pickup only), 1 decimal
     * @param distanceSource GOOGLE | ESTIMATED (null without distance)
     * @param shopCount      shops only
     */
    public record QuoteResponse(String type, String priceType, BigDecimal deliveryFee, BigDecimal minFee,
                                BigDecimal maxFee, BigDecimal distanceKm, String distanceSource, Integer shopCount) {
    }
}
