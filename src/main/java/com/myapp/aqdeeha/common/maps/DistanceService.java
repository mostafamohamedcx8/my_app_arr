package com.myapp.aqdeeha.common.maps;

/**
 * Road (driving) distance, computed on the server only — the API key never reaches the apps.
 * Implementations never throw for "no route" / provider failures: they fall back to an
 * {@link RouteDistance.DistanceSource#ESTIMATED} distance.
 * <ul>
 *   <li>{@code app.maps.provider=google} → {@link GoogleDistanceMatrixClient}</li>
 *   <li>{@code app.maps.provider=fake} → test double in {@code src/test} (no network)</li>
 * </ul>
 */
public interface DistanceService {

    RouteDistance roadDistance(GeoPoint from, GeoPoint to);
}
