package com.myapp.aqdeeha.modules.catalog.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

/**
 * POST /api/admin/categories — multipart/form-data.
 * Give an {@code iconKey} (predefined icon in the apps), an {@code icon} image, or both.
 */
@Getter
@Setter
public class CategoryForm {

    @NotBlank(message = "validation.required")
    @Size(min = 2, max = 60, message = "validation.category_name_size")
    private String name;

    @Size(max = 60, message = "validation.icon_key")
    @Pattern(regexp = "^[a-z0-9_-]*$", message = "validation.icon_key")
    private String iconKey;

    /** Optional icon image: jpg / png / webp, max 5 MB. */
    private MultipartFile icon;

    @NotBlank(message = "validation.required")
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "validation.color")
    private String color;

    /** Defaults to 0. */
    @Min(value = 0, message = "validation.sort_order")
    @Max(value = 10000, message = "validation.sort_order")
    private Integer sortOrder;

    /** Defaults to true. */
    private Boolean isActive;
}
