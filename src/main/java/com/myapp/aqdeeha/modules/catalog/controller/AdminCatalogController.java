package com.myapp.aqdeeha.modules.catalog.controller;

import com.myapp.aqdeeha.common.api.PageResponse;
import com.myapp.aqdeeha.modules.catalog.dto.CatalogResponses.CategoryResponse;
import com.myapp.aqdeeha.modules.catalog.dto.CatalogResponses.ShopResponse;
import com.myapp.aqdeeha.modules.catalog.dto.CategoryForm;
import com.myapp.aqdeeha.modules.catalog.dto.CategoryUpdateForm;
import com.myapp.aqdeeha.modules.catalog.dto.ShopForm;
import com.myapp.aqdeeha.modules.catalog.dto.ShopUpdateForm;
import com.myapp.aqdeeha.modules.catalog.service.CategoryService;
import com.myapp.aqdeeha.modules.catalog.service.ShopService;
import com.myapp.aqdeeha.security.CurrentUser;
import com.myapp.aqdeeha.security.access.AllowTo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.myapp.aqdeeha.security.AuthPrincipal.Role.ADMIN;

/**
 * Catalog management for the dashboard. The whole class is {@code @AllowTo(ADMIN)} (super admin implied):
 * nobody else can create, edit or delete categories or shops. Every write is audited.
 */
@RestController
@AllowTo(ADMIN)
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminCatalogController {

    private final CategoryService categoryService;
    private final ShopService shopService;

    // ---------------------------------------------------------------- categories

    @PostMapping(value = "/categories", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse createCategory(@Valid @ModelAttribute CategoryForm form) {
        return categoryService.create(form, CurrentUser.id());
    }

    /** All categories including inactive, ordered by sortOrder. */
    @GetMapping("/categories")
    public List<CategoryResponse> listCategories() {
        return categoryService.listAll();
    }

    @GetMapping("/categories/{id}")
    public CategoryResponse getCategory(@PathVariable Long id) {
        return categoryService.get(id);
    }

    @PatchMapping(value = "/categories/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CategoryResponse updateCategory(@PathVariable Long id, @Valid @ModelAttribute CategoryUpdateForm form) {
        return categoryService.update(id, form, CurrentUser.id());
    }

    @DeleteMapping("/categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable Long id) {
        categoryService.delete(id, CurrentUser.id());
    }

    // ---------------------------------------------------------------- shops

    @PostMapping(value = "/shops", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ShopResponse createShop(@Valid @ModelAttribute ShopForm form) {
        return shopService.create(form, CurrentUser.id());
    }

    /** ?categoryId= (optional) &q= &page= &size= — includes shops of inactive categories. */
    @GetMapping("/shops")
    public PageResponse<ShopResponse> listShops(@RequestParam(required = false) Long categoryId,
                                                @RequestParam(required = false) String q,
                                                @RequestParam(required = false) Integer page,
                                                @RequestParam(required = false) Integer size) {
        return shopService.adminList(categoryId, q, page, size);
    }

    @GetMapping("/shops/{id}")
    public ShopResponse getShop(@PathVariable Long id) {
        return shopService.adminGet(id);
    }

    @PatchMapping(value = "/shops/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ShopResponse updateShop(@PathVariable Long id, @Valid @ModelAttribute ShopUpdateForm form) {
        return shopService.update(id, form, CurrentUser.id());
    }

    @DeleteMapping("/shops/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteShop(@PathVariable Long id) {
        shopService.delete(id, CurrentUser.id());
    }
}
