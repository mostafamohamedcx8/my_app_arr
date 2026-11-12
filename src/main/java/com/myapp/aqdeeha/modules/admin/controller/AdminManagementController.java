package com.myapp.aqdeeha.modules.admin.controller;

import com.myapp.aqdeeha.modules.admin.dto.AdminResponse;
import com.myapp.aqdeeha.modules.admin.dto.CreateAdminRequest;
import com.myapp.aqdeeha.modules.admin.service.AdminService;
import com.myapp.aqdeeha.security.CurrentUser;
import com.myapp.aqdeeha.security.access.AllowTo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static com.myapp.aqdeeha.security.AuthPrincipal.Role.SUPER_ADMIN;

/** Admin account management — super admin only. */
@RestController
@AllowTo(SUPER_ADMIN)
@RequestMapping("/api/admin/admins")
@RequiredArgsConstructor
public class AdminManagementController {

    private final AdminService adminService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminResponse create(@Valid @RequestBody CreateAdminRequest request) {
        return AdminResponse.from(
                adminService.createAdmin(request.name(), request.email(), request.password(), CurrentUser.id()));
    }
}
