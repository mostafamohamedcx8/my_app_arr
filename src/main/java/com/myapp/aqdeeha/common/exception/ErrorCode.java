package com.myapp.aqdeeha.common.exception;

import org.springframework.http.HttpStatus;

import java.util.Locale;

/**
 * Stable machine-readable error codes. The user-facing (Egyptian Arabic) text lives in
 * {@code i18n/messages.properties} under {@code error.<code in lower case>}.
 */
public enum ErrorCode {
    // generic
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED),
    FORBIDDEN(HttpStatus.FORBIDDEN),
    NOT_FOUND(HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR),

    // phone / signup
    INVALID_PHONE(HttpStatus.BAD_REQUEST),
    PHONE_ALREADY_REGISTERED(HttpStatus.CONFLICT),
    PHONE_NOT_REGISTERED(HttpStatus.NOT_FOUND),
    EMAIL_ALREADY_REGISTERED(HttpStatus.CONFLICT),

    // otp
    OTP_RESEND_TOO_SOON(HttpStatus.TOO_MANY_REQUESTS),
    OTP_HOURLY_LIMIT(HttpStatus.TOO_MANY_REQUESTS),
    OTP_INVALID(HttpStatus.BAD_REQUEST),
    OTP_EXPIRED(HttpStatus.BAD_REQUEST),
    OTP_TOO_MANY_ATTEMPTS(HttpStatus.TOO_MANY_REQUESTS),
    VERIFICATION_TOKEN_INVALID(HttpStatus.BAD_REQUEST),

    // login / account state
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED),
    ADMIN_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED),
    COURIER_PENDING_APPROVAL(HttpStatus.FORBIDDEN),
    COURIER_REJECTED(HttpStatus.FORBIDDEN),
    ACCOUNT_SUSPENDED(HttpStatus.FORBIDDEN),
    ACCOUNT_BANNED(HttpStatus.FORBIDDEN),
    ACCOUNT_INACTIVE(HttpStatus.FORBIDDEN),

    // admin
    SUPER_ADMIN_ALREADY_EXISTS(HttpStatus.CONFLICT),
    BOOTSTRAP_KEY_INVALID(HttpStatus.FORBIDDEN),

    // files
    IMAGE_REQUIRED(HttpStatus.BAD_REQUEST),
    IMAGE_INVALID_TYPE(HttpStatus.BAD_REQUEST),
    IMAGE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE),

    // couriers (admin review)
    COURIER_NOT_FOUND(HttpStatus.NOT_FOUND),
    REVIEW_NOTE_REQUIRED(HttpStatus.BAD_REQUEST),
    APPLICATION_NOT_EDITABLE(HttpStatus.CONFLICT),
    FIX_FIELDS_REQUIRED(HttpStatus.BAD_REQUEST),
    INVALID_FIX_FIELD(HttpStatus.BAD_REQUEST),
    NOTHING_TO_FIX(HttpStatus.CONFLICT),
    INVALID_WALLET_NUMBER(HttpStatus.BAD_REQUEST),
    SIGNED_URL_INVALID(HttpStatus.FORBIDDEN),

    // catalog
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND),
    CATEGORY_NAME_EXISTS(HttpStatus.CONFLICT),
    CATEGORY_ICON_REQUIRED(HttpStatus.BAD_REQUEST),
    CATEGORY_HAS_SHOPS(HttpStatus.CONFLICT),
    SHOP_NOT_FOUND(HttpStatus.NOT_FOUND),
    INVALID_FILTER(HttpStatus.BAD_REQUEST),

    // application config
    CONFIG_KEY_NOT_FOUND(HttpStatus.NOT_FOUND),
    INVALID_CONFIG_VALUE(HttpStatus.BAD_REQUEST),

    // orders / pricing
    SHOPS_REQUIRED(HttpStatus.BAD_REQUEST),
    TOO_MANY_SHOPS(HttpStatus.BAD_REQUEST),
    PICKUP_INCOMPLETE(HttpStatus.BAD_REQUEST),
    FEE_OUT_OF_RANGE(HttpStatus.BAD_REQUEST),
    OUTSIDE_SERVICE_AREA(HttpStatus.BAD_REQUEST),
    SERVICE_AREA_NOT_CONFIGURED(HttpStatus.SERVICE_UNAVAILABLE),

    // terms & conditions
    TERMS_NOT_ACCEPTED(HttpStatus.BAD_REQUEST),
    TERMS_NOT_FOUND(HttpStatus.NOT_FOUND),
    TERMS_VERSION_INVALID(HttpStatus.CONFLICT),
    INVALID_TERMS_AUDIENCE(HttpStatus.BAD_REQUEST);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }

    public String messageKey() {
        return "error." + name().toLowerCase(Locale.ROOT);
    }
}
