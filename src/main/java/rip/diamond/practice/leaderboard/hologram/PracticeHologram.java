package rip.diamond.practice.leaderboard.hologram;

import lombok.Getter;
import net.minecraft.server.v1_8_R3.EntityArmorStand;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityDestroy;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityMetadata;
import net.minecraft.server.v1_8_R3.DataWatcher;
import net.minecraft.server.v1_8_R3.PacketPlayOutSpawnEntityLiving;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import rip.diamond.practice.Eden;

import java.util.ArrayList;
import java.util.List;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
public abstract class PracticeHologram {

    private final List<EntityArmorStand> entities = new ArrayList<>();
    private final List<String> rawLines = new ArrayList<>();
    private final List<String> lines = new ArrayList<>();
    private final Location location;
    private final int time;
    protected int actualTime;
    protected final Set<UUID> viewers = new HashSet<>();

    public PracticeHologram(Location location, int time) {
        this.location = location;
        this.time = time;
        this.actualTime = time;
    }

    protected org.bukkit.scheduler.BukkitTask task;

    protected java.util.function.Predicate<Player> viewerFilter = player -> true;

    protected long getTickPeriod() {
        return 20L;
    }

    public void start() {
        this.task = Bukkit.getScheduler().runTaskTimerAsynchronously(Eden.INSTANCE, () -> {
            try {
                tick();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0L, getTickPeriod());
    }

    public void stop() {
        if (this.task != null) {
            this.task.cancel();
            this.task = null;
        }
        Bukkit.getOnlinePlayers().stream().filter(viewerFilter).forEach(this::hide);
    }

    public void tick() {
        actualTime--;

        if (actualTime < 1) {
            actualTime = time;
            update();
        }

        lines.clear();
        updateLines();

        if (lines.size() == entities.size()) {
            // Update raw lines and send metadata packets
            rawLines.clear();
            rawLines.addAll(lines);
            Bukkit.getOnlinePlayers().stream().filter(viewerFilter).forEach(this::updateMetadata);
        } else {
            // Recreate entities
            Bukkit.getOnlinePlayers().stream().filter(viewerFilter).forEach(this::hide);
            entities.clear();
            rawLines.clear();
            rawLines.addAll(lines);

            double y = location.getY();

            for (String s : lines) {
                EntityArmorStand stand = new EntityArmorStand(((CraftWorld) location.getWorld()).getHandle(),
                        location.getX(), y, location.getZ());

                stand.setInvisible(true);
                stand.setGravity(false);
                stand.setSmall(true);
                entities.add(stand);
                y -= 0.25;
            }

            Bukkit.getOnlinePlayers().stream().filter(viewerFilter).forEach(this::show);
        }
    }

    public abstract void update();

    public abstract void updateLines();

    protected void updateMetadata(Player player) {
        for (int i = 0; i < entities.size(); i++) {
            EntityArmorStand stand = entities.get(i);
            String line = rawLines.get(i);
            String text = applyPlaceholders(line, player);
            sendMetadata(player, stand, text);
        }
    }

    protected void sendMetadata(Player player, EntityArmorStand stand, String text) {
        boolean isEmpty = text == null || text.trim().isEmpty() || text.equalsIgnoreCase("<void>");
        String name = isEmpty ? " " : text;
        boolean visible = !isEmpty;

        DataWatcher watcher = new DataWatcher(null);
        watcher.a(2, name); // Custom Name
        watcher.a(3, (byte) (visible ? 1 : 0)); // Custom Name Visible

        PacketPlayOutEntityMetadata packet = new PacketPlayOutEntityMetadata(stand.getId(), watcher, true);
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
    }

    public void show(Player player) {
        for (int i = 0; i < entities.size(); i++) {
            EntityArmorStand stand = entities.get(i);
            String line = rawLines.get(i);
            String text = applyPlaceholders(line, player);

            // Send spawn packet (uses entity's current state, which we don't modify for
            // name)
            PacketPlayOutSpawnEntityLiving armorStandPacket = new PacketPlayOutSpawnEntityLiving(stand);
            ((CraftPlayer) player).getHandle().playerConnection.networkManager.channel.write(armorStandPacket);

            sendMetadata(player, stand, text);
        }
    }

    public void hide(Player player) {
        for (EntityArmorStand stand : entities) {
            PacketPlayOutEntityDestroy packet = new PacketPlayOutEntityDestroy(stand.getId());
            ((CraftPlayer) player).getHandle().playerConnection.networkManager.channel.write(packet);
        }
    }

    protected String applyPlaceholders(String line, Player viewer) {
        return line;
    }
}
