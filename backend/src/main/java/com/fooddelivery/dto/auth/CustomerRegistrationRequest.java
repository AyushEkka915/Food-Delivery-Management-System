package com.fooddelivery.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerRegistrationRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Email @Size(max = 190) String email,
        @NotBlank @Size(max = 20) String phone,
        @NotBlank @Size(min = 8, max = 72) String password
) {
    public CustomerRegistrationRequest {
        if (email != null) {
            email = email.trim();
        }
    }
}
