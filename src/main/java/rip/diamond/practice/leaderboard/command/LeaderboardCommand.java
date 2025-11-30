package rip.diamond.practice.leaderboard.command;

import org.bukkit.entity.Player;
import rip.diamond.practice.Eden;
import rip.diamond.practice.leaderboard.LeaderboardType;
import rip.diamond.practice.util.CC;
import rip.diamond.practice.util.command.Command;
import rip.diamond.practice.util.command.CommandArgs;
import rip.diamond.practice.util.command.argument.CommandArguments;

public class LeaderboardCommand extends Command {

    @CommandArgs(name = "leaderboard", permission = "eden.command.leaderboard", inGameOnly = true)
    public void execute(CommandArguments command) {
        Player player = command.getPlayer();
        String[] args = command.getArgs();

        if (args.length < 1) {
            player.sendMessage(CC.translate("&cUsage: /leaderboard <hologram> ..."));
            return;
        }

        if (args[0].equalsIgnoreCase("hologram")) {
            if (args.length < 2) {
                player.sendMessage(CC.translate("&cUsage: /leaderboard hologram <add|remove|list> ..."));
                return;
            }

            if (args[1].equalsIgnoreCase("add")) {
                // /leaderboard hologram add <type> <period> <kit>
                if (args.length < 5) {
                    player.sendMessage(
                            CC.translate("&cUsage: /leaderboard hologram add <type> <period> <kit|ROTATING|GLOBAL>"));
                    return;
                }

                try {
                    LeaderboardType type = LeaderboardType.valueOf(args[2].toUpperCase());
                    LeaderboardType.TimePeriod period = LeaderboardType.TimePeriod.valueOf(args[3].toUpperCase());
                    String kitName = args[4];

                    Eden.INSTANCE.getHologramManager().saveHologram(type, period, kitName, player.getLocation());
                    player.sendMessage(CC.translate("&aHologram added!"));
                } catch (IllegalArgumentException e) {
                    player.sendMessage(CC.translate("&cInvalid type or period."));
                }
            } else if (args[1].equalsIgnoreCase("remove")) {
                Eden.INSTANCE.getHologramManager().removeNearestHologram(player);
            } else if (args[1].equalsIgnoreCase("list")) {
                player.sendMessage(
                        CC.translate(
                                "&aActive Holograms: " + Eden.INSTANCE.getHologramManager().getHolograms().size()));
            }
        } else {
            player.sendMessage(CC.translate("&cUsage: /leaderboard hologram <add|remove|list> ..."));
        }
    }
}
