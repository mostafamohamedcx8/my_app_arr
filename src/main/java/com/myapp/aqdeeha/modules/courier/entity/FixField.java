package com.myapp.aqdeeha.modules.courier.entity;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A part of the courier application the admin can ask to be corrected (needs_fix).
 * {@code key} is the API name (also stored in {@code courier_profiles.fix_fields});
 * {@code formKeys} are the multipart fields the courier must send for it on resubmit.
 */
public enum FixField {
    FULL_NAME("fullName", "text"),
    BIRTH_DATE("birthDate", "date"),
    PHOTO("photo", "image"),
    ADDRESS("address", "text"),
    VEHICLE_TYPE("vehicleType", "select"),
    VEHICLE_COLOR("vehicleColor", "text"),
    PLATE_NUMBER("plateNumber", "text"),
    VEHICLE_PHOTO("vehiclePhoto", "image"),
    ID_CARD_FRONT("idCardFront", "image"),
    ID_CARD_BACK("idCardBack", "image"),
    WALLET("wallet", "wallet");

    private final String key;
    private final String inputType;

    FixField(String key, String inputType) {
        this.key = key;
        this.inputType = inputType;
    }

    public String key() {
        return key;
    }

    /** text | date | select | image | wallet — tells the app which input to render. */
    public String inputType() {
        return inputType;
    }

    public List<String> formKeys() {
        return this == WALLET ? List.of("walletType", "walletNumber") : List.of(key);
    }

    public static Optional<FixField> fromKey(String key) {
        return Arrays.stream(values()).filter(f -> f.key.equals(key)).findFirst();
    }

    /** "idCardFront,wallet" → [ID_CARD_FRONT, WALLET] (declaration order). */
    public static Set<FixField> parse(String stored) {
        if (stored == null || stored.isBlank()) {
            return Set.of();
        }
        Set<String> keys = Arrays.stream(stored.split(",")).map(String::trim).collect(Collectors.toSet());
        return Arrays.stream(values()).filter(f -> keys.contains(f.key))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public static String format(Set<FixField> fields) {
        return fields == null || fields.isEmpty() ? null
                : Arrays.stream(values()).filter(fields::contains).map(FixField::key).collect(Collectors.joining(","));
    }
}
