package com.myapp.aqdeeha.common.i18n;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.Locale;

/** Resolves user-facing text from {@code i18n/messages.properties} (Egyptian Arabic). */
@Component
@RequiredArgsConstructor
public class Messages {

    public static final Locale ARABIC = Locale.forLanguageTag("ar-EG");

    private final MessageSource messageSource;

    public String get(String key, Object... args) {
        return messageSource.getMessage(key, args, key, ARABIC);
    }
}
