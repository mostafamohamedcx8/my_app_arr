package com.myapp.aqdeeha.common.exception;

import lombok.Getter;

/**
 * Thrown for any expected business-rule failure. Translated to an {@code ApiError}
 * by {@link GlobalExceptionHandler}. {@code args} may contain plain values or
 * {@link org.springframework.context.MessageSourceResolvable}s (resolved as nested messages).
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final transient Object[] args;
    /** Optional request field the error refers to (e.g. "photo"). */
    private final String field;

    public BusinessException(ErrorCode errorCode, Object... args) {
        this(errorCode, null, args);
    }

    private BusinessException(ErrorCode errorCode, String field, Object... args) {
        super(errorCode.name());
        this.errorCode = errorCode;
        this.field = field;
        this.args = args;
    }

    public static BusinessException forField(ErrorCode errorCode, String field, Object... args) {
        return new BusinessException(errorCode, field, args);
    }
}
