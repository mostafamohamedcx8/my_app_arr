package com.myapp.aqdeeha.modules.catalog.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;

public final class CatalogResponses {

    private CatalogResponses() {
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record CategoryResponse(Long id, String name, String iconKey, String iconUrl, String color, int sortOrder,
                                   boolean isActive, Instant createdAt) {
    }

    /**
     * {@code opensAt}/{@code closesAt} as "HH:mm" Cairo time. {@code isOpen} is the admin switch;
     * {@code openNow} = switch on AND current Cairo time inside the opening hours.
     */
    public record ShopResponse(Long id, Long categoryId, String categoryName, String name, String photoUrl,
                               String street, BigDecimal lat, BigDecimal lng, String opensAt, String closesAt,
                               boolean isOpen, boolean openNow) {
    }
}
