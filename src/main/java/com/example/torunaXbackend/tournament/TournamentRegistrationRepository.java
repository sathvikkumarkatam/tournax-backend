package com.example.torunaXbackend.tournament;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TournamentRegistrationRepository extends JpaRepository<TournamentRegistration, Long> {
    List<TournamentRegistration> findByTournamentIdOrderByCreatedAtDesc(Long tournamentId);

    long countByTournamentId(Long tournamentId);

    long countByTournamentIdAndCaptainTrue(Long tournamentId);

    boolean existsByTournamentIdAndUserId(Long tournamentId, Long userId);

    void deleteByTournamentId(Long tournamentId);
}
