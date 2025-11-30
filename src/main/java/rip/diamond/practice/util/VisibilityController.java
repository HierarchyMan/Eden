package rip.diamond.practice.util;

import lombok.experimental.UtilityClass;
import org.bukkit.entity.Player;
import rip.diamond.practice.Eden;
import rip.diamond.practice.config.Config;
import rip.diamond.practice.events.EventPlayer;
import rip.diamond.practice.events.PracticeEvent;
import rip.diamond.practice.events.games.oitc.OITCPlayer;
import rip.diamond.practice.match.Match;
import rip.diamond.practice.party.Party;
import rip.diamond.practice.profile.PlayerProfile;
import rip.diamond.practice.profile.PlayerState;
import rip.diamond.practice.profile.ProfileSettings;

@UtilityClass
public class VisibilityController {

    public void updateVisibility(Player player) {
        Tasks.run(() -> {
            for (Player target : Util.getOnlinePlayers()) {
                if (shouldSeePlayer(target, player)) {
                    target.showPlayer(player);
                } else {
                    target.hidePlayer(player);
                }

                if (shouldSeePlayer(player, target)) {
                    player.showPlayer(target);
                } else {
                    player.hidePlayer(target);
                }
            }
        });
    }

    /**
     * Make a player invisible to all other players in their event
     * Useful for respawning, spectating, etc.
     */
    public void hideFromEvent(Player player) {
        Tasks.run(() -> {
            PlayerProfile profile = PlayerProfile.get(player);
            if (profile == null || profile.getPlayerState() != PlayerState.IN_EVENT) {
                return;
            }

            PracticeEvent<?> event = Eden.INSTANCE.getEventManager().getEventPlaying(player);
            if (event == null) {
                return;
            }

            for (Player otherPlayer : event.getBukkitPlayers()) {
                if (otherPlayer != player) {
                    otherPlayer.hidePlayer(player);
                }
            }
        });
    }

    /**
     * Make a player visible to all other players in their event
     * Useful after respawning
     */
    public void showToEvent(Player player) {
        Tasks.run(() -> {
            PlayerProfile profile = PlayerProfile.get(player);
            if (profile == null || profile.getPlayerState() != PlayerState.IN_EVENT) {
                return;
            }

            PracticeEvent<?> event = Eden.INSTANCE.getEventManager().getEventPlaying(player);
            if (event == null) {
                return;
            }

            for (Player otherPlayer : event.getBukkitPlayers()) {
                if (otherPlayer != player) {
                    otherPlayer.showPlayer(player);
                }
            }
        });
    }

    private boolean shouldSeePlayer(Player viewer, Player target) {
        if (viewer == null || target == null) {
            return false;
        }

        if (viewer == target) {
            return true;
        }

        PlayerProfile pViewer = PlayerProfile.get(viewer);
        PlayerProfile pTarget = PlayerProfile.get(target);

        if (pViewer == null || pTarget == null || pViewer.getPlayerState() == PlayerState.LOADING
                || pTarget.getPlayerState() == PlayerState.LOADING) {
            return false;
        }

        // If viewer is in an event, they should ONLY see players in the same event
        if (pViewer.getPlayerState() == PlayerState.IN_EVENT) {
            // If target is not in an event, viewer cannot see them
            if (pTarget.getPlayerState() != PlayerState.IN_EVENT) {
                return false;
            }

            PracticeEvent<?> targetEvent = Eden.INSTANCE.getEventManager().getEventPlaying(target);

            if (targetEvent != null) {

                EventPlayer targetEventPlayer = targetEvent.getPlayer(target);
                boolean targetIsRespawning = isEventPlayerRespawning(targetEventPlayer);

                PracticeEvent<?> viewerEvent = Eden.INSTANCE.getEventManager().getEventPlaying(viewer);
                if (viewerEvent == null) {
                    viewerEvent = Eden.INSTANCE.getEventManager().getSpectators().get(viewer.getUniqueId());
                }

                boolean viewerInSameEvent = viewerEvent == targetEvent;

                return viewerInSameEvent && !targetIsRespawning;
            }
        }

        // If target is in an event, and viewer is NOT (since we passed the above
        // block), viewer cannot see them
        if (pTarget.getPlayerState() == PlayerState.IN_EVENT) {
            return false;
        }

        Match targetMatch = pTarget.getMatch();

        if (targetMatch == null) {

            Party targetParty = Party.getByPlayer(target);

            boolean configSettings = Config.LOBBY_DISPLAY_PLAYERS.toBoolean();
            boolean viewerPlayingMatch = pViewer.getPlayerState() == PlayerState.IN_MATCH && pViewer.getMatch() != null;
            boolean viewerSameParty = targetParty != null && targetParty.getMember(viewer.getUniqueId()) != null;

            return configSettings || viewerPlayingMatch || viewerSameParty;
        } else {

            boolean targetIsSpectator = targetMatch.getSpectators().contains(target)
                    || !targetMatch.getTeamPlayer(target).isAlive() || targetMatch.getTeamPlayer(target).isRespawning();
            boolean viewerSpectateSetting = pViewer.getSettings().get(ProfileSettings.SPECTATOR_VISIBILITY).isEnabled();
            boolean viewerIsSpectator = pViewer.getPlayerState() == PlayerState.IN_SPECTATING
                    && pViewer.getMatch() != null;

            boolean viewerMatchIsSame = targetMatch == pViewer.getMatch();

            return (!targetIsSpectator || (viewerSpectateSetting && viewerIsSpectator)) && viewerMatchIsSame;
        }
    }

    /**
     * Check if an event player is in a respawning state
     * This works for all events that have a RESPAWNING state (OITC, etc.)
     */
    private boolean isEventPlayerRespawning(EventPlayer eventPlayer) {
        if (eventPlayer == null) {
            return false;
        }

        if (eventPlayer instanceof OITCPlayer) {
            OITCPlayer oitcPlayer = (OITCPlayer) eventPlayer;
            return oitcPlayer.getState() == OITCPlayer.OITCState.RESPAWNING;
        }

        return false;
    }

}
