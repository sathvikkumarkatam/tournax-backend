package com.example.torunaXbackend.tournament;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TournamentTeamPlayerRepository extends JpaRepository<TournamentTeamPlayer, Long> {
    boolean existsByTeam_Tournament_IdAndUser_Id(Long tournamentId, Long userId);

    void deleteByTeam_Tournament_Id(Long tournamentId);
}
