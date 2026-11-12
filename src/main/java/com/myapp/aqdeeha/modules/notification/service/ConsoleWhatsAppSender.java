package com.myapp.aqdeeha.modules.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * DEV ONLY: "sends" the OTP by printing it to the application log.
 * Never enable in production — it logs the raw code.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.whatsapp.provider", havingValue = "console")
public class ConsoleWhatsAppSender implements WhatsAppSender {

    @Override
    public void sendOtp(String phone, String code, String purpose) {
        log.warn("""

                ==================== WHATSAPP (console) ====================
                  to      : {}
                  purpose : {}
                  OTP     : {}
                ============================================================""", phone, purpose, code);
    }
}
