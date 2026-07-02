package com.example.torunaXbackend.tournament;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class AuctionDraftTeamRequest {
    @NotNull(message = "Captain registration is required.")
    private Long captainRegistrationId;

    @Size(max = 120, message = "Team name is too long.")
    private String teamName;

    @Valid
    private List<AuctionDraftPlayerRequest> players;
}
