package com.myapp.aqdeeha.common.exception;

import com.myapp.aqdeeha.common.api.ApiError;
import com.myapp.aqdeeha.common.i18n.Messages;
import com.myapp.aqdeeha.security.CurrentUser;
import org.springframework.security.access.AccessDeniedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private static final String DEFAULT_FIELD_MESSAGE = "validation.invalid";

    private final Messages messages;

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusiness(BusinessException ex) {
        String message = messages.get(ex.getErrorCode().messageKey(), ex.getArgs());
        List<ApiError.FieldErrorItem> fields = ex.getField() == null
                ? List.of()
                : List.of(new ApiError.FieldErrorItem(ex.getField(), message));
        return build(ex.getErrorCode(), message, fields);
    }

    /** Thrown by @Protect / @AllowTo: no valid token → TOKEN_INVALID, wrong role → FORBIDDEN. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        ErrorCode code = CurrentUser.find().isPresent() ? ErrorCode.FORBIDDEN : ErrorCode.TOKEN_INVALID;
        return build(code, null, List.of());
    }

    @ExceptionHandler(FieldErrorsException.class)
    public ResponseEntity<ApiError> handleFieldErrors(FieldErrorsException ex) {
        List<ApiError.FieldErrorItem> fields = ex.getFieldMessageKeys().entrySet().stream()
                .map(e -> new ApiError.FieldErrorItem(e.getKey(), messages.get(e.getValue())))
                .toList();
        return build(ErrorCode.VALIDATION_FAILED, null, fields);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ApiError> handleBinding(Exception ex) {
        BindingResult result = ex instanceof MethodArgumentNotValidException manv
                ? manv.getBindingResult()
                : ((BindException) ex).getBindingResult();
        List<ApiError.FieldErrorItem> fields = result.getFieldErrors().stream()
                .map(this::toItem)
                .toList();
        return build(ErrorCode.VALIDATION_FAILED, null, fields);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> handleMethodValidation(HandlerMethodValidationException ex) {
        return build(ErrorCode.VALIDATION_FAILED, null, List.of());
    }

    @ExceptionHandler({MissingServletRequestPartException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ApiError> handleMissingPart(Exception ex) {
        String field = ex instanceof MissingServletRequestPartException p ? p.getRequestPartName()
                : ((MissingServletRequestParameterException) ex).getParameterName();
        return build(ErrorCode.VALIDATION_FAILED, null,
                List.of(new ApiError.FieldErrorItem(field, messages.get("validation.required"))));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiError> handleMissingHeader(MissingRequestHeaderException ex) {
        return build(ErrorCode.VALIDATION_FAILED, null,
                List.of(new ApiError.FieldErrorItem(ex.getHeaderName(), messages.get("validation.required"))));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleTooLarge(MaxUploadSizeExceededException ex) {
        return build(ErrorCode.IMAGE_TOO_LARGE, messages.get("error.image_too_large_generic"), List.of());
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiError> handleMultipart(MultipartException ex) {
        return build(ErrorCode.MALFORMED_REQUEST, null, List.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex) {
        return build(ErrorCode.MALFORMED_REQUEST, null, List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethod(HttpRequestMethodNotSupportedException ex) {
        return build(ErrorCode.METHOD_NOT_ALLOWED, null, List.of());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> handleMediaType(HttpMediaTypeNotSupportedException ex) {
        return build(ErrorCode.UNSUPPORTED_MEDIA_TYPE, null, List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NoResourceFoundException ex) {
        return build(ErrorCode.NOT_FOUND, null, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return build(ErrorCode.INTERNAL_ERROR, null, List.of());
    }

    /** Wrong type in a query/path parameter, e.g. {@code ?categoryId=abc}. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return build(ErrorCode.VALIDATION_FAILED, null,
                List.of(new ApiError.FieldErrorItem(ex.getName(), messages.get(DEFAULT_FIELD_MESSAGE))));
    }

    private ApiError.FieldErrorItem toItem(FieldError fe) {
        String key = fe.getDefaultMessage();
        if (fe.isBindingFailure()) {
            key = bindingFailureKey(fe);
        } else if (key == null || !key.startsWith("validation.")) {
            key = DEFAULT_FIELD_MESSAGE;
        }
        return new ApiError.FieldErrorItem(fe.getField(), messages.get(key));
    }

    /** Type-conversion failures (e.g. "9am" for a time) get a format hint instead of a generic message. */
    private static String bindingFailureKey(FieldError fe) {
        String[] codes = fe.getCodes() == null ? new String[0] : fe.getCodes();
        for (String c : codes) {
            if (c.equals("typeMismatch.java.time.LocalTime")) {
                return "validation.time_format";
            }
            if (c.equals("typeMismatch.java.time.LocalDate")) {
                return "validation.date_format";
            }
        }
        return DEFAULT_FIELD_MESSAGE;
    }

    private ResponseEntity<ApiError> build(ErrorCode code, String message, List<ApiError.FieldErrorItem> fields) {
        String text = message != null ? message : messages.get(code.messageKey());
        logRejection(code, fields);
        return ResponseEntity.status(code.status()).body(ApiError.of(code.name(), text, fields));
    }

    /** One log line per rejected request, so "why did my request fail?" is visible in the console. */
    private void logRejection(ErrorCode code, List<ApiError.FieldErrorItem> fields) {
        if (code == ErrorCode.INTERNAL_ERROR) {
            return; // already logged with stack trace
        }
        String request = "?";
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            request = attrs.getRequest().getMethod() + " " + attrs.getRequest().getRequestURI();
        }
        String fieldNames = fields.isEmpty() ? "" : " fields=" + fields.stream()
                .map(ApiError.FieldErrorItem::field).toList();
        log.info("Rejected {} -> {} {}{}", request, code.status().value(), code.name(), fieldNames);
    }
}
