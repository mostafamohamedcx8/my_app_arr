package com.myapp.aqdeeha.modules.setting.service;

/** Keys of {@code application_config}. The values themselves are never hardcoded — read them via {@link ApplicationConfigService}. */
public final class ConfigKeys {

    // shops order (fixed price)
    public static final String BASE_FEE = "base_fee";
    public static final String EXTRA_SHOP_FEE = "extra_shop_fee";
    public static final String MAX_SHOPS_PER_ORDER = "max_shops_per_order";

    // anything order (customer price inside a range)
    public static final String ANYTHING_MIN_FEE = "anything_min_fee";
    public static final String ANYTHING_NO_PICKUP_MAX = "anything_no_pickup_max";
    public static final String ANYTHING_BASE_MAX = "anything_base_max";
    public static final String ANYTHING_FREE_KM = "anything_free_km";
    public static final String ANYTHING_PER_KM_FEE = "anything_per_km_fee";

    // money / points
    public static final String COMMISSION_RATE = "commission_rate";
    public static final String POINTS_PER_EGP = "points_per_egp";

    // order timing
    public static final String ORDER_OFFER_TTL_MINUTES = "order_offer_ttl_minutes";
    public static final String ORDER_SEARCH_TTL_MINUTES = "order_search_ttl_minutes";

    // service area (circle around a centre point)
    public static final String SERVICE_AREA_LAT = "service_area_lat";
    public static final String SERVICE_AREA_LNG = "service_area_lng";
    public static final String SERVICE_AREA_RADIUS_KM = "service_area_radius_km";

    private ConfigKeys() {
    }
}
