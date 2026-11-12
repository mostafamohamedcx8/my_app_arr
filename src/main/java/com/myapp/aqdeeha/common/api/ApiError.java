package com.myapp.aqdeeha.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/** Error body returned by every endpoint: stable {@code code} + Arabic {@code message}. */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiError(String code, String message, List<FieldErrorItem> fieldErrors, Instant timestamp) {

    public record FieldErrorItem(String field, String message) {
    }

    public static ApiError of(String code, String message) {
        return new ApiError(code, message, List.of(), Instant.now());
    }

    public static ApiError of(String code, String message, List<FieldErrorItem> fieldErrors) {
        return new ApiError(code, message, fieldErrors, Instant.now());
    }
}
