package com.myapp.aqdeeha.modules.order.service;

import com.myapp.aqdeeha.common.exception.BusinessException;
import com.myapp.aqdeeha.common.exception.ErrorCode;
import com.myapp.aqdeeha.common.maps.GeoPoint;
import com.myapp.aqdeeha.modules.catalog.service.ShopService;
import com.myapp.aqdeeha.modules.order.dto.QuoteDtos.QuoteRequest;
import com.myapp.aqdeeha.modules.order.dto.QuoteDtos.QuoteResponse;
import com.myapp.aqdeeha.modules.order.service.OrderPricingService.FeeRange;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Price preview for the customer app. Order creation must recompute the same values on the server. */
@Service
@RequiredArgsConstructor
public class OrderQuoteService {

    private final OrderPricingService pricing;
    private final ShopService shopService;

    public QuoteResponse quote(QuoteRequest r) {
        GeoPoint dropoff = new GeoPoint(r.dropoffLat(), r.dropoffLng());
        pricing.requireInServiceArea(dropoff, "dropoffLat");

        if ("shops".equals(r.type())) {
            Set<Long> shopIds = new LinkedHashSet<>(r.shopIds() == null ? List.of() : r.shopIds());
            BigDecimal fee = pricing.shopsFee(shopIds.size());
            shopIds.forEach(shopService::get); // SHOP_NOT_FOUND for unknown / hidden shops
            return new QuoteResponse("shops", "fixed", fee, fee, fee, null, null, shopIds.size());
        }

        GeoPoint pickup = pickup(r);
        FeeRange range = pricing.anythingRange(pickup, dropoff);
        return new QuoteResponse("anything", "customer", null, range.minFee(), range.maxFee(), range.distanceKm(),
                range.distanceSource() == null ? null : range.distanceSource().name(), null);
    }

    private static GeoPoint pickup(QuoteRequest r) {
        if (r.pickupLat() == null && r.pickupLng() == null) {
            return null;
        }
        if (r.pickupLat() == null || r.pickupLng() == null) {
            throw BusinessException.forField(ErrorCode.PICKUP_INCOMPLETE, r.pickupLat() == null ? "pickupLat" : "pickupLng");
        }
        return new GeoPoint(r.pickupLat(), r.pickupLng());
    }
}
