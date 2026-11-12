package com.myapp.aqdeeha.modules.auth.dto;

import com.myapp.aqdeeha.common.util.PasswordPolicy;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** JSON request bodies of the auth endpoints. */
public final class AuthRequests {

    private AuthRequests() {
    }

    /**
     * Signup step 1. {@code acceptTerms} must be true and {@code termsVersion} must be the current version
     * from {@code GET /api/v1/terms} — otherwise {@code TERMS_NOT_ACCEPTED} (checked in the service).
     */
    public record SendSignupOtpRequest(
            @NotBlank(message = "validation.required") String phone,
            Boolean acceptTerms,
            Integer termsVersion) {
    }

    /** Signup step 2 / forgot-password step 2. */
    public record VerifyOtpRequest(
            @NotBlank(message = "validation.required") String phone,
            @NotBlank(message = "validation.required")
            @Pattern(regexp = "^\\d{6}$", message = "validation.otp_format") String code) {
    }

    public record LoginRequest(
            @NotBlank(message = "validation.required") String phone,
            @NotBlank(message = "validation.required") String password) {
    }

    /** Forgot-password step 1. */
    public record ForgotPasswordRequest(
            @NotBlank(message = "validation.required") String phone) {
    }

    /** Forgot-password step 3. */
    public record ResetPasswordRequest(
            @NotBlank(message = "validation.required") String resetToken,
            @NotBlank(message = "validation.required")
            @Pattern(regexp = PasswordPolicy.REGEX, message = PasswordPolicy.MESSAGE) String newPassword) {
    }

    public record AdminLoginRequest(
            @NotBlank(message = "validation.required")
            @Email(message = "validation.email")
            @Size(max = 150, message = "validation.email") String email,
            @NotBlank(message = "validation.required") String password) {
    }
}
