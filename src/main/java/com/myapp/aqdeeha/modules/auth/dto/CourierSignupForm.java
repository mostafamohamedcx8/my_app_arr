package com.myapp.aqdeeha.modules.auth.dto;

import com.myapp.aqdeeha.common.util.PasswordPolicy;
import com.myapp.aqdeeha.common.util.PlateNumbers;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

/** Courier signup step 3 — sent as multipart/form-data. All images: jpg / png / webp, max 5 MB. */
@Getter
@Setter
public class CourierSignupForm {

    @NotBlank(message = "validation.required")
    private String verificationToken;

    @NotBlank(message = "validation.required")
    @Size(min = 2, max = 100, message = "validation.name_size")
    private String fullName;

    @NotBlank(message = "validation.required")
    @Pattern(regexp = PasswordPolicy.REGEX, message = PasswordPolicy.MESSAGE)
    private String password;

    @NotNull(message = "validation.required")
    @Past(message = "validation.birth_date_past")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate birthDate;

    @NotBlank(message = "validation.required")
    @Size(max = 255, message = "validation.address_size")
    private String address;

    @NotBlank(message = "validation.required")
    @Pattern(regexp = "^(car|scooter|motorcycle|bicycle)$", message = "validation.vehicle_type")
    private String vehicleType;

    @NotBlank(message = "validation.required")
    @Size(max = 30, message = "validation.vehicle_color_size")
    private String vehicleColor;

    /** Optional. Letters and/or digits, spaces allowed between them, max 8 letters + digits. */
    @Size(max = 20, message = "validation.plate_number_size")
    @Pattern(regexp = PlateNumbers.REGEX, message = PlateNumbers.MESSAGE)
    private String plateNumber;

    /** Required: vodafone_cash | instapay. */
    @NotBlank(message = "validation.required")
    @Pattern(regexp = "^(vodafone_cash|instapay)$", message = "validation.wallet_type")
    private String walletType;

    /** Required: Egyptian mobile (both types) or name@instapay (instapay only). */
    @NotBlank(message = "validation.required")
    @Size(max = 100, message = "validation.wallet_number")
    private String walletNumber;

    private MultipartFile photo;
    private MultipartFile idCardFront;
    private MultipartFile idCardBack;
    private MultipartFile vehiclePhoto;
}
