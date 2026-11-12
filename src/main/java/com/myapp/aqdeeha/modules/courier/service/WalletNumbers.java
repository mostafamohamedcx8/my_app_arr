package com.myapp.aqdeeha.modules.courier.service;

import com.myapp.aqdeeha.common.exception.BusinessException;
import com.myapp.aqdeeha.common.exception.ErrorCode;
import com.myapp.aqdeeha.common.util.LowercaseEnumConverter;
import com.myapp.aqdeeha.common.util.PhoneNumbers;
import com.myapp.aqdeeha.modules.courier.entity.CourierProfile.WalletType;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Wallet validation, shared by signup, resubmit and wallet change.
 * Vodafone Cash: an Egyptian mobile, stored as the local 11-digit form customers type (01XXXXXXXXX).
 * InstaPay: an Egyptian mobile (same form) or an InstaPay address like {@code name@instapay} (lower-case).
 */
public final class WalletNumbers {

    public static final String TYPE_REGEX = "^(vodafone_cash|instapay)$";
    private static final Pattern INSTAPAY_ADDRESS = Pattern.compile("^[a-z0-9._-]{3,50}@instapay$");

    private WalletNumbers() {
    }

    public static WalletType type(String apiValue) {
        return LowercaseEnumConverter.parse(WalletType.class, apiValue);
    }

    public static String normalize(WalletType type, String raw) {
        String value = raw == null ? "" : raw.trim();
        Optional<String> mobile = PhoneNumbers.tryNormalize(value);
        if (mobile.isPresent()) {
            return "0" + mobile.get().substring(3); // +2010... -> 010...
        }
        String lower = value.toLowerCase(Locale.ROOT);
        if (type == WalletType.INSTAPAY && INSTAPAY_ADDRESS.matcher(lower).matches()) {
            return lower;
        }
        throw BusinessException.forField(ErrorCode.INVALID_WALLET_NUMBER, "walletNumber");
    }
}
