package com.myapp.aqdeeha.modules.catalog.service;

import com.myapp.aqdeeha.common.exception.BusinessException;
import com.myapp.aqdeeha.common.exception.ErrorCode;
import com.myapp.aqdeeha.common.storage.FileStorageService;
import com.myapp.aqdeeha.common.storage.ImageValidator;
import com.myapp.aqdeeha.common.storage.ImageValidator.ImageType;
import com.myapp.aqdeeha.common.util.AfterCommit;
import com.myapp.aqdeeha.modules.audit.service.AuditChanges;
import com.myapp.aqdeeha.modules.audit.service.AuditService;
import com.myapp.aqdeeha.modules.catalog.dto.CatalogResponses.CategoryResponse;
import com.myapp.aqdeeha.modules.catalog.dto.CategoryForm;
import com.myapp.aqdeeha.modules.catalog.dto.CategoryUpdateForm;
import com.myapp.aqdeeha.modules.catalog.entity.Category;
import com.myapp.aqdeeha.modules.catalog.mapper.CatalogMapper;
import com.myapp.aqdeeha.modules.catalog.repository.CategoryRepository;
import com.myapp.aqdeeha.modules.catalog.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Map;

/** Categories. Create / update / delete are admin-only (enforced on the controller) and audited. */
@Service
@RequiredArgsConstructor
public class CategoryService {

    public static final String ACTION_CREATED = "category.created";
    public static final String ACTION_UPDATED = "category.updated";
    public static final String ACTION_DELETED = "category.deleted";

    private final CategoryRepository repository;
    private final ShopRepository shopRepository;
    private final ImageValidator imageValidator;
    private final FileStorageService storage;
    private final AuditService auditService;
    private final CatalogMapper mapper;
    private final Clock clock;

    @Transactional
    public CategoryResponse create(CategoryForm form, Long adminId) {
        String name = form.getName().trim();
        String iconKey = blankToNull(form.getIconKey());
        ImageType iconType = imageValidator.validate(form.getIcon(), "icon", false);
        if (iconKey == null && iconType == null) {
            throw BusinessException.forField(ErrorCode.CATEGORY_ICON_REQUIRED, "icon");
        }
        if (repository.existsByName(name)) {
            throw BusinessException.forField(ErrorCode.CATEGORY_NAME_EXISTS, "name");
        }

        Category category = Category.create(name, form.getColor().toUpperCase(),
                form.getSortOrder() == null ? 0 : form.getSortOrder(),
                form.getIsActive() == null || form.getIsActive(), adminId);
        category.setIconKey(iconKey);
        String storedIcon = iconType == null ? null : storage.storePublic(form.getIcon(), iconType, "categories");
        category.setIconUrl(storedIcon);
        category = saveOrNameConflict(category, storedIcon);
        auditService.record(adminId, ACTION_CREATED, AuditService.TARGET_CATEGORY, category.getId(),
                Map.of("name", category.getName()));
        return mapper.toResponse(category);
    }

    /** Partial update: only the sent fields change. A replaced/removed icon file is deleted after commit. */
    @Transactional
    public CategoryResponse update(Long id, CategoryUpdateForm f, Long adminId) {
        Category c = require(id);
        AuditChanges changes = new AuditChanges();

        if (f.getName() != null) {
            String name = f.getName().trim();
            if (!name.equalsIgnoreCase(c.getName()) && repository.existsByName(name)) {
                throw BusinessException.forField(ErrorCode.CATEGORY_NAME_EXISTS, "name");
            }
            changes.put("name", c.getName(), name);
            c.setName(name);
        }
        if (f.getIconKey() != null) {
            String key = blankToNull(f.getIconKey());
            changes.put("iconKey", c.getIconKey(), key);
            c.setIconKey(key);
        }
        ImageType iconType = imageValidator.validate(f.getIcon(), "icon", false);
        String oldIcon = c.getIconUrl();
        String newIcon = null;
        if (iconType != null) {
            newIcon = storage.storePublic(f.getIcon(), iconType, "categories");
            c.setIconUrl(newIcon);
            changes.put("icon", oldIcon == null ? null : "image", "new image");
        } else if (Boolean.TRUE.equals(f.getRemoveIcon())) {
            c.setIconUrl(null);
            changes.put("icon", oldIcon == null ? null : "image", null);
        }
        if (c.getIconKey() == null && c.getIconUrl() == null) {
            throw BusinessException.forField(ErrorCode.CATEGORY_ICON_REQUIRED, "icon");
        }
        if (f.getColor() != null) {
            changes.put("color", c.getColor(), f.getColor().toUpperCase());
            c.setColor(f.getColor().toUpperCase());
        }
        if (f.getSortOrder() != null) {
            changes.put("sortOrder", c.getSortOrder(), f.getSortOrder());
            c.setSortOrder(f.getSortOrder());
        }
        if (f.getIsActive() != null) {
            changes.put("isActive", c.isActive(), f.getIsActive());
            c.setActive(f.getIsActive());
        }

        c = saveOrNameConflict(c, newIcon);
        if (!changes.isEmpty()) {
            auditService.record(adminId, ACTION_UPDATED, AuditService.TARGET_CATEGORY, c.getId(), changes.asMap());
        }
        if (oldIcon != null && !oldIcon.equals(c.getIconUrl())) {
            AfterCommit.run(() -> storage.deleteQuietly(List.of(oldIcon)));
        }
        return mapper.toResponse(c);
    }

    /** Soft delete. Refused while the category still has (non-deleted) shops. */
    @Transactional
    public void delete(Long id, Long adminId) {
        Category c = require(id);
        if (shopRepository.existsByCategoryId(id)) {
            throw new BusinessException(ErrorCode.CATEGORY_HAS_SHOPS);
        }
        c.setDeletedAt(clock.instant());
        repository.flush();
        auditService.record(adminId, ACTION_DELETED, AuditService.TARGET_CATEGORY, id, Map.of("name", c.getName()));
    }

    /** Customer home screen: active only, ordered by sortOrder. */
    @Transactional(readOnly = true)
    public List<CategoryResponse> listActive() {
        return repository.findByActiveTrueOrderBySortOrderAscIdAsc().stream().map(mapper::toResponse).toList();
    }

    /** Dashboard: all (not deleted), including inactive. */
    @Transactional(readOnly = true)
    public List<CategoryResponse> listAll() {
        return repository.findAllByOrderBySortOrderAscIdAsc().stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse get(Long id) {
        return mapper.toResponse(require(id));
    }

    /** Any non-deleted category (active or not) — for admin operations. */
    @Transactional(readOnly = true)
    public Category require(Long id) {
        return repository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
    }

    /** Only active categories exist from the customer's point of view. */
    @Transactional(readOnly = true)
    public Category requireActive(Long id) {
        return repository.findById(id).filter(Category::isActive)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Map<Long, String> namesById() {
        Map<Long, String> names = new java.util.HashMap<>();
        repository.findAll().forEach(c -> names.put(c.getId(), c.getName()));
        return names;
    }

    private Category saveOrNameConflict(Category category, String newlyStoredFile) {
        try {
            return repository.saveAndFlush(category);
        } catch (DataIntegrityViolationException e) {
            if (newlyStoredFile != null) {
                storage.deleteQuietly(List.of(newlyStoredFile));
            }
            throw BusinessException.forField(ErrorCode.CATEGORY_NAME_EXISTS, "name");
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
