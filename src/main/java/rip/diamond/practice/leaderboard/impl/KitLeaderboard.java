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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

@Getter
public class KitLeaderboard extends Leaderboard {

    private final Kit kit;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
    private volatile List<LeaderboardPlayerCache> snapshot = new ArrayList<>();

    public KitLeaderboard(LeaderboardType type, Kit kit) {
        super(type);
        this.kit = kit;
    }

    @Override
    public void update(List<Document> profilesSnapshot) {
        if (profilesSnapshot == null || profilesSnapshot.isEmpty()) {
            return;
        }

        List<LeaderboardPlayerCache> topEntries = profilesSnapshot.stream()
                .map(doc -> buildCacheEntry(doc, getType().getPath(kit)))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(LeaderboardPlayerCache::getData).reversed())
                .limit(10)
                .collect(Collectors.toList());

        lock.writeLock().lock();
        try {
            LinkedHashMap<Integer, LeaderboardPlayerCache> board = getLeaderboard();
            board.clear();
            int pos = 1;
            for (LeaderboardPlayerCache cache : topEntries) {
                board.put(pos++, cache);
            }
            snapshot = new ArrayList<>(board.values());
        } finally {
            lock.writeLock().unlock();
        }
    }

    private LeaderboardPlayerCache buildCacheEntry(Document document, String path) {
        String username = document.getString("username");
        UUID uuid = UUID.fromString(document.getString("uuid"));
        int data = getStatsValue(document, path);

        if (getType().getTimePeriod() != LeaderboardType.TimePeriod.LIFETIME) {
            String currentPath = getType().getPath(kit);
            String parentPath = currentPath.substring(0, currentPath.lastIndexOf('.'));
            String resetPath = "";

            switch (getType().getTimePeriod()) {
                case DAILY:
                    resetPath = parentPath + ".lastDailyReset";
                    long dailyDate = getStatsValueLong(document, resetPath);
                    if (!isSameDay(dailyDate, System.currentTimeMillis()))
                        data = 0;
                    break;
                case WEEKLY:
                    resetPath = parentPath + ".lastWeeklyReset";
                    long weeklyDate = getStatsValueLong(document, resetPath);
                    if (!isSameWeek(weeklyDate, System.currentTimeMillis()))
                        data = 0;
                    break;
                case MONTHLY:
                    resetPath = parentPath + ".lastMonthlyReset";
                    long monthlyDate = getStatsValueLong(document, resetPath);
                    if (!isSameMonth(monthlyDate, System.currentTimeMillis()))
                        data = 0;
                    break;
            }
        }

        if (data == 0) {
            return null;
        }

        return new LeaderboardPlayerCache(username, uuid, data);
    }

    /**
     * Incrementally update a single player's stats in the leaderboard
     * This is much more performant than doing a full update
     */
    public void updatePlayer(UUID uuid, String username, int newData) {
        lock.writeLock().lock();
        try {
            LinkedHashMap<Integer, LeaderboardPlayerCache> board = getLeaderboard();

            LeaderboardPlayerCache existing = board.values().stream()
                    .filter(entry -> entry.getPlayerUUID().equals(uuid)).findFirst().orElse(null);

            if (existing != null) {
            } else {
            }

            if (newData <= 0) {
                if (existing != null) {
                    // Find and remove the entry by iterating through the map
                    board.entrySet().removeIf(entry -> entry.getValue().getPlayerUUID().equals(uuid));
                    rebuildPositions(board);
                }
                snapshot = new ArrayList<>(board.values());
                return;
            }

            if (existing != null) {
                existing.setData(newData);
            } else if (board.size() < 10) {
                board.put(board.size() + 1, new LeaderboardPlayerCache(username, uuid, newData));
            } else {
                Map.Entry<Integer, LeaderboardPlayerCache> worst = board.entrySet().stream()
                        .min(Comparator.comparingInt(e -> e.getValue().getData()))
                        .orElse(null);
                if (worst != null && worst.getValue().getData() < newData) {
                    board.remove(worst.getKey());
                    board.put(worst.getKey(), new LeaderboardPlayerCache(username, uuid, newData));
                } else {
                    return;
                }
            }
            rebuildPositions(board);
            snapshot = new ArrayList<>(board.values());
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void rebuildPositions(LinkedHashMap<Integer, LeaderboardPlayerCache> board) {
        List<LeaderboardPlayerCache> sorted = board.values().stream()
                .sorted(Comparator.comparingInt(LeaderboardPlayerCache::getData).reversed())
                .collect(Collectors.toList());
        board.clear();
        for (int i = 0; i < sorted.size(); i++) {
            board.put(i + 1, sorted.get(i));
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

    private int getStatsValue(Document document, String path) {
        try {
            String[] paths = path.split("\\.");
            Object current = document;

            for (int j = 0; j < paths.length; j++) {
                if (current instanceof Document) {
                    current = ((Document) current).get(paths[j]);
                } else {
                    return 0;
                }

                if (current == null)
                    return 0;
            }

            if (current instanceof Number) {
                return ((Number) current).intValue();
            }

        } catch (Exception e) {

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

    public List<LeaderboardPlayerCache> getSnapshot() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(snapshot);
        } finally {
            lock.readLock().unlock();
        }
    }
}
