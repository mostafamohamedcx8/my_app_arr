package com.myapp.aqdeeha.common.maps;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Road distance between two points.
 *
 * @param km     kilometres, rounded to 0.1
 * @param source {@code GOOGLE} (Distance Matrix, driving) or {@code ESTIMATED} (Haversine × factor fallback)
 */
public record RouteDistance(BigDecimal km, DistanceSource source) {

    public enum DistanceSource { GOOGLE, ESTIMATED }

    public static BigDecimal roundKm(double km) {
        return BigDecimal.valueOf(km).setScale(1, RoundingMode.HALF_UP);
    }

    public static RouteDistance google(long meters) {
        return new RouteDistance(BigDecimal.valueOf(meters).movePointLeft(3).setScale(1, RoundingMode.HALF_UP),
                DistanceSource.GOOGLE);
    }

    public static RouteDistance estimated(double km) {
        return new RouteDistance(roundKm(km), DistanceSource.ESTIMATED);
    }
}
