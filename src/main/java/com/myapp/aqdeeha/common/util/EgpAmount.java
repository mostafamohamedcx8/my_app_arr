package com.myapp.aqdeeha.common.util;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates a request money field ({@link java.math.BigDecimal}): EGP, not negative,
 * at most 2 decimals (piasters) and at most 99,999,999.99 (fits {@code DECIMAL(10,2)}).
 */
@Documented
@Constraint(validatedBy = {})
@ReportAsSingleViolation
@DecimalMin("0.00")
@Digits(integer = 8, fraction = 2)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface EgpAmount {

    String message() default "validation.egp_amount";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
