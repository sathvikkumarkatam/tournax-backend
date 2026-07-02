package com.example.torunaXbackend.user;

public record UserSearchResponse(
        Long id,
        String username,
        String email
) {
}
