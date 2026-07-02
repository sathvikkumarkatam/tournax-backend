package com.example.torunaXbackend.tournament;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.time.LocalDateTime;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerAvailabilitySlot {
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private String timeZone;
}
