package com.myapp.aqdeeha.common.exception;

import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Several field-level problems at once (e.g. every required field missing from a resubmission).
 * Rendered as 400 VALIDATION_FAILED with one {@code fieldErrors} entry per field.
 */
@Getter
public class FieldErrorsException extends RuntimeException {

    /** field → message key (e.g. "validation.required"). */
    private final Map<String, String> fieldMessageKeys;

    public FieldErrorsException(Map<String, String> fieldMessageKeys) {
        super("Field errors: " + fieldMessageKeys.keySet());
        this.fieldMessageKeys = new LinkedHashMap<>(fieldMessageKeys);
    }
}
