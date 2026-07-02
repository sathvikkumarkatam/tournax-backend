package com.example.torunaXbackend.auth;

import com.example.torunaXbackend.user.Permission;

import java.util.Set;

public record UserResponse(
        Long id,
        String username,
        String email,
        Boolean emailVerified,
        Set<Permission> permissions
) {
}
