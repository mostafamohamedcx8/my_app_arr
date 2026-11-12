package com.myapp.aqdeeha.modules.catalog.service;

import com.myapp.aqdeeha.common.api.PageResponse;
import com.myapp.aqdeeha.common.api.Paging;
import com.myapp.aqdeeha.common.exception.BusinessException;
import com.myapp.aqdeeha.common.exception.ErrorCode;
import com.myapp.aqdeeha.common.storage.FileStorageService;
import com.myapp.aqdeeha.common.storage.ImageValidator;
import com.myapp.aqdeeha.common.storage.ImageValidator.ImageType;
import com.myapp.aqdeeha.common.util.AfterCommit;
import com.myapp.aqdeeha.modules.audit.service.AuditChanges;
import com.myapp.aqdeeha.modules.audit.service.AuditService;
import com.myapp.aqdeeha.modules.catalog.dto.CatalogResponses.ShopResponse;
import com.myapp.aqdeeha.modules.catalog.dto.ShopForm;
import com.myapp.aqdeeha.modules.catalog.dto.ShopUpdateForm;
import com.myapp.aqdeeha.modules.catalog.entity.Category;
import com.myapp.aqdeeha.modules.catalog.entity.Shop;
import com.myapp.aqdeeha.modules.catalog.mapper.CatalogMapper;
import com.myapp.aqdeeha.modules.catalog.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Shops. Create / update / delete are admin-only (enforced on the controller) and audited. */
@Service
@RequiredArgsConstructor
public class ShopService {

    public static final String ACTION_SHOP_CREATED = "shop.created";
    public static final String ACTION_SHOP_UPDATED = "shop.updated";
    public static final String ACTION_SHOP_DELETED = "shop.deleted";

    private final ShopRepository repository;
    private final CategoryService categoryService;
    private final ImageValidator imageValidator;
    private final FileStorageService storage;
    private final AuditService auditService;
    private final CatalogMapper mapper;
    private final Clock clock;

    /** Admin creates a shop (category may be inactive). Writes audit_log in the same transaction. */
    @Transactional
    public ShopResponse create(ShopForm form, Long adminId) {
        ImageType photoType = imageValidator.validate(form.getPhoto(), "shopPhoto", true);
        Category category = categoryService.require(form.getCategoryId());

        Shop shop = Shop.create(category.getId(), form.getName().trim(), form.getStreet().trim(), form.getLat(),
                form.getLng(), form.getOpensAt(), form.getClosesAt(), form.getIsOpen() == null || form.getIsOpen(),
                adminId);
        String photoKey = storage.storePublic(form.getPhoto(), photoType, "shops");
        try {
            shop.setPhotoUrl(photoKey);
            shop = repository.saveAndFlush(shop);

            Map<String, Object> details = new LinkedHashMap<>();
            details.put("name", shop.getName());
            details.put("categoryId", category.getId());
            details.put("categoryName", category.getName());
            auditService.record(adminId, ACTION_SHOP_CREATED, AuditService.TARGET_SHOP, shop.getId(), details);
            return mapper.toResponse(shop, category.getName());
        } catch (RuntimeException e) {
            storage.deleteQuietly(List.of(photoKey));
            throw e;
        }
    }

    /** Partial update: only the sent fields change. A replaced photo is deleted after commit. */
    @Transactional
    public ShopResponse update(Long id, ShopUpdateForm f, Long adminId) {
        Shop s = require(id);
        AuditChanges changes = new AuditChanges();
        Category category = categoryService.require(s.getCategoryId());

        if (f.getCategoryId() != null && !f.getCategoryId().equals(s.getCategoryId())) {
            category = categoryService.require(f.getCategoryId());
            changes.put("categoryId", s.getCategoryId(), category.getId());
            s.setCategoryId(category.getId());
        }
        if (f.getName() != null) {
            changes.put("name", s.getName(), f.getName().trim());
            s.setName(f.getName().trim());
        }
        if (f.getStreet() != null) {
            changes.put("street", s.getStreet(), f.getStreet().trim());
            s.setStreet(f.getStreet().trim());
        }
        if (f.getLat() != null) {
            changes.put("lat", s.getLat().stripTrailingZeros().toPlainString(), f.getLat().stripTrailingZeros().toPlainString());
            s.setLat(f.getLat());
        }
        if (f.getLng() != null) {
            changes.put("lng", s.getLng().stripTrailingZeros().toPlainString(), f.getLng().stripTrailingZeros().toPlainString());
            s.setLng(f.getLng());
        }
        if (f.getOpensAt() != null) {
            changes.put("opensAt", s.getOpensAt(), f.getOpensAt());
            s.setOpensAt(f.getOpensAt());
        }
        if (f.getClosesAt() != null) {
            changes.put("closesAt", s.getClosesAt(), f.getClosesAt());
            s.setClosesAt(f.getClosesAt());
        }
        if (f.getIsOpen() != null) {
            changes.put("isOpen", s.isOpen(), f.getIsOpen());
            s.setOpen(f.getIsOpen());
        }

        ImageType photoType = imageValidator.validate(f.getPhoto(), "shopPhoto", false);
        String oldPhoto = s.getPhotoUrl();
        String newPhoto = null;
        try {
            if (photoType != null) {
                newPhoto = storage.storePublic(f.getPhoto(), photoType, "shops");
                s.setPhotoUrl(newPhoto);
                changes.put("photo", "image", "new image");
            }
            repository.saveAndFlush(s);
            if (!changes.isEmpty()) {
                auditService.record(adminId, ACTION_SHOP_UPDATED, AuditService.TARGET_SHOP, s.getId(), changes.asMap());
            }
        } catch (RuntimeException e) {
            if (newPhoto != null) {
                storage.deleteQuietly(List.of(newPhoto));
            }
            throw e;
        }
        if (newPhoto != null) {
            AfterCommit.run(() -> storage.deleteQuietly(List.of(oldPhoto)));
        }
        return mapper.toResponse(s, category.getName());
    }

    /** Soft delete (kept for order history, hidden everywhere). */
    @Transactional
    public void delete(Long id, Long adminId) {
        Shop s = require(id);
        s.setDeletedAt(clock.instant());
        repository.flush();
        auditService.record(adminId, ACTION_SHOP_DELETED, AuditService.TARGET_SHOP, id, Map.of("name", s.getName()));
    }

    /** Dashboard list: every (non-deleted) shop, optionally of one category, incl. inactive categories. */
    @Transactional(readOnly = true)
    public PageResponse<ShopResponse> adminList(Long categoryId, String q, Integer page, Integer size) {
        if (categoryId != null) {
            categoryService.require(categoryId);
        }
        Map<Long, String> names = categoryService.namesById();
        String pattern = Paging.containsPattern(Paging.search(q));
        return PageResponse.of(repository.search(categoryId, pattern, Paging.of(page, size)),
                shop -> mapper.toResponse(shop, names.get(shop.getCategoryId())));
    }

    @Transactional(readOnly = true)
    public ShopResponse adminGet(Long id) {
        Shop s = require(id);
        return mapper.toResponse(s, categoryService.require(s.getCategoryId()).getName());
    }

    /** Customer list: the category must be active, otherwise CATEGORY_NOT_FOUND. */
    @Transactional(readOnly = true)
    public PageResponse<ShopResponse> listByCategory(Long categoryId, String q, Integer page, Integer size) {
        Category category = categoryService.requireActive(categoryId);
        String pattern = Paging.containsPattern(Paging.search(q));
        return PageResponse.of(repository.search(category.getId(), pattern, Paging.of(page, size)),
                shop -> mapper.toResponse(shop, category.getName()));
    }

    /** Customer view of one shop: hidden (SHOP_NOT_FOUND) when its category is inactive. */
    @Transactional(readOnly = true)
    public ShopResponse get(Long shopId) {
        Shop shop = require(shopId);
        Category category;
        try {
            category = categoryService.requireActive(shop.getCategoryId());
        } catch (BusinessException e) {
            throw new BusinessException(ErrorCode.SHOP_NOT_FOUND);
        }
        return mapper.toResponse(shop, category.getName());
    }

    private Shop require(Long id) {
        return repository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.SHOP_NOT_FOUND));
    }
}
