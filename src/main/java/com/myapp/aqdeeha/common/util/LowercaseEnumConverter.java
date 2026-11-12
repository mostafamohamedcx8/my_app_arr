package com.myapp.aqdeeha.common.util;

import jakarta.persistence.AttributeConverter;

import java.util.Locale;

/**
 * Stores Java enums as lower-case strings (e.g. {@code SUPER_ADMIN} -> {@code super_admin}),
 * matching the values documented in CLAUDE.md. Subclass once per enum.
 */
public abstract class LowercaseEnumConverter<E extends Enum<E>> implements AttributeConverter<E, String> {

    private final Class<E> type;

    protected LowercaseEnumConverter(Class<E> type) {
        this.type = type;
    }

    @Override
    public String convertToDatabaseColumn(E attribute) {
        return attribute == null ? null : value(attribute);
    }

    @Override
    public E convertToEntityAttribute(String dbData) {
        return dbData == null ? null : Enum.valueOf(type, dbData.toUpperCase(Locale.ROOT));
    }

    /** API / DB representation of an enum constant. */
    public static String value(Enum<?> e) {
        return e == null ? null : e.name().toLowerCase(Locale.ROOT);
    }

    /** Parses an already-validated lower-case API value. */
    public static <T extends Enum<T>> T parse(Class<T> type, String value) {
        return value == null || value.isBlank() ? null : Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
    }
}
