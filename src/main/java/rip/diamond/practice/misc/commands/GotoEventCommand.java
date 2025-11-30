package rip.diamond.practice.misc.commands;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import rip.diamond.practice.util.CC;
import rip.diamond.practice.util.command.Command;
import rip.diamond.practice.util.command.CommandArgs;
import rip.diamond.practice.util.command.argument.CommandArguments;

public class GotoEventCommand extends Command {

    @Override
    @CommandArgs(name = "gotoevent", aliases = { "eventsworld", "eventworld",
            "tpevent" }, permission = "eden.admin", inGameOnly = true)
    public void execute(CommandArguments args) {
        Player player = args.getPlayer();

        
        if (Bukkit.getWorld("event") == null) {
            player.sendMessage(CC.RED + "The event world is not loaded.");
            return;
        }

        Location eventWorld = Bukkit.getWorld("event").getSpawnLocation();
        player.teleport(eventWorld);
        player.sendMessage(CC.GREEN + "You have teleported to the events world.");
    }
}
