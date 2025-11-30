package rip.diamond.practice.events.command;

import org.bukkit.entity.Player;
import rip.diamond.practice.Eden;
import rip.diamond.practice.events.EventState;
import rip.diamond.practice.events.PracticeEvent;
import rip.diamond.practice.profile.PlayerProfile;
import rip.diamond.practice.profile.PlayerState;
import rip.diamond.practice.util.CC;
import rip.diamond.practice.util.command.Command;
import rip.diamond.practice.util.command.CommandArgs;
import rip.diamond.practice.util.command.argument.CommandArguments;

public class SpectateEventCommand extends Command {

    @Override
    @CommandArgs(name = "eventspectate", aliases = { "eventspec", "specevent" }, inGameOnly = true)
    public void execute(CommandArguments args) {
        Player player = args.getPlayer();
        Eden plugin = Eden.INSTANCE;
        PlayerProfile profile = PlayerProfile.get(player.getUniqueId());


        if (profile.getParty() != null) {
            player.sendMessage(CC.RED + "You cannot spectate an event while in a party.");
            return;
        }


        if (profile.getPlayerState() != PlayerState.IN_LOBBY && profile.getPlayerState() != PlayerState.IN_SPECTATING) {
            player.sendMessage(CC.RED + "You cannot spectate an event right now.");
            return;
        }

        if (args.length() == 0) {
            player.sendMessage(CC.RED + "Usage: /eventspectate <event>");
            return;
        }

        String eventName = args.getArgs()[0];
        PracticeEvent<?> event = plugin.getEventManager().getByName(eventName);

        if (event == null) {
            player.sendMessage(CC.RED + "That event doesn't exist.");
            return;
        }

        if (event.getState() == EventState.UNANNOUNCED) {
            player.sendMessage(CC.RED + "That event is not available right now.");
            return;
        }


        if (profile.getPlayerState() == PlayerState.IN_SPECTATING) {
            if (plugin.getEventManager().getSpectators().containsKey(player.getUniqueId())) {
                PracticeEvent<?> currentEvent = plugin.getEventManager().getSpectators().get(player.getUniqueId());
                if (currentEvent == event) {
                    player.sendMessage(CC.RED + "You are already spectating this event.");
                    return;
                }
                plugin.getEventManager().removeSpectator(player, currentEvent);
            }
        }

        plugin.getEventManager().addSpectator(player, profile, event);
        player.sendMessage(CC.GREEN + "You are now spectating the " + event.getName() + " event.");
    }
}
