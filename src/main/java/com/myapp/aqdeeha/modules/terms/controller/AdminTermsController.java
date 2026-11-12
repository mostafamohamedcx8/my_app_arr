package com.myapp.aqdeeha.modules.terms.controller;

import com.myapp.aqdeeha.modules.terms.dto.TermsDtos.AdminTermsResponse;
import com.myapp.aqdeeha.modules.terms.dto.TermsDtos.PublishTermsRequest;
import com.myapp.aqdeeha.modules.terms.service.TermsService;
import com.myapp.aqdeeha.security.CurrentUser;
import com.myapp.aqdeeha.security.access.AllowTo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.myapp.aqdeeha.security.AuthPrincipal.Role.ADMIN;
import static com.myapp.aqdeeha.security.AuthPrincipal.Role.SUPER_ADMIN;

@RestController
@RequestMapping("/api/admin/terms")
@RequiredArgsConstructor
public class AdminTermsController {

    private final TermsService termsService;

    /** All versions of an audience, newest first, with how many users accepted each. */
    @GetMapping
    @AllowTo(ADMIN)
    public List<AdminTermsResponse> history(@RequestParam String audience) {
        return termsService.history(audience);
    }

    /** New version for an audience, made current immediately. Everyone must accept it again. Audited. */
    @PostMapping
    @AllowTo(SUPER_ADMIN)
    @ResponseStatus(HttpStatus.CREATED)
    public AdminTermsResponse publish(@Valid @RequestBody PublishTermsRequest request) {
        return termsService.publish(request.audience(), request.content(), CurrentUser.id());
    }
}
