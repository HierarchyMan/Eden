package rip.diamond.practice.managers.chunk;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import rip.diamond.practice.arenas.chunk.IArenaChunk;
import rip.diamond.practice.managers.chunk.restoration.IChunkRestoration;
import rip.diamond.practice.managers.chunk.restoration.impl.EventChunkRestoration;
import rip.diamond.practice.util.cuboid.Cuboid;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class ChunkRestorationManager {

    @Getter
    @Setter(AccessLevel.PRIVATE)
    private static IChunkRestoration iChunkRestoration;

    private final Map<Cuboid, List<IArenaChunk>> eventMapChunks = new ConcurrentHashMap<>();

    public ChunkRestorationManager() {
        if (iChunkRestoration == null) {
            setIChunkRestoration(new EventChunkRestoration(this));
        }
    }

    /**
     * Copy the current state of a cuboid region for later restoration
     * 
     * @param cuboid The cuboid region to copy
     */
    public void copy(Cuboid cuboid) {
        if (iChunkRestoration != null && cuboid != null) {
            iChunkRestoration.copy(cuboid);
        }
    }

    /**
     * Reset a cuboid region to its previously copied state
     * 
     * @param cuboid The cuboid region to reset
     */
    public void reset(Cuboid cuboid) {
        if (iChunkRestoration != null && cuboid != null) {
            iChunkRestoration.reset(cuboid);
        }
    }
}
