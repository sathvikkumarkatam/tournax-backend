package com.example.torunaXbackend.tournament;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class AuctionDraftPlayerRequest {
    @NotNull(message = "Player registration is required.")
    private Long registrationId;

    @NotNull(message = "Bid amount is required.")
    @PositiveOrZero(message = "Bid amount cannot be negative.")
    private Double bidAmount;
}
