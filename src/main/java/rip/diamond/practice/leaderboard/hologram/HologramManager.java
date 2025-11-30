package rip.diamond.practice.leaderboard.hologram;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import rip.diamond.practice.Eden;
import rip.diamond.practice.kits.Kit;
import rip.diamond.practice.leaderboard.LeaderboardType;
import rip.diamond.practice.leaderboard.hologram.impl.LeaderboardHologram;
import rip.diamond.practice.util.file.ConfigCursor;
import rip.diamond.practice.util.serialization.LocationSerialization;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class HologramManager implements Listener {

    private final Set<PracticeHologram> holograms = ConcurrentHashMap.newKeySet();
    private final Eden plugin = Eden.INSTANCE;

    public HologramManager() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        loadHolograms();
    }

    public void loadHolograms() {
        holograms.forEach(PracticeHologram::stop);
        holograms.clear();
        ConfigCursor config = new ConfigCursor(plugin.getLocationFile(), "holograms");
        if (config.getConfigurationSection() == null)
            return;

        for (String key : config.getKeys()) {
            String path = "holograms." + key;
            String typeStr = plugin.getLocationFile().getConfiguration().getString(path + ".type");
            String periodStr = plugin.getLocationFile().getConfiguration().getString(path + ".period");
            String kitStr = plugin.getLocationFile().getConfiguration().getString(path + ".kit");
            String locationStr = plugin.getLocationFile().getConfiguration().getString(path + ".location");
            int updateTime = plugin.getLeaderboardsConfig().getConfiguration()
                    .getInt("holograms." + getConfigKey(typeStr) + ".update-time", 10);

            try {
                LeaderboardType type = LeaderboardType.valueOf(typeStr);
                LeaderboardType.TimePeriod period = LeaderboardType.TimePeriod.valueOf(periodStr);
                Kit kit = null;
                if (!kitStr.equals("ROTATING") && !kitStr.equals("GLOBAL")) {
                    kit = Kit.getByName(kitStr);
                }
                Location location = LocationSerialization.deserializeLocation(locationStr);

                LeaderboardHologram hologram = new LeaderboardHologram(location, updateTime, type, period, kit);
                holograms.add(hologram);
                hologram.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void saveHologram(LeaderboardType type, LeaderboardType.TimePeriod period, String kitName,
            Location location) {
        String id = type.name() + "_" + period.name() + "_" + kitName + "_" + System.currentTimeMillis();
        String path = "holograms." + id;

        plugin.getLocationFile().getConfiguration().set(path + ".type", type.name());
        plugin.getLocationFile().getConfiguration().set(path + ".period", period.name());
        plugin.getLocationFile().getConfiguration().set(path + ".kit", kitName);
        plugin.getLocationFile().getConfiguration().set(path + ".location",
                LocationSerialization.serializeLocation(location));
        plugin.getLocationFile().save();

        int updateTime = plugin.getLeaderboardsConfig().getConfiguration()
                .getInt("holograms." + getConfigKey(type.name()) + ".update-time", 10);
        Kit kit = null;
        if (!kitName.equals("ROTATING") && !kitName.equals("GLOBAL")) {
            kit = Kit.getByName(kitName);
        }

        LeaderboardHologram hologram = new LeaderboardHologram(location, updateTime, type, period, kit);
        holograms.add(hologram);
        hologram.start();

        // Show to online players immediately
        Bukkit.getOnlinePlayers().forEach(hologram::show);
    }

    public void removeNearestHologram(Player player) {
        PracticeHologram nearest = null;
        double distance = Double.MAX_VALUE;

        for (PracticeHologram hologram : holograms) {
            if (hologram.getLocation().getWorld().equals(player.getWorld())) {
                double d = hologram.getLocation().distance(player.getLocation());
                if (d < distance) {
                    distance = d;
                    nearest = hologram;
                }
            }
        }

        if (nearest != null && distance < 5) {
            holograms.remove(nearest);
            nearest.stop();

            // Remove from config
            // This is tricky because we don't store the config key in the object
            // We'll iterate the config to find the matching location
            ConfigCursor config = new ConfigCursor(plugin.getLocationFile(), "holograms");
            for (String key : config.getKeys()) {
                String locStr = plugin.getLocationFile().getConfiguration()
                        .getString("holograms." + key + ".location");
                if (locStr.equals(LocationSerialization.serializeLocation(nearest.getLocation()))) {
                    plugin.getLocationFile().getConfiguration().set("holograms." + key, null);
                    plugin.getLocationFile().save();
                    break;
                }
            }
            player.sendMessage(rip.diamond.practice.util.CC.GREEN + "Hologram removed.");
        } else {
            player.sendMessage(rip.diamond.practice.util.CC.RED + "No hologram found nearby.");
        }
    }

    public List<PracticeHologram> getHolograms() {
        return new ArrayList<>(holograms);
    }

    private String getConfigKey(String typeName) {
        if (typeName.equals("GLOBAL_WINS"))
            return "global_wins";
        if (typeName.equals("GLOBAL_ELO"))
            return "global_elo";
        if (typeName.equals("EVENT_WINS") || typeName.equals("EVENT_WINS_DAILY")
                || typeName.equals("EVENT_WINS_WEEKLY") || typeName.equals("EVENT_WINS_MONTHLY"))
            return "event_wins";
        if (typeName.equals("EVENTS_PLAYED"))
            return "events_played";
        if (typeName.contains("WINSTREAK"))
            return "winstreak";
        if (typeName.contains("WINS"))
            return "wins";
        if (typeName.contains("ELO"))
            return "elo";
        if (typeName.contains("LOSSES"))
            return "losses";
        return "wins";
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        holograms.forEach(h -> h.show(event.getPlayer()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        holograms.forEach(h -> h.hide(event.getPlayer()));
    }

    @EventHandler
    public void onChunkUnload(org.bukkit.event.world.ChunkUnloadEvent event) {
        for (PracticeHologram hologram : holograms) {
            if (hologram.getLocation().getChunk().equals(event.getChunk())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onWorldChange(org.bukkit.event.player.PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        holograms.forEach(h -> {
            if (h.getLocation().getWorld().equals(player.getWorld())) {
                h.show(player);
            }
        });
    }

    @EventHandler
    public void onTeleport(org.bukkit.event.player.PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        Location to = event.getTo();
        Location from = event.getFrom();

        if (to.getWorld().equals(from.getWorld()) && to.distanceSquared(from) < 100) {
            return;
        }

        holograms.forEach(h -> {
            if (h.getLocation().getWorld().equals(to.getWorld())) {
                h.show(player);
            }
        });
    }
}
