package com.myapp.aqdeeha.common.maps;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** A WGS84 coordinate. */
public record GeoPoint(BigDecimal lat, BigDecimal lng) {

    private static final double EARTH_RADIUS_KM = 6371.0088;

    public GeoPoint {
        if (lat == null || lng == null) {
            throw new IllegalArgumentException("lat and lng are required");
        }
    }

    public static GeoPoint of(String lat, String lng) {
        return new GeoPoint(new BigDecimal(lat), new BigDecimal(lng));
    }

    /** Rounded to 4 decimals (~11 m) — used for cache keys and for the request sent to Google. */
    public GeoPoint rounded() {
        return new GeoPoint(lat.setScale(4, RoundingMode.HALF_UP), lng.setScale(4, RoundingMode.HALF_UP));
    }

    /** "lat,lng" as Google expects it. */
    public String asParam() {
        return lat.toPlainString() + "," + lng.toPlainString();
    }

    /** Straight-line (great-circle) distance in km. */
    public double haversineKm(GeoPoint other) {
        double lat1 = Math.toRadians(lat.doubleValue());
        double lat2 = Math.toRadians(other.lat.doubleValue());
        double dLat = lat2 - lat1;
        double dLng = Math.toRadians(other.lng.doubleValue() - lng.doubleValue());
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.min(1, Math.sqrt(a)));
    }
}
