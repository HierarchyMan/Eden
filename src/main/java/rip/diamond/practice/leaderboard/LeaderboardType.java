package rip.diamond.practice.leaderboard;

import lombok.AllArgsConstructor;
import rip.diamond.practice.kits.Kit;

@AllArgsConstructor
public enum LeaderboardType {

    // All-time leaderboards
    WINS("kitData.{kit}.won"),
    ELO("kitData.{kit}.elo"),
    WINSTREAK("kitData.{kit}.winstreak"),
    BEST_WINSTREAK("kitData.{kit}.bestWinstreak"),

    // Daily leaderboards
    WINS_DAILY("kitData.{kit}.dailyWins"),
    LOSSES_DAILY("kitData.{kit}.dailyLosses"),
    WINSTREAK_DAILY("kitData.{kit}.dailyWinstreak"),

    // Weekly leaderboards
    WINS_WEEKLY("kitData.{kit}.weeklyWins"),
    LOSSES_WEEKLY("kitData.{kit}.weeklyLosses"),
    WINSTREAK_WEEKLY("kitData.{kit}.weeklyWinstreak"),

    // Monthly leaderboards
    WINS_MONTHLY("kitData.{kit}.monthlyWins"),
    LOSSES_MONTHLY("kitData.{kit}.monthlyLosses"),
    WINSTREAK_MONTHLY("kitData.{kit}.monthlyWinstreak");

    private final String path;

    public String getPath(Kit kit) {
        return path.replace("{kit}", kit.getName());
    }

}
