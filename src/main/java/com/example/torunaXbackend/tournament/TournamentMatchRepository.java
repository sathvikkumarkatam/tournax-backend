package com.example.torunaXbackend.tournament;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TournamentMatchRepository extends JpaRepository<TournamentMatch, Long> {
    List<TournamentMatch> findByTournamentIdOrderByRoundNumberAscMatchNumberAscIdAsc(Long tournamentId);

    List<TournamentMatch> findByTournamentIdAndStageNumberOrderByRoundNumberAscMatchNumberAscIdAsc(
            Long tournamentId,
            Integer stageNumber
    );

    List<TournamentMatch> findByTournamentIdAndStageNumberAndBracketTypeAndRoundNumberOrderByMatchNumberAsc(
            Long tournamentId,
            Integer stageNumber,
            String bracketType,
            Integer roundNumber
    );

    boolean existsByTournamentIdAndStageNumberAndBracketTypeAndRoundNumber(
            Long tournamentId,
            Integer stageNumber,
            String bracketType,
            Integer roundNumber
    );

    boolean existsByTournamentIdAndStageNumber(Long tournamentId, Integer stageNumber);

    void deleteByTournamentId(Long tournamentId);
}
