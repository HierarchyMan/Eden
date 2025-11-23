package rip.diamond.practice.leaderboard.impl;

import lombok.Getter;
import org.bson.Document;
import org.bukkit.inventory.ItemStack;
import rip.diamond.practice.Eden;
import rip.diamond.practice.config.Language;
import rip.diamond.practice.kits.Kit;
import rip.diamond.practice.leaderboard.Leaderboard;
import rip.diamond.practice.leaderboard.LeaderboardPlayerCache;
import rip.diamond.practice.leaderboard.LeaderboardType;
import rip.diamond.practice.util.ItemBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
public class KitLeaderboard extends Leaderboard {

    private final Kit kit;

    public KitLeaderboard(LeaderboardType type, Kit kit) {
        super(type);
        this.kit = kit;
    }

    @Override
    public void update() {
        String path = getType().getPath(kit);

        // Get all documents from abstraction layer
        List<Document> documents = Eden.INSTANCE.getDatabaseManager().getHandler().getAllProfiles();

        // Sort in Java memory
        documents.sort((doc1, doc2) -> {
            Integer val1 = getStatsValue(doc1, path);
            Integer val2 = getStatsValue(doc2, path);

            // Handle time-based winstreak leaderboards
            if (getType() == LeaderboardType.WINSTREAK_DAILY) {
                long date1 = getStatsValueLong(doc1, path.replace("dailyWinstreak", "lastDailyReset"));
                long date2 = getStatsValueLong(doc2, path.replace("dailyWinstreak", "lastDailyReset"));
                if (!isSameDay(date1, System.currentTimeMillis()))
                    val1 = 0;
                if (!isSameDay(date2, System.currentTimeMillis()))
                    val2 = 0;
            } else if (getType() == LeaderboardType.WINSTREAK_WEEKLY) {
                long date1 = getStatsValueLong(doc1, path.replace("weeklyWinstreak", "lastWeeklyReset"));
                long date2 = getStatsValueLong(doc2, path.replace("weeklyWinstreak", "lastWeeklyReset"));
                if (!isSameWeek(date1, System.currentTimeMillis()))
                    val1 = 0;
                if (!isSameWeek(date2, System.currentTimeMillis()))
                    val2 = 0;
            } else if (getType() == LeaderboardType.WINSTREAK_MONTHLY) {
                long date1 = getStatsValueLong(doc1, path.replace("monthlyWinstreak", "lastMonthlyReset"));
                long date2 = getStatsValueLong(doc2, path.replace("monthlyWinstreak", "lastMonthlyReset"));
                if (!isSameMonth(date1, System.currentTimeMillis()))
                    val1 = 0;
                if (!isSameMonth(date2, System.currentTimeMillis()))
                    val2 = 0;
            }

            return val2.compareTo(val1); // Descending order
        });

        getLeaderboard().clear();

        // Take top entries, but skip those with 0 data (stale/expired entries)
        int position = 1;
        for (int i = 0; i < documents.size() && position <= 10; i++) {
            Document document = documents.get(i);
            String username = document.getString("username");
            UUID uuid = UUID.fromString(document.getString("uuid"));
            int data = getStatsValue(document, path);

            // Handle time-based winstreak leaderboards
            if (getType() == LeaderboardType.WINSTREAK_DAILY) {
                long date = getStatsValueLong(document, path.replace("dailyWinstreak", "lastDailyReset"));
                if (!isSameDay(date, System.currentTimeMillis()))
                    data = 0;
            } else if (getType() == LeaderboardType.WINSTREAK_WEEKLY) {
                long date = getStatsValueLong(document, path.replace("weeklyWinstreak", "lastWeeklyReset"));
                if (!isSameWeek(date, System.currentTimeMillis()))
                    data = 0;
            } else if (getType() == LeaderboardType.WINSTREAK_MONTHLY) {
                long date = getStatsValueLong(document, path.replace("monthlyWinstreak", "lastMonthlyReset"));
                if (!isSameMonth(date, System.currentTimeMillis()))
                    data = 0;
            }

            // Skip entries with 0 data (stale/expired from previous time periods)
            if (data == 0) {
                continue;
            }

            getLeaderboard().put(position, new LeaderboardPlayerCache(username, uuid, data));
            position++;
        }
    }

    /**
     * Incrementally update a single player's stats in the leaderboard
     * This is much more performant than doing a full update
     */
    public void updatePlayer(UUID uuid, String username, int newData) {
        // For time-based leaderboards, check if data is stale
        if (newData == 0) {
            // Remove player if they have 0 data
            getLeaderboard().entrySet().removeIf(entry -> entry.getValue().getPlayerUUID().equals(uuid));
            return;
        }

        // Check if player is already in leaderboard
        Integer existingPosition = null;
        for (Map.Entry<Integer, LeaderboardPlayerCache> entry : getLeaderboard().entrySet()) {
            if (entry.getValue().getPlayerUUID().equals(uuid)) {
                existingPosition = entry.getKey();
                break;
            }
        }

        // If player is already in leaderboard, update their data
        if (existingPosition != null) {
            getLeaderboard().get(existingPosition).setData(newData);
        } else if (getLeaderboard().size() < 10) {
            // If leaderboard has less than 10 entries, add player
            getLeaderboard().put(getLeaderboard().size() + 1, new LeaderboardPlayerCache(username, uuid, newData));
        } else {
            // Check if new data is better than the worst entry
            int worstPosition = 10;
            LeaderboardPlayerCache worstEntry = getLeaderboard().get(worstPosition);
            if (worstEntry != null && newData > worstEntry.getData()) {
                // Replace worst entry with new player
                getLeaderboard().put(worstPosition, new LeaderboardPlayerCache(username, uuid, newData));
            }
        }

        // Re-sort the leaderboard
        List<Map.Entry<Integer, LeaderboardPlayerCache>> entries = new ArrayList<>(getLeaderboard().entrySet());
        entries.sort((e1, e2) -> Integer.compare(e2.getValue().getData(), e1.getValue().getData()));

        // Rebuild with correct positions
        getLeaderboard().clear();
        for (int i = 0; i < entries.size(); i++) {
            getLeaderboard().put(i + 1, entries.get(i).getValue());
        }
    }

    private boolean isSameDay(long time1, long time2) {
        java.util.Calendar cal1 = java.util.Calendar.getInstance();
        cal1.setTimeInMillis(time1);
        java.util.Calendar cal2 = java.util.Calendar.getInstance();
        cal2.setTimeInMillis(time2);
        return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
                cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR);
    }

    private boolean isSameWeek(long time1, long time2) {
        java.util.Calendar cal1 = java.util.Calendar.getInstance();
        cal1.setTimeInMillis(time1);
        java.util.Calendar cal2 = java.util.Calendar.getInstance();
        cal2.setTimeInMillis(time2);
        return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
                cal1.get(java.util.Calendar.WEEK_OF_YEAR) == cal2.get(java.util.Calendar.WEEK_OF_YEAR);
    }

    private boolean isSameMonth(long time1, long time2) {
        java.util.Calendar cal1 = java.util.Calendar.getInstance();
        cal1.setTimeInMillis(time1);
        java.util.Calendar cal2 = java.util.Calendar.getInstance();
        cal2.setTimeInMillis(time2);
        return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
                cal1.get(java.util.Calendar.MONTH) == cal2.get(java.util.Calendar.MONTH);
    }

    private long getStatsValueLong(Document document, String path) {
        try {
            String[] paths = path.split("\\.");
            Object current = document;

            for (int j = 0; j < paths.length; j++) {
                if (current instanceof Document) {
                    current = ((Document) current).get(paths[j]);
                } else {
                    return 0L;
                }

                if (current == null)
                    return 0L;
            }

            if (current instanceof Number) {
                return ((Number) current).longValue();
            }

        } catch (Exception e) {
        }
        return 0L;
    }

    // Helper to dig into nested BSON/JSON
    private int getStatsValue(Document document, String path) {
        try {
            String[] paths = path.split("\\.");
            Object current = document;

            for (int j = 0; j < paths.length; j++) {
                if (current instanceof Document) {
                    current = ((Document) current).get(paths[j]);
                } else {
                    return 0; // Path broken
                }

                if (current == null)
                    return 0;
            }

            // Safe Number Casting
            if (current instanceof Number) {
                return ((Number) current).intValue();
            }

        } catch (Exception e) {
            // Suppress errors for missing keys
        }
        return 0;
    }

    public ItemStack getDisplayIcon() {
        List<String> lore = new ArrayList<>();

        getLeaderboard().forEach((key, value) -> lore
                .add(Language.LEADERBOARD_TOP10_DISPLAY_LORE.toString(key, value.getPlayerName(), value.getData())));

        return new ItemBuilder(kit.getDisplayIcon())
                .name(Language.LEADERBOARD_TOP10_DISPLAY_NAME.toString(kit.getDisplayName()))
                .lore(lore)
                .build().clone();
    }

}
