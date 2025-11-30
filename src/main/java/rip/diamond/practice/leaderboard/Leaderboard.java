package rip.diamond.practice.leaderboard;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bson.Document;

import java.util.LinkedHashMap;
import java.util.List;

@Getter
@RequiredArgsConstructor
public abstract class Leaderboard {

    private final LeaderboardType type;
    private final LinkedHashMap<Integer, LeaderboardPlayerCache> leaderboard = new LinkedHashMap<>();

    public abstract void update(List<Document> profilesSnapshot);

    public int getPlayerRank(java.util.UUID uuid) {
        for (java.util.Map.Entry<Integer, LeaderboardPlayerCache> entry : leaderboard.entrySet()) {
            if (entry.getValue().getPlayerUUID().equals(uuid)) {
                return entry.getKey();
            }
        }
        return -1;
    }

}
