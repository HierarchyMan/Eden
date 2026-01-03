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

    @CommandArgs(name = "eden item", permission = "eden.command.item", inGameOnly = true)
    public void execute(CommandArguments args) {
        Player player = args.getPlayer();
        String[] arguments = args.getArgs();

        if (arguments.length < 2) {
            player.sendMessage(CC.translate("&cUsage: /eden item <set|reset|give> <type> [amount]"));
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
        } else if (action.equalsIgnoreCase("give")) {
            ItemStack customItem = Eden.INSTANCE.getCustomItemManager().getItem(type);
            
            if (customItem == null) {
                player.sendMessage(CC.translate("&cCustom item &e" + type + " &cis not configured."));
                return;
            }
            
            customItem = customItem.clone();
            
            // Parse amount if provided, default to 1
            int amount = 1;
            if (arguments.length >= 3) {
                try {
                    amount = Integer.parseInt(arguments[2]);
                    if (amount <= 0 || amount > 64) {
                        player.sendMessage(CC.translate("&cAmount must be between 1 and 64."));
                        return;
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(CC.translate("&cInvalid amount: " + arguments[2]));
                    return;
                }
            }
            
            customItem.setAmount(amount);
            player.getInventory().addItem(customItem);
            player.sendMessage(CC.translate("&aAdded &e" + amount + " &ax &e" + type + " &ato your inventory."));
        } else {
            player.sendMessage(CC.translate("&cUsage: /eden item <set|reset|give> <type> [amount]"));
        }
    }

    @Override
    public List<String> getDefaultTabComplete(CommandArguments command) {
        String[] args = command.getArgs();
        
        List<String> itemActions = Arrays.asList("set", "reset", "give");
        
        if (args.length <= 1) {
            // First argument: action (set, reset, give)
            return filterCompletions(args, 0, itemActions);
        } else if (args.length == 2) {
            // Second argument: item type
            return filterCompletions(args, 1, rip.diamond.practice.managers.DefaultItem.getNames());
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            // Third argument for 'give': amount suggestions
            return filterCompletions(args, 2, Arrays.asList("1", "16", "32", "64"));
        }
        
        return java.util.Collections.emptyList();
    }
}
