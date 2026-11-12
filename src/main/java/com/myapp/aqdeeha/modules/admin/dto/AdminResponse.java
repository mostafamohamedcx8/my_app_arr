package com.myapp.aqdeeha.modules.admin.dto;

import com.myapp.aqdeeha.common.util.LowercaseEnumConverter;
import com.myapp.aqdeeha.modules.admin.entity.Admin;

import java.time.Instant;

public record AdminResponse(Long id, String name, String email, String role, boolean active, Instant createdAt) {

    public static AdminResponse from(Admin a) {
        return new AdminResponse(a.getId(), a.getName(), a.getEmail(), LowercaseEnumConverter.value(a.getRole()),
                a.isActive(), a.getCreatedAt());
    }
}
