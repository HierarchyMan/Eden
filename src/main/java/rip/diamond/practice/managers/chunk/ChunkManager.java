package rip.diamond.practice.managers.chunk;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;
import rip.diamond.practice.Eden;

/**
 * ChunkManager preloads chunks for event spawn bounded areas to prevent lag spikes.
 * Arena chunks are already loaded by ArenaDetail.copyChunk(), so we only handle event locations.
 */
public class ChunkManager {

    private final Eden plugin = Eden.INSTANCE;
    private boolean chunksLoaded = false;

    public ChunkManager() {
        
        new BukkitRunnable() {
            @Override
            public void run() {
                loadEventChunks();
            }
        }.runTaskLater(plugin, 40L); 
    }

    /**
     * Loads chunks for event spawn bounded areas (SkyWars, Spleef, Four Corners)
     */
    private void loadEventChunks() {
        plugin.getLogger().info("[ChunkManager] Pre-loading event area chunks...");

        int count = 0;

        
        count += loadRegion(
            plugin.getSpawnManager().getSkywarsMin(),
            plugin.getSpawnManager().getSkywarsMax(),
            "SkyWars"
        );

        
        count += loadRegion(
            plugin.getSpawnManager().getSpleefMin(),
            plugin.getSpawnManager().getSpleefMax(),
            "Spleef"
        );

        
        count += loadRegion(
            plugin.getSpawnManager().getCornersMin(),
            plugin.getSpawnManager().getCornersMax(),
            "Four Corners"
        );

        plugin.getLogger().info("[ChunkManager] Loaded " + count + " event area chunks!");
        this.chunksLoaded = true;
    }

    /**
     * Loads all chunks in a bounded region with null safety
     */
    private int loadRegion(Location min, Location max, String name) {
        
        if (min == null || max == null) {
            return 0;
        }

        if (min.getWorld() == null || max.getWorld() == null) {
            plugin.getLogger().warning("[ChunkManager] " + name + " has null world!");
            return 0;
        }

        if (!min.getWorld().equals(max.getWorld())) {
            plugin.getLogger().warning("[ChunkManager] " + name + " has mismatched worlds!");
            return 0;
        }

        World world = min.getWorld();
        
        
        int minX = min.getBlockX() >> 4;
        int minZ = min.getBlockZ() >> 4;
        int maxX = max.getBlockX() >> 4;
        int maxZ = max.getBlockZ() >> 4;

        
        if (minX > maxX) {
            int temp = minX;
            minX = maxX;
            maxX = temp;
        }
        if (minZ > maxZ) {
            int temp = minZ;
            minZ = maxZ;
            maxZ = temp;
        }

        
        int loaded = 0;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                Chunk chunk = world.getChunkAt(x, z);
                if (!chunk.isLoaded()) {
                    chunk.load();
                    loaded++;
                }
            }
        }

        return loaded;
    }

    public boolean areChunksLoaded() {
        return chunksLoaded;
    }
}
