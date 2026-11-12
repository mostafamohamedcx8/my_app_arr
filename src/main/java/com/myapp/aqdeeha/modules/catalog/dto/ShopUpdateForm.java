package com.myapp.aqdeeha.modules.catalog.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalTime;

/** PATCH /api/admin/shops/{id} — multipart/form-data. Send only the fields to change. */
@Getter
@Setter
public class ShopUpdateForm {

    @Size(min = 2, max = 120, message = "validation.shop_name_size")
    private String name;

    private Long categoryId;

    private MultipartFile photo;

    @Size(min = 2, max = 255, message = "validation.street_size")
    private String street;

    @DecimalMin(value = "-90", message = "validation.latitude")
    @DecimalMax(value = "90", message = "validation.latitude")
    private BigDecimal lat;

    @DecimalMin(value = "-180", message = "validation.longitude")
    @DecimalMax(value = "180", message = "validation.longitude")
    private BigDecimal lng;

    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime opensAt;

    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime closesAt;

    private Boolean isOpen;
}
