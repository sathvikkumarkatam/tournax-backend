package com.example.torunaXbackend.tournament;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TournamentRequest {
    private String name;
    private GameType game;
    private String format;
    private String formatPlan;
    private Integer teamSize;
    private Boolean auctionEnabled;
    private String region;
    private Double entryFee;
    private Double prizePool;
    private LocalDateTime registrationOpenAt;
    private LocalDateTime registrationCloseAt;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private String description;
    private String rules;
    private String bannerUrl;
}
