package com.myapp.aqdeeha.modules.setting.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class ConfigDtos {

    private ConfigDtos() {
    }

    /** {@code valueType}: INT | DECIMAL | STRING | BOOLEAN. An empty {@code value} = not set yet. */
    public record ConfigResponse(String key, String value, String valueType, String description, Long updatedBy,
                                 Instant updatedAt) {
    }

    /** The value as text, parsed by the key's type: "15", "3.5", "true". A value can't be cleared. */
    public record UpdateConfigRequest(
            @NotBlank(message = "validation.required")
            @Size(max = 255, message = "validation.config_value_size")
            String value) {
    }
}
