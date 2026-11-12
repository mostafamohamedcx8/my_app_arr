package com.myapp.aqdeeha.common.maps;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Google Distance Matrix (driving, metric). One origin → one destination per call, 3 s timeout.
 * <ul>
 *   <li>Coordinates are rounded to 4 decimals; Google results are cached by them for {@code app.maps.cache-ttl}.</li>
 *   <li>Any failure (no key, timeout, HTTP error, status != OK, no route) → Haversine × factor,
 *       a warning in the log and source {@code ESTIMATED}. Estimates are not cached, so Google is retried.</li>
 * </ul>
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.maps.provider", havingValue = "google")
public class GoogleDistanceMatrixClient implements DistanceService {

    private static final String PATH = "/maps/api/distancematrix/json";

    private final MapsProperties props;
    private final RestClient restClient;
    private final JsonMapper jsonMapper;
    private final Cache<String, RouteDistance> cache;

    @Autowired
    public GoogleDistanceMatrixClient(MapsProperties props, JsonMapper jsonMapper) {
        this(props, RestClient.builder().requestFactory(requestFactory(props)).build(), jsonMapper);
    }

    /** For tests: a RestClient bound to a mock server. */
    GoogleDistanceMatrixClient(MapsProperties props, RestClient restClient, JsonMapper jsonMapper) {
        this.props = props;
        this.restClient = restClient;
        this.jsonMapper = jsonMapper;
        this.cache = Caffeine.newBuilder().expireAfterWrite(props.cacheTtl()).maximumSize(props.cacheMaxSize()).build();
    }

    private static SimpleClientHttpRequestFactory requestFactory(MapsProperties props) {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(props.timeout());
        f.setReadTimeout(props.timeout());
        return f;
    }

    @Override
    public RouteDistance roadDistance(GeoPoint from, GeoPoint to) {
        GeoPoint a = from.rounded();
        GeoPoint b = to.rounded();
        String cacheKey = a.asParam() + "|" + b.asParam();
        RouteDistance cached = cache.getIfPresent(cacheKey);
        if (cached != null) {
            return cached;
        }
        String apiKey = props.google() == null ? null : props.google().apiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return estimate(a, b, "GOOGLE_MAPS_API_KEY is not set");
        }
        try {
            String body = restClient.get()
                    .uri(props.google().baseUrl() + PATH + "?origins={o}&destinations={d}&mode=driving&units=metric&key={k}",
                            a.asParam(), b.asParam(), apiKey)
                    .retrieve()
                    .body(String.class);
            Long meters = parseMeters(body);
            if (meters == null) {
                return estimate(a, b, "no route in response");
            }
            RouteDistance result = RouteDistance.google(meters);
            cache.put(cacheKey, result);
            return result;
        } catch (RuntimeException e) {
            // error messages may contain the request URL — never log the key
            String msg = String.valueOf(e.getMessage()).replace(apiKey, "***");
            return estimate(a, b, e.getClass().getSimpleName() + ": " + msg);
        }
    }

    /** Meters of the first element, or null when Google says there is no usable route. */
    private Long parseMeters(String body) {
        if (body == null) {
            return null;
        }
        JsonNode root = jsonMapper.readTree(body);
        if (!"OK".equals(root.path("status").asString())) {
            log.warn("Distance Matrix status {} ({})", root.path("status").asString(),
                    root.path("error_message").asString(""));
            return null;
        }
        JsonNode element = root.path("rows").path(0).path("elements").path(0);
        if (!"OK".equals(element.path("status").asString()) || !element.path("distance").path("value").isNumber()) {
            return null;
        }
        return element.path("distance").path("value").asLong();
    }

    private RouteDistance estimate(GeoPoint a, GeoPoint b, String reason) {
        RouteDistance est = RouteDistance.estimated(a.haversineKm(b) * props.fallbackRoadFactor());
        log.warn("Road distance {} -> {} ESTIMATED ({} km): {}", a.asParam(), b.asParam(), est.km(), reason);
        return est;
    }
}
