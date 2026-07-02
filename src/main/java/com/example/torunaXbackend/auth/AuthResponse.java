package com.example.torunaXbackend.auth;

public record AuthResponse(
        String token,
        UserResponse user,
        String message
) {
}
