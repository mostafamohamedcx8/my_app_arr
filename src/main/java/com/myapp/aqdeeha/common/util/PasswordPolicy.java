package com.myapp.aqdeeha.common.util;

/** Password rule: 8–64 chars, at least one letter, one digit and one special character. */
public final class PasswordPolicy {

    /** Use in {@code @Pattern(regexp = PasswordPolicy.REGEX, message = PasswordPolicy.MESSAGE)}. */
    public static final String REGEX = "^(?=.*\\p{L})(?=.*\\p{N})(?=.*[^\\p{L}\\p{N}\\s]).{8,64}$";
    public static final String MESSAGE = "validation.password_weak";

    private PasswordPolicy() {
    }
}
