package com.example.torunaXbackend.tournament;

import com.example.torunaXbackend.user.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TournamentRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 40)
    private String phoneNumber;

    private String discord;

    @Column(nullable = false)
    private String rank;

    @ElementCollection
    @CollectionTable(
            name = "tournament_registration_availability",
            joinColumns = @JoinColumn(name = "registration_id")
    )
    @Builder.Default
    private List<PlayerAvailabilitySlot> availabilitySlots = new ArrayList<>();

    @Builder.Default
    private boolean captain = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
