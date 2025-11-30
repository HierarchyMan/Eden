package rip.diamond.practice.util;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Utility class for editing items in-hand
 * Used by /eden edititem commands
 */
public class ItemEditorUtil {

    /**
     * Remove all attributes from the item in hand
     * This includes hiding enchantments, attributes, unbreakable, etc.
     */
    public static boolean removeAttributes(Player player) {
        ItemStack item = player.getItemInHand();

        if (item == null || item.getType() == Material.AIR) {
            Common.sendMessage(player, CC.RED + "You must be holding an item!");
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            Common.sendMessage(player, CC.RED + "This item cannot have attributes removed!");
            return false;
        }

        
        for (ItemFlag flag : ItemFlag.values()) {
            meta.addItemFlags(flag);
        }

        item.setItemMeta(meta);
        Common.sendMessage(player, CC.GREEN + "Successfully removed all attributes from your item!");
        return true;
    }

    /**
     * Set the item in hand as unbreakable
     */
    public static boolean setUnbreakable(Player player) {
        ItemStack item = player.getItemInHand();

        if (item == null || item.getType() == Material.AIR) {
            Common.sendMessage(player, CC.RED + "You must be holding an item!");
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            Common.sendMessage(player, CC.RED + "This item cannot be made unbreakable!");
            return false;
        }

        try {
            
            meta.spigot().setUnbreakable(true);
            item.setItemMeta(meta);
            Common.sendMessage(player, CC.GREEN + "Successfully made your item unbreakable!");
            return true;
        } catch (Exception e) {
            Common.sendMessage(player, CC.RED + "Failed to make item unbreakable: " + e.getMessage());
            return false;
        }
    }

    /**
     * Add an enchantment to the item in hand
     *
     * @param player The player holding the item
     * @param enchantmentName The name of the enchantment
     * @param level The level of the enchantment
     */
    public static boolean addEnchantment(Player player, String enchantmentName, int level) {
        ItemStack item = player.getItemInHand();

        if (item == null || item.getType() == Material.AIR) {
            Common.sendMessage(player, CC.RED + "You must be holding an item!");
            return false;
        }

        if (!Checker.isEnchantment(enchantmentName)) {
            Common.sendMessage(player, CC.RED + "Invalid enchantment: " + enchantmentName);
            return false;
        }

        Enchantment enchantment = Enchantment.getByName(enchantmentName.toUpperCase());
        if (enchantment == null) {
            Common.sendMessage(player, CC.RED + "Invalid enchantment: " + enchantmentName);
            return false;
        }

        item.addUnsafeEnchantment(enchantment, level);
        Common.sendMessage(player, CC.GREEN + "Successfully enchanted your item with " + CC.YELLOW +
            enchantmentName.toUpperCase() + " " + level + CC.GREEN + "!");
        return true;
    }
}

