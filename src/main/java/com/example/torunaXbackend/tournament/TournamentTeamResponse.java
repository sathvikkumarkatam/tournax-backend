package com.example.torunaXbackend.tournament;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class TournamentTeamResponse {
    private Long id;
    private Long tournamentId;
    private Long captainUserId;
    private String teamName;
    private LocalDateTime createdAt;
    private List<TournamentTeamPlayerResponse> players;
}
