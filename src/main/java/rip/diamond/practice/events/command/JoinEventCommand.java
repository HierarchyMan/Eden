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

public class JoinEventCommand extends Command {

    @Override
    @CommandArgs(name = "joinevent", aliases = { "join_event", "event.join" }, inGameOnly = true)
    public void execute(CommandArguments args) {
        Player player = args.getPlayer();
        Eden plugin = Eden.INSTANCE;
        PlayerProfile profile = PlayerProfile.get(player.getUniqueId());

        
        if (profile.getParty() != null) {
            player.sendMessage(CC.RED + "You cannot join an event while in a party.");
            return;
        }

        
        if (profile.getPlayerState() != PlayerState.IN_LOBBY) {
            player.sendMessage(CC.RED + "You can only join events from the lobby.");
            return;
        }

        PracticeEvent<?> event;

        if (args.length() == 0) {
            
            event = plugin.getEventManager().getOngoingEvent();
            if (event == null) {
                player.sendMessage(CC.RED + "There is no active event to join.");
                return;
            }
        } else {
            
            String eventName = args.getArgs()[0];
            event = plugin.getEventManager().getByName(eventName);
            if (event == null) {
                player.sendMessage(CC.RED + "That event doesn't exist.");
                return;
            }
        }

        
        if (event.getState() != EventState.WAITING) {
            player.sendMessage(CC.RED + "That event is not accepting players.");
            return;
        }

        
        if (event.getPlayers().containsKey(player.getUniqueId())) {
            player.sendMessage(CC.RED + "You're already in this event.");
            return;
        }

        
        if (event.getPlayers().size() >= event.getLimit()) {
            player.sendMessage(CC.RED + "The event is already full.");
            return;
        }

        event.join(player);
    }
}
