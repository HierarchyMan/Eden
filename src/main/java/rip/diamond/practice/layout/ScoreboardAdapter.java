package rip.diamond.practice.layout;

import io.github.epicgo.sconey.element.SconeyElement;
import io.github.epicgo.sconey.element.SconeyElementAdapter;
import io.github.epicgo.sconey.element.SconeyElementMode;
import org.bukkit.entity.Player;
import rip.diamond.practice.Eden;
import rip.diamond.practice.config.Language;
import rip.diamond.practice.event.ScoreboardUpdateEvent;
import rip.diamond.practice.events.PracticeEvent;
import rip.diamond.practice.match.Match;
import rip.diamond.practice.party.Party;
import rip.diamond.practice.profile.PlayerProfile;
import rip.diamond.practice.profile.ProfileSettings;
import rip.diamond.practice.profile.PlayerState;
import rip.diamond.practice.queue.Queue;
import rip.diamond.practice.queue.QueueProfile;
import rip.diamond.practice.queue.QueueType;
import rip.diamond.practice.util.CC;

import java.util.List;

public class ScoreboardAdapter implements SconeyElementAdapter {

    private final Eden plugin = Eden.INSTANCE;

    @Override
    public SconeyElement getElement(final Player player) {
        SconeyElement element = new SconeyElement();

        element.setTitle(Language.SCOREBOARD_TITLE.toString(player));
        element.setMode(SconeyElementMode.CUSTOM);

        PlayerProfile profile = PlayerProfile.get(player);

        if (profile == null) {
            return element;
        }

        if (profile.getSettings() != null) {
            rip.diamond.practice.util.option.Option matchScoreboardSetting = profile.getSettings()
                    .get(ProfileSettings.MATCH_SCOREBOARD);
            if (matchScoreboardSetting != null && !matchScoreboardSetting.isEnabled()) {

                if (profile.getPlayerState() == PlayerState.IN_MATCH
                        || profile.getPlayerState() == PlayerState.IN_SPECTATING) {
                    return element;
                }
            }
        }

        PracticeEvent<?> event = plugin.getEventManager().getEventPlaying(player);

        if (event == null && plugin.getEventManager().isSpectating(player)) {
            event = plugin.getEventManager().getSpectators().get(player.getUniqueId());
        }

        if (event != null) {

            String title = Eden.INSTANCE.getScoreboardFile().getTitle(event.getName(), event.getState().name());
            if (title != null) {
                element.setTitle(CC.translate(title));
            }

            List<String> scoreboardLines = event.getScoreboard(player);
            if (scoreboardLines != null) {
                // Replace {server-ip} placeholder
                scoreboardLines = scoreboardLines.stream()
                        .map(line -> line.replace("{server-ip}", plugin.getLanguageFile().getStringRaw("server-ip")))
                        .collect(java.util.stream.Collectors.toList());

                if (org.bukkit.Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                    scoreboardLines = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, scoreboardLines);
                }
                element.addAll(scoreboardLines);
            }
            return element;
        }

        ScoreboardUpdateEvent scoreboardUpdateEvent = new ScoreboardUpdateEvent(player);
        scoreboardUpdateEvent.call();
        if (!scoreboardUpdateEvent.getLayout().isEmpty()) {
            element.addAll(scoreboardUpdateEvent.getLayout());
            return element;
        }

        Party party = Party.getByPlayer(player);
        Match match = profile.getMatch();

        switch (profile.getPlayerState()) {
            case LOADING:
                element.addAll(Language.SCOREBOARD_LOADING.toStringList(player));
                break;

            case IN_LOBBY:
                List<String> lines;
                if (party != null) {
                    lines = Language.SCOREBOARD_IN_PARTY.toStringList(player);
                } else {
                    lines = Language.SCOREBOARD_IN_LOBBY.toStringList(player);
                }

                PracticeEvent<?> ongoingEvent = plugin.getEventManager().getOngoingEvent();

                for (String line : lines) {

                    if (line.contains("{event-information}")) {
                        if (ongoingEvent != null) {

                            element.add("&7&m----------------------");
                            element.add("&bEvent: &f" + ongoingEvent.getName());
                            element.add("&bHost: &f"
                                    + (ongoingEvent.getHost() != null ? ongoingEvent.getHost().getName() : "None"));
                            element.add(
                                    "&bPlayers: &f" + ongoingEvent.getPlayers().size() + "/" + ongoingEvent.getLimit());
                            element.add("&bState: &f" + ongoingEvent.getState().name());
                        }

                    } else {

                        element.add(line);
                    }
                }
                break;

            case IN_QUEUE:
                QueueProfile qProfile = Queue.getPlayers().get(player.getUniqueId());
                if (qProfile != null) {
                    if (qProfile.getQueueType() == QueueType.UNRANKED) {
                        element.addAll(Language.SCOREBOARD_IN_QUEUE_UNRANKED.toStringList(player));
                    } else {
                        element.addAll(Language.SCOREBOARD_IN_QUEUE_RANKED.toStringList(player));
                    }
                }
                break;

            case IN_MATCH:
                if (match != null) {
                    List<String> matchScoreboard = match.getMatchScoreboard(player);
                    if (matchScoreboard != null) {
                        element.addAll(matchScoreboard);
                    }
                }
                break;

            case IN_SPECTATING:
                if (match != null) {
                    List<String> spectateScoreboard = match.getSpectateScoreboard(player);
                    if (spectateScoreboard != null) {
                        element.addAll(spectateScoreboard);
                    }
                }
                break;

            case IN_EDIT:
                if (plugin.getConfigFile().getString("kit-editor-mode").equalsIgnoreCase("GUI")) {
                    element.addAll(Language.SCOREBOARD_IN_EDIT_GUI.toStringList(player));
                } else {
                    element.addAll(Language.SCOREBOARD_IN_EDIT.toStringList(player));
                }
                break;

            case IN_EVENT:

                element.add("&7Loading event...");
                break;
        }

        return element;
    }
}
