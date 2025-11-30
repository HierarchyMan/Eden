package rip.diamond.practice.leaderboard.hologram.impl;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import rip.diamond.practice.Eden;
import rip.diamond.practice.kits.Kit;
import rip.diamond.practice.leaderboard.LeaderboardManager;
import rip.diamond.practice.leaderboard.LeaderboardPlayerCache;
import rip.diamond.practice.leaderboard.LeaderboardType;
import rip.diamond.practice.leaderboard.hologram.PracticeHologram;
import rip.diamond.practice.leaderboard.impl.KitLeaderboard;
import rip.diamond.practice.util.CC;
import rip.diamond.practice.util.file.ConfigCursor;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class LeaderboardHologram extends PracticeHologram {

    private final LeaderboardType type;
    private final LeaderboardType.TimePeriod period;
    private final boolean rotating;
    private Kit kit;
    private Kit nextKit;
    private int kitIndex = 0;

    public LeaderboardHologram(Location location, int time, LeaderboardType type, LeaderboardType.TimePeriod period,
            Kit kit) {
        super(location, time);
        this.type = type;
        this.period = period;
        this.kit = kit;
        this.rotating = (kit == null && type != LeaderboardType.GLOBAL_WINS && type != LeaderboardType.GLOBAL_ELO
                && type != LeaderboardType.EVENT_WINS && type != LeaderboardType.EVENT_WINS_DAILY
                && type != LeaderboardType.EVENT_WINS_WEEKLY && type != LeaderboardType.EVENT_WINS_MONTHLY
                && type != LeaderboardType.EVENTS_PLAYED);

        if (rotating) {
            updateKit();
        }
    }

    private void updateKit() {
        List<Kit> kits = Kit.getKits().stream().filter(Kit::isEnabled).filter(Kit::isRanked)
                .collect(Collectors.toList());
        if (kits.isEmpty())
            return;

        if (kitIndex >= kits.size())
            kitIndex = 0;
        this.kit = kits.get(kitIndex);

        int nextIndex = kitIndex + 1;
        if (nextIndex >= kits.size())
            nextIndex = 0;
        this.nextKit = kits.get(nextIndex);
    }

    @Override
    public void update() {
        if (rotating) {
            kitIndex++;
            updateKit();
        }
    }

    @Override
    public void updateLines() {
        ConfigCursor config = new ConfigCursor(Eden.INSTANCE.getLeaderboardsConfig(), "holograms." + getConfigKey());
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
                        .replace("{kit}", kit != null ? kit.getDisplayName() : "Global")
                        .replace("{next_kit}", nextKit != null ? nextKit.getDisplayName() : "Global")
                        .replace("{period}", getPeriodDisplay())
                        .replace("{update}", String.valueOf(getActualTime()))
                        .replace("{lb-type}", getTypeDisplay())));
            }
        }
    }

    @Override
    protected String applyPlaceholders(String line, Player viewer) {
        if (line.contains("{lb-amount}") || line.contains("{lb-position}")) {
            LeaderboardManager manager = Eden.INSTANCE.getLeaderboardManager();
            int amount = 0;
            int position = -1;

            rip.diamond.practice.profile.PlayerProfile profile = rip.diamond.practice.profile.PlayerProfile
                    .get(viewer.getUniqueId());
            if (profile != null) {
                if (type == LeaderboardType.GLOBAL_WINS) {
                    amount = profile.getKitData().values().stream()
                            .mapToInt(data -> data.getRankedWon() + data.getUnrankedWon())
                            .sum();
                    position = manager.getGlobalWinsLeaderboard().getPlayerRank(viewer.getUniqueId());
                } else if (type == LeaderboardType.GLOBAL_ELO) {
                    int totalElo = 0;
                    int count = 0;
                    for (Kit k : Kit.getKits()) {
                        if (k.isEnabled() && k.isRanked()) {
                            rip.diamond.practice.profile.data.ProfileKitData data = profile.getKitData()
                                    .get(k.getName());
                            if (data != null) {
                                totalElo += data.getElo();
                                count++;
                            }
                        }
                    }
                    amount = count == 0 ? 1000 : totalElo / count;
                    position = manager.getGlobalEloLeaderboard().getPlayerRank(viewer.getUniqueId());
                } else if (type == LeaderboardType.EVENT_WINS) {
                    amount = profile.getEventWins();
                    position = manager.getEventWinsLeaderboard().getPlayerRank(viewer.getUniqueId());
                } else if (type == LeaderboardType.EVENT_WINS_DAILY) {
                    amount = profile.getDailyEventWins();
                    position = manager.getEventWinsDailyLeaderboard().getPlayerRank(viewer.getUniqueId());
                } else if (type == LeaderboardType.EVENT_WINS_WEEKLY) {
                    amount = profile.getWeeklyEventWins();
                    position = manager.getEventWinsWeeklyLeaderboard().getPlayerRank(viewer.getUniqueId());
                } else if (type == LeaderboardType.EVENT_WINS_MONTHLY) {
                    amount = profile.getMonthlyEventWins();
                    position = manager.getEventWinsMonthlyLeaderboard().getPlayerRank(viewer.getUniqueId());
                } else if (type == LeaderboardType.EVENTS_PLAYED) {
                    amount = profile.getEventsPlayed();
                    position = manager.getEventsPlayedLeaderboard().getPlayerRank(viewer.getUniqueId());
                } else if (kit != null) {
                    rip.diamond.practice.profile.data.ProfileKitData data = profile.getKitData().get(kit.getName());
                    if (data != null) {
                        if (type.name().contains("WINSTREAK")) {
                            if (period == LeaderboardType.TimePeriod.DAILY)
                                amount = data.getDailyWinstreak();
                            else if (period == LeaderboardType.TimePeriod.WEEKLY)
                                amount = data.getWeeklyWinstreak();
                            else if (period == LeaderboardType.TimePeriod.MONTHLY)
                                amount = data.getMonthlyWinstreak();
                            else
                                amount = data.getWinstreak();

                            KitLeaderboard lb = manager.getWinstreakLeaderboard().get(kit);
                            if (period == LeaderboardType.TimePeriod.DAILY)
                                lb = manager.getWinstreakDailyLeaderboard().get(kit);
                            else if (period == LeaderboardType.TimePeriod.WEEKLY)
                                lb = manager.getWinstreakWeeklyLeaderboard().get(kit);
                            else if (period == LeaderboardType.TimePeriod.MONTHLY)
                                lb = manager.getWinstreakMonthlyLeaderboard().get(kit);

                            if (lb != null)
                                position = lb.getPlayerRank(viewer.getUniqueId());
                        } else if (type.name().contains("WINS")) {
                            if (period == LeaderboardType.TimePeriod.DAILY)
                                amount = data.getDailyWins();
                            else if (period == LeaderboardType.TimePeriod.WEEKLY)
                                amount = data.getWeeklyWins();
                            else if (period == LeaderboardType.TimePeriod.MONTHLY)
                                amount = data.getMonthlyWins();
                            else
                                amount = data.getRankedWon() + data.getUnrankedWon();

                            KitLeaderboard lb = manager.getWinsLeaderboard().get(kit);
                            if (period == LeaderboardType.TimePeriod.DAILY)
                                lb = manager.getWinsDailyLeaderboard().get(kit);
                            else if (period == LeaderboardType.TimePeriod.WEEKLY)
                                lb = manager.getWinsWeeklyLeaderboard().get(kit);
                            else if (period == LeaderboardType.TimePeriod.MONTHLY)
                                lb = manager.getWinsMonthlyLeaderboard().get(kit);

                            if (lb != null)
                                position = lb.getPlayerRank(viewer.getUniqueId());
                        } else if (type.name().contains("ELO")) {
                            amount = data.getElo();
                            KitLeaderboard lb = manager.getEloLeaderboard().get(kit);
                            if (lb != null)
                                position = lb.getPlayerRank(viewer.getUniqueId());
                        } else if (type.name().contains("LOSSES")) {
                            if (period == LeaderboardType.TimePeriod.DAILY)
                                amount = data.getDailyLosses();
                            else if (period == LeaderboardType.TimePeriod.WEEKLY)
                                amount = data.getWeeklyLosses();
                            else if (period == LeaderboardType.TimePeriod.MONTHLY)
                                amount = data.getMonthlyLosses();
                            else
                                amount = data.getRankedLost() + data.getUnrankedLost();

                            KitLeaderboard lb = manager.getLossesLeaderboard().get(kit);
                            if (period == LeaderboardType.TimePeriod.DAILY)
                                lb = manager.getLossesDailyLeaderboard().get(kit);
                            else if (period == LeaderboardType.TimePeriod.WEEKLY)
                                lb = manager.getLossesWeeklyLeaderboard().get(kit);
                            else if (period == LeaderboardType.TimePeriod.MONTHLY)
                                lb = manager.getLossesMonthlyLeaderboard().get(kit);

                            if (lb != null)
                                position = lb.getPlayerRank(viewer.getUniqueId());
                        }
                    }
                }
            }

            return line.replace("{lb-amount}", String.valueOf(amount))
                    .replace("{lb-position}", position == -1 ? "Unranked" : "#" + position);
        }

        return line;
    }

    protected String getTypeDisplay() {
        if (type == LeaderboardType.GLOBAL_WINS)
            return "Global Wins";
        if (type == LeaderboardType.GLOBAL_ELO)
            return "Global Elo";
        if (type == LeaderboardType.EVENT_WINS)
            return "Event Wins";
        if (type == LeaderboardType.EVENT_WINS_DAILY)
            return "Daily Event Wins";
        if (type == LeaderboardType.EVENT_WINS_WEEKLY)
            return "Weekly Event Wins";
        if (type == LeaderboardType.EVENT_WINS_MONTHLY)
            return "Monthly Event Wins";
        if (type == LeaderboardType.EVENTS_PLAYED)
            return "Events Played";

        String typeName = "";
        if (type.name().contains("WINSTREAK"))
            typeName = "Winstreak";
        else if (type.name().contains("WINS"))
            typeName = "Wins";
        else if (type.name().contains("ELO"))
            typeName = "Elo";
        else if (type.name().contains("LOSSES"))
            typeName = "Losses";

        return getPeriodDisplay() + " " + typeName;
    }

    private String getConfigKey() {
        if (type == LeaderboardType.GLOBAL_WINS)
            return "global_wins";
        if (type == LeaderboardType.GLOBAL_ELO)
            return "global_elo";
        if (type == LeaderboardType.EVENT_WINS || type == LeaderboardType.EVENT_WINS_DAILY
                || type == LeaderboardType.EVENT_WINS_WEEKLY || type == LeaderboardType.EVENT_WINS_MONTHLY)
            return "event_wins";
        if (type == LeaderboardType.EVENTS_PLAYED)
            return "events_played";
        if (type.name().contains("WINSTREAK"))
            return "winstreak";
        if (type.name().contains("WINS"))
            return "wins";
        if (type.name().contains("ELO"))
            return "elo";
        if (type.name().contains("LOSSES"))
            return "losses";
        return "wins";
    }

    protected String getPeriodDisplay() {
        switch (period) {
            case DAILY:
                return "Daily";
            case WEEKLY:
                return "Weekly";
            case MONTHLY:
                return "Monthly";
            default:
                return "Lifetime";
        }
    }

    protected List<LeaderboardPlayerCache> getTopPlayers() {
        LeaderboardManager manager = Eden.INSTANCE.getLeaderboardManager();
        LinkedHashMap<Integer, LeaderboardPlayerCache> leaderboard = null;

        if (type == LeaderboardType.GLOBAL_WINS) {
            leaderboard = manager.getGlobalWinsLeaderboard().getLeaderboard();
        } else if (type == LeaderboardType.GLOBAL_ELO) {
            leaderboard = manager.getGlobalEloLeaderboard().getLeaderboard();
        } else if (type == LeaderboardType.EVENT_WINS) {
            leaderboard = manager.getEventWinsLeaderboard().getLeaderboard();
        } else if (type == LeaderboardType.EVENT_WINS_DAILY) {
            leaderboard = manager.getEventWinsDailyLeaderboard().getLeaderboard();
        } else if (type == LeaderboardType.EVENT_WINS_WEEKLY) {
            leaderboard = manager.getEventWinsWeeklyLeaderboard().getLeaderboard();
        } else if (type == LeaderboardType.EVENT_WINS_MONTHLY) {
            leaderboard = manager.getEventWinsMonthlyLeaderboard().getLeaderboard();
        } else if (type == LeaderboardType.EVENTS_PLAYED) {
            leaderboard = manager.getEventsPlayedLeaderboard().getLeaderboard();
        } else if (kit != null) {
            KitLeaderboard kitLeaderboard = null;

            if (type.name().contains("WINSTREAK")) {
                if (period == LeaderboardType.TimePeriod.DAILY)
                    kitLeaderboard = manager.getWinstreakDailyLeaderboard().get(kit);
                else if (period == LeaderboardType.TimePeriod.WEEKLY)
                    kitLeaderboard = manager.getWinstreakWeeklyLeaderboard().get(kit);
                else if (period == LeaderboardType.TimePeriod.MONTHLY)
                    kitLeaderboard = manager.getWinstreakMonthlyLeaderboard().get(kit);
                else
                    kitLeaderboard = manager.getWinstreakLeaderboard().get(kit);
            } else if (type.name().contains("WINS")) {
                if (period == LeaderboardType.TimePeriod.DAILY)
                    kitLeaderboard = manager.getWinsDailyLeaderboard().get(kit);
                else if (period == LeaderboardType.TimePeriod.WEEKLY)
                    kitLeaderboard = manager.getWinsWeeklyLeaderboard().get(kit);
                else if (period == LeaderboardType.TimePeriod.MONTHLY)
                    kitLeaderboard = manager.getWinsMonthlyLeaderboard().get(kit);
                else
                    kitLeaderboard = manager.getWinsLeaderboard().get(kit);
            } else if (type.name().contains("ELO")) {
                kitLeaderboard = manager.getEloLeaderboard().get(kit);
            } else if (type.name().contains("LOSSES")) {
                if (period == LeaderboardType.TimePeriod.DAILY)
                    kitLeaderboard = manager.getLossesDailyLeaderboard().get(kit);
                else if (period == LeaderboardType.TimePeriod.WEEKLY)
                    kitLeaderboard = manager.getLossesWeeklyLeaderboard().get(kit);
                else if (period == LeaderboardType.TimePeriod.MONTHLY)
                    kitLeaderboard = manager.getLossesMonthlyLeaderboard().get(kit);
                else
                    kitLeaderboard = manager.getLossesLeaderboard().get(kit);
            }

            if (kitLeaderboard != null) {
                leaderboard = kitLeaderboard.getLeaderboard();
            }
        }

        if (leaderboard == null)
            return java.util.Collections.emptyList();

        return leaderboard.values().stream()
                .sorted(Comparator.comparingInt(LeaderboardPlayerCache::getData).reversed())
                .limit(10)
                .collect(Collectors.toList());
    }
}
