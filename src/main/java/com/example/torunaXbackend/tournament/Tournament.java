package com.example.torunaXbackend.tournament;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tournament {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    private GameType game;

    private String format;

    @Column(length = 8000)
    private String formatPlan;

    private Integer teamSize;

    @Builder.Default
    private Boolean auctionEnabled = false;

    private String region;

    private Double entryFee;

    private Double prizePool;

    private LocalDateTime registrationOpenAt;

    private LocalDateTime registrationCloseAt;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    private TournamentStatus status;

    @Column(length = 2000)
    private String description;

    @Column(length = 5000)
    private String rules;

    private String bannerUrl;
}
