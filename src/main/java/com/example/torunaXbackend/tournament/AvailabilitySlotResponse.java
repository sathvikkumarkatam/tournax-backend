package com.example.torunaXbackend.tournament;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@Builder
public class AvailabilitySlotResponse {
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private String timeZone;
    private Instant startUtc;
    private Instant endUtc;
}
