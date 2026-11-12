package com.myapp.aqdeeha.modules.notification.controller;

import com.myapp.aqdeeha.modules.notification.dto.NotificationResponse;
import com.myapp.aqdeeha.modules.notification.service.NotificationService;
import com.myapp.aqdeeha.security.CurrentUser;
import com.myapp.aqdeeha.security.access.AllowTo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.myapp.aqdeeha.security.AuthPrincipal.Role.COURIER;
import static com.myapp.aqdeeha.security.AuthPrincipal.Role.COURIER_APPLICANT;
import static com.myapp.aqdeeha.security.AuthPrincipal.Role.CUSTOMER;

/** The logged-in customer's / courier's notifications (newest first). */
@RestController
@AllowTo({CUSTOMER, COURIER, COURIER_APPLICANT})
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public NotificationResponse.Inbox inbox(@RequestParam(required = false) Integer page,
                                            @RequestParam(required = false) Integer size) {
        return notificationService.inbox(CurrentUser.id(), page, size);
    }

    @PostMapping("/{id}/read")
    public NotificationResponse markRead(@PathVariable Long id) {
        return notificationService.markRead(CurrentUser.id(), id);
    }
}
