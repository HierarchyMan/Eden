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
    private final boolean isRanked;

    public MatchStartingHologram(Location location, int time, LeaderboardType type, LeaderboardType.TimePeriod period,
            Kit kit, Team team, String side, boolean isRanked) {
        super(location, time, type, period, kit);
        this.team = team;
        this.side = side;
        this.isRanked = isRanked;
        this.viewerFilter = player -> team.getPlayers().contains(player);
    }

    @Override
    public void updateLines() {
        if (getLines() == null) {
            return;
        }
        getLines().clear();
        
        ConfigCursor cursor = new ConfigCursor(Eden.INSTANCE.getLeaderboardsConfig(), "match-starting-holograms." + side);
        List<String> lines = cursor.getStringList("lines");
        String entryFormat = cursor.getString("format");

        rip.diamond.practice.title.TitleManager titleManager = Eden.INSTANCE.getTitleManager();
        
        for (String line : lines) {
            if (line.contains("{entries}")) {
                List<LeaderboardPlayerCache> topPlayers = getTopPlayers();
                if (topPlayers.isEmpty()) {
                    getLines().add(CC.translate("&cLoading..."));
                } else {
                    for (int i = 0; i < 10; i++) {
                        if (i < topPlayers.size()) {
                            LeaderboardPlayerCache player = topPlayers.get(i);
                            String titleDisplay = "";
                            String titleShort = "";
                            
                            if (titleManager != null && titleManager.isEnabled()) {
                                rip.diamond.practice.profile.PlayerProfile profile = rip.diamond.practice.profile.PlayerProfile.get(player.getPlayerUUID());
                                if (profile != null) {
                                    if (isRanked && getKit() != null) {
                                        // SHOW ELO FOR RANKED MATCHES
                                        rip.diamond.practice.profile.data.ProfileKitData kitData = profile.getKitData().get(getKit().getName());
                                        int elo = kitData != null ? kitData.getElo() : 1000;
                                        // Use Elo Display format for both full and short title in ranked context
                                        titleDisplay = titleManager.getEloDisplay(elo);
                                        titleShort = titleDisplay; 
                                    } else {
                                        // SHOW TITLES FOR UNRANKED / GLOBAL
                                        rip.diamond.practice.title.Title title;
                                        if (getKit() != null) {
                                            rip.diamond.practice.profile.data.ProfileKitData kitData = profile.getKitData().get(getKit().getName());
                                            int wins = kitData != null ? kitData.getWon() : 0;
                                            title = titleManager.getTitleFromKitWins(wins);
                                        } else {
                                            int totalWins = profile.getKitData().values().stream()
                                                    .mapToInt(rip.diamond.practice.profile.data.ProfileKitData::getWon)
                                                    .sum();
                                            title = titleManager.getTitleFromWins(totalWins);
                                        }
                                        titleDisplay = titleManager.getTitleDisplay(title);
                                        titleShort = titleManager.getShortTitleDisplay(title);
                                    }
                                }
                            }
                            getLines().add(CC.translate(entryFormat
                                    .replace("{number}", String.valueOf(i + 1))
                                    .replace("{name}", player.getPlayerName())
                                    .replace("{value}", String.valueOf(player.getData()))
                                    .replace("{title}", titleDisplay)
                                    .replace("{title-short}", titleShort)));
                        } else {
                            getLines().add(CC.translate(entryFormat
                                    .replace("{number}", String.valueOf(i + 1))
                                    .replace("{name}", "-")
                                    .replace("{value}", "-")
                                    .replace("{title}", "")
                                    .replace("{title-short}", "")));
                        }

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
