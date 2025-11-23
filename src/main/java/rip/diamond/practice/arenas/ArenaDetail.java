package rip.diamond.practice.arenas;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import rip.diamond.practice.arenas.chunk.ArenaChunk;
import rip.diamond.practice.arenas.chunk.IArenaChunk;
import rip.diamond.practice.util.Common;
import rip.diamond.practice.util.cuboid.Cuboid;
import rip.diamond.practice.config.Config;
import rip.diamond.practice.util.serialization.LocationSerialization;
import com.boydti.fawe.util.EditSessionBuilder;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ArenaDetail {

    private final Arena arena;
    private final List<IArenaChunk> cachedChunks;

    private Location a;
    private Location b;
    private Location spectator;

    private Location min;
    private Location max;

    private boolean using = false;

    public ArenaDetail(Arena arena) {
        this.arena = arena;
        this.cachedChunks = new ArrayList<>();
    }

    public ArenaDetail(Arena arena, Location a, Location b, Location spectator, Location min, Location max) {
        this.arena = arena;
        this.cachedChunks = new ArrayList<>();
        this.a = a;
        this.b = b;
        this.spectator = spectator;
        this.min = min;
        this.max = max;
    }

    public boolean isFinishedSetup() {
        return a != null && b != null && min != null && max != null;
    }

    public Cuboid getCuboid() {
        return new Cuboid(min, max);
    }

    public void copyChunk() {
        if (Config.EXPERIMENT_DISABLE_ORIGINAL_ARENA.toBoolean()) {
            // If this is a copy (not the first one), do not cache chunks
            if (arena.getArenaDetails().indexOf(this) > 0) {
                return;
            }
        }

        cachedChunks.clear();
        Cuboid cuboid = new Cuboid(min, max);
        try {
            cuboid.getChunks().forEach(chunk -> cachedChunks.add(new ArenaChunk(chunk)));
        } catch (Exception e) {
            Common.log(
                    "&c[Eden] An error occurred while trying to copy your arena. This will cause arena will not reset and strongly recommend to fix it ASAP. (min:"
                            + LocationSerialization.toReadable(min) + " &cmax:" + LocationSerialization.toReadable(max)
                            + "&c)");
            e.printStackTrace();
        }
    }

    public void restoreChunk(boolean async, boolean releaseArena) {
        long started = System.currentTimeMillis();
        Common.debug("正在嘗試還原場地...");

        if (Config.EXPERIMENT_DISABLE_ORIGINAL_ARENA.toBoolean()) {
            ArenaDetail originalDetail = arena.getArenaDetails().get(0);

            if (this != originalDetail) {
                Location origMinLoc = originalDetail.getMin();
                Location origMaxLoc = originalDetail.getMax();
                Location destMinLoc = this.getMin();
                Location destMaxLoc = this.getMax();

                // FAWE operations - fully async now
                Runnable faweRunnable = () -> {
                    try {
                        // Use proper min/max ordering for source
                        Vector sourceMin = new Vector(
                                Math.min(origMinLoc.getBlockX(), origMaxLoc.getBlockX()),
                                Math.min(origMinLoc.getBlockY(), origMaxLoc.getBlockY()),
                                Math.min(origMinLoc.getBlockZ(), origMaxLoc.getBlockZ()));
                        Vector sourceMax = new Vector(
                                Math.max(origMinLoc.getBlockX(), origMaxLoc.getBlockX()),
                                Math.max(origMinLoc.getBlockY(), origMaxLoc.getBlockY()),
                                Math.max(origMinLoc.getBlockZ(), origMaxLoc.getBlockZ()));

                        // 1. Copy from Original using modern API (FAWE handles chunk loading
                        // internally)
                        EditSession copySession = new EditSessionBuilder(origMinLoc.getWorld().getName())
                                .fastmode(true)
                                .allowedRegionsEverywhere()
                                .autoQueue(false)
                                .limitUnlimited()
                                .build();

                        com.sk89q.worldedit.regions.CuboidRegion sourceRegion = new com.sk89q.worldedit.regions.CuboidRegion(
                                sourceMin, sourceMax);
                        com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard clipboard = new com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard(
                                sourceRegion);

                        com.sk89q.worldedit.function.operation.ForwardExtentCopy copy = new com.sk89q.worldedit.function.operation.ForwardExtentCopy(
                                copySession, sourceRegion, clipboard, sourceMin);
                        com.sk89q.worldedit.function.operation.Operations.complete(copy);

                        // 2. Paste to Current location
                        EditSession pasteSession = new EditSessionBuilder(destMinLoc.getWorld().getName())
                                .fastmode(true)
                                .allowedRegionsEverywhere()
                                .autoQueue(false)
                                .limitUnlimited()
                                .build();

                        Vector destMin = new Vector(
                                Math.min(destMinLoc.getBlockX(), destMaxLoc.getBlockX()),
                                Math.min(destMinLoc.getBlockY(), destMaxLoc.getBlockY()),
                                Math.min(destMinLoc.getBlockZ(), destMaxLoc.getBlockZ()));

                        com.sk89q.worldedit.function.operation.ForwardExtentCopy paste = new com.sk89q.worldedit.function.operation.ForwardExtentCopy(
                                clipboard, clipboard.getRegion(), pasteSession, destMin);
                        com.sk89q.worldedit.function.operation.Operations.complete(paste);
                        pasteSession.flushQueue();

                        Common.debug("還原場地耗時: " + (System.currentTimeMillis() - started) + "ms");
                    } catch (Exception e) {
                        Common.log("&c[Eden] Error restoring arena: " + e.getMessage());
                        e.printStackTrace();
                    } finally {
                        if (releaseArena) {
                            TaskManager.IMP.task(() -> this.using = false);
                        }
                    }
                };

                if (async) {
                    TaskManager.IMP.async(faweRunnable);
                } else {
                    faweRunnable.run();
                }
            } else {
                // Original arena - just release if needed
                if (releaseArena) {
                    this.using = false;
                }
            }
        } else {
            // Legacy mode: use cached chunks (MUST run on main thread due to NMS)
            Runnable runnable = () -> {
                try {
                    cachedChunks.forEach(IArenaChunk::restore);
                    Common.debug("還原場地耗時: " + (System.currentTimeMillis() - started) + "ms");
                } catch (Exception e) {
                    Common.log("&c[Eden] Error restoring arena: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    if (releaseArena) {
                        this.using = false;
                    }
                }
            };

            // NMS chunk operations MUST run on main thread
            if (async) {
                TaskManager.IMP.task(runnable);
            } else {
                runnable.run();
            }
        }
    }
}
