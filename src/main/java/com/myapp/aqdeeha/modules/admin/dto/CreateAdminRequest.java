package com.myapp.aqdeeha.modules.admin.dto;

import com.myapp.aqdeeha.common.util.PasswordPolicy;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Body for creating an admin (also used by the super-admin bootstrap). */
public record CreateAdminRequest(
        @NotBlank(message = "validation.required")
        @Size(min = 2, max = 100, message = "validation.name_size")
        String name,

        @NotBlank(message = "validation.required")
        @Email(message = "validation.email")
        @Size(max = 150, message = "validation.email")
        String email,

        @NotBlank(message = "validation.required")
        @Pattern(regexp = PasswordPolicy.REGEX, message = PasswordPolicy.MESSAGE)
        String password
) {
}
