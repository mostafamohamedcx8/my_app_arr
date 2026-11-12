package com.myapp.aqdeeha.common.util;

/**
 * Vehicle plate number: letters (Arabic or Latin) and/or digits (Arabic-Indic or Latin), in any order,
 * optionally separated by spaces (e.g. {@code "س ع د ١٢٣"}, {@code "سعد123"}, {@code "1234 ب ج"}).
 * At most 8 letters + digits in total — spaces don't count. Stored with single spaces, trimmed.
 */
public final class PlateNumbers {

    public static final int MAX_CHARS = 8;

    /**
     * Use in {@code @Pattern(regexp = PlateNumbers.REGEX, message = PlateNumbers.MESSAGE)}.
     * 1–8 letters/digits, whitespace allowed only between them (leading/trailing is trimmed anyway).
     */
    public static final String REGEX = "^\\s*[\\p{L}\\p{Nd}](?:\\s*[\\p{L}\\p{Nd}]){0," + (MAX_CHARS - 1) + "}\\s*$";
    public static final String MESSAGE = "validation.plate_number";

    private PlateNumbers() {
    }

    /** Trims and collapses runs of whitespace to one space. Call only on a value that matched {@link #REGEX}. */
    public static String normalize(String raw) {
        return raw.trim().replaceAll("\\s+", " ");
    }
}
