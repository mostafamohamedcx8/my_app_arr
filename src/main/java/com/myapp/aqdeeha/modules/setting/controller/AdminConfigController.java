package com.myapp.aqdeeha.modules.setting.controller;

import com.myapp.aqdeeha.modules.setting.dto.ConfigDtos.ConfigResponse;
import com.myapp.aqdeeha.modules.setting.dto.ConfigDtos.UpdateConfigRequest;
import com.myapp.aqdeeha.modules.setting.service.ApplicationConfigService;
import com.myapp.aqdeeha.security.CurrentUser;
import com.myapp.aqdeeha.security.access.AllowTo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.myapp.aqdeeha.security.AuthPrincipal.Role.ADMIN;
import static com.myapp.aqdeeha.security.AuthPrincipal.Role.SUPER_ADMIN;

/** Business values (application_config): admins read, only the super admin changes them (audited). */
@RestController
@RequestMapping("/api/admin/config")
@RequiredArgsConstructor
public class AdminConfigController {

    private final ApplicationConfigService configService;

    @GetMapping
    @AllowTo(ADMIN)
    public List<ConfigResponse> list() {
        return configService.list();
    }

    @PutMapping("/{key}")
    @AllowTo(SUPER_ADMIN)
    public ConfigResponse update(@PathVariable String key, @Valid @RequestBody UpdateConfigRequest request) {
        return configService.update(key, request.value(), CurrentUser.id());
    }
}
