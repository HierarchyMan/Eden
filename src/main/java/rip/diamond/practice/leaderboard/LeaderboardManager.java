package rip.diamond.practice.leaderboard;

import lombok.Getter;
import org.bson.Document;
import rip.diamond.practice.Eden;
import rip.diamond.practice.kits.Kit;
import rip.diamond.practice.leaderboard.impl.GlobalLeaderboard;
import rip.diamond.practice.leaderboard.impl.KitLeaderboard;
import rip.diamond.practice.util.Common;
import rip.diamond.practice.util.Tasks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
public class LeaderboardManager {
    private final Map<Kit, KitLeaderboard> winsLeaderboard = new HashMap<>();
    private final Map<Kit, KitLeaderboard> eloLeaderboard = new HashMap<>();
    private final Map<Kit, KitLeaderboard> winstreakLeaderboard = new HashMap<>();
    private final Map<Kit, KitLeaderboard> bestWinstreakLeaderboard = new HashMap<>();
    private final Map<Kit, KitLeaderboard> lossesLeaderboard = new HashMap<>();

    private final Map<Kit, KitLeaderboard> winsDailyLeaderboard = new HashMap<>();
    private final Map<Kit, KitLeaderboard> winsWeeklyLeaderboard = new HashMap<>();
    private final Map<Kit, KitLeaderboard> winsMonthlyLeaderboard = new HashMap<>();

    private final Map<Kit, KitLeaderboard> lossesDailyLeaderboard = new HashMap<>();
    private final Map<Kit, KitLeaderboard> lossesWeeklyLeaderboard = new HashMap<>();
    private final Map<Kit, KitLeaderboard> lossesMonthlyLeaderboard = new HashMap<>();

    private final Map<Kit, KitLeaderboard> winstreakDailyLeaderboard = new HashMap<>();
    private final Map<Kit, KitLeaderboard> winstreakWeeklyLeaderboard = new HashMap<>();
    private final Map<Kit, KitLeaderboard> winstreakMonthlyLeaderboard = new HashMap<>();

    private GlobalLeaderboard globalWinsLeaderboard;
    private GlobalLeaderboard globalEloLeaderboard;
    private GlobalLeaderboard eventWinsLeaderboard;
    private GlobalLeaderboard eventWinsDailyLeaderboard;
    private GlobalLeaderboard eventWinsWeeklyLeaderboard;
    private GlobalLeaderboard eventWinsMonthlyLeaderboard;
    private GlobalLeaderboard eventsPlayedLeaderboard;

    private volatile List<Document> lastProfilesSnapshot;
    private volatile long lastSnapshotAt;

    public void init() {
        if (Eden.INSTANCE.getDatabaseManager().getHandler() == null) {
            return;
        }
        rebuildLeaderboards();

        Tasks.runAsyncTimer(this::update, 0L, 20L * 60L * 5L);
    }

    /**
     * Rebuild all leaderboard maps with current Kit instances
     * This should be called after Kit.reload() to ensure maps use new Kit objects
     */
    public void rebuildLeaderboards() {
        if (Eden.INSTANCE.getDatabaseManager().getHandler() == null) {
            return;
        }

        winsLeaderboard.clear();
        eloLeaderboard.clear();
        winstreakLeaderboard.clear();
        winstreakLeaderboard.clear();
        bestWinstreakLeaderboard.clear();
        lossesLeaderboard.clear();

        winsDailyLeaderboard.clear();
        winsWeeklyLeaderboard.clear();
        winsMonthlyLeaderboard.clear();

        lossesDailyLeaderboard.clear();
        lossesWeeklyLeaderboard.clear();
        lossesMonthlyLeaderboard.clear();

        winstreakDailyLeaderboard.clear();
        winstreakWeeklyLeaderboard.clear();
        winstreakMonthlyLeaderboard.clear();

        for (Kit kit : Kit.getKits()) {
            winsLeaderboard.put(kit, new KitLeaderboard(LeaderboardType.WINS, kit));
            eloLeaderboard.put(kit, new KitLeaderboard(LeaderboardType.ELO, kit));
            winstreakLeaderboard.put(kit, new KitLeaderboard(LeaderboardType.WINSTREAK, kit));
            bestWinstreakLeaderboard.put(kit, new KitLeaderboard(LeaderboardType.BEST_WINSTREAK, kit));
            lossesLeaderboard.put(kit, new KitLeaderboard(LeaderboardType.LOSSES, kit));

            winsDailyLeaderboard.put(kit, new KitLeaderboard(LeaderboardType.WINS_DAILY, kit));
            winsWeeklyLeaderboard.put(kit, new KitLeaderboard(LeaderboardType.WINS_WEEKLY, kit));
            winsMonthlyLeaderboard.put(kit, new KitLeaderboard(LeaderboardType.WINS_MONTHLY, kit));

            lossesDailyLeaderboard.put(kit, new KitLeaderboard(LeaderboardType.LOSSES_DAILY, kit));
            lossesWeeklyLeaderboard.put(kit, new KitLeaderboard(LeaderboardType.LOSSES_WEEKLY, kit));
            lossesMonthlyLeaderboard.put(kit, new KitLeaderboard(LeaderboardType.LOSSES_MONTHLY, kit));

            winstreakDailyLeaderboard.put(kit, new KitLeaderboard(LeaderboardType.WINSTREAK_DAILY, kit));
            winstreakWeeklyLeaderboard.put(kit, new KitLeaderboard(LeaderboardType.WINSTREAK_WEEKLY, kit));
            winstreakMonthlyLeaderboard.put(kit, new KitLeaderboard(LeaderboardType.WINSTREAK_MONTHLY, kit));
        }

        globalWinsLeaderboard = new GlobalLeaderboard(LeaderboardType.GLOBAL_WINS);
        globalEloLeaderboard = new GlobalLeaderboard(LeaderboardType.GLOBAL_ELO);
        eventWinsLeaderboard = new GlobalLeaderboard(LeaderboardType.EVENT_WINS);
        eventWinsDailyLeaderboard = new GlobalLeaderboard(LeaderboardType.EVENT_WINS_DAILY);
        eventWinsWeeklyLeaderboard = new GlobalLeaderboard(LeaderboardType.EVENT_WINS_WEEKLY);
        eventWinsMonthlyLeaderboard = new GlobalLeaderboard(LeaderboardType.EVENT_WINS_MONTHLY);
        eventsPlayedLeaderboard = new GlobalLeaderboard(LeaderboardType.EVENTS_PLAYED);

        if (lastProfilesSnapshot != null && !lastProfilesSnapshot.isEmpty()) {
            updateAllLeaderboards(lastProfilesSnapshot);
        }
    }

    public void update() {
        long previous = System.currentTimeMillis();
        Common.debug("正在更新排行榜... 這可能需要一段時間");

        List<Document> snapshot = Eden.INSTANCE.getDatabaseManager().getHandler().getAllProfiles();

        // Merge online players data to ensure latest stats
        Map<String, Document> snapshotMap = new HashMap<>();
        if (snapshot != null) {
            for (Document doc : snapshot) {
                snapshotMap.put(doc.getString("uuid"), doc);
            }
        }

        for (rip.diamond.practice.profile.PlayerProfile profile : rip.diamond.practice.profile.PlayerProfile
                .getProfiles().values()) {
            if (profile.getPlayer() != null && profile.getPlayer().isOnline()) {
                snapshotMap.put(profile.getUniqueId().toString(), profile.toBson());
            }
        }

        List<Document> finalSnapshot = new ArrayList<>(snapshotMap.values());

        lastProfilesSnapshot = finalSnapshot;
        lastSnapshotAt = previous;

        Common.debug("Snapshot size: " + finalSnapshot.size());
        updateAllLeaderboards(finalSnapshot);

        long current = System.currentTimeMillis();
        Common.debug("排行榜更新完畢! 耗時" + (current - previous) + "ms | profiles=" + finalSnapshot.size());
    }

    private void updateAllLeaderboards(List<Document> snapshot) {
        for (Map<Kit, KitLeaderboard> datas : Arrays.asList(
                winsLeaderboard, eloLeaderboard, winstreakLeaderboard, bestWinstreakLeaderboard,
                winsDailyLeaderboard, winsWeeklyLeaderboard, winsMonthlyLeaderboard,
                lossesDailyLeaderboard, lossesWeeklyLeaderboard, lossesMonthlyLeaderboard,
                lossesLeaderboard,
                winstreakDailyLeaderboard, winstreakWeeklyLeaderboard, winstreakMonthlyLeaderboard)) {
            datas.values().forEach(lb -> lb.update(snapshot));
        }
        if (globalWinsLeaderboard != null)
            globalWinsLeaderboard.update(snapshot);
        if (globalEloLeaderboard != null)
            globalEloLeaderboard.update(snapshot);
        if (eventWinsLeaderboard != null)
            eventWinsLeaderboard.update(snapshot);
        if (eventWinsDailyLeaderboard != null)
            eventWinsDailyLeaderboard.update(snapshot);
        if (eventWinsWeeklyLeaderboard != null)
            eventWinsWeeklyLeaderboard.update(snapshot);
        if (eventWinsMonthlyLeaderboard != null)
            eventWinsMonthlyLeaderboard.update(snapshot);
        if (eventsPlayedLeaderboard != null)
            eventsPlayedLeaderboard.update(snapshot);
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

        updateKitLeaderboard(winsLeaderboard, kit, uuid, username, kitData.getWon());
        if (kit.isRanked()) {
            updateKitLeaderboard(eloLeaderboard, kit, uuid, username, kitData.getElo());
        }
        updateKitLeaderboard(winstreakLeaderboard, kit, uuid, username, kitData.getWinstreak());

        updateKitLeaderboard(bestWinstreakLeaderboard, kit, uuid, username, kitData.getBestWinstreak());
        updateKitLeaderboard(lossesLeaderboard, kit, uuid, username, kitData.getLost());

        updateKitLeaderboard(winsDailyLeaderboard, kit, uuid, username, kitData.getDailyWins());
        updateKitLeaderboard(winsWeeklyLeaderboard, kit, uuid, username, kitData.getWeeklyWins());
        updateKitLeaderboard(winsMonthlyLeaderboard, kit, uuid, username, kitData.getMonthlyWins());

        updateKitLeaderboard(lossesDailyLeaderboard, kit, uuid, username, kitData.getDailyLosses());
        updateKitLeaderboard(lossesWeeklyLeaderboard, kit, uuid, username, kitData.getWeeklyLosses());
        updateKitLeaderboard(lossesMonthlyLeaderboard, kit, uuid, username, kitData.getMonthlyLosses());

        updateKitLeaderboard(winstreakDailyLeaderboard, kit, uuid, username, kitData.getDailyWinstreak());
        updateKitLeaderboard(winstreakWeeklyLeaderboard, kit, uuid, username, kitData.getWeeklyWinstreak());
        updateKitLeaderboard(winstreakMonthlyLeaderboard, kit, uuid, username, kitData.getMonthlyWinstreak());

        // Global updates
        if (globalWinsLeaderboard != null) {
            // Calculate total wins for this player
            int totalWins = 0;
            rip.diamond.practice.profile.PlayerProfile profile = rip.diamond.practice.profile.PlayerProfile.get(uuid);
            if (profile != null) {
                totalWins = profile.getKitData().values().stream()
                        .mapToInt(data -> data.getRankedWon() + data.getUnrankedWon())
                        .sum();
                globalWinsLeaderboard.updatePlayer(uuid, username, totalWins);
            }
        }

        if (globalEloLeaderboard != null) {
            // Calculate average Elo for this player
            int globalElo = 1000;
            rip.diamond.practice.profile.PlayerProfile profile = rip.diamond.practice.profile.PlayerProfile.get(uuid);
            if (profile != null) {
                int totalElo = 0;
                int count = 0;
                for (Kit k : Kit.getKits()) {
                    if (k.isEnabled() && k.isRanked()) {
                        rip.diamond.practice.profile.data.ProfileKitData data = profile.getKitData().get(k.getName());
                        if (data != null) {
                            totalElo += data.getElo();
                            count++;
                        }
                    }
                }
                globalElo = count == 0 ? 1000 : totalElo / count;
                globalEloLeaderboard.updatePlayer(uuid, username, globalElo);
            }
        }
    }

    private void updateKitLeaderboard(Map<Kit, KitLeaderboard> map, Kit kit, UUID uuid, String username, int value) {
        KitLeaderboard lb = map.get(kit);
        if (lb != null) {
            lb.updatePlayer(uuid, username, value);
        }
    }

    public void updateEventStats(rip.diamond.practice.profile.PlayerProfile profile) {
        if (eventWinsLeaderboard != null) {
            eventWinsLeaderboard.updatePlayer(profile.getUniqueId(), profile.getUsername(), profile.getEventWins());
        }
        if (eventWinsDailyLeaderboard != null) {
            eventWinsDailyLeaderboard.updatePlayer(profile.getUniqueId(), profile.getUsername(),
                    profile.getDailyEventWins());
        }
        if (eventWinsWeeklyLeaderboard != null) {
            eventWinsWeeklyLeaderboard.updatePlayer(profile.getUniqueId(), profile.getUsername(),
                    profile.getWeeklyEventWins());
        }
        if (eventWinsMonthlyLeaderboard != null) {
            eventWinsMonthlyLeaderboard.updatePlayer(profile.getUniqueId(), profile.getUsername(),
                    profile.getMonthlyEventWins());
        }
        if (eventsPlayedLeaderboard != null) {
            eventsPlayedLeaderboard.updatePlayer(profile.getUniqueId(), profile.getUsername(),
                    profile.getEventsPlayed());
        }
    }

}
