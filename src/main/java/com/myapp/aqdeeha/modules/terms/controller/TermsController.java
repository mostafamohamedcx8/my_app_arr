package com.myapp.aqdeeha.modules.terms.controller;

import com.myapp.aqdeeha.modules.terms.dto.TermsDtos.AcceptTermsRequest;
import com.myapp.aqdeeha.modules.terms.dto.TermsDtos.TermsResponse;
import com.myapp.aqdeeha.modules.terms.dto.TermsDtos.TermsStatusResponse;
import com.myapp.aqdeeha.modules.terms.service.TermsService;
import com.myapp.aqdeeha.security.AuthPrincipal;
import com.myapp.aqdeeha.security.CurrentUser;
import com.myapp.aqdeeha.security.access.AllowTo;
import com.myapp.aqdeeha.security.access.PublicEndpoint;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.myapp.aqdeeha.security.AuthPrincipal.Role.COURIER;
import static com.myapp.aqdeeha.security.AuthPrincipal.Role.COURIER_APPLICANT;
import static com.myapp.aqdeeha.security.AuthPrincipal.Role.CUSTOMER;

@RestController
@RequestMapping("/api/v1/terms")
@RequiredArgsConstructor
public class TermsController {

    private final TermsService termsService;

    /** Current terms text (shown before signup, so no token). ?audience=CUSTOMER|COURIER */
    @GetMapping
    @PublicEndpoint
    public TermsResponse current(@RequestParam String audience) {
        return termsService.current(audience);
    }

    /** Accept the current version after login/me returned termsUpToDate=false. */
    @PostMapping("/accept")
    @AllowTo({CUSTOMER, COURIER, COURIER_APPLICANT})
    public TermsStatusResponse accept(@Valid @RequestBody AcceptTermsRequest request, HttpServletRequest http) {
        AuthPrincipal me = CurrentUser.get();
        return termsService.accept(me.id(), me.role(), request.termsVersionId(), http.getRemoteAddr(),
                http.getHeader(HttpHeaders.USER_AGENT));
    }
}
