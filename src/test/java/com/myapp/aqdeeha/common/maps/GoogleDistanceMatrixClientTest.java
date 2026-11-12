package com.myapp.aqdeeha.common.maps;

import com.myapp.aqdeeha.common.maps.RouteDistance.DistanceSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.hamcrest.Matchers.startsWith;

/** Google client against a mock HTTP server — no real API calls. */
class GoogleDistanceMatrixClientTest {

    private static final GeoPoint A = GeoPoint.of("30.0444123", "31.2357456");
    private static final GeoPoint B = GeoPoint.of("30.0600", "31.2500");

    private MockRestServiceServer server;
    private GoogleDistanceMatrixClient client;

    @BeforeEach
    void setUp() {
        client = clientWithKey("test-key");
    }

    private GoogleDistanceMatrixClient clientWithKey(String key) {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        MapsProperties props = new MapsProperties("google", new MapsProperties.Google(key, "https://maps.test"),
                Duration.ofSeconds(3), Duration.ofHours(24), 100, 1.3);
        return new GoogleDistanceMatrixClient(props, builder.build(), JsonMapper.builder().build());
    }

    private static String ok(long meters) {
        return """
                {"status":"OK","rows":[{"elements":[{"status":"OK","distance":{"value":%d,"text":"x"}}]}]}
                """.formatted(meters);
    }

    @Test
    void parsesMetersRoundsTo01Km_andCachesByRoundedCoordinates() {
        server.expect(requestTo(startsWith("https://maps.test/maps/api/distancematrix/json")))
                .andExpect(queryParam("origins", "30.0444%2C31.2357"))
                .andExpect(queryParam("destinations", "30.0600%2C31.2500"))
                .andExpect(queryParam("mode", "driving"))
                .andExpect(queryParam("key", "test-key"))
                .andRespond(withSuccess(ok(3460), MediaType.APPLICATION_JSON));

        RouteDistance first = client.roadDistance(A, B);
        assertThat(first.km()).isEqualByComparingTo("3.5");
        assertThat(first.source()).isEqualTo(DistanceSource.GOOGLE);

        // same coordinates after rounding to 4 decimals → served from cache, no second request
        RouteDistance second = client.roadDistance(GeoPoint.of("30.04441", "31.23571"), B);
        assertThat(second).isEqualTo(first);
        server.verify();
    }

    @Test
    void noRoute_fallsBackToHaversineTimesFactor_andIsNotCached() {
        String zero = """
                {"status":"OK","rows":[{"elements":[{"status":"ZERO_RESULTS"}]}]}
                """;
        server.expect(requestTo(startsWith("https://maps.test"))).andRespond(withSuccess(zero, MediaType.APPLICATION_JSON));
        server.expect(requestTo(startsWith("https://maps.test"))).andRespond(withSuccess(ok(5000), MediaType.APPLICATION_JSON));

        RouteDistance est = client.roadDistance(A, B);
        assertThat(est.source()).isEqualTo(DistanceSource.ESTIMATED);
        BigDecimal expected = RouteDistance.roundKm(A.rounded().haversineKm(B.rounded()) * 1.3);
        assertThat(est.km()).isEqualByComparingTo(expected);

        // estimate was not cached → Google is asked again
        assertThat(client.roadDistance(A, B).source()).isEqualTo(DistanceSource.GOOGLE);
        server.verify();
    }

    @Test
    void httpErrorOrBadStatus_isEstimated() {
        server.expect(requestTo(startsWith("https://maps.test"))).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        assertThat(client.roadDistance(A, B).source()).isEqualTo(DistanceSource.ESTIMATED);

        server.reset();
        server.expect(requestTo(startsWith("https://maps.test")))
                .andRespond(withSuccess("{\"status\":\"REQUEST_DENIED\",\"error_message\":\"bad key\"}",
                        MediaType.APPLICATION_JSON));
        assertThat(client.roadDistance(A, B).source()).isEqualTo(DistanceSource.ESTIMATED);
        server.verify();
    }

    @Test
    void missingApiKey_neverCallsGoogle() {
        GoogleDistanceMatrixClient noKey = clientWithKey("");
        assertThat(noKey.roadDistance(A, B).source()).isEqualTo(DistanceSource.ESTIMATED);
        server.verify(); // no expectations → any request would have failed
    }
}
