package rip.diamond.practice.misc.commands;

import rip.diamond.practice.util.Tasks;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import rip.diamond.practice.Eden;
import rip.diamond.practice.config.Config;
import rip.diamond.practice.misc.task.EloResetTask;
import rip.diamond.practice.profile.PlayerProfile;
import rip.diamond.practice.util.CC;
import rip.diamond.practice.util.Common;
import rip.diamond.practice.util.command.Command;
import rip.diamond.practice.util.command.CommandArgs;
import rip.diamond.practice.util.command.argument.CommandArguments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class EloResetCommand extends Command {

    @CommandArgs(name = "eloreset", permission = "eden.command.eloreset", inGameOnly = false)
    public void execute(CommandArguments command) {
        CommandSender sender = command.getSender();

        if (Bukkit.getOnlinePlayers().size() != 0) {
            Common.sendMessage(sender, CC.RED + "You cannot use this command when there is online players.");
            return;
        }

        if (PlayerProfile.getProfiles().size() != 0) {
            Common.sendMessage(sender, CC.RED + "You cannot use this command when player profile is not equal to 0");
            return;
        }

        String[] args = command.getArgs();

        if (args.length == 0) {
            
            Tasks.runAsync(() -> {
                List<Document> documents = Eden.INSTANCE.getDatabaseManager().getHandler().getAllProfiles();
                Tasks.run(() -> new EloResetTask(documents));
            });
            Common.sendMessage(sender, CC.YELLOW + "Starting Elo Reset calculation...");
        } else if (args.length == 1) {
            UUID uuid;
            try {
                uuid = UUID.fromString(args[0]);
            } catch (Exception e) {
                Common.sendMessage(sender, CC.RED + "Invalid UUID.");
                return;
            }

            Eden.INSTANCE.getDatabaseManager().getHandler().loadProfile(uuid, (document) -> {
                if (document == null) {
                    Common.sendMessage(sender, CC.RED + "Cannot find a document with uuid '" + uuid.toString() + "'");
                    return;
                }
                
                Tasks.run(() -> new EloResetTask(Collections.singletonList(document)));
            });
        }
    }
}
