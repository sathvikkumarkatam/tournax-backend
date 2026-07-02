package com.example.torunaXbackend.tournament;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TournamentTeamRepository extends JpaRepository<TournamentTeam, Long> {
    List<TournamentTeam> findByTournamentIdOrderByCreatedAtDesc(Long tournamentId);

    void deleteByTournamentId(Long tournamentId);
}
