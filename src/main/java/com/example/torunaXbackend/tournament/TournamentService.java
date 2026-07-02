package com.example.torunaXbackend.tournament;
import com.example.torunaXbackend.user.AppUser;
import com.example.torunaXbackend.user.Permission;
import com.example.torunaXbackend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TournamentService {
    private static final String PHONE_ERROR = "Phone number must include country code and exactly 10 local digits, for example +15551234567.";
    private static final String PHONE_REGEX = "^\\+[1-9]\\d{10,13}$";
    private static final Duration MATCH_DURATION = Duration.ofMinutes(60);
    private static final String BRACKET_ROUND_ROBIN = "ROUND_ROBIN";
    private static final String BRACKET_GROUP_STAGE = "GROUP_STAGE";
    private static final String BRACKET_SWISS = "SWISS";
    private static final String BRACKET_SINGLE_ELIMINATION = "SINGLE_ELIMINATION";
    private static final String BRACKET_UPPER_BRACKET = "UPPER_BRACKET";
    private static final String BRACKET_LOWER_BRACKET = "LOWER_BRACKET";
    private static final String BRACKET_GRAND_FINAL = "GRAND_FINAL";

    private final TournamentRepository tournamentRepository;
    private final TournamentRegistrationRepository registrationRepository;
    private final TournamentTeamRepository teamRepository;
    private final TournamentTeamPlayerRepository teamPlayerRepository;
    private final TournamentMatchRepository matchRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public TournamentResponse createTournament(TournamentRequest request) {
        Tournament tournament = Tournament.builder()
                .name(request.getName())
                .game(request.getGame())
                .format(request.getFormat())
                .formatPlan(request.getFormatPlan())
                .teamSize(request.getTeamSize())
                .auctionEnabled(Boolean.TRUE.equals(request.getAuctionEnabled()))
                .region(request.getRegion())
                .entryFee(request.getEntryFee())
                .prizePool(request.getPrizePool())
                .registrationOpenAt(request.getRegistrationOpenAt())
                .registrationCloseAt(request.getRegistrationCloseAt())
                .startAt(request.getStartAt())
                .endAt(request.getEndAt())
                .description(request.getDescription())
                .rules(request.getRules())
                .bannerUrl(request.getBannerUrl())
                .status(TournamentStatus.DRAFT)
                .build();

        return mapToResponse(tournamentRepository.save(tournament));
    }

    public List<TournamentResponse> getAllTournaments() {
        return tournamentRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public TournamentResponse getTournamentById(Long id) {
        Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tournament not found"));

        return mapToResponse(tournament);
    }

    public TournamentResponse updateTournament(Long id, TournamentRequest request) {
        Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tournament not found"));

        tournament.setName(request.getName());
        tournament.setGame(request.getGame());
        tournament.setFormat(request.getFormat());
        tournament.setFormatPlan(request.getFormatPlan());
        tournament.setTeamSize(request.getTeamSize());
        tournament.setAuctionEnabled(Boolean.TRUE.equals(request.getAuctionEnabled()));
        tournament.setRegion(request.getRegion());
        tournament.setEntryFee(request.getEntryFee());
        tournament.setPrizePool(request.getPrizePool());
        tournament.setRegistrationOpenAt(request.getRegistrationOpenAt());
        tournament.setRegistrationCloseAt(request.getRegistrationCloseAt());
        tournament.setStartAt(request.getStartAt());
        tournament.setEndAt(request.getEndAt());
        tournament.setDescription(request.getDescription());
        tournament.setRules(request.getRules());
        tournament.setBannerUrl(request.getBannerUrl());

        return mapToResponse(tournamentRepository.save(tournament));
    }

    @Transactional
    public void deleteTournament(Long id) {
        if (!tournamentRepository.existsById(id)) {
            throw new RuntimeException("Tournament not found");
        }

        matchRepository.deleteByTournamentId(id);
        registrationRepository.deleteByTournamentId(id);
        teamPlayerRepository.deleteByTeam_Tournament_Id(id);
        teamRepository.deleteByTournamentId(id);
        tournamentRepository.deleteById(id);
    }

    public Map<String, List<String>> getRankOptions() {
        return Arrays.stream(GameType.values())
                .collect(Collectors.toMap(
                        GameType::name,
                        GameType::getRanks,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    @Transactional
    public TournamentRegistrationResponse registerPlayer(
            Long tournamentId,
            TournamentRegistrationRequest request,
            Authentication authentication
    ) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new RuntimeException("Tournament not found"));
        if (!Boolean.TRUE.equals(tournament.getAuctionEnabled())) {
            throw new RuntimeException("This tournament uses team registration, not auction registration");
        }

        AppUser currentUser = requireAuthenticatedUser(authentication);
        AppUser user = resolveRegistrationUser(request, currentUser);
        if (registrationRepository.existsByTournamentIdAndUserId(tournamentId, user.getId())) {
            throw new RuntimeException(user.getUsername() + " is already registered for this tournament");
        }

        String rank = trimToNull(request.getRank());

        if (!tournament.getGame().getRanks().contains(rank)) {
            throw new RuntimeException("Rank is not valid for " + tournament.getGame().name());
        }

        TournamentRegistration registration = TournamentRegistration.builder()
                .tournament(tournament)
                .user(user)
                .name(user.getUsername())
                .phoneNumber(normalizePhoneNumber(request.getPhoneNumber()))
                .discord(resolveDiscord(user))
                .rank(rank)
                .availabilitySlots(mapAvailabilitySlots(request.getAvailabilitySlots()))
                .captain(false)
                .build();

        return mapToRegistrationResponse(registrationRepository.save(registration));
    }

    @Transactional(readOnly = true)
    public List<TournamentRegistrationResponse> getRegistrations(Long tournamentId) {
        if (!tournamentRepository.existsById(tournamentId)) {
            throw new RuntimeException("Tournament not found");
        }

        return registrationRepository.findByTournamentIdOrderByCreatedAtDesc(tournamentId)
                .stream()
                .map(this::mapToRegistrationResponse)
                .toList();
    }

    @Transactional
    public TournamentRegistrationResponse setCaptain(
            Long tournamentId,
            Long registrationId,
            CaptainAssignmentRequest request
    ) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new RuntimeException("Tournament not found"));
        if (!Boolean.TRUE.equals(tournament.getAuctionEnabled())) {
            throw new RuntimeException("Captain assignment is only available for auction tournaments");
        }

        TournamentRegistration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new RuntimeException("Registered player not found"));
        if (!registration.getTournament().getId().equals(tournamentId)) {
            throw new RuntimeException("Registered player does not belong to this tournament");
        }

        boolean captain = Boolean.TRUE.equals(request.getCaptain());
        if (captain && !registration.isCaptain()) {
            long captainCount = registrationRepository.countByTournamentIdAndCaptainTrue(tournamentId);
            int captainSlots = calculateCaptainSlots(
                    registrationRepository.countByTournamentId(tournamentId),
                    tournament.getTeamSize()
            );
            if (captainCount >= captainSlots) {
                throw new RuntimeException("Captain slots are already filled for the current player count");
            }
        }

        registration.setCaptain(captain);
        return mapToRegistrationResponse(registrationRepository.save(registration));
    }

    @Transactional
    public TournamentTeamResponse registerTeam(
            Long tournamentId,
            TournamentTeamRegistrationRequest request,
            Authentication authentication
    ) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new RuntimeException("Tournament not found"));
        if (Boolean.TRUE.equals(tournament.getAuctionEnabled())) {
            throw new RuntimeException("This tournament uses auction registration, not team registration");
        }

        int teamSize = tournament.getTeamSize() == null ? 1 : tournament.getTeamSize();
        if (request.getPlayers() == null || request.getPlayers().size() != teamSize) {
            throw new RuntimeException("Team roster must contain exactly " + teamSize + " players");
        }

        AppUser currentUser = requireAuthenticatedUser(authentication);

        TournamentTeam team = TournamentTeam.builder()
                .tournament(tournament)
                .teamName(request.getTeamName().trim())
                .build();
        Set<Long> rosterUserIds = new HashSet<>();

        for (int index = 0; index < request.getPlayers().size(); index++) {
            TournamentTeamPlayerRequest playerRequest = request.getPlayers().get(index);
            AppUser playerUser = resolveTeamPlayerUser(index, playerRequest, currentUser);
            if (!rosterUserIds.add(playerUser.getId())) {
                throw new RuntimeException(playerUser.getUsername() + " appears more than once in this team roster");
            }
            if (teamPlayerRepository.existsByTeam_Tournament_IdAndUser_Id(tournamentId, playerUser.getId())) {
                throw new RuntimeException(playerUser.getUsername() + " is already registered in a team for this tournament");
            }

            String rank = trimToNull(playerRequest.getRank());
            if (!tournament.getGame().getRanks().contains(rank)) {
                throw new RuntimeException("Rank is not valid for " + tournament.getGame().name());
            }

            team.addPlayer(TournamentTeamPlayer.builder()
                    .user(playerUser)
                    .name(playerUser.getUsername())
                    .email(playerUser.getEmail())
                    .phoneNumber(normalizePhoneNumber(playerRequest.getPhoneNumber()))
                    .discord(resolveDiscord(playerUser))
                    .rank(rank)
                    .availabilitySlots(mapAvailabilitySlots(playerRequest.getAvailabilitySlots()))
                    .captain(index == 0)
                    .build());

            if (index == 0) {
                team.setCaptainUser(playerUser);
            }
        }

        return mapToTeamResponse(teamRepository.save(team));
    }

    @Transactional(readOnly = true)
    public List<TournamentTeamResponse> getTeams(Long tournamentId) {
        if (!tournamentRepository.existsById(tournamentId)) {
            throw new RuntimeException("Tournament not found");
        }

        return teamRepository.findByTournamentIdOrderByCreatedAtDesc(tournamentId)
                .stream()
                .map(this::mapToTeamResponse)
                .toList();
    }

    @Transactional
    public List<TournamentTeamResponse> finalizeAuctionTeams(
            Long tournamentId,
            AuctionFinalizeRequest request
    ) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new RuntimeException("Tournament not found"));
        if (!Boolean.TRUE.equals(tournament.getAuctionEnabled())) {
            throw new RuntimeException("Auction finalization is only available for auction tournaments");
        }

        List<TournamentRegistration> registrations =
                registrationRepository.findByTournamentIdOrderByCreatedAtDesc(tournamentId);
        Map<Long, TournamentRegistration> registrationsById = registrations
                .stream()
                .collect(Collectors.toMap(TournamentRegistration::getId, Function.identity()));
        Set<Long> captainIds = registrations
                .stream()
                .filter(TournamentRegistration::isCaptain)
                .map(TournamentRegistration::getId)
                .collect(Collectors.toSet());
        Set<Long> remainingPlayerIds = registrations
                .stream()
                .filter(registration -> !registration.isCaptain())
                .map(TournamentRegistration::getId)
                .collect(Collectors.toSet());

        if (captainIds.isEmpty()) {
            throw new RuntimeException("Assign captains before finalizing the auction");
        }
        if (request.getTeams() == null || request.getTeams().size() != captainIds.size()) {
            throw new RuntimeException("Submit exactly one team for every assigned captain");
        }

        int teamSize = tournament.getTeamSize() == null || tournament.getTeamSize() < 1
                ? 1
                : tournament.getTeamSize();
        Set<Long> submittedCaptainIds = new HashSet<>();
        Set<Long> assignedPlayerIds = new HashSet<>();

        matchRepository.deleteByTournamentId(tournamentId);
        teamPlayerRepository.deleteByTeam_Tournament_Id(tournamentId);
        teamRepository.deleteByTournamentId(tournamentId);

        List<TournamentTeam> teams = request.getTeams()
                .stream()
                .map(teamRequest -> {
                    TournamentRegistration captain = registrationsById.get(teamRequest.getCaptainRegistrationId());
                    if (captain == null || !captain.isCaptain()) {
                        throw new RuntimeException("Selected captain does not belong to this tournament");
                    }
                    if (!submittedCaptainIds.add(captain.getId())) {
                        throw new RuntimeException(captain.getName() + " was submitted as captain more than once");
                    }

                    List<AuctionDraftPlayerRequest> draftedPlayers =
                            teamRequest.getPlayers() == null ? List.of() : teamRequest.getPlayers();
                    if (draftedPlayers.size() + 1 > teamSize) {
                        throw new RuntimeException(captain.getName() + "'s team exceeds the team size of " + teamSize);
                    }

                    TournamentTeam team = TournamentTeam.builder()
                            .tournament(tournament)
                            .captainUser(captain.getUser())
                            .teamName(resolveAuctionTeamName(teamRequest.getTeamName(), captain))
                            .build();
                    team.addPlayer(mapRegistrationToTeamPlayer(captain, true, null));

                    for (AuctionDraftPlayerRequest playerRequest : draftedPlayers) {
                        TournamentRegistration player = registrationsById.get(playerRequest.getRegistrationId());
                        if (player == null || player.isCaptain()) {
                            throw new RuntimeException("Drafted player does not belong to the available player pool");
                        }
                        if (!assignedPlayerIds.add(player.getId())) {
                            throw new RuntimeException(player.getName() + " was drafted more than once");
                        }

                        team.addPlayer(mapRegistrationToTeamPlayer(
                                player,
                                false,
                                playerRequest.getBidAmount()
                        ));
                    }

                    return team;
                })
                .toList();

        if (!submittedCaptainIds.equals(captainIds)) {
            throw new RuntimeException("Every assigned captain must have a submitted team");
        }
        if (!assignedPlayerIds.equals(remainingPlayerIds)) {
            throw new RuntimeException("Every non-captain player must be assigned to a team before submitting");
        }

        return teamRepository.saveAll(teams)
                .stream()
                .map(this::mapToTeamResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TournamentScheduleResponse generateSchedule(Long tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new RuntimeException("Tournament not found"));
        List<TournamentTeam> teams = teamRepository.findByTournamentIdOrderByCreatedAtDesc(tournamentId)
                .stream()
                .sorted(Comparator.comparing(TournamentTeam::getId))
                .toList();

        if (teams.size() < 2) {
            return TournamentScheduleResponse.builder()
                    .tournamentId(tournamentId)
                    .format(tournament.getFormat())
                    .teamCount(teams.size())
                    .message("At least two formed teams are required to generate a schedule.")
                    .matches(List.of())
                    .build();
        }

        List<MatchPair> matchPairs = buildMatchPairs(teams, tournament.getFormat());
        List<TournamentScheduleMatchResponse> matches = new ArrayList<>();

        for (int index = 0; index < matchPairs.size(); index++) {
            matches.add(scoreMatch(tournament, matchPairs.get(index), index + 1));
        }

        return TournamentScheduleResponse.builder()
                .tournamentId(tournamentId)
                .format(tournament.getFormat())
                .teamCount(teams.size())
                .message("Schedule is generated from saved player availability and timezone-aware UTC comparisons.")
                .matches(matches)
                .build();
    }

    @Transactional(readOnly = true)
    public List<TournamentMatchResponse> getMatches(Long tournamentId) {
        if (!tournamentRepository.existsById(tournamentId)) {
            throw new RuntimeException("Tournament not found");
        }

        return matchRepository.findByTournamentIdOrderByRoundNumberAscMatchNumberAscIdAsc(tournamentId)
                .stream()
                .map(this::mapToMatchResponse)
                .toList();
    }

    @Transactional
    public List<TournamentMatchResponse> generateMatches(Long tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new RuntimeException("Tournament not found"));
        List<TournamentMatch> existingMatches =
                matchRepository.findByTournamentIdOrderByRoundNumberAscMatchNumberAscIdAsc(tournamentId);
        if (!existingMatches.isEmpty()) {
            return existingMatches.stream()
                    .map(this::mapToMatchResponse)
                    .toList();
        }

        List<TournamentTeam> teams = getSortedTournamentTeams(tournamentId);
        if (teams.size() < 2) {
            throw new RuntimeException("At least two formed teams are required to generate matches");
        }

        FormatStage firstStage = resolveFormatStages(tournament).get(0);
        String bracketType = resolveInitialBracketType(firstStage.format());
        List<MatchPair> matchPairs = buildStageMatchPairs(teams, firstStage.format(), firstStage.name());
        if (matchPairs.isEmpty()) {
            throw new RuntimeException("No matches could be generated for this tournament format");
        }

        List<TournamentMatch> matches = new ArrayList<>();
        for (int index = 0; index < matchPairs.size(); index++) {
            matches.add(createMatchFromPair(
                    tournament,
                    matchPairs.get(index),
                    firstStage.order(),
                    firstStage.name(),
                    1,
                    index + 1,
                    bracketType
            ));
        }

        return matchRepository.saveAll(matches)
                .stream()
                .map(this::mapToMatchResponse)
                .toList();
    }

    @Transactional
    public List<TournamentMatchResponse> submitMatchResult(
            Long tournamentId,
            Long matchId,
            MatchResultRequest request
    ) {
        TournamentMatch match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));
        if (!match.getTournament().getId().equals(tournamentId)) {
            throw new RuntimeException("Match does not belong to this tournament");
        }
        if (TournamentMatchStatus.COMPLETED.equals(match.getStatus())) {
            throw new RuntimeException("This match result has already been submitted");
        }

        Long winnerTeamId = request.getWinnerTeamId();
        boolean teamAWon = match.getTeamA().getId().equals(winnerTeamId);
        boolean teamBWon = match.getTeamB().getId().equals(winnerTeamId);
        if (!teamAWon && !teamBWon) {
            throw new RuntimeException("Winner must be one of the match teams");
        }
        validateWinnerScore(teamAWon, request.getTeamAScore(), request.getTeamBScore());

        match.setTeamAScore(request.getTeamAScore());
        match.setTeamBScore(request.getTeamBScore());
        match.setWinner(teamAWon ? match.getTeamA() : match.getTeamB());
        match.setStatus(TournamentMatchStatus.COMPLETED);
        matchRepository.save(match);

        createNextMatchesIfReady(
                match.getTournament(),
                match.getStageNumber() == null ? 1 : match.getStageNumber(),
                match.getStageName(),
                match.getBracketType(),
                match.getRoundNumber()
        );

        return matchRepository.findByTournamentIdOrderByRoundNumberAscMatchNumberAscIdAsc(tournamentId)
                .stream()
                .map(this::mapToMatchResponse)
                .toList();
    }

    private TournamentResponse mapToResponse(Tournament tournament) {
        return TournamentResponse.builder()
                .id(tournament.getId())
                .name(tournament.getName())
                .game(tournament.getGame())
                .format(tournament.getFormat())
                .formatPlan(tournament.getFormatPlan())
                .teamSize(tournament.getTeamSize())
                .auctionEnabled(Boolean.TRUE.equals(tournament.getAuctionEnabled()))
                .region(tournament.getRegion())
                .entryFee(tournament.getEntryFee())
                .prizePool(tournament.getPrizePool())
                .registrationOpenAt(tournament.getRegistrationOpenAt())
                .registrationCloseAt(tournament.getRegistrationCloseAt())
                .startAt(tournament.getStartAt())
                .endAt(tournament.getEndAt())
                .status(tournament.getStatus())
                .description(tournament.getDescription())
                .rules(tournament.getRules())
                .bannerUrl(tournament.getBannerUrl())
                .build();
    }

    private TournamentRegistrationResponse mapToRegistrationResponse(
            TournamentRegistration registration
    ) {
        AppUser user = registration.getUser();

        return TournamentRegistrationResponse.builder()
                .id(registration.getId())
                .tournamentId(registration.getTournament().getId())
                .userId(user == null ? null : user.getId())
                .name(registration.getName())
                .email(user == null ? null : user.getEmail())
                .phoneNumber(registration.getPhoneNumber())
                .discord(registration.getDiscord())
                .rank(registration.getRank())
                .availabilitySlots(mapAvailabilityResponses(registration.getAvailabilitySlots()))
                .captain(registration.isCaptain())
                .createdAt(registration.getCreatedAt())
                .build();
    }

    private TournamentTeamResponse mapToTeamResponse(TournamentTeam team) {
        AppUser captainUser = team.getCaptainUser();

        return TournamentTeamResponse.builder()
                .id(team.getId())
                .tournamentId(team.getTournament().getId())
                .captainUserId(captainUser == null ? null : captainUser.getId())
                .teamName(team.getTeamName())
                .createdAt(team.getCreatedAt())
                .players(team.getPlayers()
                        .stream()
                        .map(this::mapToTeamPlayerResponse)
                        .toList())
                .build();
    }

    private TournamentTeamPlayerResponse mapToTeamPlayerResponse(TournamentTeamPlayer player) {
        AppUser user = player.getUser();

        return TournamentTeamPlayerResponse.builder()
                .id(player.getId())
                .userId(user == null ? null : user.getId())
                .name(player.getName())
                .email(player.getEmail())
                .phoneNumber(player.getPhoneNumber())
                .discord(player.getDiscord())
                .rank(player.getRank())
                .availabilitySlots(mapAvailabilityResponses(player.getAvailabilitySlots()))
                .bidAmount(player.getBidAmount())
                .captain(player.isCaptain())
                .build();
    }

    private TournamentMatchResponse mapToMatchResponse(TournamentMatch match) {
        TournamentTeam winner = match.getWinner();

        return TournamentMatchResponse.builder()
                .id(match.getId())
                .tournamentId(match.getTournament().getId())
                .teamAId(match.getTeamA().getId())
                .teamAName(match.getTeamA().getTeamName())
                .teamBId(match.getTeamB().getId())
                .teamBName(match.getTeamB().getTeamName())
                .winnerTeamId(winner == null ? null : winner.getId())
                .winnerTeamName(winner == null ? null : winner.getTeamName())
                .stageNumber(match.getStageNumber() == null ? 1 : match.getStageNumber())
                .stageName(match.getStageName())
                .roundNumber(match.getRoundNumber())
                .matchNumber(match.getMatchNumber())
                .roundName(match.getRoundName())
                .bracketType(match.getBracketType())
                .scheduledStartUtc(match.getScheduledStartUtc())
                .scheduledEndUtc(match.getScheduledEndUtc())
                .availablePlayers(match.getAvailablePlayers())
                .totalPlayers(match.getTotalPlayers())
                .coverageScore(match.getCoverageScore())
                .missingPlayers(match.getMissingPlayers() == null ? List.of() : match.getMissingPlayers())
                .teamAScore(match.getTeamAScore())
                .teamBScore(match.getTeamBScore())
                .status(match.getStatus() == null ? null : match.getStatus().name())
                .build();
    }

    private TournamentTeamPlayer mapRegistrationToTeamPlayer(
            TournamentRegistration registration,
            boolean captain,
            Double bidAmount
    ) {
        AppUser user = registration.getUser();

        return TournamentTeamPlayer.builder()
                .user(user)
                .name(registration.getName())
                .email(user == null ? null : user.getEmail())
                .phoneNumber(registration.getPhoneNumber())
                .discord(registration.getDiscord())
                .rank(registration.getRank())
                .availabilitySlots(copyAvailabilitySlots(registration.getAvailabilitySlots()))
                .bidAmount(captain ? null : bidAmount)
                .captain(captain)
                .build();
    }

    private List<MatchPair> buildMatchPairs(List<TournamentTeam> teams, String format) {
        String normalizedFormat = normalizeFormat(format);
        List<MatchPair> pairs = new ArrayList<>();

        if ("ROUND_ROBIN".equals(normalizedFormat)) {
            for (int i = 0; i < teams.size(); i++) {
                for (int j = i + 1; j < teams.size(); j++) {
                    pairs.add(new MatchPair(teams.get(i), teams.get(j), "Round Robin"));
                }
            }
            return pairs;
        }

        if ("GROUP_STAGE_PLAYOFFS".equals(normalizedFormat)) {
            for (int i = 0; i < teams.size(); i++) {
                for (int j = i + 1; j < teams.size(); j++) {
                    pairs.add(new MatchPair(teams.get(i), teams.get(j), "Group Stage"));
                }
            }
            return pairs;
        }

        String roundName = switch (normalizedFormat) {
            case "DOUBLE_ELIMINATION" -> "Upper Bracket Round 1";
            case "SWISS" -> "Swiss Round 1";
            default -> "Round 1";
        };

        for (int index = 0; index + 1 < teams.size(); index += 2) {
            pairs.add(new MatchPair(teams.get(index), teams.get(index + 1), roundName));
        }

        return pairs;
    }

    private List<MatchPair> buildStageMatchPairs(
            List<TournamentTeam> teams,
            String format,
            String stageName
    ) {
        String normalizedFormat = normalizeFormat(format);
        String roundName = requireValue(stageName, "Stage name is required");

        if ("ROUND_ROBIN".equals(normalizedFormat) || "GROUP_STAGE_PLAYOFFS".equals(normalizedFormat)) {
            List<MatchPair> pairs = new ArrayList<>();
            for (int i = 0; i < teams.size(); i++) {
                for (int j = i + 1; j < teams.size(); j++) {
                    pairs.add(new MatchPair(teams.get(i), teams.get(j), roundName));
                }
            }
            return pairs;
        }

        if ("SWISS".equals(normalizedFormat)) {
            return buildSeededPairs(teams, roundName);
        }

        return buildSeededPairs(teams, roundName);
    }

    private List<MatchPair> buildSeededPairs(List<TournamentTeam> teams, String roundName) {
        List<MatchPair> pairs = new ArrayList<>();
        for (int index = 0; index < teams.size() / 2; index++) {
            pairs.add(new MatchPair(
                    teams.get(index),
                    teams.get(teams.size() - index - 1),
                    roundName
            ));
        }

        return pairs;
    }

    private List<TournamentTeam> getSortedTournamentTeams(Long tournamentId) {
        return teamRepository.findByTournamentIdOrderByCreatedAtDesc(tournamentId)
                .stream()
                .sorted(Comparator.comparing(TournamentTeam::getId))
                .toList();
    }

    private String resolveInitialBracketType(String format) {
        return switch (normalizeFormat(format)) {
            case "ROUND_ROBIN" -> BRACKET_ROUND_ROBIN;
            case "GROUP_STAGE_PLAYOFFS" -> BRACKET_GROUP_STAGE;
            case "SWISS" -> BRACKET_SWISS;
            case "DOUBLE_ELIMINATION" -> BRACKET_UPPER_BRACKET;
            default -> BRACKET_SINGLE_ELIMINATION;
        };
    }

    private TournamentMatch createMatchFromPair(
            Tournament tournament,
            MatchPair matchPair,
            int stageNumber,
            String stageName,
            int roundNumber,
            int matchNumber,
            String bracketType
    ) {
        TournamentScheduleMatchResponse scheduledMatch = scoreMatch(tournament, matchPair, matchNumber);

        return TournamentMatch.builder()
                .tournament(tournament)
                .teamA(matchPair.teamA())
                .teamB(matchPair.teamB())
                .stageNumber(stageNumber)
                .stageName(stageName)
                .roundNumber(roundNumber)
                .matchNumber(matchNumber)
                .roundName(matchPair.roundName())
                .bracketType(bracketType)
                .scheduledStartUtc(scheduledMatch.getRecommendedStartUtc())
                .scheduledEndUtc(scheduledMatch.getRecommendedEndUtc())
                .availablePlayers(scheduledMatch.getAvailablePlayers())
                .totalPlayers(scheduledMatch.getTotalPlayers())
                .coverageScore(scheduledMatch.getCoverageScore())
                .missingPlayers(new ArrayList<>(
                        scheduledMatch.getMissingPlayers() == null
                                ? List.of()
                                : scheduledMatch.getMissingPlayers()
                ))
                .status(TournamentMatchStatus.SCHEDULED)
                .build();
    }

    private void createNextMatchesIfReady(
            Tournament tournament,
            int stageNumber,
            String stageName,
            String bracketType,
            Integer roundNumber
    ) {
        if (roundNumber == null || bracketType == null) {
            return;
        }

        boolean createdNextRound = false;
        if (BRACKET_SWISS.equals(bracketType)) {
            createdNextRound = createNextSwissRoundIfReady(tournament, stageNumber, stageName, roundNumber);
        } else if (BRACKET_UPPER_BRACKET.equals(bracketType)
                || BRACKET_LOWER_BRACKET.equals(bracketType)) {
            createNextDoubleEliminationMatchesIfReady(tournament, stageNumber, stageName, bracketType, roundNumber);
            return;
        } else if (BRACKET_GRAND_FINAL.equals(bracketType)) {
            createNextStageIfReady(tournament, stageNumber);
            return;
        } else if (BRACKET_SINGLE_ELIMINATION.equals(bracketType)) {
            createdNextRound = createNextEliminationRoundIfReady(
                    tournament,
                    stageNumber,
                    stageName,
                    bracketType,
                    roundNumber
            );
        }

        if (!createdNextRound) {
            createNextStageIfReady(tournament, stageNumber);
        }
    }

    private boolean createNextEliminationRoundIfReady(
            Tournament tournament,
            int stageNumber,
            String stageName,
            String bracketType,
            int roundNumber
    ) {
        List<TournamentMatch> currentRoundMatches =
                matchRepository.findByTournamentIdAndStageNumberAndBracketTypeAndRoundNumberOrderByMatchNumberAsc(
                        tournament.getId(),
                        stageNumber,
                        bracketType,
                        roundNumber
                );
        if (currentRoundMatches.isEmpty()
                || currentRoundMatches.stream().anyMatch(match -> !TournamentMatchStatus.COMPLETED.equals(match.getStatus()))) {
            return false;
        }

        int nextRound = roundNumber + 1;
        if (matchRepository.existsByTournamentIdAndStageNumberAndBracketTypeAndRoundNumber(
                tournament.getId(),
                stageNumber,
                bracketType,
                nextRound
        )) {
            return false;
        }

        List<TournamentTeam> winners = currentRoundMatches.stream()
                .map(TournamentMatch::getWinner)
                .filter(winner -> winner != null)
                .toList();
        if (winners.size() < 2) {
            return false;
        }

        String roundName = resolveEliminationRoundName(bracketType, nextRound);
        List<TournamentMatch> nextMatches = new ArrayList<>();
        for (int index = 0; index + 1 < winners.size(); index += 2) {
            nextMatches.add(createMatchFromPair(
                    tournament,
                    new MatchPair(winners.get(index), winners.get(index + 1), roundName),
                    stageNumber,
                    stageName,
                    nextRound,
                    nextMatches.size() + 1,
                    bracketType
            ));
        }

        if (!nextMatches.isEmpty()) {
            matchRepository.saveAll(nextMatches);
            return true;
        }

        return false;
    }

    private boolean createNextSwissRoundIfReady(
            Tournament tournament,
            int stageNumber,
            String stageName,
            int roundNumber
    ) {
        List<TournamentMatch> currentRoundMatches =
                matchRepository.findByTournamentIdAndStageNumberAndBracketTypeAndRoundNumberOrderByMatchNumberAsc(
                        tournament.getId(),
                        stageNumber,
                        BRACKET_SWISS,
                        roundNumber
                );
        if (currentRoundMatches.isEmpty()
                || currentRoundMatches.stream().anyMatch(match -> !TournamentMatchStatus.COMPLETED.equals(match.getStatus()))) {
            return false;
        }

        List<TournamentTeam> teams = getStageTeams(
                matchRepository.findByTournamentIdAndStageNumberOrderByRoundNumberAscMatchNumberAscIdAsc(
                        tournament.getId(),
                        stageNumber
                )
        );
        int maxRounds = calculateSwissRoundCount(teams.size());
        int nextRound = roundNumber + 1;
        if (roundNumber >= maxRounds
                || matchRepository.existsByTournamentIdAndStageNumberAndBracketTypeAndRoundNumber(
                        tournament.getId(),
                        stageNumber,
                        BRACKET_SWISS,
                        nextRound
                )) {
            return false;
        }

        List<TournamentMatch> allMatches =
                matchRepository.findByTournamentIdAndStageNumberOrderByRoundNumberAscMatchNumberAscIdAsc(
                        tournament.getId(),
                        stageNumber
                );
        Map<Long, Integer> winCounts = buildWinCounts(teams, allMatches);
        List<MatchPair> pairs = buildSwissPairs(teams, winCounts, allMatches, nextRound);
        List<TournamentMatch> nextMatches = new ArrayList<>();

        for (MatchPair pair : pairs) {
            nextMatches.add(createMatchFromPair(
                    tournament,
                    pair,
                    stageNumber,
                    stageName,
                    nextRound,
                    nextMatches.size() + 1,
                    BRACKET_SWISS
            ));
        }

        if (!nextMatches.isEmpty()) {
            matchRepository.saveAll(nextMatches);
            return true;
        }

        return false;
    }

    private void createNextDoubleEliminationMatchesIfReady(
            Tournament tournament,
            int stageNumber,
            String stageName,
            String completedBracketType,
            int completedRoundNumber
    ) {
        if (BRACKET_UPPER_BRACKET.equals(completedBracketType)) {
            createNextEliminationRoundIfReady(
                    tournament,
                    stageNumber,
                    stageName,
                    BRACKET_UPPER_BRACKET,
                    completedRoundNumber
            );
        }

        boolean createdLowerRound = createLowerBracketRoundIfReady(tournament, stageNumber, stageName);
        if (!createdLowerRound) {
            createGrandFinalIfReady(tournament, stageNumber, stageName);
        }
    }

    private boolean createLowerBracketRoundIfReady(
            Tournament tournament,
            int stageNumber,
            String stageName
    ) {
        List<TournamentMatch> stageMatches =
                matchRepository.findByTournamentIdAndStageNumberOrderByRoundNumberAscMatchNumberAscIdAsc(
                        tournament.getId(),
                        stageNumber
                );
        int nextLowerRound = getMaxRound(stageMatches, BRACKET_LOWER_BRACKET) + 1;
        if (bracketRoundExists(stageMatches, BRACKET_LOWER_BRACKET, nextLowerRound)) {
            return false;
        }

        List<MatchPair> pairs;
        if (nextLowerRound == 1) {
            List<TournamentTeam> upperRoundOneLosers =
                    getCompletedRoundTeams(stageMatches, BRACKET_UPPER_BRACKET, 1, false);
            if (upperRoundOneLosers == null || upperRoundOneLosers.size() < 2) {
                return false;
            }
            pairs = buildSeededPairs(upperRoundOneLosers, "Lower Bracket Round 1");
        } else if (nextLowerRound % 2 == 0) {
            List<TournamentTeam> previousLowerWinners =
                    getCompletedRoundTeams(stageMatches, BRACKET_LOWER_BRACKET, nextLowerRound - 1, true);
            List<TournamentTeam> upperLosers =
                    getCompletedRoundTeams(
                            stageMatches,
                            BRACKET_UPPER_BRACKET,
                            (nextLowerRound / 2) + 1,
                            false
                    );
            if (previousLowerWinners == null
                    || upperLosers == null
                    || previousLowerWinners.isEmpty()
                    || upperLosers.isEmpty()) {
                return false;
            }
            pairs = buildCrossPairs(
                    previousLowerWinners,
                    upperLosers,
                    "Lower Bracket Round " + nextLowerRound
            );
        } else {
            List<TournamentTeam> previousLowerWinners =
                    getCompletedRoundTeams(stageMatches, BRACKET_LOWER_BRACKET, nextLowerRound - 1, true);
            if (previousLowerWinners == null || previousLowerWinners.size() < 2) {
                return false;
            }
            pairs = buildSeededPairs(previousLowerWinners, "Lower Bracket Round " + nextLowerRound);
        }

        if (pairs.isEmpty()) {
            return false;
        }

        List<TournamentMatch> matches = new ArrayList<>();
        for (MatchPair pair : pairs) {
            matches.add(createMatchFromPair(
                    tournament,
                    pair,
                    stageNumber,
                    stageName,
                    nextLowerRound,
                    matches.size() + 1,
                    BRACKET_LOWER_BRACKET
            ));
        }
        matchRepository.saveAll(matches);

        return true;
    }

    private boolean createGrandFinalIfReady(
            Tournament tournament,
            int stageNumber,
            String stageName
    ) {
        List<TournamentMatch> stageMatches =
                matchRepository.findByTournamentIdAndStageNumberOrderByRoundNumberAscMatchNumberAscIdAsc(
                        tournament.getId(),
                        stageNumber
                );
        if (stageMatches.stream().anyMatch(match -> BRACKET_GRAND_FINAL.equals(match.getBracketType()))) {
            return false;
        }

        TournamentTeam upperChampion = getUpperBracketChampion(stageMatches);
        TournamentTeam lowerChampion = getLowerBracketChampion(stageMatches);
        if (upperChampion == null
                || lowerChampion == null
                || upperChampion.getId().equals(lowerChampion.getId())) {
            return false;
        }

        TournamentMatch grandFinal = createMatchFromPair(
                tournament,
                new MatchPair(upperChampion, lowerChampion, "Grand Final"),
                stageNumber,
                stageName,
                1,
                1,
                BRACKET_GRAND_FINAL
        );
        matchRepository.save(grandFinal);

        return true;
    }

    private List<MatchPair> buildCrossPairs(
            List<TournamentTeam> firstTeams,
            List<TournamentTeam> secondTeams,
            String roundName
    ) {
        List<MatchPair> pairs = new ArrayList<>();
        int pairCount = Math.min(firstTeams.size(), secondTeams.size());
        for (int index = 0; index < pairCount; index++) {
            pairs.add(new MatchPair(firstTeams.get(index), secondTeams.get(index), roundName));
        }

        return pairs;
    }

    private List<TournamentTeam> getCompletedRoundTeams(
            List<TournamentMatch> matches,
            String bracketType,
            int roundNumber,
            boolean winners
    ) {
        List<TournamentMatch> roundMatches = matches.stream()
                .filter(match -> bracketType.equals(match.getBracketType()))
                .filter(match -> Integer.valueOf(roundNumber).equals(match.getRoundNumber()))
                .sorted(Comparator.comparing(TournamentMatch::getMatchNumber))
                .toList();
        if (roundMatches.isEmpty()
                || roundMatches.stream().anyMatch(match -> !TournamentMatchStatus.COMPLETED.equals(match.getStatus()))) {
            return null;
        }

        return roundMatches.stream()
                .map(match -> winners ? match.getWinner() : getMatchLoser(match))
                .filter(team -> team != null)
                .toList();
    }

    private TournamentTeam getUpperBracketChampion(List<TournamentMatch> stageMatches) {
        int maxUpperRound = getMaxRound(stageMatches, BRACKET_UPPER_BRACKET);
        if (maxUpperRound == 0) {
            return null;
        }

        List<TournamentTeam> upperWinners =
                getCompletedRoundTeams(stageMatches, BRACKET_UPPER_BRACKET, maxUpperRound, true);
        if (upperWinners == null || upperWinners.size() != 1) {
            return null;
        }

        return upperWinners.get(0);
    }

    private TournamentTeam getLowerBracketChampion(List<TournamentMatch> stageMatches) {
        int maxLowerRound = getMaxRound(stageMatches, BRACKET_LOWER_BRACKET);
        if (maxLowerRound > 0) {
            List<TournamentTeam> lowerWinners =
                    getCompletedRoundTeams(stageMatches, BRACKET_LOWER_BRACKET, maxLowerRound, true);
            if (lowerWinners != null && lowerWinners.size() == 1) {
                return lowerWinners.get(0);
            }
            return null;
        }

        List<TournamentTeam> upperFinalLosers =
                getCompletedRoundTeams(
                        stageMatches,
                        BRACKET_UPPER_BRACKET,
                        getMaxRound(stageMatches, BRACKET_UPPER_BRACKET),
                        false
                );
        if (upperFinalLosers != null && upperFinalLosers.size() == 1) {
            return upperFinalLosers.get(0);
        }

        return null;
    }

    private TournamentTeam getMatchLoser(TournamentMatch match) {
        TournamentTeam winner = match.getWinner();
        if (winner == null) {
            return null;
        }
        if (winner.getId().equals(match.getTeamA().getId())) {
            return match.getTeamB();
        }
        if (winner.getId().equals(match.getTeamB().getId())) {
            return match.getTeamA();
        }

        return null;
    }

    private int getMaxRound(List<TournamentMatch> matches, String bracketType) {
        return matches.stream()
                .filter(match -> bracketType.equals(match.getBracketType()))
                .map(TournamentMatch::getRoundNumber)
                .filter(round -> round != null)
                .max(Integer::compareTo)
                .orElse(0);
    }

    private boolean bracketRoundExists(
            List<TournamentMatch> matches,
            String bracketType,
            int roundNumber
    ) {
        return matches.stream()
                .anyMatch(match -> bracketType.equals(match.getBracketType())
                        && Integer.valueOf(roundNumber).equals(match.getRoundNumber()));
    }

    private void createNextStageIfReady(Tournament tournament, int stageNumber) {
        List<FormatStage> stages = resolveFormatStages(tournament);
        int currentStageIndex = findStageIndex(stages, stageNumber);
        if (currentStageIndex < 0 || currentStageIndex + 1 >= stages.size()) {
            return;
        }

        FormatStage currentStage = stages.get(currentStageIndex);
        FormatStage nextStage = stages.get(currentStageIndex + 1);
        if (matchRepository.existsByTournamentIdAndStageNumber(tournament.getId(), nextStage.order())) {
            return;
        }

        List<TournamentMatch> currentStageMatches =
                matchRepository.findByTournamentIdAndStageNumberOrderByRoundNumberAscMatchNumberAscIdAsc(
                        tournament.getId(),
                        stageNumber
                );
        if (currentStageMatches.isEmpty()
                || currentStageMatches.stream().anyMatch(match -> !TournamentMatchStatus.COMPLETED.equals(match.getStatus()))) {
            return;
        }

        List<TournamentTeam> advancingTeams = selectAdvancingTeams(currentStageMatches, currentStage);
        if (advancingTeams.size() < 2) {
            return;
        }

        String bracketType = resolveInitialBracketType(nextStage.format());
        List<MatchPair> matchPairs = buildStageMatchPairs(
                advancingTeams,
                nextStage.format(),
                nextStage.name()
        );
        List<TournamentMatch> nextStageMatches = new ArrayList<>();

        for (MatchPair pair : matchPairs) {
            nextStageMatches.add(createMatchFromPair(
                    tournament,
                    pair,
                    nextStage.order(),
                    nextStage.name(),
                    1,
                    nextStageMatches.size() + 1,
                    bracketType
            ));
        }

        if (!nextStageMatches.isEmpty()) {
            matchRepository.saveAll(nextStageMatches);
        }
    }

    private int findStageIndex(List<FormatStage> stages, int stageNumber) {
        for (int index = 0; index < stages.size(); index++) {
            if (stages.get(index).order() == stageNumber) {
                return index;
            }
        }

        return -1;
    }

    private List<TournamentTeam> selectAdvancingTeams(
            List<TournamentMatch> stageMatches,
            FormatStage stage
    ) {
        List<TournamentTeam> stageTeams = getStageTeams(stageMatches);
        int advanceCount = stage.advanceCount();
        if (advanceCount <= 0 && stage.eliminateCount() > 0) {
            advanceCount = Math.max(2, stageTeams.size() - stage.eliminateCount());
        }
        if (advanceCount <= 0) {
            advanceCount = stageTeams.size();
        }

        String format = normalizeFormat(stage.format());
        if ("DOUBLE_ELIMINATION".equals(format)) {
            List<TournamentTeam> grandFinalWinners = stageMatches.stream()
                    .filter(match -> BRACKET_GRAND_FINAL.equals(match.getBracketType()))
                    .filter(match -> TournamentMatchStatus.COMPLETED.equals(match.getStatus()))
                    .map(TournamentMatch::getWinner)
                    .filter(winner -> winner != null)
                    .toList();
            if (!grandFinalWinners.isEmpty()) {
                return grandFinalWinners.stream()
                        .limit(advanceCount)
                        .toList();
            }
        }

        if ("SINGLE_ELIMINATION".equals(format)) {
            int lastRound = stageMatches.stream()
                    .map(TournamentMatch::getRoundNumber)
                    .filter(round -> round != null)
                    .max(Integer::compareTo)
                    .orElse(1);

            List<TournamentTeam> finalWinners = stageMatches.stream()
                    .filter(match -> Integer.valueOf(lastRound).equals(match.getRoundNumber()))
                    .map(TournamentMatch::getWinner)
                    .filter(winner -> winner != null)
                    .toList();

            if (!finalWinners.isEmpty()) {
                return finalWinners.stream()
                        .limit(advanceCount)
                        .toList();
            }
        }

        return buildStandings(stageMatches).stream()
                .map(TeamStanding::team)
                .limit(advanceCount)
                .toList();
    }

    private List<TournamentTeam> getStageTeams(List<TournamentMatch> matches) {
        Map<Long, TournamentTeam> teamsById = new LinkedHashMap<>();
        for (TournamentMatch match : matches) {
            teamsById.put(match.getTeamA().getId(), match.getTeamA());
            teamsById.put(match.getTeamB().getId(), match.getTeamB());
        }

        return new ArrayList<>(teamsById.values());
    }

    private List<TeamStanding> buildStandings(List<TournamentMatch> matches) {
        Map<Long, TeamStandingBuilder> standings = new LinkedHashMap<>();

        for (TournamentMatch match : matches) {
            TeamStandingBuilder teamA = standings.computeIfAbsent(
                    match.getTeamA().getId(),
                    teamId -> new TeamStandingBuilder(match.getTeamA())
            );
            TeamStandingBuilder teamB = standings.computeIfAbsent(
                    match.getTeamB().getId(),
                    teamId -> new TeamStandingBuilder(match.getTeamB())
            );

            if (!TournamentMatchStatus.COMPLETED.equals(match.getStatus())
                    || match.getTeamAScore() == null
                    || match.getTeamBScore() == null) {
                continue;
            }

            teamA.matches++;
            teamB.matches++;
            teamA.scoreFor += match.getTeamAScore();
            teamA.scoreAgainst += match.getTeamBScore();
            teamB.scoreFor += match.getTeamBScore();
            teamB.scoreAgainst += match.getTeamAScore();

            if (match.getTeamAScore() > match.getTeamBScore()) {
                teamA.wins++;
                teamB.losses++;
            } else {
                teamB.wins++;
                teamA.losses++;
            }
        }

        return standings.values()
                .stream()
                .map(TeamStandingBuilder::build)
                .sorted(Comparator
                        .comparing(TeamStanding::wins, Comparator.reverseOrder())
                        .thenComparing(TeamStanding::scoreDifference, Comparator.reverseOrder())
                        .thenComparing(TeamStanding::scoreFor, Comparator.reverseOrder())
                        .thenComparing(standing -> standing.team().getTeamName()))
                .toList();
    }

    private List<FormatStage> resolveFormatStages(Tournament tournament) {
        String plan = trimToNull(tournament.getFormatPlan());
        if (plan != null) {
            try {
                JsonNode root = objectMapper.readTree(plan);
                if (root.isArray() && !root.isEmpty()) {
                    List<FormatStage> stages = new ArrayList<>();
                    for (int index = 0; index < root.size(); index++) {
                        JsonNode node = root.get(index);
                        String format = normalizeFormat(node.path("format").asText(tournament.getFormat()));
                        if (format.isEmpty()) {
                            format = normalizeFormat(tournament.getFormat());
                        }
                        stages.add(new FormatStage(
                                index + 1,
                                requireValue(
                                        node.path("name").asText(defaultStageName(format, index + 1)),
                                        "Stage name is required"
                                ),
                                format,
                                Math.max(0, node.path("advanceCount").asInt(0)),
                                Math.max(0, node.path("eliminateCount").asInt(0))
                        ));
                    }

                    return stages;
                }
            } catch (Exception ignored) {
                return defaultFormatStages(tournament);
            }
        }

        return defaultFormatStages(tournament);
    }

    private List<FormatStage> defaultFormatStages(Tournament tournament) {
        String format = normalizeFormat(tournament.getFormat());
        if ("GROUP_STAGE_PLAYOFFS".equals(format)) {
            return List.of(
                    new FormatStage(1, "Group Stage", "ROUND_ROBIN", 4, 0),
                    new FormatStage(2, "Playoffs", "SINGLE_ELIMINATION", 0, 0)
            );
        }

        return List.of(new FormatStage(1, defaultStageName(format, 1), format, 0, 0));
    }

    private String defaultStageName(String format, int order) {
        return switch (normalizeFormat(format)) {
            case "ROUND_ROBIN" -> order == 1 ? "Round Robin" : "Round Robin Stage";
            case "SWISS" -> "Swiss Stage";
            case "DOUBLE_ELIMINATION" -> "Double Elimination";
            case "GROUP_STAGE_PLAYOFFS" -> "Group Stage";
            default -> order == 1 ? "Playoffs" : "Playoffs Stage";
        };
    }

    private int calculateSwissRoundCount(int teamCount) {
        if (teamCount < 2) {
            return 0;
        }

        return Math.max(1, (int) Math.ceil(Math.log(teamCount) / Math.log(2)));
    }

    private Map<Long, Integer> buildWinCounts(
            List<TournamentTeam> teams,
            List<TournamentMatch> matches
    ) {
        Map<Long, Integer> winCounts = new LinkedHashMap<>();
        for (TournamentTeam team : teams) {
            winCounts.put(team.getId(), 0);
        }

        for (TournamentMatch match : matches) {
            TournamentTeam winner = match.getWinner();
            if (TournamentMatchStatus.COMPLETED.equals(match.getStatus()) && winner != null) {
                winCounts.computeIfPresent(winner.getId(), (teamId, wins) -> wins + 1);
            }
        }

        return winCounts;
    }

    private List<MatchPair> buildSwissPairs(
            List<TournamentTeam> teams,
            Map<Long, Integer> winCounts,
            List<TournamentMatch> previousMatches,
            int roundNumber
    ) {
        Set<String> previousPairs = previousMatches.stream()
                .map(match -> pairKey(match.getTeamA().getId(), match.getTeamB().getId()))
                .collect(Collectors.toSet());
        List<TournamentTeam> sortedTeams = teams.stream()
                .sorted(Comparator
                        .comparing(
                                (TournamentTeam team) -> winCounts.getOrDefault(team.getId(), 0),
                                Comparator.reverseOrder()
                        )
                        .thenComparing(TournamentTeam::getId))
                .toList();
        Set<Long> usedTeamIds = new HashSet<>();
        List<MatchPair> pairs = new ArrayList<>();

        for (TournamentTeam team : sortedTeams) {
            if (usedTeamIds.contains(team.getId())) {
                continue;
            }

            TournamentTeam opponent = sortedTeams.stream()
                    .filter(candidate -> !candidate.getId().equals(team.getId()))
                    .filter(candidate -> !usedTeamIds.contains(candidate.getId()))
                    .filter(candidate -> !previousPairs.contains(pairKey(team.getId(), candidate.getId())))
                    .findFirst()
                    .orElseGet(() -> sortedTeams.stream()
                            .filter(candidate -> !candidate.getId().equals(team.getId()))
                            .filter(candidate -> !usedTeamIds.contains(candidate.getId()))
                            .findFirst()
                            .orElse(null));

            if (opponent != null) {
                usedTeamIds.add(team.getId());
                usedTeamIds.add(opponent.getId());
                pairs.add(new MatchPair(team, opponent, "Swiss Round " + roundNumber));
            }
        }

        return pairs;
    }

    private String resolveEliminationRoundName(String bracketType, int roundNumber) {
        if (BRACKET_UPPER_BRACKET.equals(bracketType)) {
            return "Upper Bracket Round " + roundNumber;
        }

        return "Round " + roundNumber;
    }

    private String pairKey(Long teamAId, Long teamBId) {
        Long first = Math.min(teamAId, teamBId);
        Long second = Math.max(teamAId, teamBId);

        return first + ":" + second;
    }

    private void validateWinnerScore(boolean teamAWon, Integer teamAScore, Integer teamBScore) {
        if (teamAScore == null || teamBScore == null) {
            throw new RuntimeException("Both team scores are required");
        }
        if (teamAScore.equals(teamBScore)) {
            throw new RuntimeException("A completed tournament match cannot have a tied score");
        }
        if (teamAWon && teamAScore <= teamBScore) {
            throw new RuntimeException("Team A score must be higher when Team A is selected as winner");
        }
        if (!teamAWon && teamBScore <= teamAScore) {
            throw new RuntimeException("Team B score must be higher when Team B is selected as winner");
        }
    }

    private TournamentScheduleMatchResponse scoreMatch(
            Tournament tournament,
            MatchPair matchPair,
            int matchNumber
    ) {
        List<TournamentTeamPlayer> players = new ArrayList<>();
        players.addAll(matchPair.teamA().getPlayers());
        players.addAll(matchPair.teamB().getPlayers());

        Instant tournamentStart = tournament.getStartAt() == null
                ? Instant.now()
                : tournament.getStartAt().atZone(ZoneId.systemDefault()).toInstant();
        Instant tournamentEnd = tournament.getEndAt() == null
                ? tournamentStart.plus(Duration.ofDays(14))
                : tournament.getEndAt().atZone(ZoneId.systemDefault()).toInstant();
        TreeSet<Instant> candidates = buildCandidateStartTimes(players, tournamentStart, tournamentEnd);

        if (candidates.isEmpty()) {
            candidates.add(tournamentStart);
        }

        Instant bestStart = candidates.first();
        int bestAvailable = -1;
        List<String> bestMissing = List.of();

        for (Instant candidate : candidates) {
            Instant candidateEnd = candidate.plus(MATCH_DURATION);
            List<String> missingPlayers = players.stream()
                    .filter(player -> !isPlayerAvailable(player, candidate, candidateEnd))
                    .map(TournamentTeamPlayer::getName)
                    .toList();
            int available = players.size() - missingPlayers.size();

            if (available > bestAvailable) {
                bestAvailable = available;
                bestStart = candidate;
                bestMissing = missingPlayers;
            }
        }

        int totalPlayers = players.size();

        return TournamentScheduleMatchResponse.builder()
                .matchNumber(matchNumber)
                .roundName(matchPair.roundName())
                .teamAId(matchPair.teamA().getId())
                .teamAName(matchPair.teamA().getTeamName())
                .teamBId(matchPair.teamB().getId())
                .teamBName(matchPair.teamB().getTeamName())
                .recommendedStartUtc(bestStart)
                .recommendedEndUtc(bestStart.plus(MATCH_DURATION))
                .availablePlayers(Math.max(bestAvailable, 0))
                .totalPlayers(totalPlayers)
                .coverageScore(totalPlayers == 0 ? 0 : Math.round((bestAvailable * 10000.0) / totalPlayers) / 100.0)
                .missingPlayers(bestMissing)
                .build();
    }

    private TreeSet<Instant> buildCandidateStartTimes(
            List<TournamentTeamPlayer> players,
            Instant tournamentStart,
            Instant tournamentEnd
    ) {
        TreeSet<Instant> candidates = new TreeSet<>();

        for (TournamentTeamPlayer player : players) {
            for (PlayerAvailabilitySlot slot : player.getAvailabilitySlots()) {
                Instant slotStart = toInstant(slot.getStartAt(), slot.getTimeZone());
                Instant slotEnd = toInstant(slot.getEndAt(), slot.getTimeZone());
                Instant candidate = slotStart.isBefore(tournamentStart) ? tournamentStart : slotStart;
                Instant lastStart = slotEnd.minus(MATCH_DURATION);

                while (!candidate.isAfter(lastStart) && candidate.isBefore(tournamentEnd)) {
                    candidates.add(candidate);
                    if (candidates.size() > 500) {
                        return candidates;
                    }
                    candidate = candidate.plus(Duration.ofMinutes(30));
                }
            }
        }

        return candidates;
    }

    private boolean isPlayerAvailable(
            TournamentTeamPlayer player,
            Instant candidateStart,
            Instant candidateEnd
    ) {
        return player.getAvailabilitySlots()
                .stream()
                .anyMatch(slot -> {
                    Instant slotStart = toInstant(slot.getStartAt(), slot.getTimeZone());
                    Instant slotEnd = toInstant(slot.getEndAt(), slot.getTimeZone());

                    return !candidateStart.isBefore(slotStart) && !candidateEnd.isAfter(slotEnd);
                });
    }

    private List<PlayerAvailabilitySlot> mapAvailabilitySlots(List<AvailabilitySlotRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new RuntimeException("At least one availability slot is required");
        }

        return requests.stream()
                .map(request -> {
                    ZoneId zoneId = requireTimeZone(request.getTimeZone());
                    if (request.getStartAt() == null || request.getEndAt() == null) {
                        throw new RuntimeException("Availability start and end are required");
                    }
                    Instant startUtc = request.getStartAt().atZone(zoneId).toInstant();
                    Instant endUtc = request.getEndAt().atZone(zoneId).toInstant();
                    if (!endUtc.isAfter(startUtc)) {
                        throw new RuntimeException("Availability end must be after availability start");
                    }

                    return PlayerAvailabilitySlot.builder()
                            .startAt(request.getStartAt())
                            .endAt(request.getEndAt())
                            .timeZone(zoneId.getId())
                            .build();
                })
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private List<AvailabilitySlotResponse> mapAvailabilityResponses(List<PlayerAvailabilitySlot> slots) {
        if (slots == null) {
            return List.of();
        }

        return slots.stream()
                .map(slot -> AvailabilitySlotResponse.builder()
                        .startAt(slot.getStartAt())
                        .endAt(slot.getEndAt())
                        .timeZone(slot.getTimeZone())
                        .startUtc(toInstant(slot.getStartAt(), slot.getTimeZone()))
                        .endUtc(toInstant(slot.getEndAt(), slot.getTimeZone()))
                        .build())
                .toList();
    }

    private List<PlayerAvailabilitySlot> copyAvailabilitySlots(List<PlayerAvailabilitySlot> slots) {
        if (slots == null) {
            return List.of();
        }

        return slots.stream()
                .map(slot -> PlayerAvailabilitySlot.builder()
                        .startAt(slot.getStartAt())
                        .endAt(slot.getEndAt())
                        .timeZone(slot.getTimeZone())
                        .build())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private Instant toInstant(LocalDateTime value, String timeZone) {
        return value.atZone(requireTimeZone(timeZone)).toInstant();
    }

    private ZoneId requireTimeZone(String timeZone) {
        String zone = requireValue(timeZone, "Time zone is required");

        try {
            return ZoneId.of(zone);
        } catch (DateTimeException ex) {
            throw new RuntimeException("Invalid time zone: " + zone);
        }
    }

    private String resolveAuctionTeamName(String requestedName, TournamentRegistration captain) {
        String teamName = trimToNull(requestedName);
        if (teamName != null) {
            return teamName;
        }

        return captain.getName() + "'s Team";
    }

    private AppUser getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUser user)) {
            return null;
        }

        return user;
    }

    private AppUser requireAuthenticatedUser(Authentication authentication) {
        AppUser user = getAuthenticatedUser(authentication);
        if (user == null) {
            throw new RuntimeException("You must be logged in to register");
        }

        return user;
    }

    private AppUser resolveRegistrationUser(TournamentRegistrationRequest request, AppUser currentUser) {
        if (!isSuperAdmin(currentUser)) {
            Long requestedUserId = request.getUserId();
            String requestedEmail = trimToNull(request.getEmail());
            boolean differentUserId = requestedUserId != null && !requestedUserId.equals(currentUser.getId());
            boolean differentEmail = requestedEmail != null && !requestedEmail.equalsIgnoreCase(currentUser.getEmail());

            if (differentUserId || differentEmail) {
                throw new RuntimeException("Only Super Admin can register another player");
            }

            return currentUser;
        }

        return findRegisteredUser(request.getUserId(), request.getEmail(), currentUser);
    }

    private AppUser resolveTeamPlayerUser(
            int index,
            TournamentTeamPlayerRequest request,
            AppUser currentUser
    ) {
        if (index == 0 && !isSuperAdmin(currentUser)) {
            return currentUser;
        }

        return findRegisteredUser(request.getUserId(), request.getEmail(), index == 0 ? currentUser : null);
    }

    private AppUser findRegisteredUser(Long userId, String email, AppUser fallbackUser) {
        if (userId != null) {
            return userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("No registered user found with id " + userId));
        }

        String normalizedEmail = trimToNull(email);
        if (normalizedEmail != null) {
            return findRegisteredUserByEmail(normalizedEmail);
        }

        if (fallbackUser != null) {
            return fallbackUser;
        }

        throw new RuntimeException("Select a registered player");
    }

    private AppUser findRegisteredUserByEmail(String email) {
        String normalizedEmail = requireValue(email, "Player email is required for every teammate");
        return userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new RuntimeException("No registered user found with email " + normalizedEmail));
    }

    private int calculateCaptainSlots(long playerCount, Integer teamSize) {
        if (playerCount <= 0) {
            return 0;
        }

        int size = teamSize == null || teamSize < 1 ? 1 : teamSize;
        return (int) Math.ceil((double) playerCount / size);
    }

    private String normalizePhoneNumber(String value) {
        String normalized = requireValue(value, PHONE_ERROR)
                .replaceAll("[\\s\\-()]", "");
        if (!normalized.matches(PHONE_REGEX)) {
            throw new RuntimeException(PHONE_ERROR);
        }

        return normalized;
    }

    private String requireValue(String value, String message) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new RuntimeException(message);
        }

        return trimmed;
    }

    private String resolveDiscord(AppUser user) {
        if (user == null || !"discord".equalsIgnoreCase(trimToNull(user.getProvider()))) {
            return null;
        }

        return user.getUsername();
    }

    private boolean isSuperAdmin(AppUser user) {
        return user != null
                && user.getPermissions() != null
                && user.getPermissions().contains(Permission.SUPER_ADMIN);
    }

    private String normalizeFormat(String format) {
        String normalized = trimToNull(format);
        return normalized == null ? "" : normalized.toUpperCase();
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }

    private record MatchPair(
            TournamentTeam teamA,
            TournamentTeam teamB,
            String roundName
    ) {
    }

    private record FormatStage(
            int order,
            String name,
            String format,
            int advanceCount,
            int eliminateCount
    ) {
    }

    private record TeamStanding(
            TournamentTeam team,
            int matches,
            int wins,
            int losses,
            int scoreFor,
            int scoreAgainst
    ) {
        int scoreDifference() {
            return scoreFor - scoreAgainst;
        }
    }

    private static class TeamStandingBuilder {
        private final TournamentTeam team;
        private int matches;
        private int wins;
        private int losses;
        private int scoreFor;
        private int scoreAgainst;

        private TeamStandingBuilder(TournamentTeam team) {
            this.team = team;
        }

        private TeamStanding build() {
            return new TeamStanding(team, matches, wins, losses, scoreFor, scoreAgainst);
        }
    }
}
