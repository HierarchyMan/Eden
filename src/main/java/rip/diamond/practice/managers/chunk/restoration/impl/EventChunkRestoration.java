package rip.diamond.practice.managers.chunk.restoration.impl;

import lombok.RequiredArgsConstructor;
import rip.diamond.practice.arenas.chunk.ArenaChunk;
import rip.diamond.practice.arenas.chunk.IArenaChunk;
import rip.diamond.practice.managers.chunk.ChunkRestorationManager;
import rip.diamond.practice.managers.chunk.restoration.IChunkRestoration;
import rip.diamond.practice.util.cuboid.Cuboid;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class EventChunkRestoration implements IChunkRestoration {

    private final ChunkRestorationManager chunkRestorationManager;

    @Override
    public void copy(Cuboid cuboid) {
        long startTime = System.currentTimeMillis();

        List<IArenaChunk> chunks = new ArrayList<>();
        cuboid.getChunks().forEach(chunk -> chunks.add(new ArenaChunk(chunk)));

        chunkRestorationManager.getEventMapChunks().put(cuboid, chunks);

        System.out.println("Chunks copied for Event! (" + (System.currentTimeMillis() - startTime) + "ms)");
    }

    @Override
    public void reset(Cuboid cuboid) {
        long startTime = System.currentTimeMillis();

        List<IArenaChunk> chunks = chunkRestorationManager.getEventMapChunks().get(cuboid);
        if (chunks != null) {
            chunks.forEach(IArenaChunk::restore);
        }

        System.out
                .println("Chunks have been reset for Event! (took " + (System.currentTimeMillis() - startTime) + "ms)");
    }
}
