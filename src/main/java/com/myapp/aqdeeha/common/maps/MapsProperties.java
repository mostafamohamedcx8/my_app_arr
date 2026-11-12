package com.myapp.aqdeeha.common.maps;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * @param provider           google | fake
 * @param google             Distance Matrix settings (api key from env {@code GOOGLE_MAPS_API_KEY})
 * @param timeout            connect + read timeout of one Google call
 * @param cacheTtl           how long a Google distance is reused for the same rounded coordinates
 * @param cacheMaxSize       max cached routes
 * @param fallbackRoadFactor Haversine km × this = estimated road km when Google fails
 */
@ConfigurationProperties(prefix = "app.maps")
public record MapsProperties(String provider, Google google, Duration timeout, Duration cacheTtl, long cacheMaxSize,
                             double fallbackRoadFactor) {

    public record Google(String apiKey, String baseUrl) {
    }
}
