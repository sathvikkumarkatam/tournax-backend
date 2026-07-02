package com.example.torunaXbackend.tournament;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TournamentMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_a_id", nullable = false)
    private TournamentTeam teamA;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_b_id", nullable = false)
    private TournamentTeam teamB;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_team_id")
    private TournamentTeam winner;

    private Integer stageNumber;

    private String stageName;

    @Column(nullable = false)
    private Integer roundNumber;

    @Column(nullable = false)
    private Integer matchNumber;

    @Column(nullable = false)
    private String roundName;

    @Column(nullable = false)
    private String bracketType;

    private Instant scheduledStartUtc;

    private Instant scheduledEndUtc;

    private Integer availablePlayers;

    private Integer totalPlayers;

    private Double coverageScore;

    @ElementCollection
    @CollectionTable(
            name = "tournament_match_missing_players",
            joinColumns = @JoinColumn(name = "match_id")
    )
    @Column(name = "player_name")
    @Builder.Default
    private List<String> missingPlayers = new ArrayList<>();

    private Integer teamAScore;

    private Integer teamBScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TournamentMatchStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (status == null) {
            status = TournamentMatchStatus.SCHEDULED;
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
