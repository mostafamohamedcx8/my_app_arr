package com.myapp.aqdeeha.modules.order.service;

import com.myapp.aqdeeha.common.exception.BusinessException;
import com.myapp.aqdeeha.common.exception.ErrorCode;
import com.myapp.aqdeeha.common.maps.DistanceService;
import com.myapp.aqdeeha.common.maps.GeoPoint;
import com.myapp.aqdeeha.common.maps.RouteDistance;
import com.myapp.aqdeeha.common.maps.RouteDistance.DistanceSource;
import com.myapp.aqdeeha.common.util.Money;
import com.myapp.aqdeeha.modules.setting.service.ApplicationConfigService;
import com.myapp.aqdeeha.modules.setting.service.ConfigKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * Delivery-fee rules. Every number comes from {@code application_config}.
 * <ul>
 *   <li><b>shops</b>: fixed = base_fee + extra_shop_fee × (shops − 1), 1..max_shops_per_order shops.</li>
 *   <li><b>anything</b> without pickup: anything_min_fee .. anything_no_pickup_max.</li>
 *   <li><b>anything</b> with pickup: road km (Google, rounded 0.1) pickup → dropoff;
 *       km ≤ anything_free_km → min .. anything_base_max,
 *       else min .. anything_base_max + anything_per_km_fee × ceil(km − anything_free_km).</li>
 * </ul>
 * The customer's fee and every courier offer must be inside the range ({@link #requireInRange}).
 * Store {@link FeeRange#distanceKm()}, {@code minFee}, {@code maxFee} and {@code distanceSource} on the order.
 */
@Service
@RequiredArgsConstructor
public class OrderPricingService {

    private final ApplicationConfigService config;
    private final DistanceService distanceService;

    /**
     * Allowed fee range of an order.
     *
     * @param distanceKm     road km pickup → dropoff; null when there is no pickup point (or a shops order)
     * @param distanceSource GOOGLE / ESTIMATED; null when distanceKm is null
     */
    public record FeeRange(BigDecimal minFee, BigDecimal maxFee, BigDecimal distanceKm,
                           DistanceSource distanceSource) {

        public boolean contains(BigDecimal fee) {
            return fee.compareTo(minFee) >= 0 && fee.compareTo(maxFee) <= 0;
        }
    }

    /** Fixed price of a shops order; validates the number of shops. */
    public BigDecimal shopsFee(int shopCount) {
        int max = config.getInt(ConfigKeys.MAX_SHOPS_PER_ORDER);
        if (shopCount < 1) {
            throw BusinessException.forField(ErrorCode.SHOPS_REQUIRED, "shopIds");
        }
        if (shopCount > max) {
            throw BusinessException.forField(ErrorCode.TOO_MANY_SHOPS, "shopIds", max);
        }
        BigDecimal base = config.getDecimal(ConfigKeys.BASE_FEE);
        BigDecimal extra = config.getDecimal(ConfigKeys.EXTRA_SHOP_FEE);
        return Money.egp(base.add(extra.multiply(BigDecimal.valueOf(shopCount - 1L))));
    }

    /** Fee range of an "anything" order. {@code pickup} may be null. Calls Google only when there is a pickup. */
    public FeeRange anythingRange(GeoPoint pickup, GeoPoint dropoff) {
        BigDecimal min = Money.egp(config.getDecimal(ConfigKeys.ANYTHING_MIN_FEE));
        if (pickup == null) {
            return new FeeRange(min, Money.egp(config.getDecimal(ConfigKeys.ANYTHING_NO_PICKUP_MAX)), null, null);
        }
        RouteDistance route = distanceService.roadDistance(pickup, dropoff);
        return new FeeRange(min, maxForDistance(route.km()), route.km(), route.source());
    }

    /** Max fee for a road distance (km, 1 decimal) — anything order with a pickup point. */
    public BigDecimal maxForDistance(BigDecimal km) {
        BigDecimal baseMax = config.getDecimal(ConfigKeys.ANYTHING_BASE_MAX);
        BigDecimal freeKm = config.getDecimal(ConfigKeys.ANYTHING_FREE_KM);
        if (km.compareTo(freeKm) <= 0) {
            return Money.egp(baseMax);
        }
        BigDecimal extraKm = km.subtract(freeKm).setScale(0, RoundingMode.CEILING);
        return Money.egp(baseMax.add(config.getDecimal(ConfigKeys.ANYTHING_PER_KM_FEE).multiply(extraKm)));
    }

    /** Rejects a customer fee or courier offer outside the range ({@code FEE_OUT_OF_RANGE}, shows min and max). */
    public void requireInRange(FeeRange range, BigDecimal fee, String field) {
        if (fee == null || !range.contains(fee)) {
            throw BusinessException.forField(ErrorCode.FEE_OUT_OF_RANGE, field,
                    range.minFee().toPlainString(), range.maxFee().toPlainString());
        }
    }

    /** The dropoff must be inside the service-area circle (straight-line distance from its centre). */
    public void requireInServiceArea(GeoPoint dropoff, String field) {
        Optional<BigDecimal> lat = config.findDecimal(ConfigKeys.SERVICE_AREA_LAT);
        Optional<BigDecimal> lng = config.findDecimal(ConfigKeys.SERVICE_AREA_LNG);
        Optional<BigDecimal> radius = config.findDecimal(ConfigKeys.SERVICE_AREA_RADIUS_KM);
        if (lat.isEmpty() || lng.isEmpty() || radius.isEmpty()) {
            throw new BusinessException(ErrorCode.SERVICE_AREA_NOT_CONFIGURED);
        }
        double km = new GeoPoint(lat.get(), lng.get()).haversineKm(dropoff);
        if (km > radius.get().doubleValue()) {
            throw BusinessException.forField(ErrorCode.OUTSIDE_SERVICE_AREA, field);
        }
    }
}
