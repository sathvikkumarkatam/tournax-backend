package com.example.torunaXbackend.tournament;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class AuctionFinalizeRequest {
    @Valid
    @NotEmpty(message = "Auction teams are required.")
    private List<AuctionDraftTeamRequest> teams;
}
