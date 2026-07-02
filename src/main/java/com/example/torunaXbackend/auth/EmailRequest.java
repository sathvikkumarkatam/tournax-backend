package com.example.torunaXbackend.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailRequest(
        @Email(message = "Invalid email")
        @NotBlank(message = "Email is required")
        String email
) {
}
