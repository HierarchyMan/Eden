package rip.diamond.practice.match.parkour;

import org.bukkit.Location;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Match-scoped storage for parkour progress.
 *
 * We store the last checkpoint a player has successfully passed.
 * If none was passed, respawn falls back to team spawn.
 */
public class ParkourCheckpointTracker {

    private final Map<UUID, Location> lastCheckpoint = new ConcurrentHashMap<>();

    public void clear(UUID uuid) {
        lastCheckpoint.remove(uuid);
    }

    public void clearAll() {
        lastCheckpoint.clear();
    }

    public Location get(UUID uuid) {
        return lastCheckpoint.get(uuid);
    }

    public void set(UUID uuid, Location checkpoint) {
        if (uuid == null || checkpoint == null) {
            return;
        }
        lastCheckpoint.put(uuid, checkpoint.clone());
    }
}

