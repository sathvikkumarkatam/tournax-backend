package com.example.torunaXbackend.tournament;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TournamentTeamPlayerResponse {
    private Long id;
    private Long userId;
    private String name;
    private String email;
    private String phoneNumber;
    private String discord;
    private String rank;
    private List<AvailabilitySlotResponse> availabilitySlots;
    private Double bidAmount;
    private boolean captain;
}
