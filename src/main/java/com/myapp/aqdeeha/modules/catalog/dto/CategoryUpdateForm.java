package com.myapp.aqdeeha.modules.catalog.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

/**
 * PATCH /api/admin/categories/{id} — multipart/form-data. Send only the fields to change.
 * {@code iconKey} sent empty = remove the key; {@code removeIcon=true} = remove the uploaded image.
 * The category must always keep at least one of them.
 */
@Getter
@Setter
public class CategoryUpdateForm {

    @Size(min = 2, max = 60, message = "validation.category_name_size")
    private String name;

    @Size(max = 60, message = "validation.icon_key")
    @Pattern(regexp = "^[a-z0-9_-]*$", message = "validation.icon_key")
    private String iconKey;

    private MultipartFile icon;

    private Boolean removeIcon;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "validation.color")
    private String color;

    @Min(value = 0, message = "validation.sort_order")
    @Max(value = 10000, message = "validation.sort_order")
    private Integer sortOrder;

    private Boolean isActive;
}
