package com.myapp.aqdeeha.support;

import com.myapp.aqdeeha.modules.notification.service.WhatsAppSender;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/** Test double: remembers every OTP sent (WhatsApp carries nothing but OTPs). */
@Component
@ConditionalOnProperty(name = "app.whatsapp.provider", havingValue = "test")
public class RecordingWhatsAppSender implements WhatsAppSender {

    private final Map<String, List<String>> codesByPhone = new ConcurrentHashMap<>();

    @Override
    public void sendOtp(String phone, String code, String purpose) {
        codesByPhone.computeIfAbsent(phone, k -> new CopyOnWriteArrayList<>()).add(purpose + ":" + code);
    }

    public void clear() {
        codesByPhone.clear();
    }

    public String lastCode(String e164Phone) {
        List<String> sent = codesByPhone.get(e164Phone);
        if (sent == null || sent.isEmpty()) {
            throw new IllegalStateException("No OTP was sent to " + e164Phone);
        }
        String last = sent.get(sent.size() - 1);
        return last.substring(last.indexOf(':') + 1);
    }

    /** Every message sent to a phone, as "purpose:code". */
    public List<String> messages(String e164Phone) {
        return codesByPhone.getOrDefault(e164Phone, List.of());
    }
}
