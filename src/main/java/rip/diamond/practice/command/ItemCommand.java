package rip.diamond.practice.command;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import rip.diamond.practice.Eden;
import rip.diamond.practice.util.CC;
import rip.diamond.practice.util.command.Command;
import rip.diamond.practice.util.command.CommandArgs;
import rip.diamond.practice.util.command.argument.CommandArguments;

import java.util.Arrays;
import java.util.List;

public class ItemCommand extends Command {

    @CommandArgs(name = "eden.item", permission = "eden.command.item", inGameOnly = true)
    public void execute(CommandArguments args) {
        Player player = args.getPlayer();
        String[] arguments = args.getArgs();

        if (arguments.length < 2) {
            player.sendMessage(CC.translate("&cUsage: /eden item <set|reset> <type>"));
            return;
        }

        String action = arguments[0];
        String type = arguments[1].toUpperCase();

        if (action.equalsIgnoreCase("set")) {
            ItemStack item = player.getItemInHand();
            if (item == null || item.getType() == Material.AIR) {
                player.sendMessage(CC.translate("&cYou must be holding an item."));
                return;
            }

            Eden.INSTANCE.getCustomItemManager().setItem(type, item);
            player.sendMessage(CC.translate("&aSet custom item for &e" + type + "&a."));
        } else if (action.equalsIgnoreCase("reset")) {
            Eden.INSTANCE.getCustomItemManager().resetItem(type);
            player.sendMessage(CC.translate("&aReset custom item for &e" + type + "&a."));
        } else {
            player.sendMessage(CC.translate("&cUsage: /eden item <set|reset> <type>"));
        }
    }

    @Override
    public List<String> getDefaultTabComplete(CommandArguments command) {
        return Arrays.asList("set", "reset");
    }
}
