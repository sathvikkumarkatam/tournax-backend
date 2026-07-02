package com.example.torunaXbackend.tournament;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class TournamentTeamRegistrationRequest {
    @NotBlank(message = "Team name is required.")
    @Size(max = 120, message = "Team name is too long.")
    private String teamName;

    @Valid
    @NotEmpty(message = "Team players are required.")
    private List<TournamentTeamPlayerRequest> players;
}
