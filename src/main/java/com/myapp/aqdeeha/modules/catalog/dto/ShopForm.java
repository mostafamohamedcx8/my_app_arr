package com.myapp.aqdeeha.modules.catalog.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalTime;

/** POST /api/admin/shops — multipart/form-data. Times are Cairo time, format HH:mm (e.g. 09:00, 23:30). */
@Getter
@Setter
public class ShopForm {

    @NotBlank(message = "validation.required")
    @Size(min = 2, max = 120, message = "validation.shop_name_size")
    private String name;

    @NotNull(message = "validation.required")
    private Long categoryId;

    /** Required: jpg / png / webp, max 5 MB. */
    private MultipartFile photo;

    @NotBlank(message = "validation.required")
    @Size(min = 2, max = 255, message = "validation.street_size")
    private String street;

    @NotNull(message = "validation.required")
    @DecimalMin(value = "-90", message = "validation.latitude")
    @DecimalMax(value = "90", message = "validation.latitude")
    private BigDecimal lat;

    @NotNull(message = "validation.required")
    @DecimalMin(value = "-180", message = "validation.longitude")
    @DecimalMax(value = "180", message = "validation.longitude")
    private BigDecimal lng;

    @NotNull(message = "validation.required")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime opensAt;

    @NotNull(message = "validation.required")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime closesAt;

    /** Manual open/closed switch. Defaults to true. */
    private Boolean isOpen;
}
