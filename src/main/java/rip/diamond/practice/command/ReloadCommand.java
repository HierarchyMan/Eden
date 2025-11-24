package rip.diamond.practice.command;

import org.bukkit.command.CommandSender;
import rip.diamond.practice.Eden;
import rip.diamond.practice.util.command.Command;
import rip.diamond.practice.util.command.CommandArgs;
import rip.diamond.practice.util.command.argument.CommandArguments;

public class ReloadCommand extends Command {

    @CommandArgs(name = "eden reload", permission = "eden.command.reload", inGameOnly = false)
    public void execute(CommandArguments args) {
        Eden.INSTANCE.reloadPlugin(args.getSender());
    }
}
