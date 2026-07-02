package com.example.torunaXbackend.user;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "app_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    private String password;

    private String provider;
    private String providerId;

    @Builder.Default
    private Boolean emailVerified = false;

    @Column(length = 120)
    private String emailVerificationToken;

    private LocalDateTime emailVerificationTokenExpiresAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(
            name = "user_permissions",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @Column(name = "permission")
    @Builder.Default
    private Set<Permission> permissions = new HashSet<>();
}
