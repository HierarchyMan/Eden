package rip.diamond.practice.match.task;

import org.bukkit.scheduler.BukkitRunnable;
import rip.diamond.practice.Eden;
import rip.diamond.practice.EdenItems;
import rip.diamond.practice.config.Config;
import rip.diamond.practice.event.MatchResetEvent;
import rip.diamond.practice.kits.Kit;
import rip.diamond.practice.match.Match;
import rip.diamond.practice.match.MatchTaskTicker;
import rip.diamond.practice.match.MatchType;
import rip.diamond.practice.match.impl.SoloMatch;
import rip.diamond.practice.party.Party;
import rip.diamond.practice.profile.PlayerProfile;
import rip.diamond.practice.util.TaskTicker;
import rip.diamond.practice.util.Util;

import java.util.Objects;

public class MatchResetTask extends MatchTaskTicker {
    private final Eden plugin = Eden.INSTANCE;
    private final Match match;

    public MatchResetTask(Match match) {
        super(1, Config.MATCH_END_DURATION.toInteger(), false, match);
        this.match = match;
    }

    @Override
    public void onRun() {
        if (getTicks() <= 0) {
            cancel();

            MatchResetEvent event = new MatchResetEvent(match);
            event.call();

            match.clearEntities(true);
            match.getMatchPlayers().stream().filter(player -> Objects.nonNull(player) && player.isOnline())
                    .filter(player -> PlayerProfile.get(player).getMatch() == match)

                    .forEach(player -> plugin.getLobbyManager().sendToSpawnAndReset(player));
            match.getSpectators().forEach(match::leaveSpectate);
            match.getTasks().forEach(BukkitRunnable::cancel);
            match.getArenaDetail().restoreChunk(true, true);
            rip.diamond.practice.arenas.ActiveArenaTracker.remove(match.getArenaDetail());
            Match.getMatches().remove(match.getUuid());
            Kit.processPendingReload(match.getKit());
        }
    }

    @Override
    public void preRun() {

        match.getTasks().stream().filter(taskTicker -> taskTicker instanceof MatchClearBlockTask)
                .forEach(BukkitRunnable::cancel);

        if (Config.MATCH_ALLOW_REQUEUE.toBoolean() && match instanceof SoloMatch) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    match.getMatchPlayers().stream()
                            .filter(Objects::nonNull)

                            .forEach(player -> {
                                PlayerProfile profile = PlayerProfile.get(player);
                                if (profile.getMatch() == match) {
                                    EdenItems.giveItem(player, EdenItems.MATCH_REQUEUE);
                                }
                            });
                }
            }.runTaskLater(plugin, 20L);
        }
    }

    @Override
    public TickType getTickType() {
        return TickType.COUNT_DOWN;
    }

    @Override
    public int getStartTick() {
        return 1;
    }
}
