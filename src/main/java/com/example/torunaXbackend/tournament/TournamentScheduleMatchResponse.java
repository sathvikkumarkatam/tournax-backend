package com.example.torunaXbackend.tournament;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class TournamentScheduleMatchResponse {
    private int matchNumber;
    private String roundName;
    private Long teamAId;
    private String teamAName;
    private Long teamBId;
    private String teamBName;
    private Instant recommendedStartUtc;
    private Instant recommendedEndUtc;
    private int availablePlayers;
    private int totalPlayers;
    private double coverageScore;
    private List<String> missingPlayers;
}
