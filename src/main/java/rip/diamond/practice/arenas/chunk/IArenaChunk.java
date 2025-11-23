package rip.diamond.practice.arenas.chunk;

public interface IArenaChunk {

    void restore();

    void restore(org.bukkit.World world, int x, int z);

    int getX();

    int getZ();

}
