package com.myapp.aqdeeha.modules.order;

import com.myapp.aqdeeha.common.exception.BusinessException;
import com.myapp.aqdeeha.common.exception.ErrorCode;
import com.myapp.aqdeeha.common.maps.RouteDistance;
import com.myapp.aqdeeha.common.maps.RouteDistance.DistanceSource;
import com.myapp.aqdeeha.modules.order.service.OrderPricingService;
import com.myapp.aqdeeha.modules.order.service.OrderPricingService.FeeRange;
import com.myapp.aqdeeha.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.Instant;
import java.sql.Timestamp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderQuoteFlowTest extends IntegrationTestBase {

    private static final String DROPOFF = "\"dropoffLat\": 30.0100, \"dropoffLng\": 31.0100";

    @Autowired OrderPricingService pricing;

    private String customer;

    @BeforeEach
    void serviceArea() {
        setConfig("service_area_lat", "30.0");
        setConfig("service_area_lng", "31.0");
        setConfig("service_area_radius_km", "5");
        customer = customerToken("+201000000001");
    }

    private org.springframework.test.web.servlet.ResultActions quote(String body) throws Exception {
        return mvc.perform(post("/api/v1/orders/quote").header("Authorization", bearer(customer))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private long shop(long categoryId, String name) {
        Timestamp now = Timestamp.from(Instant.now());
        jdbc.update("INSERT INTO shops (category_id, name, photo_url, street, lat, lng, opens_at, closes_at, is_open,"
                + " created_at, updated_at) VALUES (?, ?, 'public/x.png', 'st', 30, 31, '00:00', '00:00', TRUE, ?, ?)",
                categoryId, name, now, now);
        return jdbc.queryForObject("SELECT id FROM shops WHERE name = ?", Long.class, name);
    }

    private long category() {
        Timestamp now = Timestamp.from(Instant.now());
        jdbc.update("INSERT INTO categories (name, icon_key, color, sort_order, is_active, created_at, updated_at)"
                + " VALUES ('Food', 'food', '#FF5722', 0, TRUE, ?, ?)", now, now);
        return jdbc.queryForObject("SELECT id FROM categories WHERE name = 'Food'", Long.class);
    }

    @Test
    void shopsQuote_isFixed_baseFeePlusExtraPerShop_andLimitedByMaxShops() throws Exception {
        long cat = category();
        long s1 = shop(cat, "A"), s2 = shop(cat, "B"), s3 = shop(cat, "C");

        quote("{\"type\":\"shops\",\"shopIds\":[%d,%d,%d,%d],%s}".formatted(s1, s2, s3, s1, DROPOFF))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priceType").value("fixed"))
                .andExpect(jsonPath("$.shopCount").value(3))          // duplicates ignored
                .andExpect(jsonPath("$.deliveryFee").value(25.00))    // 15 + 5 × 2
                .andExpect(jsonPath("$.minFee").value(25.00))
                .andExpect(jsonPath("$.maxFee").value(25.00));

        setConfig("max_shops_per_order", "2");
        quote("{\"type\":\"shops\",\"shopIds\":[%d,%d,%d],%s}".formatted(s1, s2, s3, DROPOFF))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TOO_MANY_SHOPS"));

        quote("{\"type\":\"shops\",\"shopIds\":[],%s}".formatted(DROPOFF))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SHOPS_REQUIRED"));

        quote("{\"type\":\"shops\",\"shopIds\":[99999],%s}".formatted(DROPOFF))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SHOP_NOT_FOUND"));
    }

    @Test
    void anythingWithoutPickup_is15to30_andNeverCallsMaps() throws Exception {
        quote("{\"type\":\"anything\",%s}".formatted(DROPOFF))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priceType").value("customer"))
                .andExpect(jsonPath("$.minFee").value(15.00))
                .andExpect(jsonPath("$.maxFee").value(30.00))
                .andExpect(jsonPath("$.distanceKm").doesNotExist());
        assertThat(distance.calls()).isZero();
    }

    @Test
    void anythingWithPickup_maxGrowsPerStartedKmAfterFreeKm() throws Exception {
        String body = "{\"type\":\"anything\",\"pickupLat\":30.2,\"pickupLng\":31.3,%s}".formatted(DROPOFF);

        distance.willReturn(new RouteDistance(new BigDecimal("3.5"), DistanceSource.GOOGLE));
        quote(body).andExpect(status().isOk())
                .andExpect(jsonPath("$.distanceKm").value(3.5))
                .andExpect(jsonPath("$.distanceSource").value("GOOGLE"))
                .andExpect(jsonPath("$.minFee").value(15.00))
                .andExpect(jsonPath("$.maxFee").value(25.00));

        distance.willReturn(new RouteDistance(new BigDecimal("3.6"), DistanceSource.GOOGLE));
        quote(body).andExpect(jsonPath("$.maxFee").value(32.00));           // 25 + 7 × ceil(0.1)

        distance.willReturn(new RouteDistance(new BigDecimal("8.0"), DistanceSource.ESTIMATED));
        quote(body).andExpect(jsonPath("$.maxFee").value(60.00))            // 25 + 7 × ceil(4.5)=5
                .andExpect(jsonPath("$.distanceSource").value("ESTIMATED"));

        quote("{\"type\":\"anything\",\"pickupLat\":30.2,%s}".formatted(DROPOFF))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PICKUP_INCOMPLETE"));
    }

    @Test
    void dropoffMustBeInsideServiceArea_andAreaMustBeConfigured() throws Exception {
        quote("{\"type\":\"anything\",\"dropoffLat\":30.2,\"dropoffLng\":31.2}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("OUTSIDE_SERVICE_AREA"));

        setConfig("service_area_radius_km", "");
        quote("{\"type\":\"anything\",%s}".formatted(DROPOFF))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SERVICE_AREA_NOT_CONFIGURED"));
    }

    @Test
    void quoteIsForCustomersOnly() throws Exception {
        mvc.perform(post("/api/v1/orders/quote").header("Authorization", bearer(adminToken("a@test.com")))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"anything\",%s}".formatted(DROPOFF)))
                .andExpect(status().isForbidden());
    }

    @Test
    void feeAndOffersMustBeInsideRange() {
        FeeRange range = new FeeRange(new BigDecimal("15.00"), new BigDecimal("32.00"), new BigDecimal("3.6"),
                DistanceSource.GOOGLE);
        pricing.requireInRange(range, new BigDecimal("15.00"), "deliveryFee");
        pricing.requireInRange(range, new BigDecimal("32.00"), "deliveryFee");
        assertThatThrownBy(() -> pricing.requireInRange(range, new BigDecimal("32.01"), "price"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.FEE_OUT_OF_RANGE);
        assertThatThrownBy(() -> pricing.requireInRange(range, new BigDecimal("14.99"), "price"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void superAdminChangesConfig_takesEffectImmediately_audited_adminCannot() throws Exception {
        String superAdmin = superAdminToken();
        String admin = adminToken("admin@test.com");

        mvc.perform(get("/api/admin/config").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.key=='commission_rate')].value").value("20"));

        mvc.perform(put("/api/admin/config/anything_no_pickup_max").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"value\":\"40\"}"))
                .andExpect(status().isForbidden());

        quote("{\"type\":\"anything\",%s}".formatted(DROPOFF)).andExpect(jsonPath("$.maxFee").value(30.00)); // cached now

        mvc.perform(put("/api/admin/config/anything_no_pickup_max").header("Authorization", bearer(superAdmin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"value\":\"40.50\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value("40.5"))
                .andExpect(jsonPath("$.valueType").value("DECIMAL"));

        quote("{\"type\":\"anything\",%s}".formatted(DROPOFF)).andExpect(jsonPath("$.maxFee").value(40.50));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_log WHERE action = 'config.updated'", Integer.class))
                .isEqualTo(1);

        mvc.perform(put("/api/admin/config/max_shops_per_order").header("Authorization", bearer(superAdmin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"value\":\"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CONFIG_VALUE"));

        mvc.perform(put("/api/admin/config/no_such_key").header("Authorization", bearer(superAdmin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"value\":\"1\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CONFIG_KEY_NOT_FOUND"));
    }
}
