package rip.diamond.practice.leaderboard.impl;

import org.bson.Document;
import rip.diamond.practice.kits.Kit;
import rip.diamond.practice.leaderboard.Leaderboard;
import rip.diamond.practice.leaderboard.LeaderboardPlayerCache;
import rip.diamond.practice.leaderboard.LeaderboardType;

import java.util.*;
import java.util.stream.Collectors;

public class GlobalLeaderboard extends Leaderboard {

    public GlobalLeaderboard(LeaderboardType type) {
        super(type);
    }

    @Override
    public void update(List<Document> profilesSnapshot) {
        if (profilesSnapshot == null || profilesSnapshot.isEmpty()) {
            return;
        }

        List<LeaderboardPlayerCache> topEntries = profilesSnapshot.stream()
                .map(this::buildCacheEntry)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(LeaderboardPlayerCache::getData).reversed())
                .limit(10)
                .collect(Collectors.toList());

        LinkedHashMap<Integer, LeaderboardPlayerCache> board = getLeaderboard();
        board.clear();
        int pos = 1;
        for (LeaderboardPlayerCache cache : topEntries) {
            board.put(pos++, cache);
        }
    }

    private LeaderboardPlayerCache buildCacheEntry(Document document) {
        String username = document.getString("username");
        UUID uuid = UUID.fromString(document.getString("uuid"));
        int data = 0;

        if (getType() == LeaderboardType.GLOBAL_WINS) {
            // Sum wins from all kits
            Document kitData = document.get("kitData", Document.class);
            if (kitData != null) {
                for (String key : kitData.keySet()) {
                    Document kitDoc = kitData.get(key, Document.class);
                    if (kitDoc != null) {
                        data += kitDoc.getInteger("rankedWon", 0);
                        data += kitDoc.getInteger("unrankedWon", 0);
                    }
                }
            }
        } else if (getType() == LeaderboardType.GLOBAL_ELO) {
            Document temp = document.get("temporary", Document.class);
            if (temp != null && temp.containsKey("globalElo")) {
                data = temp.getInteger("globalElo");
            } else {
                // Fallback calculation
                Document kitData = document.get("kitData", Document.class);
                if (kitData != null && !kitData.isEmpty()) {
                    int totalElo = 0;
                    int count = 0;
                    for (String key : kitData.keySet()) {
                        Document kitDoc = kitData.get(key, Document.class);
                        if (kitDoc != null) {
                            Kit kit = Kit.getByName(key);
                            if (kit != null && kit.isRanked()) {
                                totalElo += kitDoc.getInteger("elo", 1000);
                                count++;
                            }
                        }
                    }
                    data = count == 0 ? 1000 : totalElo / count;
                } else {
                    data = 1000;
                }
            }
        }

        if (data == 0 && getType() == LeaderboardType.GLOBAL_WINS) {
            return null;
        }

        return new LeaderboardPlayerCache(username, uuid, data);
    }

    /**
     * Incrementally update a single player's stats in the leaderboard
     * This is much more performant than doing a full update
     */
    public void updatePlayer(UUID uuid, String username, int newData) {
        LinkedHashMap<Integer, LeaderboardPlayerCache> board = getLeaderboard();
        LeaderboardPlayerCache existing = board.values().stream()
                .filter(entry -> entry.getPlayerUUID().equals(uuid)).findFirst().orElse(null);
        if (newData <= 0) {
            if (existing != null) {
                // Find and remove the entry by iterating through the map
                board.entrySet().removeIf(entry -> entry.getValue().getPlayerUUID().equals(uuid));
                rebuildPositions(board);
            }
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
}
