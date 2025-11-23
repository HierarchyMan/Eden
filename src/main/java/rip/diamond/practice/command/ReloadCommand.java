package rip.diamond.practice.command;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rip.diamond.practice.Eden;
import rip.diamond.practice.arenas.Arena;
import rip.diamond.practice.config.Config;
import rip.diamond.practice.config.Language;
import rip.diamond.practice.kits.Kit;
import rip.diamond.practice.util.CC;
import rip.diamond.practice.util.command.Command;
import rip.diamond.practice.util.command.CommandArgs;
import rip.diamond.practice.util.command.argument.CommandArguments;

public class ReloadCommand extends Command {

    @CommandArgs(name = "eden reload", permission = "eden.command.reload", inGameOnly = false)
    public void execute(CommandArguments args) {
        CommandSender sender = args.getSender();

        sender.sendMessage(CC.translate("&eReloading EdenPractice..."));

        // Reload Configs
        Eden.INSTANCE.reload();

        // Reload Arenas
        Arena.reload();

        // Reload Kits
        Kit.reload();

        // Instantly reapply visual elements to all online players
        sender.sendMessage(CC.translate("&eReapplying visual elements to all players..."));

        for (Player player : Bukkit.getOnlinePlayers()) {
            // Reapply Scoreboard
            if (Eden.INSTANCE.getScoreboardHandler() != null) {
                Eden.INSTANCE.getScoreboardHandler().removeScoreboard(player);
                Eden.INSTANCE.getScoreboardHandler().addScoreboard(player);
            }

            // Reapply Tab List (if enabled)
            if (Config.FANCY_TABLIST_ENABLED.toBoolean() && Eden.INSTANCE.getTabHandler() != null) {
                Eden.INSTANCE.getTabHandler().removePlayerTablist(player);
                Eden.INSTANCE.getTabHandler().registerPlayerTablist(player);
            }

            // Reapply NameTags (if enabled)
            if (Config.NAMETAG_ENABLED.toBoolean() && Eden.INSTANCE.getNameTagManager() != null) {
                Eden.INSTANCE.getNameTagManager().reloadPlayer(player);
                Eden.INSTANCE.getNameTagManager().reloadOthersFor(player);
            }
        }

        sender.sendMessage(CC.translate("&aReload complete! All changes have been applied instantly."));
    }
}
