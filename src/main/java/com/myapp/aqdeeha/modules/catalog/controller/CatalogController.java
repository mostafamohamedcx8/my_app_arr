package com.myapp.aqdeeha.modules.catalog.controller;

import com.myapp.aqdeeha.common.api.PageResponse;
import com.myapp.aqdeeha.modules.catalog.dto.CatalogResponses.CategoryResponse;
import com.myapp.aqdeeha.modules.catalog.dto.CatalogResponses.ShopResponse;
import com.myapp.aqdeeha.modules.catalog.service.CategoryService;
import com.myapp.aqdeeha.modules.catalog.service.ShopService;
import com.myapp.aqdeeha.security.access.Protect;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Catalog for the apps — any logged-in account. Only active categories (and their shops) are visible. */
@RestController
@Protect
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CatalogController {

    private final CategoryService categoryService;
    private final ShopService shopService;

    @GetMapping("/categories")
    public List<CategoryResponse> categories() {
        return categoryService.listActive();
    }

    /** ?categoryId= (required) &q=name or street &page=0 &size=20 */
    @GetMapping("/shops")
    public PageResponse<ShopResponse> shops(@RequestParam Long categoryId,
                                            @RequestParam(required = false) String q,
                                            @RequestParam(required = false) Integer page,
                                            @RequestParam(required = false) Integer size) {
        return shopService.listByCategory(categoryId, q, page, size);
    }

    @GetMapping("/shops/{id}")
    public ShopResponse shop(@PathVariable Long id) {
        return shopService.get(id);
    }
}
