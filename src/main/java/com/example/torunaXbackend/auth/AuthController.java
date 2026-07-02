package com.example.torunaXbackend.auth;

import com.example.torunaXbackend.user.AppUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public AuthResponse register(
            @Valid @RequestBody RegisterRequest request
    ) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(
            @Valid @RequestBody LoginRequest request
    ) {
        return authService.login(request);
    }

    @GetMapping("/verify-email")
    public AuthMessageResponse verifyEmail(@RequestParam String token) {
        return authService.verifyEmail(token);
    }

    @PostMapping("/resend-verification")
    public AuthMessageResponse resendVerification(
            @Valid @RequestBody EmailRequest request
    ) {
        return authService.resendVerification(request);
    }

    @GetMapping("/me")
    public UserResponse me(
            @AuthenticationPrincipal AppUser user
    ) {
        return authService.me(user);
    }
}
