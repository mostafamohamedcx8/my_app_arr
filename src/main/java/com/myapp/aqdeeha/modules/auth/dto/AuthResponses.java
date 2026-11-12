package com.myapp.aqdeeha.modules.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.myapp.aqdeeha.modules.admin.dto.AdminResponse;

import java.time.Instant;
import java.time.LocalDate;

/** JSON response bodies of the auth endpoints. ID-card images are intentionally absent everywhere. */
public final class AuthResponses {

    private AuthResponses() {
    }

    public record OtpSentResponse(String message, long expiresInSeconds, long resendAfterSeconds) {
    }

    /** Returned after a correct OTP: pass {@code verificationToken} to the next step. */
    public record OtpVerifiedResponse(String verificationToken, long expiresInSeconds) {
    }

    /**
     * {@code nextStep} tells the app where to go after login:
     * {@code null} = home; {@code "fix_application"} = courier must fix and resubmit (limited token);
     * {@code "add_wallet"} = approved courier must add a wallet number before working.
     */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record AuthResponse(String accessToken, String tokenType, Instant expiresAt, String nextStep,
                               UserProfileResponse user) {
    }

    public record AdminAuthResponse(String accessToken, String tokenType, Instant expiresAt, AdminResponse admin) {
    }

    public record CourierSignupResponse(Long userId, String status, String approvalStatus, String message) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record UserProfileResponse(Long id, String role, String name, String phone, String email,
                                      String gender, LocalDate birthDate, String photoUrl, String status,
                                      Instant createdAt, CourierInfo courier, Boolean termsUpToDate) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CourierInfo(String address, String vehicleType, String vehicleColor, String plateNumber,
                              String vehiclePhotoUrl, String approvalStatus, String reviewNote,
                              String walletNumber, String walletType) {
    }
}
