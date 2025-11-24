# Eden Practice - Placeholders Documentation

This document outlines all available placeholders for the Eden Practice plugin. Placeholders are used in scoreboards, menus, and other text displays to show dynamic information.

## Table of Contents
- [General Placeholders](#general-placeholders)
- [Queue Placeholders](#queue-placeholders)
- [Match Placeholders](#match-placeholders)
  - [Solo Match](#solo-match)
  - [Team Match (Split)](#team-match-split)
  - [FFA Match](#ffa-match)
  - [Event Match (Sumo)](#event-match-sumo)
- [Spectate Placeholders](#spectate-placeholders)
- [Party Placeholders](#party-placeholders)
- [Event Placeholders](#event-placeholders)
- [PlaceholderAPI Expansion Placeholders](#placeholderapi-expansion-placeholders)
  - [Kit Status](#kit-status)
  - [Party Status](#party-status)
  - [Queue Status](#queue-status)
  - [Match Info](#match-info)
  - [Player Stats](#player-stats)
  - [Leaderboard](#leaderboard)

---

## General Placeholders

These placeholders are available globally and don't depend on player state.

| Placeholder | Description |
|---|---|
| `{online-players}` | Displays the total number of online players on the server |
| `{queue-players}` | Displays the total number of players currently in queue |
| `{match-players}` | Displays the total number of players currently in matches |
| `{new-line}` | Inserts a new line in the text output |
| `<skip-line>` | Skips the line if specific conditions aren't met |

---

## Queue Placeholders

These placeholders are only available when a player is in queue (PlayerState.IN_QUEUE).

| Placeholder | Description |
|---|---|
| `{queue-kit}` | Shows the name of the kit the player is queued for |
| `{queue-time}` | Displays how long the player has been waiting in queue (formatted as timer) |
| `{queue-ranked-min}` | Shows the minimum ELO range for ranked matchmaking |
| `{queue-ranked-max}` | Shows the maximum ELO range for ranked matchmaking |
| `{ping-range}` | Displays the player's current ping range setting |

---

## Match Placeholders

### General Match Info

Available when a player is in a match (PlayerState.IN_MATCH).

| Placeholder | Description |
|---|---|
| `{match-kit}` | Shows the name of the kit being played in the match |
| `{match-duration}` | Displays the elapsed time of the match (formatted as timer) |
| `{match-build-limit}` | Shows the maximum build height for the arena |
| `{match-build-limit-difference}` | Shows the player's current height relative to the build limit |
| `{match-team1-logo}` | Displays the logo/symbol for team 1 |
| `{match-team1-bed-status}` | Shows team 1's bed status (✔ if intact, ✘ if destroyed, or count of alive players) |
| `{match-team1-points}` | Displays team 1's points as a visual bar |
| `{match-team2-logo}` | Displays the logo/symbol for team 2 |
| `{match-team2-bed-status}` | Shows team 2's bed status (✔ if intact, ✘ if destroyed, or count of alive players) |
| `{match-team2-points}` | Displays team 2's points as a visual bar |

### Solo Match

Available when playing a 1v1 match.

| Placeholder | Description |
|---|---|
| `{match-solo-opponent}` | Shows the opponent's username |
| `{match-solo-winner}` | Shows the winner's name (only when match is ending) |
| `{match-solo-loser}` | Shows the loser's name (only when match is ending) |
| `{match-solo-boxing-self-hit}` | Shows how many hits the player has landed |
| `{match-solo-boxing-opponent-hit}` | Shows how many hits the opponent has landed |
| `{match-solo-boxing-combo}` | Shows the player's current hit combo |
| `{match-solo-boxing-difference}` | Shows the absolute difference in hits between players |
| `{match-solo-boxing-difference-number}` | Shows the signed difference in hits (includes +/- sign) |
| `{match-solo-boxing-difference-symbol}` | Shows +, -, or nothing based on hit difference |
| `{match-solo-boxing-difference-color}` | Returns color code based on who's winning (GREEN/YELLOW/RED) |
| `{match-solo-boxing-difference-text}` | Shows formatted combo text with color (green for player, red for opponent, or no combo message) |
| `{match-solo-self-ping}` | Shows the player's ping in milliseconds |
| `{match-solo-opponent-ping}` | Shows the opponent's ping in milliseconds |

### Team Match (Split)

Available when playing a team-based match.

| Placeholder | Description |
|---|---|
| `{match-team-self-alive}` | Shows how many players on the player's team are alive |
| `{match-team-self-size}` | Shows the total size of the player's team |
| `{match-team-opponent-alive}` | Shows how many players on the opponent team are alive |
| `{match-team-opponent-size}` | Shows the total size of the opponent team |
| `{match-team-winner}` | Shows the winning team leader's name (only when match is ending) |
| `{match-team-loser}` | Shows the losing team leader's name (only when match is ending) |
| `{match-team-boxing-self-hit}` | Shows total hits landed by the player's team |
| `{match-team-boxing-opponent-hit}` | Shows total hits landed by the opponent team |
| `{match-team-boxing-combo}` | Shows the player's team's current combo |
| `{match-team-boxing-difference}` | Shows the absolute difference in hits between teams |
| `{match-team-boxing-difference-number}` | Shows the signed difference in hits |
| `{match-team-boxing-difference-symbol}` | Shows +, -, or nothing based on hit difference |
| `{match-team-boxing-difference-color}` | Returns color code based on which team is winning |
| `{match-team-boxing-difference-text}` | Shows formatted combo text with color |

### FFA Match

Available when playing a Free-for-All match.

| Placeholder | Description |
|---|---|
| `{match-ffa-alive}` | Shows the number of players still alive in the FFA |
| `{match-ffa-player-size}` | Shows the total number of players in the FFA |
| `{match-ffa-winner}` | Shows the FFA winner's name (only when match is ending) |
| `{match-ffa-loser}` | Shows all loser names separated by commas (only when match is ending) |

### Event Match (Sumo)

Available when playing a Sumo event match.

| Placeholder | Description |
|---|---|
| `{match-event-type}` | Shows the name of the event type (e.g., "Sumo Event") |
| `{match-event-round}` | Shows the current round number of the event |
| `{match-event-winner}` | Shows the name of the team that won (only when match is ending) |

---

## Spectate Placeholders

### General Spectate Info

Available when a player is spectating a match (PlayerState.IN_SPECTATING).

| Placeholder | Description |
|---|---|
| `{spectate-kit}` | Shows the name of the kit being played in the spectated match |
| `{spectate-duration}` | Displays the elapsed time of the spectated match |
| `{spectate-build-limit}` | Shows the maximum build height for the arena |
| `{spectate-build-limit-difference}` | Shows the spectated player's height relative to the build limit |
| `{spectate-team1-logo}` | Displays the logo/symbol for team 1 in the spectated match |
| `{spectate-team1-bed-status}` | Shows team 1's bed status in the spectated match |
| `{spectate-team1-points}` | Displays team 1's points as a visual bar |
| `{spectate-team2-logo}` | Displays the logo/symbol for team 2 in the spectated match |
| `{spectate-team2-bed-status}` | Shows team 2's bed status in the spectated match |
| `{spectate-team2-points}` | Displays team 2's points as a visual bar |

### Solo Spectate

Available when spectating a 1v1 match.

| Placeholder | Description |
|---|---|
| `{spectate-solo-player1}` | Shows the name of player 1 |
| `{spectate-solo-player2}` | Shows the name of player 2 |
| `{spectate-solo-winner}` | Shows the winner's name (only when match is ending) |
| `{spectate-solo-loser}` | Shows the loser's name (only when match is ending) |
| `{spectate-solo-boxing-player1-hit}` | Shows hits landed by player 1 |
| `{spectate-solo-boxing-player2-hit}` | Shows hits landed by player 2 |
| `{spectate-solo-boxing-player1-combo}` | Shows player 1's current combo |
| `{spectate-solo-boxing-player2-combo}` | Shows player 2's current combo |
| `{spectate-solo-player1-ping}` | Shows player 1's ping |
| `{spectate-solo-player2-ping}` | Shows player 2's ping |

### Team Spectate

Available when spectating a team match.

| Placeholder | Description |
|---|---|
| `{spectate-team1-leader}` | Shows the leader's name of team 1 |
| `{spectate-team2-leader}` | Shows the leader's name of team 2 |
| `{spectate-team1-alive}` | Shows the number of alive players on team 1 |
| `{spectate-team2-alive}` | Shows the number of alive players on team 2 |
| `{spectate-team1-size}` | Shows the total size of team 1 |
| `{spectate-team2-size}` | Shows the total size of team 2 |
| `{spectate-team-winner}` | Shows the winning team leader's name (only when match is ending) |
| `{spectate-team-loser}` | Shows the losing team leader's name (only when match is ending) |
| `{spectate-team1-boxing-hit}` | Shows total hits landed by team 1 |
| `{spectate-team2-boxing-hit}` | Shows total hits landed by team 2 |
| `{spectate-team1-boxing-combo}` | Shows team 1's current combo |
| `{spectate-team2-boxing-combo}` | Shows team 2's current combo |

### FFA Spectate

Available when spectating a Free-for-All match.

| Placeholder | Description |
|---|---|
| `{spectate-ffa-alive}` | Shows the number of players still alive in the FFA |
| `{spectate-ffa-player-size}` | Shows the total number of players in the FFA |
| `{spectate-ffa-winner}` | Shows the FFA winner's name |
| `{spectate-ffa-loser}` | Shows all loser names separated by commas |

---

## Party Placeholders

These placeholders are available when a player is in a party.

| Placeholder | Description |
|---|---|
| `{party-leader}` | Shows the name of the party leader |
| `{party-members}` | Shows the total number of members in the party |
| `{party-max}` | Shows the maximum size allowed for the party |

---

## Event Placeholders

These placeholders are available when an event is running.

| Placeholder | Description |
|---|---|
| `{event-information}` | Shows lobby scoreboard information for the ongoing event |
| `{event-uncolored-name}` | Shows the name of the event without color codes |
| `{event-total-players}` | Shows how many players are currently in the event |
| `{event-max-players}` | Shows the maximum number of players allowed in the event |
| `{event-countdown}` | Shows the time remaining until the event starts (in milliseconds, converted to seconds) |

---

## PlaceholderAPI Expansion Placeholders

These placeholders use the PlaceholderAPI format: `%eden_placeholder%`

### Kit Status

| Placeholder | Description |
|---|---|
| `%eden_kit_status_<kitname>%` | Shows if a kit is enabled or disabled (returns "Enabled" or "Disabled") |

### Party Status

| Placeholder | Description |
|---|---|
| `%eden_in_party%` | Shows if player is in a party (returns "Enabled" or "Disabled") |
| `%eden_party_privacy%` | Shows the privacy setting of the player's party |
| `%eden_party_leader%` | Shows the name of the party leader |

### Queue Status

| Placeholder | Description |
|---|---|
| `%eden_queue_unranked_<kitname>%` | Shows the number of players in unranked queue for a specific kit |
| `%eden_queue_ranked_<kitname>%` | Shows the number of players in ranked queue for a specific kit |

### Match Info

| Placeholder | Description |
|---|---|
| `%eden_match_unranked_<kitname>%` | Shows the number of players currently in unranked matches for a specific kit |
| `%eden_match_ranked_<kitname>%` | Shows the number of players currently in ranked matches for a specific kit |
| `%eden_match_match_type%` | Shows the type of match the player is in (e.g., "Solo", "Team", "FFA") |
| `%eden_match_queue_type%` | Shows if the match is ranked or unranked |
| `%eden_match_player_team_color%` | Shows the color code of the player's team |
| `%eden_match_player_team_name%` | Shows the name of the player's team |
| `%eden_match_player_team_logo%` | Shows the logo/symbol of the player's team |
| `%eden_match_arena_name%` | Shows the name of the arena being played in |
| `%eden_match_kit_name%` | Shows the name of the kit being played |

### Player Stats

These placeholders show statistics for the player.

| Placeholder | Description |
|---|---|
| `%eden_player_status%` | Shows the player's current status (e.g., "IN_LOBBY", "IN_QUEUE", "IN_MATCH") |
| `%eden_player_ranked_win%` | Shows the total number of ranked wins across all kits |
| `%eden_player_ranked_loss%` | Shows the total number of ranked losses across all kits |
| `%eden_player_unranked_win%` | Shows the total number of unranked wins across all kits |
| `%eden_player_unranked_loss%` | Shows the total number of unranked losses across all kits |
| `%eden_player_overall_win%` | Shows the total number of wins (ranked + unranked) across all kits |
| `%eden_player_overall_loss%` | Shows the total number of losses (ranked + unranked) across all kits |
| `%eden_player_total_elo%` | Shows the sum of ELO across all ranked kits |
| `%eden_player_global_elo%` | Shows the average ELO across all ranked kits |
| `%eden_player_elo_<kitname>%` | Shows the current ELO for a specific kit |
| `%eden_player_peakElo_<kitname>%` | Shows the peak/highest ELO ever achieved for a specific kit |
| `%eden_player_unrankedWon_<kitname>%` | Shows unranked wins for a specific kit |
| `%eden_player_unrankedLost_<kitname>%` | Shows unranked losses for a specific kit |
| `%eden_player_rankedWon_<kitname>%` | Shows ranked wins for a specific kit |
| `%eden_player_rankedLost_<kitname>%` | Shows ranked losses for a specific kit |
| `%eden_player_bestWinstreak_<kitname>%` | Shows the best win streak ever achieved for a specific kit |
| `%eden_player_winstreak_<kitname>%` | Shows the current win streak for a specific kit |

### Leaderboard

These placeholders display leaderboard information. Position is 1-indexed.

**Best Winstreak Leaderboard:**

| Placeholder | Description |
|---|---|
| `%eden_leaderboard_bestWinstreak_player_<kitname>_<position>%` | Shows the player name at the given position on the best winstreak leaderboard |
| `%eden_leaderboard_bestWinstreak_winstreak_<kitname>_<position>%` | Shows the best winstreak value at the given position |

**ELO Leaderboard:**

| Placeholder | Description |
|---|---|
| `%eden_leaderboard_elo_player_<kitname>_<position>%` | Shows the player name at the given position on the ELO leaderboard |
| `%eden_leaderboard_elo_elo_<kitname>_<position>%` | Shows the ELO value at the given position |

**Wins Leaderboard:**

| Placeholder | Description |
|---|---|
| `%eden_leaderboard_wins_player_<kitname>_<position>%` | Shows the player name at the given position on the wins leaderboard |
| `%eden_leaderboard_wins_win_<kitname>_<position>%` | Shows the wins count at the given position |

**Current Winstreak Leaderboard:**

| Placeholder | Description |
|---|---|
| `%eden_leaderboard_winstreak_player_<kitname>_<position>%` | Shows the player name at the given position on the current winstreak leaderboard |
| `%eden_leaderboard_winstreak_winstreak_<kitname>_<position>%` | Shows the current winstreak value at the given position |

---

## Usage Examples

### In Scoreboards
```yaml
scoreboard:
  lobby:
    - "&eOnline: {online-players}"
    - "&eQueue: {queue-players}"
    - "&eMatches: {match-players}"
```

### In Match Display
```yaml
scoreboard:
  in_match:
    - "{match-kit}"
    - "Time: {match-duration}"
    - "{match-team1-logo} {match-team1-bed-status}"
    - "{match-solo-opponent}"
    - "Hits: {match-solo-boxing-self-hit} vs {match-solo-boxing-opponent-hit}"
```

### With PlaceholderAPI
```
%eden_player_global_elo%
%eden_match_arena_name%
%eden_leaderboard_elo_player_boxing_1%
```

---

## Notes

- Placeholders are **case-sensitive** (except for PlaceholderAPI format which is case-insensitive)
- Use `<kitname>` with the actual kit name (e.g., `boxing`, `sumo`, `nodebuff`)
- Position in leaderboard placeholders is 1-indexed (position 1 is 1st place)
- If a player is not in a match, match-related placeholders will return empty strings or specific error messages
- If database is not enabled, leaderboard placeholders will return "Database isn't enabled"
- Some placeholders (like winners/losers) only show values when a match is in the ENDING state

