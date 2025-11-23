package rip.diamond.practice.leaderboard;

import lombok.Getter;
import rip.diamond.practice.Eden;
import rip.diamond.practice.kits.Kit;
import rip.diamond.practice.leaderboard.impl.KitLeaderboard;
import rip.diamond.practice.util.Common;
import rip.diamond.practice.util.Tasks;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
public class LeaderboardManager {
    private final Map<Kit, KitLeaderboard> winsLeaderboard = new HashMap<>();
    private final Map<Kit, KitLeaderboard> eloLeaderboard = new HashMap<>();
    private final Map<Kit, KitLeaderboard> winstreakLeaderboard = new HashMap<>();
    private final Map<Kit, KitLeaderboard> bestWinstreakLeaderboard = new HashMap<>();
    private final Map<Kit, KitLeaderboard> dailyWinstreakLeaderboard = new HashMap<>();

    public void init() {
        if (Eden.INSTANCE.getDatabaseManager().getHandler() == null) {
            return;
        }
        for (Kit kit : Kit.getKits()) {
            winsLeaderboard.put(kit, new KitLeaderboard(LeaderboardType.WINS, kit));
            eloLeaderboard.put(kit, new KitLeaderboard(LeaderboardType.ELO, kit));
            winstreakLeaderboard.put(kit, new KitLeaderboard(LeaderboardType.WINSTREAK, kit));
            bestWinstreakLeaderboard.put(kit, new KitLeaderboard(LeaderboardType.BEST_WINSTREAK, kit));
            dailyWinstreakLeaderboard.put(kit, new KitLeaderboard(LeaderboardType.WINSTREAK_DAILY, kit));
        }

        Tasks.runAsyncTimer(this::update, 0L, 20L * 60L * 5L); // Updates every 5 minutes
    }

    public void update() {
        long previous = System.currentTimeMillis();
        Common.debug("正在更新排行榜... 這可能需要一段時間");
        for (Map<Kit, KitLeaderboard> datas : Arrays.asList(winsLeaderboard, eloLeaderboard, winstreakLeaderboard,
                bestWinstreakLeaderboard, dailyWinstreakLeaderboard)) {
            // 每五分鐘更新排行榜
            datas.values().forEach(Leaderboard::update);
        }
        long current = System.currentTimeMillis();
        Common.debug("排行榜更新完畢! 耗時" + (current - previous) + "ms");
    }

    /**
     * Incrementally update a player's stats for a specific kit across all
     * leaderboards
     * This is called immediately after a match ends for instant updates
     */
    public void updatePlayerStats(UUID uuid, String username, Kit kit,
            rip.diamond.practice.profile.data.ProfileKitData kitData) {
        if (Eden.INSTANCE.getDatabaseManager().getHandler() == null) {
            return;
        }

        // Update all leaderboard types for this kit
        KitLeaderboard winsLb = winsLeaderboard.get(kit);
        if (winsLb != null) {
            winsLb.updatePlayer(uuid, username, kitData.getWon());
        }

        KitLeaderboard eloLb = eloLeaderboard.get(kit);
        if (eloLb != null && kit.isRanked()) {
            eloLb.updatePlayer(uuid, username, kitData.getElo());
        }

        KitLeaderboard winstreakLb = winstreakLeaderboard.get(kit);
        if (winstreakLb != null) {
            winstreakLb.updatePlayer(uuid, username, kitData.getWinstreak());
        }

        KitLeaderboard bestWinstreakLb = bestWinstreakLeaderboard.get(kit);
        if (bestWinstreakLb != null) {
            bestWinstreakLb.updatePlayer(uuid, username, kitData.getBestWinstreak());
        }

        KitLeaderboard dailyWinstreakLb = dailyWinstreakLeaderboard.get(kit);
        if (dailyWinstreakLb != null) {
            dailyWinstreakLb.updatePlayer(uuid, username, kitData.getDailyWinstreak());
        }
    }

}
