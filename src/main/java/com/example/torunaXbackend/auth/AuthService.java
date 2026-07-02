package com.example.torunaXbackend.auth;

import com.example.torunaXbackend.security.JwtService;
import com.example.torunaXbackend.user.AppUser;
import com.example.torunaXbackend.user.Permission;
import com.example.torunaXbackend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailVerificationService emailVerificationService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new RuntimeException("Email already exists");
        }

        AppUser user = AppUser.builder()
                .username(request.username().trim())
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .emailVerified(false)
                .emailVerificationToken(createVerificationToken())
                .emailVerificationTokenExpiresAt(LocalDateTime.now().plusHours(24))
                .permissions(Set.of(
                        Permission.DASHBOARD_VIEW,
                        Permission.TOURNAMENT_VIEW
                ))
                .build();

        AppUser savedUser = userRepository.save(user);
        emailVerificationService.sendVerificationEmail(savedUser);

        return new AuthResponse(
                null,
                toUserResponse(savedUser),
                "Account created. Please verify your email before logging in."
        );
    }

    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        email,
                        request.password()
                )
        );

        AppUser user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            refreshVerificationTokenIfNeeded(user);
            emailVerificationService.sendVerificationEmail(user);
            throw new RuntimeException("Please verify your email before logging in. We sent a verification link to your email.");
        }

        String token = jwtService.generateToken(user);

        return new AuthResponse(
                token,
                toUserResponse(user),
                "Login successful."
        );
    }

    public UserResponse me(AppUser user) {
        return toUserResponse(user);
    }

    @Transactional
    public AuthMessageResponse verifyEmail(String token) {
        String cleanToken = token == null ? "" : token.trim();
        if (cleanToken.isBlank()) {
            throw new RuntimeException("Verification token is required");
        }

        AppUser user = userRepository.findByEmailVerificationToken(cleanToken)
                .orElseThrow(() -> new RuntimeException("Verification link is invalid"));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            return new AuthMessageResponse("Email is already verified. You can log in.");
        }

        LocalDateTime expiresAt = user.getEmailVerificationTokenExpiresAt();
        if (expiresAt == null || expiresAt.isBefore(LocalDateTime.now())) {
            refreshVerificationToken(user);
            emailVerificationService.sendVerificationEmail(user);
            throw new RuntimeException("Verification link expired. We sent a new verification link to your email.");
        }

        user.setEmailVerified(true);
        userRepository.save(user);

        return new AuthMessageResponse("Email verified successfully. You can now log in.");
    }

    @Transactional
    public AuthMessageResponse resendVerification(EmailRequest request) {
        String email = normalizeEmail(request.email());
        userRepository.findByEmailIgnoreCase(email)
                .filter(user -> !Boolean.TRUE.equals(user.getEmailVerified()))
                .ifPresent(user -> {
                    refreshVerificationToken(user);
                    emailVerificationService.sendVerificationEmail(user);
                });

        return new AuthMessageResponse("If that account exists and is not verified, a verification link has been sent.");
    }

    private UserResponse toUserResponse(AppUser user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getEmailVerified(),
                user.getPermissions()
        );
    }

    public AuthResponse oauthLogin(String email, String username, String provider, String providerId) {
        AppUser user = userRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .orElseGet(() -> userRepository.save(
                        AppUser.builder()
                                .username(username)
                                .email(normalizeEmail(email))
                                .provider(provider)
                                .providerId(providerId)
                                .emailVerified(true)
                                .permissions(Set.of(
                                        Permission.DASHBOARD_VIEW,
                                        Permission.TOURNAMENT_VIEW
                                ))
                                .build()
                ));
        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            user.setEmailVerified(true);
            user.setEmailVerificationToken(null);
            user.setEmailVerificationTokenExpiresAt(null);
            userRepository.save(user);
        }

        String token = jwtService.generateToken(user);

        return new AuthResponse(token, toUserResponse(user), "Login successful.");
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private String createVerificationToken() {
        return UUID.randomUUID() + "-" + UUID.randomUUID();
    }

    private void refreshVerificationTokenIfNeeded(AppUser user) {
        LocalDateTime expiresAt = user.getEmailVerificationTokenExpiresAt();
        if (user.getEmailVerificationToken() == null
                || expiresAt == null
                || expiresAt.isBefore(LocalDateTime.now())) {
            refreshVerificationToken(user);
        }
    }

    private void refreshVerificationToken(AppUser user) {
        user.setEmailVerificationToken(createVerificationToken());
        user.setEmailVerificationTokenExpiresAt(LocalDateTime.now().plusHours(24));
        userRepository.save(user);
    }
}
