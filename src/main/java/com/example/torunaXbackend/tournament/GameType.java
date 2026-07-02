package com.example.torunaXbackend.tournament;

import java.util.List;

public enum GameType {
    VALORANT(List.of(
            "Unranked",
            "Iron",
            "Bronze",
            "Silver",
            "Gold",
            "Platinum",
            "Diamond",
            "Ascendant",
            "Immortal",
            "Radiant"
    )),
    CS2(List.of(
            "Unranked",
            "Silver",
            "Gold Nova",
            "Master Guardian",
            "Legendary Eagle",
            "Supreme Master First Class",
            "Global Elite"
    )),
    DOTA2(List.of(
            "Unranked",
            "Herald",
            "Guardian",
            "Crusader",
            "Archon",
            "Legend",
            "Ancient",
            "Divine",
            "Immortal"
    )),
    LEAGUE_OF_LEGENDS(List.of(
            "Unranked",
            "Iron",
            "Bronze",
            "Silver",
            "Gold",
            "Platinum",
            "Emerald",
            "Diamond",
            "Master",
            "Grandmaster",
            "Challenger"
    )),
    ROCKET_LEAGUE(List.of(
            "Unranked",
            "Bronze",
            "Silver",
            "Gold",
            "Platinum",
            "Diamond",
            "Champion",
            "Grand Champion",
            "Supersonic Legend"
    )),
    OVERWATCH2(List.of(
            "Unranked",
            "Bronze",
            "Silver",
            "Gold",
            "Platinum",
            "Diamond",
            "Master",
            "Grandmaster",
            "Champion"
    )),
    APEX_LEGENDS(List.of(
            "Unranked",
            "Rookie",
            "Bronze",
            "Silver",
            "Gold",
            "Platinum",
            "Diamond",
            "Master",
            "Apex Predator"
    )),
    FORTNITE(List.of(
            "Unranked",
            "Bronze",
            "Silver",
            "Gold",
            "Platinum",
            "Diamond",
            "Elite",
            "Champion",
            "Unreal"
    )),
    PUBG(List.of(
            "Unranked",
            "Bronze",
            "Silver",
            "Gold",
            "Platinum",
            "Diamond",
            "Master",
            "Grandmaster"
    )),
    RAINBOW_SIX_SIEGE(List.of(
            "Unranked",
            "Copper",
            "Bronze",
            "Silver",
            "Gold",
            "Platinum",
            "Emerald",
            "Diamond",
            "Champion"
    )),
    EA_FC(List.of(
            "Unranked",
            "Division 10",
            "Division 9",
            "Division 8",
            "Division 7",
            "Division 6",
            "Division 5",
            "Division 4",
            "Division 3",
            "Division 2",
            "Division 1",
            "Elite Division"
    )),
    STREET_FIGHTER(List.of(
            "Unranked",
            "Rookie",
            "Iron",
            "Bronze",
            "Silver",
            "Gold",
            "Platinum",
            "Diamond",
            "Master",
            "Legend"
    ));

    private final List<String> ranks;

    GameType(List<String> ranks) {
        this.ranks = ranks;
    }

    public List<String> getRanks() {
        return ranks;
    }
}
