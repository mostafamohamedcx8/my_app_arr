package com.myapp.aqdeeha.modules.auth.controller;

import com.myapp.aqdeeha.common.api.MessageResponse;
import com.myapp.aqdeeha.common.i18n.Messages;
import com.myapp.aqdeeha.modules.auth.dto.AuthRequests.ForgotPasswordRequest;
import com.myapp.aqdeeha.modules.auth.dto.AuthRequests.LoginRequest;
import com.myapp.aqdeeha.modules.auth.dto.AuthRequests.ResetPasswordRequest;
import com.myapp.aqdeeha.modules.auth.dto.AuthRequests.SendSignupOtpRequest;
import com.myapp.aqdeeha.modules.auth.dto.AuthRequests.VerifyOtpRequest;
import com.myapp.aqdeeha.modules.auth.dto.AuthResponses.AuthResponse;
import com.myapp.aqdeeha.modules.auth.dto.AuthResponses.CourierSignupResponse;
import com.myapp.aqdeeha.modules.auth.dto.AuthResponses.OtpSentResponse;
import com.myapp.aqdeeha.modules.auth.dto.AuthResponses.OtpVerifiedResponse;
import com.myapp.aqdeeha.modules.auth.dto.AuthResponses.UserProfileResponse;
import com.myapp.aqdeeha.modules.auth.dto.CourierSignupForm;
import com.myapp.aqdeeha.modules.auth.dto.CustomerSignupForm;
import com.myapp.aqdeeha.modules.auth.entity.OtpCode.OtpSubjectType;
import com.myapp.aqdeeha.modules.auth.service.CourierFixAccessService;
import com.myapp.aqdeeha.modules.auth.service.MobileAuthService;
import com.myapp.aqdeeha.modules.auth.service.OtpService;
import com.myapp.aqdeeha.modules.auth.service.PasswordResetService;
import com.myapp.aqdeeha.modules.auth.service.SignupService;
import com.myapp.aqdeeha.security.CurrentUser;
import com.myapp.aqdeeha.security.access.AllowTo;
import com.myapp.aqdeeha.security.access.PublicEndpoint;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static com.myapp.aqdeeha.security.AuthPrincipal.Role.COURIER;
import static com.myapp.aqdeeha.security.AuthPrincipal.Role.CUSTOMER;

/** Auth endpoints for the customer and courier mobile apps. */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class MobileAuthController {

    private final SignupService signupService;
    private final MobileAuthService mobileAuthService;
    private final PasswordResetService passwordResetService;
    private final CourierFixAccessService courierFixAccessService;
    private final Messages messages;

    // ---------- customer signup ----------

    @PublicEndpoint
    @PostMapping("/customer/signup/send-otp")
    public OtpSentResponse customerSendOtp(@Valid @RequestBody SendSignupOtpRequest req, HttpServletRequest http) {
        return sent(signupService.sendOtp(req.phone(), req.acceptTerms(), req.termsVersion(), OtpSubjectType.CUSTOMER,
                http.getRemoteAddr(), http.getHeader("User-Agent")));
    }

    @PublicEndpoint
    @PostMapping("/customer/signup/verify-otp")
    public OtpVerifiedResponse customerVerifyOtp(@Valid @RequestBody VerifyOtpRequest req) {
        return signupService.verifyOtp(req.phone(), req.code(), OtpSubjectType.CUSTOMER);
    }

    @PublicEndpoint
    @PostMapping(value = "/customer/signup/complete", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse customerComplete(@Valid @ModelAttribute CustomerSignupForm form) {
        return signupService.completeCustomer(form);
    }

    // ---------- courier signup ----------

    @PublicEndpoint
    @PostMapping("/courier/signup/send-otp")
    public OtpSentResponse courierSendOtp(@Valid @RequestBody SendSignupOtpRequest req, HttpServletRequest http) {
        return sent(signupService.sendOtp(req.phone(), req.acceptTerms(), req.termsVersion(), OtpSubjectType.COURIER,
                http.getRemoteAddr(), http.getHeader("User-Agent")));
    }

    @PublicEndpoint
    @PostMapping("/courier/signup/verify-otp")
    public OtpVerifiedResponse courierVerifyOtp(@Valid @RequestBody VerifyOtpRequest req) {
        return signupService.verifyOtp(req.phone(), req.code(), OtpSubjectType.COURIER);
    }

    @PublicEndpoint
    @PostMapping(value = "/courier/signup/complete", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public CourierSignupResponse courierComplete(@Valid @ModelAttribute CourierSignupForm form) {
        return signupService.completeCourier(form);
    }

    // ---------- courier: open a needs_fix application by phone + WhatsApp code ----------

    @PublicEndpoint
    @PostMapping("/courier/fix/send-otp")
    public OtpSentResponse courierFixSendOtp(@Valid @RequestBody ForgotPasswordRequest req, HttpServletRequest http) {
        return sent(courierFixAccessService.sendOtp(req.phone(), http.getRemoteAddr()));
    }

    /** Returns a limited token (nextStep = fix_application). */
    @PublicEndpoint
    @PostMapping("/courier/fix/verify-otp")
    public AuthResponse courierFixVerifyOtp(@Valid @RequestBody VerifyOtpRequest req) {
        return courierFixAccessService.verify(req.phone(), req.code());
    }

    // ---------- login ----------

    @PublicEndpoint
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return mobileAuthService.login(req.phone(), req.password());
    }

    @GetMapping("/me")
    @AllowTo({CUSTOMER, COURIER})
    public UserProfileResponse me() {
        return mobileAuthService.me(CurrentUser.id());
    }

    // ---------- forgot password ----------

    @PublicEndpoint
    @PostMapping("/password/forgot")
    public OtpSentResponse forgot(@Valid @RequestBody ForgotPasswordRequest req, HttpServletRequest http) {
        OtpService.OtpSent info = passwordResetService.forgot(req.phone(), http.getRemoteAddr());
        return new OtpSentResponse(messages.get("message.reset_otp_sent"), info.expiresInSeconds(),
                info.resendAfterSeconds());
    }

    @PublicEndpoint
    @PostMapping("/password/verify-otp")
    public OtpVerifiedResponse verifyResetOtp(@Valid @RequestBody VerifyOtpRequest req) {
        return passwordResetService.verify(req.phone(), req.code());
    }

    @PublicEndpoint
    @PostMapping("/password/reset")
    public MessageResponse reset(@Valid @RequestBody ResetPasswordRequest req) {
        passwordResetService.reset(req.resetToken(), req.newPassword());
        return new MessageResponse(messages.get("message.password_reset_done"));
    }

    private OtpSentResponse sent(OtpService.OtpSent info) {
        return new OtpSentResponse(messages.get("message.otp_sent"), info.expiresInSeconds(),
                info.resendAfterSeconds());
    }
}
