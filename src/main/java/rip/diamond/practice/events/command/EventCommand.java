package rip.diamond.practice.events.command;

import org.bukkit.entity.Player;
import rip.diamond.practice.Eden;
import rip.diamond.practice.events.EventState;
import rip.diamond.practice.events.PracticeEvent;
import rip.diamond.practice.events.menu.EventHostMenu;
import rip.diamond.practice.events.menu.EventManagerMenu;
import rip.diamond.practice.util.CC;
import rip.diamond.practice.util.command.Command;
import rip.diamond.practice.util.command.CommandArgs;
import rip.diamond.practice.util.command.argument.CommandArguments;

public class EventCommand extends Command {

    private final Eden plugin = Eden.INSTANCE;

    @CommandArgs(name = "event")
    public void execute(CommandArguments command) {
        Player player = command.getPlayer();
        String[] args = command.getArgs();

        PracticeEvent<?> event = plugin.getEventManager().getOngoingEvent();

        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("create") || args[0].equalsIgnoreCase("host")) {
                if (!player.hasPermission("eden.command.event.create")) {
                    player.sendMessage(CC.RED + "No permission.");
                    return;
                }
                if (event != null) {
                    player.sendMessage(CC.RED + "There is already an event running!");
                    return;
                }
                new EventHostMenu().openMenu(player);
                return;
            } else if (args[0].equalsIgnoreCase("forcestart") || args[0].equalsIgnoreCase("start")) {
                if (!player.hasPermission("eden.command.event.forcestart")) {
                    player.sendMessage(CC.RED + "No permission.");
                    return;
                }
                if (event == null) {
                    player.sendMessage(CC.RED + "There is no event running.");
                    return;
                }
                if (event.getState() == EventState.STARTED || event.getState() == EventState.STARTING
                        || event.getState() == EventState.PLAYING) {
                    player.sendMessage(CC.RED + "Event has already started!");
                    return;
                }

                
                event.getCountdownTask().setTimeUntilStart(10);
                player.sendMessage(CC.GREEN + "Event countdown shortened to 10 seconds!");
                return;
            } else if (args[0].equalsIgnoreCase("cancel") || args[0].equalsIgnoreCase("end")) {
                if (!player.hasPermission("eden.command.event.cancel")) {
                    player.sendMessage(CC.RED + "No permission.");
                    return;
                }
                if (event == null) {
                    player.sendMessage(CC.RED + "There is no event running.");
                    return;
                }
                event.end();
                player.sendMessage(CC.GREEN + "Event cancelled!");
                return;
            } else if (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("status")) {
                if (event == null) {
                    player.sendMessage(CC.RED + "There is no event running.");
                    return;
                }
                player.sendMessage(CC.translate("&7&m-------------------"));
                player.sendMessage(CC.translate("&bEvent: &f" + event.getName()));
                player.sendMessage(
                        CC.translate("&bHost: &f" + (event.getHost() != null ? event.getHost().getName() : "None")));
                player.sendMessage(CC.translate("&bPlayers: &f" + event.getPlayers().size() + "/" + event.getLimit()));
                player.sendMessage(CC.translate("&bState: &f" + event.getState().name()));
                player.sendMessage(CC.translate("&7&m-------------------"));
                return;
            } else if (args[0].equalsIgnoreCase("manage")) {
                if (!player.hasPermission("eden.command.event.manage")) {
                    player.sendMessage(CC.RED + "No permission.");
                    return;
                }
                new EventManagerMenu().openMenu(player);
                return;
            }
        }

        
        player.sendMessage(CC.translate("&c&lEvent Commands:"));
        player.sendMessage(CC.translate("&e/event create &7- Host a new event"));
        player.sendMessage(CC.translate("&e/event start &7- Force start the event"));
        player.sendMessage(CC.translate("&e/event cancel &7- Cancel the event"));
        player.sendMessage(CC.translate("&e/event info &7- View event info"));
        player.sendMessage(CC.translate("&e/event manage &7- Manage ongoing event"));
    }
}
