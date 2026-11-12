package com.myapp.aqdeeha.modules.notification.service;

/**
 * WhatsApp is used ONLY for verification codes (signup, login-to-fix, password recovery).
 * All other notifications are stored in the {@code notifications} table (real-time delivery via WebSockets later).
 * Pick the implementation with {@code app.whatsapp.provider}; only {@code console} exists for now.
 */
public interface WhatsAppSender {

    /**
     * @param phone   E.164 number, e.g. +201001234567
     * @param code    the raw 6-digit OTP
     * @param purpose signup / reset_password (lets the provider pick the right template)
     */
    void sendOtp(String phone, String code, String purpose);
}
