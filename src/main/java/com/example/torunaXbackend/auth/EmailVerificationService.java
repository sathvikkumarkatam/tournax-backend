package com.example.torunaXbackend.auth;

import com.example.torunaXbackend.user.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${app.email.from:no-reply@tournax.local}")
    private String fromAddress;

    @Value("${app.email.dev-mode:true}")
    private boolean devMode;

    public void sendVerificationEmail(AppUser user) {
        String verificationUrl = buildVerificationUrl(user.getEmailVerificationToken());

        if (devMode) {
            logDevVerificationLink(user, verificationUrl);
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            throw new RuntimeException("Email service is not configured. Set SMTP environment variables or enable app.email.dev-mode.");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(user.getEmail());
        message.setSubject("Verify your TournaX email");
        message.setText("""
                Welcome to TournaX.

                Verify your email to activate your account:
                %s

                This link expires in 24 hours.
                """.formatted(verificationUrl));

        mailSender.send(message);
    }

    private String buildVerificationUrl(String token) {
        return UriComponentsBuilder
                .fromUriString(frontendUrl)
                .path("/verify-email")
                .queryParam("token", token)
                .build()
                .toUriString();
    }

    private void logDevVerificationLink(AppUser user, String verificationUrl) {
        System.out.println("===========================================");
        System.out.println(" TournaX Email Verification");
        System.out.println("-------------------------------------------");
        System.out.println(" Email : " + user.getEmail());
        System.out.println(" Link  : " + verificationUrl);
        System.out.println("===========================================");
    }
}
