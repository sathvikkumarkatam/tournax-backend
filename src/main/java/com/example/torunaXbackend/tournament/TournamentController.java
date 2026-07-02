package com.example.torunaXbackend.tournament;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tournaments")
@RequiredArgsConstructor
public class TournamentController {

    private final TournamentService tournamentService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'TOURNAMENT_CREATE')")
    public TournamentResponse createTournament(@RequestBody TournamentRequest request) {
        return tournamentService.createTournament(request);
    }

    @GetMapping
    public List<TournamentResponse> getAllTournaments() {
        return tournamentService.getAllTournaments();
    }

    @GetMapping("/{id}")
    public TournamentResponse getTournamentById(@PathVariable Long id) {
        return tournamentService.getTournamentById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'TOURNAMENT_EDIT')")
    public TournamentResponse updateTournament(
            @PathVariable Long id,
            @RequestBody TournamentRequest request
    ) {
        return tournamentService.updateTournament(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'TOURNAMENT_DELETE')")
    public void deleteTournament(@PathVariable Long id) {
        tournamentService.deleteTournament(id);
    }

    @GetMapping("/ranks")
    public Map<String, List<String>> getRankOptions() {
        return tournamentService.getRankOptions();
    }

    @PostMapping("/{id}/registrations")
    public TournamentRegistrationResponse registerPlayer(
            @PathVariable Long id,
            @Valid @RequestBody TournamentRegistrationRequest request,
            Authentication authentication
    ) {
        return tournamentService.registerPlayer(id, request, authentication);
    }

    @GetMapping("/{id}/registrations")
    public List<TournamentRegistrationResponse> getRegistrations(@PathVariable Long id) {
        return tournamentService.getRegistrations(id);
    }

    @PatchMapping("/{id}/registrations/{registrationId}/captain")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public TournamentRegistrationResponse setCaptain(
            @PathVariable Long id,
            @PathVariable Long registrationId,
            @RequestBody CaptainAssignmentRequest request
    ) {
        return tournamentService.setCaptain(id, registrationId, request);
    }

    @PostMapping("/{id}/teams")
    public TournamentTeamResponse registerTeam(
            @PathVariable Long id,
            @Valid @RequestBody TournamentTeamRegistrationRequest request,
            Authentication authentication
    ) {
        return tournamentService.registerTeam(id, request, authentication);
    }

    @GetMapping("/{id}/teams")
    public List<TournamentTeamResponse> getTeams(@PathVariable Long id) {
        return tournamentService.getTeams(id);
    }

    @GetMapping("/{id}/schedule")
    public TournamentScheduleResponse getSchedule(@PathVariable Long id) {
        return tournamentService.generateSchedule(id);
    }

    @GetMapping("/{id}/matches")
    public List<TournamentMatchResponse> getMatches(@PathVariable Long id) {
        return tournamentService.getMatches(id);
    }

    @PostMapping("/{id}/matches/generate")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public List<TournamentMatchResponse> generateMatches(@PathVariable Long id) {
        return tournamentService.generateMatches(id);
    }

    @PatchMapping("/{id}/matches/{matchId}/result")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public List<TournamentMatchResponse> submitMatchResult(
            @PathVariable Long id,
            @PathVariable Long matchId,
            @Valid @RequestBody MatchResultRequest request
    ) {
        return tournamentService.submitMatchResult(id, matchId, request);
    }

    @PostMapping("/{id}/auction/finalize")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public List<TournamentTeamResponse> finalizeAuctionTeams(
            @PathVariable Long id,
            @Valid @RequestBody AuctionFinalizeRequest request
    ) {
        return tournamentService.finalizeAuctionTeams(id, request);
    }
}
