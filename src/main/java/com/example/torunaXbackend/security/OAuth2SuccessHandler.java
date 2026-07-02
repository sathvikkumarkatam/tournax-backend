package com.example.torunaXbackend.security;

import com.example.torunaXbackend.user.AppUser;
import com.example.torunaXbackend.user.Permission;
import com.example.torunaXbackend.user.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();

        String email = oauthUser.getAttribute("email");

        if (email == null || email.isBlank()) {
            response.sendRedirect(frontendUrl + "/login?error=email_missing");
            return;
        }

        String username = oauthUser.getAttribute("name");

        if (username == null) {
            username = oauthUser.getAttribute("global_name");
        }

        if (username == null) {
            username = oauthUser.getAttribute("username");
        }

        if (username == null) {
            username = email;
        }
        final String finalUsername = username;
        String providerId = oauthUser.getName();

        AppUser user = userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.save(
                        AppUser.builder()
                                .username(finalUsername)
                                .email(email)
                                .provider("discord")
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

        String redirectUrl = UriComponentsBuilder
                .fromUriString(frontendUrl)
                .path("/oauth/callback")
                .queryParam("token", token)
                .build()
                .toUriString();

        response.sendRedirect(redirectUrl);
    }
}
