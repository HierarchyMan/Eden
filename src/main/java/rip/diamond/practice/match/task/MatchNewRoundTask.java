package rip.diamond.practice.match.task;

import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import rip.diamond.practice.Eden;
import rip.diamond.practice.config.Config;
import rip.diamond.practice.config.EdenSound;
import rip.diamond.practice.config.Language;
import rip.diamond.practice.event.MatchRoundEndEvent;
import rip.diamond.practice.event.MatchRoundStartEvent;
import rip.diamond.practice.match.Match;
import rip.diamond.practice.match.MatchState;
import rip.diamond.practice.match.MatchTaskTicker;
import rip.diamond.practice.match.team.Team;
import rip.diamond.practice.match.team.TeamPlayer;
import rip.diamond.practice.util.CenteredMessageSender;

import java.util.stream.Collectors;

public class MatchNewRoundTask extends MatchTaskTicker {

    private final Match match;
    private final TeamPlayer scoredPlayer;
    private final boolean newRound;

    public MatchNewRoundTask(Match match, TeamPlayer scoredPlayer, boolean newRound) {
        super(0, 20, false, match);
        this.match = match;
        this.scoredPlayer = scoredPlayer;
        this.newRound = newRound;
    }

    @Override
    public void onRun() {
        if (match.getState() == MatchState.ENDING) {
            cancel();
            return;
        }

        if (getTicks() == 0) {
            match.broadcastMessage(Language.MATCH_NEW_ROUND_START_MESSAGE.toString());
            match.broadcastTitle("");
            match.setState(MatchState.FIGHTING);
            match.broadcastSound(EdenSound.MATCH_START);
            match.removeStartingHolograms();

            MatchRoundStartEvent event = new MatchRoundStartEvent(match);
            event.call();

            cancel();
            return;
        }

        match.broadcastMessage(Language.MATCH_NEW_ROUND_START_COUNTDOWN.toString(getTicks()));
        match.broadcastTitle(Language.MATCH_NEW_ROUND_START_TITLE.toString(getTicks()));
        match.broadcastSound(EdenSound.NEW_ROUND_COUNTDOWN);
    }

    @Override
    public void preRun() {
        match.clearEntities(true);

        if (match.getTasks().stream().filter(taskTicker -> taskTicker != this)
                .anyMatch(taskTicker -> taskTicker instanceof MatchNewRoundTask)) {
            cancel();
            return;
        }

        if (scoredPlayer != null && match.getState() != MatchState.STARTING
                && match.getKit().getGameRules().isPoint(match)) {
            Team team = match.getTeam(scoredPlayer);
            Player player = scoredPlayer.getPlayer();

            match.broadcastMessage(Language.MATCH_NEW_ROUND_START_SCORE.toStringList(player,
                    team.getTeamColor().getColor(),
                    scoredPlayer.getUsername(),
                    Eden.DECIMAL
                            .format((player.getHealth() + ((CraftPlayer) player).getHandle().getAbsorptionHearts())),
                    team.getPoints(),
                    match.getOpponentTeam(team).getTeamColor().getColor(),
                    match.getOpponentTeam(team).getPoints()).stream().map(CenteredMessageSender::getCenteredMessage)
                    .collect(Collectors.toList()));

            if (Config.MATCH_TITLE_SCORE.toBoolean()) {
                String scoredTeamColor = team.getTeamColor().getColor();
                String opponentTeamColor = match.getOpponentTeam(team).getTeamColor().getColor();

                match.broadcastTitle(
                        Language.MATCH_NEW_ROUND_START_SCORED_TITLE.toString(scoredTeamColor,
                                scoredPlayer.getUsername()),
                        Language.MATCH_NEW_ROUND_START_SCORED_SUBTITLE.toString(scoredTeamColor, team.getPoints(),
                                opponentTeamColor, match.getOpponentTeam(team).getPoints()),
                        20, 60, 20);
            }
        }

        if (newRound) {
            match.setState(MatchState.STARTING);

            match.getTeams().forEach(t -> t.teleport(t.getSpawnLocation()));
            match.getTeamPlayers().forEach(teamPlayer -> {
                if (teamPlayer.isRespawning()) {

                    match.respawn(teamPlayer);
                } else {
                    teamPlayer.respawn(match);
                }
            });

            if (match.getKit().getGameRules().isResetArenaWhenGetPoint()) {
                match.getArenaDetail().restoreChunk(true, false);

                match.getTasks().stream().filter(taskTicker -> taskTicker instanceof MatchClearBlockTask)
                        .forEach(BukkitRunnable::cancel);
            }

            MatchRoundEndEvent event = new MatchRoundEndEvent(match);
            event.call();
        }
    }

    @Override
    public TickType getTickType() {
        return TickType.COUNT_DOWN;
    }

    @Override
    public int getStartTick() {
        if (!newRound) {
            return match.getKit().getGameRules().getMatchCountdownDuration();
        }
        return match.getKit().getGameRules().getNewRoundTime();
    }
}
