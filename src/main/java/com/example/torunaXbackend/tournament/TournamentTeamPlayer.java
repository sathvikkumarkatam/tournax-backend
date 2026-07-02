package com.example.torunaXbackend.tournament;

import com.example.torunaXbackend.user.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TournamentTeamPlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private TournamentTeam team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Column(nullable = false)
    private String name;

    private String email;

    @Column(nullable = false, length = 40)
    private String phoneNumber;

    private String discord;

    @Column(nullable = false)
    private String rank;

    @ElementCollection
    @CollectionTable(
            name = "tournament_team_player_availability",
            joinColumns = @JoinColumn(name = "team_player_id")
    )
    @Builder.Default
    private List<PlayerAvailabilitySlot> availabilitySlots = new ArrayList<>();

    private Double bidAmount;

    private boolean captain;
}
