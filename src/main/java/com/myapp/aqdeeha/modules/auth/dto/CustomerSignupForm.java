package com.myapp.aqdeeha.modules.auth.dto;

import com.myapp.aqdeeha.common.util.PasswordPolicy;
import jakarta.validation.constraints.Email;
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

/** Customer signup step 3 — sent as multipart/form-data. */
@Getter
@Setter
public class CustomerSignupForm {

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
    @Pattern(regexp = "^(male|female)$", message = "validation.gender")
    private String gender;

    /** Optional. */
    @Email(message = "validation.email")
    @Size(max = 150, message = "validation.email")
    private String email;

    /** Optional: jpg / png / webp, max 5 MB. */
    private MultipartFile photo;
}
