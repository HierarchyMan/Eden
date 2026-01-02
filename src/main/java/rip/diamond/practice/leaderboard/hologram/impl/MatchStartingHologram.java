package rip.diamond.practice.leaderboard.hologram.impl;

import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.entity.Player;
import net.minecraft.server.v1_8_R3.EntityArmorStand;
import rip.diamond.practice.kits.Kit;
import rip.diamond.practice.leaderboard.LeaderboardType;

import java.util.List;

import rip.diamond.practice.Eden;
import rip.diamond.practice.leaderboard.LeaderboardPlayerCache;
import rip.diamond.practice.util.CC;
import rip.diamond.practice.util.file.ConfigCursor;

import rip.diamond.practice.match.team.Team;

public class MatchStartingHologram extends LeaderboardHologram {

    private final Team team;
    private final String side;

    public MatchStartingHologram(Location location, int time, LeaderboardType type, LeaderboardType.TimePeriod period,
            Kit kit, Team team, String side) {
        super(location, time, type, period, kit);
        this.team = team;
        this.side = side;
        this.viewerFilter = player -> team.getPlayers().contains(player);
    }

    @Override
    public void updateLines() {
        ConfigCursor config = new ConfigCursor(Eden.INSTANCE.getLeaderboardsConfig(),
                "match-starting-holograms." + side);
        List<String> formatLines = config.getStringList("lines");

        for (String line : formatLines) {
            if (line.contains("{entries}")) {
                List<LeaderboardPlayerCache> topPlayers = getTopPlayers();
                String entryFormat = config.getString("format");
                for (int i = 0; i < 10; i++) {
                    if (i < topPlayers.size()) {
                        LeaderboardPlayerCache player = topPlayers.get(i);
                        getLines().add(CC.translate(entryFormat
                                .replace("{number}", String.valueOf(i + 1))
                                .replace("{name}", player.getPlayerName())
                                .replace("{value}", String.valueOf(player.getData()))));
                    } else {
                        getLines().add(CC.translate(entryFormat
                                .replace("{number}", String.valueOf(i + 1))
                                .replace("{name}", "-")
                                .replace("{value}", "-")));
                    }
                }
            } else {
                getLines().add(CC.translate(line
                        .replace("{kit}", getKit() != null ? getKit().getDisplayName() : "Global")
                        .replace("{period}", getPeriodDisplay())
                        .replace("{update}", String.valueOf(getActualTime()))
                        .replace("{lb-type}", getTypeDisplay())));
            }
        }
    }

    private int tickCounter = 0;

    @Override
    protected long getTickPeriod() {
        return 2L;
    }

    @Override
    public void tick() {
        if (team.getAliveCount() == 0 && team.getDisconnectedCount() == team.getTeamPlayers().size()) {
            stop();
            return;
        }

        tickCounter++;
        if (tickCounter % 10 == 0) {
            actualTime--;
            if (actualTime < 1) {
                actualTime = getTime();
                update();
            }
        }

        getLines().clear();
        updateLines();

        if (getLines().isEmpty()) {
        }

        boolean recreate = false;
        if (getLines().size() != getEntities().size()) {
            recreate = true;
        }

        if (recreate) {
            for (java.util.UUID uuid : viewers) {
                Player p = org.bukkit.Bukkit.getPlayer(uuid);
                if (p != null)
                    hide(p);
            }
            viewers.clear();

            getEntities().clear();
            getRawLines().clear();
            getRawLines().addAll(getLines());

            double y = getLocation().getY();

            for (String s : getLines()) {
                EntityArmorStand stand = new EntityArmorStand(((CraftWorld) getLocation().getWorld()).getHandle(),
                        getLocation().getX(), y, getLocation().getZ());

                stand.setInvisible(true);
                stand.setGravity(false);
                stand.setSmall(true);
                getEntities().add(stand);
                y -= 0.25;
            }
        } else {
            getRawLines().clear();
            getRawLines().addAll(getLines());
        }

        for (Player player : team.getPlayers()) {
            if (player != null && player.isOnline()) {
                boolean inWorld = player.getWorld().equals(getLocation().getWorld());
                boolean inRange = inWorld && player.getLocation().distanceSquared(getLocation()) < 64 * 64;

                if (inRange) {
                    if (!viewers.contains(player.getUniqueId())) {
                        show(player);
                        viewers.add(player.getUniqueId());
                    } else {
                        updateMetadata(player);
                    }
                } else {
                    if (viewers.contains(player.getUniqueId())) {
                        hide(player);
                        viewers.remove(player.getUniqueId());
                    }
                }
            }
        }
    }

    @Override
    public void stop() {
        super.stop();
        for (java.util.UUID uuid : viewers) {
            Player player = org.bukkit.Bukkit.getPlayer(uuid);
            if (player != null) {
                hide(player);
            }
        }
        viewers.clear();
    }
}
