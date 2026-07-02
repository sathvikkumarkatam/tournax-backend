package com.example.torunaXbackend.config;

import com.example.torunaXbackend.user.AppUser;
import com.example.torunaXbackend.user.Permission;
import com.example.torunaXbackend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Value("${app.super-admin.username}")
    private String username;

    @Value("${app.super-admin.email}")
    private String email;

    @Value("${app.super-admin.password}")
    private String password;

    @Override
    public void run(String... args) {
        cleanupOrphanRows();
        markLegacyUsersVerified();

        if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
            System.out.println("Super Admin seed skipped. Set APP_SUPER_ADMIN_EMAIL and APP_SUPER_ADMIN_PASSWORD to enable it.");
            return;
        }

        if (!StringUtils.hasText(username)) {
            username = "Super Admin";
        }

        AppUser superAdmin = userRepository.findByEmailIgnoreCase(email)
                .orElse(null);
        Set<Permission> allPermissions = new HashSet<>(Arrays.asList(Permission.values()));

        if (superAdmin != null) {
            boolean changed = false;
            if (superAdmin.getPermissions() == null) {
                superAdmin.setPermissions(new HashSet<>());
            }
            if (!superAdmin.getPermissions().containsAll(allPermissions)) {
                superAdmin.getPermissions().addAll(allPermissions);
                changed = true;
            }
            if (!username.equals(superAdmin.getUsername())) {
                superAdmin.setUsername(username);
                changed = true;
            }
            if (!Boolean.TRUE.equals(superAdmin.getEmailVerified())) {
                superAdmin.setEmailVerified(true);
                superAdmin.setEmailVerificationToken(null);
                superAdmin.setEmailVerificationTokenExpiresAt(null);
                changed = true;
            }
            if (changed) {
                userRepository.save(superAdmin);
                System.out.println("Super Admin updated.");
            } else {
                System.out.println("Super Admin already exists.");
            }
            return;
        }

        superAdmin = AppUser.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(password))
                .emailVerified(true)
                .permissions(allPermissions)
                .build();

        userRepository.save(superAdmin);

        System.out.println("===========================================");
        System.out.println(" Super Admin Created Successfully");
        System.out.println("-------------------------------------------");
        System.out.println(" Username : " + username);
        System.out.println(" Email    : " + email);
        System.out.println(" Password : configured from environment");
        System.out.println("===========================================");
    }

    private void cleanupOrphanRows() {
        cleanupTable("tournament_match_missing_players", """
                delete from tournament_match_missing_players missing
                where not exists (
                    select 1 from tournament_match match where match.id = missing.match_id
                )
                or exists (
                    select 1 from tournament_match match
                    where match.id = missing.match_id
                    and (
                        not exists (
                            select 1 from tournament tournament
                            where tournament.id = match.tournament_id
                        )
                        or not exists (
                            select 1 from tournament_team team
                            where team.id = match.team_a_id
                        )
                        or not exists (
                            select 1 from tournament_team team
                            where team.id = match.team_b_id
                        )
                        or (
                            match.winner_team_id is not null
                            and not exists (
                                select 1 from tournament_team team
                                where team.id = match.winner_team_id
                            )
                        )
                    )
                )
                """);
        cleanupTable("tournament_registration_availability", """
                delete from tournament_registration_availability availability
                where not exists (
                    select 1 from tournament_registration registration
                    where registration.id = availability.registration_id
                )
                or exists (
                    select 1 from tournament_registration registration
                    where registration.id = availability.registration_id
                    and (
                        not exists (
                            select 1 from tournament tournament
                            where tournament.id = registration.tournament_id
                        )
                        or (
                            registration.user_id is not null
                            and not exists (
                                select 1 from app_users users
                                where users.id = registration.user_id
                            )
                        )
                    )
                )
                """);
        cleanupTable("tournament_team_player_availability", """
                delete from tournament_team_player_availability availability
                where not exists (
                    select 1 from tournament_team_player player
                    where player.id = availability.team_player_id
                )
                or exists (
                    select 1 from tournament_team_player player
                    where player.id = availability.team_player_id
                    and (
                        not exists (
                            select 1 from tournament_team team
                            where team.id = player.team_id
                        )
                        or (
                            player.user_id is not null
                            and not exists (
                                select 1 from app_users users
                                where users.id = player.user_id
                            )
                        )
                    )
                )
                """);
        cleanupTable("tournament_match", """
                delete from tournament_match match
                where not exists (
                    select 1 from tournament tournament
                    where tournament.id = match.tournament_id
                )
                or not exists (
                    select 1 from tournament_team team
                    where team.id = match.team_a_id
                )
                or not exists (
                    select 1 from tournament_team team
                    where team.id = match.team_b_id
                )
                or (
                    match.winner_team_id is not null
                    and not exists (
                        select 1 from tournament_team team
                        where team.id = match.winner_team_id
                    )
                )
                """);
        cleanupTable("tournament_team_player", """
                delete from tournament_team_player player
                where not exists (
                    select 1 from tournament_team team where team.id = player.team_id
                )
                or (
                    player.user_id is not null
                    and not exists (
                        select 1 from app_users users where users.id = player.user_id
                    )
                )
                """);
        cleanupTable("tournament_registration", """
                delete from tournament_registration registration
                where not exists (
                    select 1 from tournament tournament
                    where tournament.id = registration.tournament_id
                )
                or (
                    registration.user_id is not null
                    and not exists (
                        select 1 from app_users users where users.id = registration.user_id
                    )
                )
                """);
        cleanupTable("tournament_team", """
                delete from tournament_team team
                where not exists (
                    select 1 from tournament tournament
                    where tournament.id = team.tournament_id
                )
                or (
                    team.captain_user_id is not null
                    and not exists (
                        select 1 from app_users users where users.id = team.captain_user_id
                    )
                )
                """);
        cleanupTable("user_permissions", """
                delete from user_permissions permissions
                where not exists (
                    select 1 from app_users users where users.id = permissions.user_id
                )
                """);
    }

    private void cleanupTable(String tableName, String sql) {
        Boolean tableExists = jdbcTemplate.queryForObject(
                "select to_regclass(?) is not null",
                Boolean.class,
                tableName
        );
        if (Boolean.TRUE.equals(tableExists)) {
            jdbcTemplate.update(sql);
        }
    }

    private void markLegacyUsersVerified() {
        if (!columnExists("app_users", "email_verified")
                || !columnExists("app_users", "email_verification_token")) {
            return;
        }

        jdbcTemplate.update("""
                update app_users
                set email_verified = true
                where (email_verified is null or email_verified = false)
                and (email_verification_token is null or provider is not null)
                """);
    }

    private boolean columnExists(String tableName, String columnName) {
        Boolean exists = jdbcTemplate.queryForObject(
                """
                        select exists (
                            select 1
                            from information_schema.columns
                            where table_name = ?
                            and column_name = ?
                        )
                        """,
                Boolean.class,
                tableName,
                columnName
        );
        return Boolean.TRUE.equals(exists);
    }
}
