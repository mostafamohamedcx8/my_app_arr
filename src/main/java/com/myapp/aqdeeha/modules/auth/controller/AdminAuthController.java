package com.myapp.aqdeeha.modules.auth.controller;

import com.myapp.aqdeeha.modules.admin.dto.AdminResponse;
import com.myapp.aqdeeha.modules.admin.dto.CreateAdminRequest;
import com.myapp.aqdeeha.modules.auth.dto.AuthRequests.AdminLoginRequest;
import com.myapp.aqdeeha.modules.auth.dto.AuthResponses.AdminAuthResponse;
import com.myapp.aqdeeha.modules.auth.service.AdminAuthService;
import com.myapp.aqdeeha.security.CurrentUser;
import com.myapp.aqdeeha.security.access.AllowTo;
import com.myapp.aqdeeha.security.access.PublicEndpoint;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static com.myapp.aqdeeha.security.AuthPrincipal.Role.ADMIN;

/** Auth endpoints for the admin dashboard. */
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    public static final String BOOTSTRAP_HEADER = "X-Bootstrap-Key";

    private final AdminAuthService adminAuthService;

    @PublicEndpoint
    @PostMapping("/login")
    public AdminAuthResponse login(@Valid @RequestBody AdminLoginRequest req) {
        return adminAuthService.login(req.email(), req.password());
    }

    /** One-time: creates the single super admin. Fails forever once one exists. */
    @PublicEndpoint
    @PostMapping("/bootstrap-super-admin")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminAuthResponse bootstrap(@RequestHeader(BOOTSTRAP_HEADER) String bootstrapKey,
                                       @Valid @RequestBody CreateAdminRequest req) {
        return adminAuthService.bootstrapSuperAdmin(bootstrapKey, req);
    }

    @GetMapping("/me")
    @AllowTo(ADMIN)
    public AdminResponse me() {
        return adminAuthService.me(CurrentUser.id());
    }
}
