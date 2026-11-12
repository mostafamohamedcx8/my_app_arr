package com.myapp.aqdeeha.common.util;

import com.myapp.aqdeeha.common.exception.BusinessException;
import com.myapp.aqdeeha.common.exception.ErrorCode;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Egyptian mobile numbers only, normalized to {@code +201XXXXXXXXX}. */
public final class PhoneNumbers {

    private static final Pattern EGYPT_MOBILE = Pattern.compile("^(?:\\+?20|0)?(1[0125]\\d{8})$");

    private PhoneNumbers() {
    }

    public static String normalize(String raw) {
        return tryNormalize(raw).orElseThrow(() -> BusinessException.forField(ErrorCode.INVALID_PHONE, "phone"));
    }

    public static Optional<String> tryNormalize(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String cleaned = toAsciiDigits(raw).replaceAll("[\\s\\-()]", "");
        if (cleaned.startsWith("00")) {
            cleaned = "+" + cleaned.substring(2);
        }
        Matcher m = EGYPT_MOBILE.matcher(cleaned);
        return m.matches() ? Optional.of("+20" + m.group(1)) : Optional.empty();
    }

    /** Converts Arabic-Indic and Persian digits to ASCII. */
    public static String toAsciiDigits(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            if (c >= '٠' && c <= '٩') {
                sb.append((char) ('0' + (c - '٠')));
            } else if (c >= '۰' && c <= '۹') {
                sb.append((char) ('0' + (c - '۰')));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
