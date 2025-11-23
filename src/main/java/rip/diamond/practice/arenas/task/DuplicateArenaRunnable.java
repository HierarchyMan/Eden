package rip.diamond.practice.arenas.task;

import com.boydti.fawe.util.EditSessionBuilder;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.regions.CuboidRegion;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;
import rip.diamond.practice.Eden;
import rip.diamond.practice.arenas.Arena;
import rip.diamond.practice.arenas.ArenaDetail;

/**
 * @since 11/25/2017
 * @author Zonix
 */

@Getter
public abstract class DuplicateArenaRunnable extends BukkitRunnable {

    private final Eden plugin = Eden.INSTANCE;
    private final Arena copiedArena;
    private int offsetX;
    private int offsetZ;
    private final int incrementX;
    private final int incrementZ;

    private BlockArrayClipboard clipboard;
    private Vector clipboardOrigin;

    public DuplicateArenaRunnable(Arena copiedArena, int offsetX, int offsetZ, int incrementX, int incrementZ) {
        this.copiedArena = copiedArena;
        this.offsetX = offsetX;
        this.offsetZ = offsetZ;
        this.incrementX = incrementX;
        this.incrementZ = incrementZ;
    }

    @Override
    public void run() {
        // Step 1: Copy the arena if not already copied
        if (this.clipboard == null) {
            ArenaDetail originalDetail = copiedArena.getArenaDetails().get(0);
            Location minLoc = originalDetail.getMin();
            Location maxLoc = originalDetail.getMax();

            // Ensure chunks are loaded at source
            int minChunkX = minLoc.getBlockX() >> 4;
            int minChunkZ = minLoc.getBlockZ() >> 4;
            int maxChunkX = maxLoc.getBlockX() >> 4;
            int maxChunkZ = maxLoc.getBlockZ() >> 4;

            for (int cx = minChunkX; cx <= maxChunkX; cx++) {
                for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                    if (!minLoc.getWorld().isChunkLoaded(cx, cz)) {
                        minLoc.getWorld().loadChunk(cx, cz);
                    }
                }
            }

            // Use proper min/max ordering
            Vector min = new Vector(
                    Math.min(minLoc.getBlockX(), maxLoc.getBlockX()),
                    Math.min(minLoc.getBlockY(), maxLoc.getBlockY()),
                    Math.min(minLoc.getBlockZ(), maxLoc.getBlockZ()));
            Vector max = new Vector(
                    Math.max(minLoc.getBlockX(), maxLoc.getBlockX()),
                    Math.max(minLoc.getBlockY(), maxLoc.getBlockY()),
                    Math.max(minLoc.getBlockZ(), maxLoc.getBlockZ()));

            CuboidRegion region = new CuboidRegion(min, max);
            this.clipboard = new BlockArrayClipboard(region);
            this.clipboardOrigin = min;

            EditSession copySession = new EditSessionBuilder(minLoc.getWorld().getName())
                    .fastmode(true)
                    .allowedRegionsEverywhere()
                    .autoQueue(false)
                    .limitUnlimited()
                    .build();

            ForwardExtentCopy copy = new ForwardExtentCopy(copySession, region, this.clipboard, min);
            try {
                Operations.complete(copy);
            } catch (Exception e) {
                e.printStackTrace();
                this.cancel();
                return;
            }

            // Wait for next tick to start searching
            return;
        }

        // Step 2: Find a safe spot
        // We check if the proposed location overlaps with any existing arena
        ArenaDetail originalDetail = copiedArena.getArenaDetails().get(0);
        Location minLoc = originalDetail.getMin();
        Location maxLoc = originalDetail.getMax();

        double proposedMinX = minLoc.getX() + this.offsetX;
        double proposedMinZ = minLoc.getZ() + this.offsetZ;
        double proposedMaxX = maxLoc.getX() + this.offsetX;
        double proposedMaxZ = maxLoc.getZ() + this.offsetZ;

        boolean safe = true;

        // Check collision with all other arenas
        for (Arena arena : Arena.getArenas()) {
            for (ArenaDetail detail : arena.getArenaDetails()) {
                // Simple AABB collision check
                // We only care about X and Z overlap, Y is usually same or irrelevant if we
                // assume flat world
                // But let's check X and Z strictly

                double otherMinX = Math.min(detail.getMin().getX(), detail.getMax().getX());
                double otherMaxX = Math.max(detail.getMin().getX(), detail.getMax().getX());
                double otherMinZ = Math.min(detail.getMin().getZ(), detail.getMax().getZ());
                double otherMaxZ = Math.max(detail.getMin().getZ(), detail.getMax().getZ());

                double myMinX = Math.min(proposedMinX, proposedMaxX);
                double myMaxX = Math.max(proposedMinX, proposedMaxX);
                double myMinZ = Math.min(proposedMinZ, proposedMaxZ);
                double myMaxZ = Math.max(proposedMinZ, proposedMaxZ);

                boolean overlapX = myMinX <= otherMaxX && myMaxX >= otherMinX;
                boolean overlapZ = myMinZ <= otherMaxZ && myMaxZ >= otherMinZ;

                if (overlapX && overlapZ) {
                    safe = false;
                    break;
                }
            }
            if (!safe)
                break;
        }

        if (!safe) {
            this.offsetX += this.incrementX;
            this.offsetZ += this.incrementZ;
            return;
        }

        // Step 3: Paste
        this.cancel();

        final int finalOffsetX = this.offsetX;
        final int finalOffsetZ = this.offsetZ;

        TaskManager.IMP.async(() -> {
            try {
                EditSession pasteSession = new EditSessionBuilder(minLoc.getWorld().getName())
                        .fastmode(true)
                        .allowedRegionsEverywhere()
                        .autoQueue(false)
                        .limitUnlimited()
                        .build();

                Vector pasteLocation = new Vector(
                        clipboardOrigin.getX() + finalOffsetX,
                        clipboardOrigin.getY(),
                        clipboardOrigin.getZ() + finalOffsetZ);

                ForwardExtentCopy paste = new ForwardExtentCopy(
                        this.clipboard,
                        this.clipboard.getRegion(),
                        pasteSession,
                        pasteLocation);

                Operations.complete(paste);
                pasteSession.flushQueue();

                // Callback to main thread
                TaskManager.IMP.task(this::onComplete);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public abstract void onComplete();
}
