package com.example.torunaXbackend.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByEmail(String email);

    Optional<AppUser> findByEmailIgnoreCase(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailIgnoreCase(String email);

    Optional<AppUser> findByEmailVerificationToken(String emailVerificationToken);

    List<AppUser> findTop10ByOrderByUsernameAsc();

    List<AppUser> findTop10ByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrderByUsernameAsc(
            String username,
            String email
    );
}
