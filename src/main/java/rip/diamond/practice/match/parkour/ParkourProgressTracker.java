package rip.diamond.practice.match.parkour;

import org.bukkit.Location;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores parkour progression per player inside a match.
 *
 * - lastCheckpoint: last checkpoint the player has passed.
 * - lastCheckpointIndex: which checkpoint index (1..N), or 0 if none.
 */
public class ParkourProgressTracker {

    private final Map<UUID, Location> lastCheckpoint = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> lastCheckpointIndex = new ConcurrentHashMap<>();

    public void clear(UUID uuid) {
        if (uuid == null) {
            return;
        }
        lastCheckpoint.remove(uuid);
        lastCheckpointIndex.remove(uuid);
    }

    public Location getLastCheckpoint(UUID uuid) {
        Location loc = lastCheckpoint.get(uuid);
        return loc == null ? null : loc.clone();
    }

    public int getLastCheckpointIndex(UUID uuid) {
        Integer idx = lastCheckpointIndex.get(uuid);
        return idx == null ? 0 : idx;
    }

    public void set(UUID uuid, Location checkpoint, int index) {
        if (uuid == null || checkpoint == null) {
            return;
        }
        lastCheckpoint.put(uuid, checkpoint.clone());
        lastCheckpointIndex.put(uuid, Math.max(0, index));
    }
}

