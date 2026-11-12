package com.myapp.aqdeeha.modules.courier.controller;

import com.myapp.aqdeeha.common.api.PageResponse;
import com.myapp.aqdeeha.modules.courier.dto.CourierDtos.ApprovalRequest;
import com.myapp.aqdeeha.modules.courier.dto.CourierDtos.CourierAdminDetailResponse;
import com.myapp.aqdeeha.modules.courier.dto.CourierDtos.CourierSummaryResponse;
import com.myapp.aqdeeha.modules.courier.service.CourierAdminService;
import com.myapp.aqdeeha.security.CurrentUser;
import com.myapp.aqdeeha.security.access.AllowTo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.myapp.aqdeeha.security.AuthPrincipal.Role.ADMIN;

/** Courier review for the dashboard — admin and super admin. */
@RestController
@AllowTo(ADMIN)
@RequestMapping("/api/admin/couriers")
@RequiredArgsConstructor
public class AdminCourierController {

    private final CourierAdminService courierAdminService;

    /** ?status=pending|approved|rejected|needs_fix &q=name or phone &page=0 &size=20 */
    @GetMapping
    public PageResponse<CourierSummaryResponse> list(@RequestParam(required = false) String status,
                                                     @RequestParam(required = false) String q,
                                                     @RequestParam(required = false) Integer page,
                                                     @RequestParam(required = false) Integer size) {
        return courierAdminService.list(status, q, page, size);
    }

    @GetMapping("/{id}")
    public CourierAdminDetailResponse detail(@PathVariable Long id) {
        return courierAdminService.detail(id, CurrentUser.id());
    }

    /** Decrypted ID-card image (side = front | back). Never cached, never public. */
    @GetMapping("/{id}/id-card/{side}")
    public ResponseEntity<byte[]> idCard(@PathVariable Long id, @PathVariable String side) {
        CourierAdminService.IdCardImage image = courierAdminService.idCard(id, side);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.mediaType()))
                .cacheControl(CacheControl.noStore().cachePrivate())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .header("X-Content-Type-Options", "nosniff")
                .body(image.bytes());
    }

    @PatchMapping("/{id}/approval")
    public CourierAdminDetailResponse decide(@PathVariable Long id, @Valid @RequestBody ApprovalRequest request) {
        return courierAdminService.decide(id, request, CurrentUser.id());
    }
}
