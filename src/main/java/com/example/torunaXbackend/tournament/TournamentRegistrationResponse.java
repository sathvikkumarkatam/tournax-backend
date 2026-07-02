package com.example.torunaXbackend.tournament;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class TournamentRegistrationResponse {
    private Long id;
    private Long tournamentId;
    private Long userId;
    private String name;
    private String email;
    private String phoneNumber;
    private String discord;
    private String rank;
    private List<AvailabilitySlotResponse> availabilitySlots;
    private boolean captain;
    private LocalDateTime createdAt;
}
