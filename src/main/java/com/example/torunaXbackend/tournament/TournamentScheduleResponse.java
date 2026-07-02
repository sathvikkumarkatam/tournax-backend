package com.example.torunaXbackend.tournament;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TournamentScheduleResponse {
    private Long tournamentId;
    private String format;
    private int teamCount;
    private String message;
    private List<TournamentScheduleMatchResponse> matches;
}
