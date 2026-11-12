package com.myapp.aqdeeha.modules.courier.controller;

import com.myapp.aqdeeha.modules.courier.dto.CourierDtos.CourierMeResponse;
import com.myapp.aqdeeha.modules.courier.dto.CourierDtos.FixApplicationResponse;
import com.myapp.aqdeeha.modules.courier.dto.CourierDtos.ResubmitForm;
import com.myapp.aqdeeha.modules.courier.dto.CourierDtos.ResubmitResponse;
import com.myapp.aqdeeha.modules.courier.dto.CourierDtos.WalletRequest;
import com.myapp.aqdeeha.modules.courier.service.CourierApplicationService;
import com.myapp.aqdeeha.modules.courier.service.CourierService;
import com.myapp.aqdeeha.security.CurrentUser;
import com.myapp.aqdeeha.security.access.AllowTo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.myapp.aqdeeha.security.AuthPrincipal.Role.COURIER;
import static com.myapp.aqdeeha.security.AuthPrincipal.Role.COURIER_APPLICANT;

/** Courier app endpoints. */
@RestController
@AllowTo(COURIER)
@RequestMapping("/api/v1/courier")
@RequiredArgsConstructor
public class CourierController {

    private final CourierService courierService;
    private final CourierApplicationService applicationService;

    /**
     * Everything about the logged-in courier except the ID-card images. Also reachable with the limited
     * needs_fix token, so the app can show the admin's note and the current data.
     */
    @GetMapping("/me")
    @AllowTo({COURIER, COURIER_APPLICANT})
    public CourierMeResponse me() {
        return courierService.me(CurrentUser.id());
    }

    /** needs_fix only: the admin's note and ONLY the fields that must be corrected. */
    @GetMapping("/application")
    @AllowTo(COURIER_APPLICANT)
    public FixApplicationResponse application() {
        return applicationService.application(CurrentUser.id());
    }

    /** needs_fix only: send exactly the flagged fields; the application goes back to pending. */
    @PostMapping(value = "/application/resubmit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @AllowTo(COURIER_APPLICANT)
    public ResubmitResponse resubmit(@Valid @ModelAttribute ResubmitForm form) {
        return applicationService.resubmit(CurrentUser.id(), form);
    }

    /** Approved courier: set or change the wallet number (required before working). */
    @PutMapping("/wallet")
    public CourierMeResponse setWallet(@Valid @RequestBody WalletRequest request) {
        return applicationService.setWallet(CurrentUser.id(), request);
    }
}
