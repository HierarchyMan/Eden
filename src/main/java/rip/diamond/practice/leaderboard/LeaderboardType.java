package rip.diamond.practice.leaderboard;

import lombok.AllArgsConstructor;
import lombok.Getter;
import rip.diamond.practice.kits.Kit;

@AllArgsConstructor
@Getter
public enum LeaderboardType {

    WINS("kitData.{kit}.won", TimePeriod.LIFETIME),
    ELO("kitData.{kit}.elo", TimePeriod.LIFETIME),
    WINSTREAK("kitData.{kit}.winstreak", TimePeriod.LIFETIME),
    BEST_WINSTREAK("kitData.{kit}.bestWinstreak", TimePeriod.LIFETIME),
    LOSSES("kitData.{kit}.lost", TimePeriod.LIFETIME),

    WINS_DAILY("kitData.{kit}.dailyWins", TimePeriod.DAILY),
    LOSSES_DAILY("kitData.{kit}.dailyLosses", TimePeriod.DAILY),
    WINSTREAK_DAILY("kitData.{kit}.dailyWinstreak", TimePeriod.DAILY),

    WINS_WEEKLY("kitData.{kit}.weeklyWins", TimePeriod.WEEKLY),
    LOSSES_WEEKLY("kitData.{kit}.weeklyLosses", TimePeriod.WEEKLY),
    WINSTREAK_WEEKLY("kitData.{kit}.weeklyWinstreak", TimePeriod.WEEKLY),

    WINS_MONTHLY("kitData.{kit}.monthlyWins", TimePeriod.MONTHLY),
    LOSSES_MONTHLY("kitData.{kit}.monthlyLosses", TimePeriod.MONTHLY),
    WINSTREAK_MONTHLY("kitData.{kit}.monthlyWinstreak", TimePeriod.MONTHLY),

    GLOBAL_WINS("globalWins", TimePeriod.LIFETIME),
    GLOBAL_ELO("globalElo", TimePeriod.LIFETIME),

    EVENT_WINS("eventWins", TimePeriod.LIFETIME),
    EVENT_WINS_DAILY("dailyEventWins", TimePeriod.DAILY),
    EVENT_WINS_WEEKLY("weeklyEventWins", TimePeriod.WEEKLY),
    EVENT_WINS_MONTHLY("monthlyEventWins", TimePeriod.MONTHLY),
    EVENTS_PLAYED("eventsPlayed", TimePeriod.LIFETIME);

    private final String path;
    private final TimePeriod timePeriod;

    public String getPath(Kit kit) {
        if (kit == null)
            return path;
        return path.replace("{kit}", kit.getName());
    }

    public enum TimePeriod {
        LIFETIME,
        DAILY,
        WEEKLY,
        MONTHLY
    }

}
