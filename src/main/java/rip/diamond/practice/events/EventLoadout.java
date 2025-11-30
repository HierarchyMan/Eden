package rip.diamond.practice.events;

import lombok.Getter;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import rip.diamond.practice.Eden;
import rip.diamond.practice.util.PlayerUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a predefined event loadout from eventloadouts.yml
 * Supports full ItemStack configuration including enchantments, amounts, etc.
 */
@Getter
public class EventLoadout {

    private final String eventName;
    private ItemStack helmet;
    private ItemStack chestplate;
    private ItemStack leggings;
    private ItemStack boots;
    private final Map<Integer, ItemStack> items = new HashMap<>();

    public EventLoadout(String eventName) {
        this.eventName = eventName;
        load();
    }

    /**
     * Load loadout from eventloadouts.yml
     */
    private void load() {
        ConfigurationSection section = Eden.INSTANCE.getEventLoadoutsFile().getConfiguration()
                .getConfigurationSection(eventName);
        if (section == null) {
            return;
        }

        // Load armor
        if (section.contains("helmet")) {
            this.helmet = parseItemStack(section, "helmet");
        }
        if (section.contains("chestplate")) {
            this.chestplate = parseItemStack(section, "chestplate");
        }
        if (section.contains("leggings")) {
            this.leggings = parseItemStack(section, "leggings");
        }
        if (section.contains("boots")) {
            this.boots = parseItemStack(section, "boots");
        }

        // Load items
        if (section.contains("items")) {
            ConfigurationSection itemsSection = section.getConfigurationSection("items");
            if (itemsSection != null) {
                for (String slotStr : itemsSection.getKeys(false)) {
                    int slot = Integer.parseInt(slotStr);
                    ItemStack item = parseItemStack(itemsSection, slotStr);
                    if (item != null) {
                        items.put(slot, item);
                    }
                }
            }
        }
    }

    /**
     * Parse ItemStack from configuration section
     * Supports:
     * - Simple format: "STONE_SWORD"
     * - Complex format with material, amount, enchantments
     * - Material:Amount format: "ARROW:16"
     */
    private ItemStack parseItemStack(ConfigurationSection section, String key) {
        Object value = section.get(key);

        if (value instanceof String) {
            String str = (String) value;

            // Check for Material:Amount format (e.g., "ARROW:16")
            if (str.contains(":")) {
                String[] parts = str.split(":");
                Material material = Material.getMaterial(parts[0]);
                if (material == null) {
                    return null;
                }
                int amount = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
                return new ItemStack(material, amount);
            }

            // Simple material format
            Material material = Material.getMaterial(str);
            if (material == null) {
                return null;
            }
            return new ItemStack(material);
        }

        if (value instanceof ConfigurationSection) {
            ConfigurationSection itemSection = (ConfigurationSection) value;

            // Get material
            String materialStr = itemSection.getString("material");
            if (materialStr == null) {
                return null;
            }

            Material material = Material.getMaterial(materialStr);
            if (material == null) {
                return null;
            }

            // Get amount
            int amount = itemSection.getInt("amount", 1);

            ItemStack item = new ItemStack(material, amount);

            // Apply enchantments
            if (itemSection.contains("enchantments")) {
                for (String enchantStr : itemSection.getStringList("enchantments")) {
                    String[] parts = enchantStr.split(":");
                    String enchantName = parts[0];
                    int level = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;

                    Enchantment enchantment = Enchantment.getByName(enchantName);
                    if (enchantment != null) {
                        item.addUnsafeEnchantment(enchantment, level);
                    }
                }
            }

            return item;
        }

        return null;
    }

    /**
     * Apply this loadout to a player
     * 
     * @param player The player to apply the loadout to
     */
    public void apply(Player player) {
        apply(player, null);
    }

    /**
     * Apply this loadout to a player with optional leather armor color override
     * 
     * @param player       The player to apply the loadout to
     * @param leatherColor Optional color to apply to leather armor (for OITC team
     *                     colors)
     */
    public void apply(Player player, Color leatherColor) {
        PlayerUtil.reset(player);

        // Apply armor
        if (helmet != null) {
            ItemStack helmetCopy = helmet.clone();
            if (leatherColor != null && helmetCopy.getType().name().contains("LEATHER")) {
                LeatherArmorMeta meta = (LeatherArmorMeta) helmetCopy.getItemMeta();
                meta.setColor(leatherColor);
                helmetCopy.setItemMeta(meta);
            }
            player.getInventory().setHelmet(helmetCopy);
        }

        if (chestplate != null) {
            ItemStack chestplateCopy = chestplate.clone();
            if (leatherColor != null && chestplateCopy.getType().name().contains("LEATHER")) {
                LeatherArmorMeta meta = (LeatherArmorMeta) chestplateCopy.getItemMeta();
                meta.setColor(leatherColor);
                chestplateCopy.setItemMeta(meta);
            }
            player.getInventory().setChestplate(chestplateCopy);
        }

        if (leggings != null) {
            ItemStack leggingsCopy = leggings.clone();
            if (leatherColor != null && leggingsCopy.getType().name().contains("LEATHER")) {
                LeatherArmorMeta meta = (LeatherArmorMeta) leggingsCopy.getItemMeta();
                meta.setColor(leatherColor);
                leggingsCopy.setItemMeta(meta);
            }
            player.getInventory().setLeggings(leggingsCopy);
        }

        if (boots != null) {
            ItemStack bootsCopy = boots.clone();
            if (leatherColor != null && bootsCopy.getType().name().contains("LEATHER")) {
                LeatherArmorMeta meta = (LeatherArmorMeta) bootsCopy.getItemMeta();
                meta.setColor(leatherColor);
                bootsCopy.setItemMeta(meta);
            }
            player.getInventory().setBoots(bootsCopy);
        }

        // Apply items
        for (Map.Entry<Integer, ItemStack> entry : items.entrySet()) {
            player.getInventory().setItem(entry.getKey(), entry.getValue().clone());
        }

        player.updateInventory();
    }

    /**
     * Check if this loadout exists in the config
     */
    public boolean exists() {
        return helmet != null || chestplate != null || leggings != null || boots != null || !items.isEmpty();
    }
}
