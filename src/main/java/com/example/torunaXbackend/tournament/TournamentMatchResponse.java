package com.example.torunaXbackend.tournament;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class TournamentMatchResponse {
    private Long id;
    private Long tournamentId;
    private Long teamAId;
    private String teamAName;
    private Long teamBId;
    private String teamBName;
    private Long winnerTeamId;
    private String winnerTeamName;
    private Integer stageNumber;
    private String stageName;
    private Integer roundNumber;
    private Integer matchNumber;
    private String roundName;
    private String bracketType;
    private Instant scheduledStartUtc;
    private Instant scheduledEndUtc;
    private Integer availablePlayers;
    private Integer totalPlayers;
    private Double coverageScore;
    private List<String> missingPlayers;
    private Integer teamAScore;
    private Integer teamBScore;
    private String status;
}
