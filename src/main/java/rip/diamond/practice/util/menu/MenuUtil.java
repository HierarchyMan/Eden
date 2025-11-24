package rip.diamond.practice.util.menu;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import rip.diamond.practice.util.BasicConfigFile;
import rip.diamond.practice.util.ItemBuilder;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Utility class to reduce redundancy in menu code
 */
public class MenuUtil {

    /**
     * Add filler buttons to a menu
     */
    public static void addFillerButtons(Map<Integer, Button> buttons, BasicConfigFile config, String menuPrefix, int size) {
        if (config.getBoolean(menuPrefix + ".filler.enabled")) {
            ItemStack filler = new ItemBuilder(
                    Material.valueOf(config.getString(menuPrefix + ".filler.material")))
                    .durability(config.getInt(menuPrefix + ".filler.data"))
                    .name(" ")
                    .build();
            for (int i = 0; i < size; i++) {
                buttons.put(i, new Button() {
                    @Override
                    public ItemStack getButtonItem(Player player) {
                        return filler;
                    }
                });
            }
        }
    }

    /**
     * Add border buttons to a menu
     */
    public static void addBorderButtons(Map<Integer, Button> buttons, BasicConfigFile config, String menuPrefix, int size) {
        if (config.getBoolean(menuPrefix + ".border.enabled")) {
            ItemStack border = new ItemBuilder(
                    Material.valueOf(config.getString(menuPrefix + ".border.material")))
                    .durability(config.getInt(menuPrefix + ".border.data"))
                    .name(" ")
                    .build();
            for (int i = 0; i < size; i++) {
                if (i < 9 || i >= size - 9 || i % 9 == 0 || i % 9 == 8) {
                    buttons.put(i, new Button() {
                        @Override
                        public ItemStack getButtonItem(Player player) {
                            return border;
                        }
                    });
                }
            }
        }
    }

    /**
     * Add a previous page button to a menu
     */
    public static void addPreviousPageButton(Map<Integer, Button> buttons, BasicConfigFile config,
                                             String menuPrefix, int currentPage, Consumer<Player> onClickAction) {
        if (currentPage > 1) {
            int prevSlot = config.getInt(menuPrefix + ".items.previous-page.slot");
            buttons.put(prevSlot, new Button() {
                @Override
                public ItemStack getButtonItem(Player player) {
                    return new ItemBuilder(
                            Material.valueOf(config.getString(menuPrefix + ".items.previous-page.material")))
                            .name(config.getString(menuPrefix + ".items.previous-page.name"))
                            .lore(config.getStringList(menuPrefix + ".items.previous-page.lore"))
                            .build();
                }

                @Override
                public void clicked(Player player, ClickType clickType) {
                    onClickAction.accept(player);
                }
            });
        }
    }

    /**
     * Add a next page button to a menu
     */
    public static void addNextPageButton(Map<Integer, Button> buttons, BasicConfigFile config,
                                        String menuPrefix, boolean hasNextPage, Consumer<Player> onClickAction) {
        if (hasNextPage) {
            int nextSlot = config.getInt(menuPrefix + ".items.next-page.slot");
            buttons.put(nextSlot, new Button() {
                @Override
                public ItemStack getButtonItem(Player player) {
                    return new ItemBuilder(
                            Material.valueOf(config.getString(menuPrefix + ".items.next-page.material")))
                            .name(config.getString(menuPrefix + ".items.next-page.name"))
                            .lore(config.getStringList(menuPrefix + ".items.next-page.lore"))
                            .build();
                }

                @Override
                public void clicked(Player player, ClickType clickType) {
                    onClickAction.accept(player);
                }
            });
        }
    }

    /**
     * Calculate items per page based on menu configuration
     */
    public static int getItemsPerPage(BasicConfigFile config, String menuPrefix) {
        int size;
        if (config.getString(menuPrefix + ".size").equalsIgnoreCase("dynamic")) {
            size = config.getInt(menuPrefix + ".max-size");
        } else {
            size = config.getInt(menuPrefix + ".size");
        }

        if (config.getBoolean(menuPrefix + ".border.enabled")) {
            int rows = size / 9;
            return (rows - 2) * 7;
        } else {
            return size - 9;
        }
    }

    /**
     * Calculate dynamic menu size
     */
    public static int calculateDynamicSize(BasicConfigFile config, String menuPrefix, int page,
                                          int itemsPerPage, int totalItems) {
        String sizeStr = config.getString(menuPrefix + ".size");

        if ("dynamic".equalsIgnoreCase(sizeStr)) {
            if (page > 1) {
                return config.getInt(menuPrefix + ".max-size");
            }

            int itemsOnThisPage = Math.min(totalItems - ((page - 1) * itemsPerPage), itemsPerPage);

            boolean hasBorder = config.getBoolean(menuPrefix + ".border.enabled");
            int rowsNeeded = (int) Math.ceil(itemsOnThisPage / 7.0);
            int totalRows = rowsNeeded + (hasBorder ? 2 : 0);

            int maxSize = config.getInt(menuPrefix + ".max-size");
            int calculatedSize = Math.max(27, Math.min(totalRows * 9, maxSize));

            return ((calculatedSize + 8) / 9) * 9;
        } else {
            return config.getInt(menuPrefix + ".size");
        }
    }

    /**
     * Check if a slot should be skipped when placing items (border slots)
     */
    public static boolean isBorderSlot(int slot, int size, boolean hasBorder) {
        if (!hasBorder) {
            return false;
        }
        return slot < 9 || slot >= size - 9 || slot % 9 == 0 || slot % 9 == 8;
    }
}

