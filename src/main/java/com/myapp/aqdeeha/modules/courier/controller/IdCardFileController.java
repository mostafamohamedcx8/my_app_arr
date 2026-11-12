package com.myapp.aqdeeha.modules.courier.controller;

import com.myapp.aqdeeha.common.exception.BusinessException;
import com.myapp.aqdeeha.common.exception.ErrorCode;
import com.myapp.aqdeeha.modules.admin.service.AdminService;
import com.myapp.aqdeeha.modules.courier.service.CourierAdminService;
import com.myapp.aqdeeha.modules.courier.service.IdCardUrlSigner;
import com.myapp.aqdeeha.security.access.PublicEndpoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves ID-card images through signed, expiring links (for {@code <img src>} on the dashboard).
 * No bearer token: the signature IS the authorization. It is only valid if minted for an admin who is still active.
 */
@Slf4j
@RestController
@RequestMapping("/api/files/id-cards")
@RequiredArgsConstructor
public class IdCardFileController {

    private final IdCardUrlSigner signer;
    private final AdminService adminService;
    private final CourierAdminService courierAdminService;

    @PublicEndpoint
    @GetMapping("/{courierId}/{side}")
    public ResponseEntity<byte[]> signed(@PathVariable Long courierId, @PathVariable String side,
                                         @RequestParam("a") Long adminId, @RequestParam long exp,
                                         @RequestParam String sig) {
        signer.verify(courierId, side, adminId, exp, sig);
        if (adminService.findById(adminId).filter(a -> a.isActive()).isEmpty()) {
            throw new BusinessException(ErrorCode.SIGNED_URL_INVALID);
        }
        CourierAdminService.IdCardImage image = courierAdminService.idCard(courierId, side);
        log.info("ID card {} of courier {} viewed via signed link by admin {}", side, courierId, adminId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.mediaType()))
                .cacheControl(CacheControl.noStore().cachePrivate())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .header("X-Content-Type-Options", "nosniff")
                .header("Referrer-Policy", "no-referrer")
                .body(image.bytes());
    }
}
