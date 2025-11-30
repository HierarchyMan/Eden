package rip.diamond.practice.match.task;

import org.bukkit.entity.Player;
import rip.diamond.practice.config.Config;
import rip.diamond.practice.match.Match;
import rip.diamond.practice.match.MatchTaskTicker;
import rip.diamond.practice.util.TaskTicker;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MatchBuildHeightDamageTask extends MatchTaskTicker {

    private final Match match;
    private final Map<UUID, Integer> ticksAboveMap = new HashMap<>();

    public MatchBuildHeightDamageTask(Match match) {
        super(0, 1, false, match);
        this.match = match;
    }

    @Override
    public void onRun() {
        if (!Config.MATCH_ABOVE_BUILDHEIGHT_DAMAGE_ENABLED.toBoolean()) {
            return;
        }

        int thresholdTicks = Config.MATCH_ABOVE_BUILDHEIGHT_DAMAGE_THRESHOLD.toInteger() * 20;
        int delayTicks = Config.MATCH_ABOVE_BUILDHEIGHT_DAMAGE_DELAY.toInteger();
        double damageAmount = Config.MATCH_ABOVE_BUILDHEIGHT_DAMAGE_AMOUNT.toDouble();
        int buildMax = match.getArenaDetail().getArena().getBuildMax();

        for (Player player : match.getMatchPlayers()) {
            if (player.getLocation().getBlockY() >= buildMax + 1) {
                rip.diamond.practice.match.team.TeamPlayer teamPlayer = match.getTeamPlayer(player);
                if (teamPlayer != null && (teamPlayer.isRespawning() || !teamPlayer.isAlive())) {
                    continue;
                }

                int ticks = ticksAboveMap.getOrDefault(player.getUniqueId(), 0);
                ticks++;
                ticksAboveMap.put(player.getUniqueId(), ticks);

                if (delayTicks <= 0)
                    delayTicks = 20;

                if (ticks >= thresholdTicks) {
                    if ((ticks - thresholdTicks) % delayTicks == 0) {
                        player.damage(damageAmount);
                    }
                }
            } else {
                ticksAboveMap.remove(player.getUniqueId());
            }
        }
    }

    @Override
    public void preRun() {

    }

    @Override
    public TaskTicker.TickType getTickType() {
        return TaskTicker.TickType.NONE;
    }

    @Override
    public int getStartTick() {
        return 0;
    }
}
