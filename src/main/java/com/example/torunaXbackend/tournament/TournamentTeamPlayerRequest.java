package com.example.torunaXbackend.tournament;

import jakarta.validation.constraints.Email;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class TournamentTeamPlayerRequest {
    private Long userId;

    @Email(message = "Player email must be valid.")
    @Size(max = 160, message = "Player email is too long.")
    private String email;

    @NotBlank(message = "Phone number is required for every player.")
    @Size(max = 40, message = "Phone number is too long.")
    private String phoneNumber;

    @NotBlank(message = "Rank is required for every player.")
    private String rank;

    @Valid
    @NotEmpty(message = "At least one availability slot is required for every player.")
    private List<AvailabilitySlotRequest> availabilitySlots;
}
