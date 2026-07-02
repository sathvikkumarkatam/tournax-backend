package com.example.torunaXbackend.tournament;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AvailabilitySlotRequest {
    @NotNull(message = "Availability start is required.")
    private LocalDateTime startAt;

    @NotNull(message = "Availability end is required.")
    private LocalDateTime endAt;

    @NotBlank(message = "Availability time zone is required.")
    @Size(max = 80, message = "Availability time zone is too long.")
    private String timeZone;
}
