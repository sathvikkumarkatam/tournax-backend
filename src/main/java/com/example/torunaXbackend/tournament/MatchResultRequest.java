package com.example.torunaXbackend.tournament;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class MatchResultRequest {
    @NotNull
    private Long winnerTeamId;

    @NotNull
    @PositiveOrZero
    private Integer teamAScore;

    @NotNull
    @PositiveOrZero
    private Integer teamBScore;
}
