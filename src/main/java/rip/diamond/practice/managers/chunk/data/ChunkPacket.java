package rip.diamond.practice.managers.chunk.data;

import net.minecraft.server.v1_8_R3.PacketPlayOutMapChunk;
import org.bukkit.craftbukkit.v1_8_R3.CraftChunk;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

public class ChunkPacket {

    private final net.minecraft.server.v1_8_R3.Chunk chunk;

    public ChunkPacket(org.bukkit.Chunk chunk) {
        this.chunk = ((CraftChunk) chunk).getHandle();
    }

    public final void send(Player player) {
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(
                new PacketPlayOutMapChunk(chunk, true, 20));
    }
}
