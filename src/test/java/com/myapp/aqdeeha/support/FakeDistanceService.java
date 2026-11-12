package com.myapp.aqdeeha.support;

import com.myapp.aqdeeha.common.maps.DistanceService;
import com.myapp.aqdeeha.common.maps.GeoPoint;
import com.myapp.aqdeeha.common.maps.RouteDistance;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/** Test double: never calls Google. Returns the distance set with {@link #willReturn}, else Haversine as GOOGLE. */
@Component
@ConditionalOnProperty(name = "app.maps.provider", havingValue = "fake")
public class FakeDistanceService implements DistanceService {

    private volatile RouteDistance next;
    private final AtomicInteger calls = new AtomicInteger();

    @Override
    public RouteDistance roadDistance(GeoPoint from, GeoPoint to) {
        calls.incrementAndGet();
        RouteDistance n = next;
        return n != null ? n : new RouteDistance(RouteDistance.roundKm(from.haversineKm(to)),
                RouteDistance.DistanceSource.GOOGLE);
    }

    public void willReturn(RouteDistance distance) {
        this.next = distance;
    }

    public int calls() {
        return calls.get();
    }

    public void reset() {
        next = null;
        calls.set(0);
    }
}
