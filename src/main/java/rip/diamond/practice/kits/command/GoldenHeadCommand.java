package rip.diamond.practice.kits.command;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import rip.diamond.practice.config.Language;
import rip.diamond.practice.util.CC;
import rip.diamond.practice.util.Checker;
import rip.diamond.practice.util.Common;
import rip.diamond.practice.util.exception.PracticeUnexpectedException;
import rip.diamond.practice.util.serialization.BukkitSerialization;

public class GoldenHeadCommand {
    public void executeGoldenHead(Player player, String[] args) {
        ItemStack goldenHead = rip.diamond.practice.Eden.INSTANCE.getCustomItemManager().getItem("GOLDEN_HEAD");

        if (goldenHead == null) {
            player.sendMessage(CC.translate("&cGolden Head item is not configured."));
            return;
        }

        goldenHead = goldenHead.clone();

        if (args.length != 1) {
            player.getInventory().addItem(goldenHead);
        } else {
            if (!Checker.isInteger(args[0])) {
                Language.INVALID_SYNTAX.sendMessage(player);
                return;
            }
            goldenHead.setAmount(Integer.parseInt(args[0]));
            player.getInventory().addItem(goldenHead);
        }

        Common.sendMessage(player, CC.YELLOW + "[Eden] Added " + CC.GREEN + (args.length != 1 ? 1 : args[0]) + CC.GOLD
                + " Golden Head " + CC.YELLOW + "into your inventory.");
    }
}
