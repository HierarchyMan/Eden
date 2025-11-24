package rip.diamond.practice.kiteditor.menu;

import lombok.RequiredArgsConstructor;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import rip.diamond.practice.Eden;
import rip.diamond.practice.config.Language;
import rip.diamond.practice.kits.Kit;
import rip.diamond.practice.kits.KitLoadout;
import rip.diamond.practice.match.team.TeamColor;
import rip.diamond.practice.profile.PlayerProfile;
import rip.diamond.practice.profile.procedure.Procedure;
import rip.diamond.practice.profile.procedure.ProcedureType;
import rip.diamond.practice.util.BasicConfigFile;
import rip.diamond.practice.util.CC;
import rip.diamond.practice.util.ItemBuilder;
import rip.diamond.practice.util.menu.Button;
import rip.diamond.practice.util.menu.Menu;
import rip.diamond.practice.util.menu.MenuUtil;

import java.util.*;

@RequiredArgsConstructor
public class KitEditorMenu extends Menu {

    private final Kit kit;
    private final int page;

    @Override
    public String getTitle(Player player) {
        return CC.translate(Eden.INSTANCE.getMenusConfig().getConfig().getString("kit-editor-menu.title"));
    }

    @Override
    public int getSize() {
        return Eden.INSTANCE.getMenusConfig().getConfig().getInt("kit-editor-menu.size");
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        BasicConfigFile config = Eden.INSTANCE.getMenusConfig().getConfig();
        PlayerProfile profile = PlayerProfile.get(player);

        // Filler and Border
        MenuUtil.addFillerButtons(buttons, config, "kit-editor-menu", getSize());
        MenuUtil.addBorderButtons(buttons, config, "kit-editor-menu", getSize());

        // Team Colorization Logic - Use consistent color per player session
        final TeamColor teamColor;
        if (kit.getGameRules().isTeamProjectile()) {
            // Use player UUID to determine consistent team color (prevents volatile color
            // changes)
            teamColor = (player.getUniqueId().hashCode() % 2 == 0) ? TeamColor.RED : TeamColor.BLUE;
        } else {
            teamColor = null;
        }

        // Armor
        ItemStack[] armor = kit.getKitLoadout().getArmor();
        if (armor != null) {
            setArmorButton(buttons, config.getInt("kit-editor-menu.items.armor.helmet"), armor[3], teamColor);
            setArmorButton(buttons, config.getInt("kit-editor-menu.items.armor.chestplate"), armor[2], teamColor);
            setArmorButton(buttons, config.getInt("kit-editor-menu.items.armor.leggings"), armor[1], teamColor);
            setArmorButton(buttons, config.getInt("kit-editor-menu.items.armor.boots"), armor[0], teamColor);
        }

        // Kit Slots
        List<Integer> slotPositions = config.getConfiguration().getIntegerList("kit-editor-menu.items.kit-slots");
        int slotsPerPage = slotPositions.size();
        int startIndex = (page - 1) * slotsPerPage;

        for (int i = 0; i < slotsPerPage; i++) {
            int kitIndex = startIndex + i;
            // Max 8 kits total as per request
            if (kitIndex >= 8)
                break;

            int slot = slotPositions.get(i);
            KitLoadout loadout = profile.getKitData().get(kit.getName()).getLoadouts()[kitIndex];
            String loadoutName = (loadout == null) ? "Kit " + (kitIndex + 1) : loadout.getCustomName();

            // Book Button (Book when empty, Book and Quill when saved)
            buttons.put(slot, new Button() {
                @Override
                public ItemStack getButtonItem(Player player) {
                    if (loadout == null) {
                        // Empty slot configuration
                        String emptyMaterial = config.getString("kit-editor-menu.items.book-button.empty.material");
                        int emptyData = config.getInt("kit-editor-menu.items.book-button.empty.data");
                        String emptyName = config.getString("kit-editor-menu.items.book-button.empty.name")
                                .replace("%slot%", String.valueOf(kitIndex + 1));
                        List<String> emptyLore = config.getStringList("kit-editor-menu.items.book-button.empty.lore");

                        return new ItemBuilder(Material.valueOf(emptyMaterial))
                                .durability(emptyData)
                                .name(emptyName)
                                .lore(emptyLore)
                                .build();
                    } else {
                        // Saved kit configuration
                        String savedMaterial = config.getString("kit-editor-menu.items.book-button.saved.material");
                        int savedData = config.getInt("kit-editor-menu.items.book-button.saved.data");
                        String savedName = config.getString("kit-editor-menu.items.book-button.saved.name")
                                .replace("%name%", loadoutName)
                                .replace("%slot%", String.valueOf(kitIndex + 1));
                        List<String> savedLore = config.getStringList("kit-editor-menu.items.book-button.saved.lore");

                        return new ItemBuilder(Material.valueOf(savedMaterial))
                                .durability(savedData)
                                .name(savedName)
                                .lore(savedLore)
                                .build();
                    }
                }

                @Override
                public void clicked(Player player, ClickType clickType) {
                    if (loadout == null) {
                        // Create new kit
                        KitLoadout newLoadout = new KitLoadout("Kit " + (kitIndex + 1), kit);
                        newLoadout.setContents(kit.getKitLoadout().getContents());
                        newLoadout.setArmor(kit.getKitLoadout().getArmor());
                        profile.getKitData().get(kit.getName()).replaceKit(kitIndex, newLoadout);
                        new KitEditorMenu(kit, page).openMenu(player);
                    } else {
                        // Load kit into inventory
                        player.closeInventory();
                        loadout.apply(kit, null, player); // Match is null, just applies to inventory
                        colorize(player, teamColor);
                        player.sendMessage(CC.translate("&aKit loaded."));
                        // Reopen menu after 1 tick delay to show updated inventory
                        Eden.INSTANCE.getServer().getScheduler().runTaskLater(Eden.INSTANCE, () -> {
                            new KitEditorMenu(kit, page).openMenu(player);
                        }, 1L);
                    }
                }
            });

            // Action Buttons (Only if loadout exists)
            if (loadout != null) {
                // Save Button (formerly Load Button)
                if (config.getBoolean("kit-editor-menu.items.dynamic-buttons.save.enabled")) {
                    int loadSlot = slot + config.getInt("kit-editor-menu.items.dynamic-buttons.save.slot-offset");
                    buttons.put(loadSlot, new Button() {
                        @Override
                        public ItemStack getButtonItem(Player player) {
                            return new ItemBuilder(
                                    Material.valueOf(
                                            config.getString("kit-editor-menu.items.dynamic-buttons.save.material")))
                                    .durability(config.getInt("kit-editor-menu.items.dynamic-buttons.save.data"))
                                    .name(config.getString("kit-editor-menu.items.dynamic-buttons.save.name"))
                                    .lore(config.getStringList("kit-editor-menu.items.dynamic-buttons.save.lore"))
                                    .build();
                        }

                        @Override
                        public void clicked(Player player, ClickType clickType) {
                            // Save current inventory to this kit
                            loadout.setContents(player.getInventory().getContents());
                            loadout.setArmor(player.getInventory().getArmorContents());
                            profile.getKitData().get(kit.getName()).replaceKit(kitIndex, loadout);
                            player.sendMessage(CC.translate("&aKit saved."));
                            new KitEditorMenu(kit, page).openMenu(player);
                        }
                    });
                }

                // Rename Button
                if (config.getBoolean("kit-editor-menu.items.dynamic-buttons.rename.enabled")) {
                    int renameSlot = slot + config.getInt("kit-editor-menu.items.dynamic-buttons.rename.slot-offset");
                    buttons.put(renameSlot, new Button() {
                        @Override
                        public ItemStack getButtonItem(Player player) {
                            return new ItemBuilder(
                                    Material.valueOf(
                                            config.getString("kit-editor-menu.items.dynamic-buttons.rename.material")))
                                    .durability(config.getInt("kit-editor-menu.items.dynamic-buttons.rename.data"))
                                    .name(config.getString("kit-editor-menu.items.dynamic-buttons.rename.name"))
                                    .lore(config.getStringList("kit-editor-menu.items.dynamic-buttons.rename.lore"))
                                    .build();
                        }

                        @Override
                        public void clicked(Player player, ClickType clickType) {
                            player.closeInventory();
                            Procedure.buildProcedure(player, "&aType the new name for this kit:", ProcedureType.CHAT,
                                    (string) -> {
                                        String message = (String) string;
                                        if (!message.matches("[a-zA-Z0-9_\\s+]*")) {
                                            player.sendMessage(CC.translate("&cInvalid characters."));
                                            return;
                                        }
                                        loadout.setCustomName(message);
                                        new KitEditorMenu(kit, page).openMenu(player);
                                    });
                        }
                    });
                }

                // Reset Button
                if (config.getBoolean("kit-editor-menu.items.dynamic-buttons.reset.enabled")) {
                    int resetSlot = slot + config.getInt("kit-editor-menu.items.dynamic-buttons.reset.slot-offset");
                    buttons.put(resetSlot, new Button() {
                        @Override
                        public ItemStack getButtonItem(Player player) {
                            return new ItemBuilder(
                                    Material.valueOf(
                                            config.getString("kit-editor-menu.items.dynamic-buttons.reset.material")))
                                    .durability(config.getInt("kit-editor-menu.items.dynamic-buttons.reset.data"))
                                    .name(config.getString("kit-editor-menu.items.dynamic-buttons.reset.name"))
                                    .lore(config.getStringList("kit-editor-menu.items.dynamic-buttons.reset.lore"))
                                    .build();
                        }

                        @Override
                        public void clicked(Player player, ClickType clickType) {
                            loadout.setContents(kit.getKitLoadout().getContents());
                            loadout.setArmor(kit.getKitLoadout().getArmor());
                            profile.getKitData().get(kit.getName()).replaceKit(kitIndex, loadout);
                            player.sendMessage(CC.translate("&aKit reset to default layout."));
                            // Close menu, apply default kit to player, then reopen
                            player.closeInventory();
                            player.getInventory().setContents(kit.getKitLoadout().getContents());
                            player.getInventory().setArmorContents(kit.getKitLoadout().getArmor());
                            colorize(player, teamColor);
                            player.updateInventory();
                            Eden.INSTANCE.getServer().getScheduler().runTaskLater(Eden.INSTANCE, () -> {
                                new KitEditorMenu(kit, page).openMenu(player);
                            }, 1L);
                        }
                    });
                }

                // Delete Button
                if (config.getBoolean("kit-editor-menu.items.dynamic-buttons.delete.enabled")) {
                    int deleteSlot = slot + config.getInt("kit-editor-menu.items.dynamic-buttons.delete.slot-offset");
                    buttons.put(deleteSlot, new Button() {
                        @Override
                        public ItemStack getButtonItem(Player player) {
                            return new ItemBuilder(
                                    Material.valueOf(
                                            config.getString("kit-editor-menu.items.dynamic-buttons.delete.material")))
                                    .durability(config.getInt("kit-editor-menu.items.dynamic-buttons.delete.data"))
                                    .name(config.getString("kit-editor-menu.items.dynamic-buttons.delete.name"))
                                    .lore(config.getStringList("kit-editor-menu.items.dynamic-buttons.delete.lore"))
                                    .build();
                        }

                        @Override
                        public void clicked(Player player, ClickType clickType) {
                            profile.getKitData().get(kit.getName()).deleteKit(kitIndex);
                            new KitEditorMenu(kit, page).openMenu(player);
                        }
                    });
                }
            }
        }

        // Extra Items
        if (!kit.getKitExtraItems().isEmpty()) {
            buttons.put(config.getInt("kit-editor-menu.items.extra-items.slot"), new Button() {
                @Override
                public ItemStack getButtonItem(Player player) {
                    return new ItemBuilder(
                            Material.valueOf(config.getString("kit-editor-menu.items.extra-items.material")))
                            .name(config.getString("kit-editor-menu.items.extra-items.name"))
                            .lore(config.getStringList("kit-editor-menu.items.extra-items.lore"))
                            .build();
                }

                @Override
                public void clicked(Player player, ClickType clickType) {
                    new KitEditorExtraItemsMenu(kit, KitEditorMenu.this).openMenu(player);
                }
            });
        }

        // Pagination
        if (page > 1) {
            buttons.put(config.getInt("kit-editor-menu.items.previous-page.slot"), new Button() {
                @Override
                public ItemStack getButtonItem(Player player) {
                    return new ItemBuilder(
                            Material.valueOf(config.getString("kit-editor-menu.items.previous-page.material")))
                            .name(config.getString("kit-editor-menu.items.previous-page.name"))
                            .build();
                }

                @Override
                public void clicked(Player player, ClickType clickType) {
                    new KitEditorMenu(kit, page - 1).openMenu(player);
                }
            });
        }

        if (startIndex + slotsPerPage < 8) { // Max 8 kits
            // Only show next page button if on page 1 and all kits 1-4 are saved
            boolean showNextPage = true;
            if (page == 1) {
                // Check if all kits 1-4 are saved
                for (int i = 0; i < 4; i++) {
                    if (profile.getKitData().get(kit.getName()).getLoadouts()[i] == null) {
                        showNextPage = false;
                        break;
                    }
                }
            }

            if (showNextPage) {
                buttons.put(config.getInt("kit-editor-menu.items.next-page.slot"), new Button() {
                    @Override
                    public ItemStack getButtonItem(Player player) {
                        return new ItemBuilder(
                                Material.valueOf(config.getString("kit-editor-menu.items.next-page.material")))
                                .name(config.getString("kit-editor-menu.items.next-page.name"))
                                .build();
                    }

                    @Override
                    public void clicked(Player player, ClickType clickType) {
                        new KitEditorMenu(kit, page + 1).openMenu(player);
                    }
                });
            }
        }

        // Back Button
        buttons.put(config.getInt("kit-editor-menu.items.back-button.slot"), new Button() {
            @Override
            public ItemStack getButtonItem(Player player) {
                return new ItemBuilder(
                        Material.valueOf(config.getString("kit-editor-menu.items.back-button.material")))
                        .name(config.getString("kit-editor-menu.items.back-button.name"))
                        .lore(config.getStringList("kit-editor-menu.items.back-button.lore"))
                        .build();
            }

            @Override
            public void clicked(Player player, ClickType clickType) {
                // Exit editing mode and restore lobby items
                Eden.INSTANCE.getKitEditorManager().leaveKitEditor(player, true);
                // Schedule opening the selector menu after lobby items are given
                Eden.INSTANCE.getServer().getScheduler().runTaskLater(Eden.INSTANCE, () -> {
                    new KitEditorSelectKitMenu().openMenu(player);
                }, 2L);
            }
        });

        return buttons;
    }

    private void colorize(Player player, TeamColor teamColor) {
        if (teamColor == null)
            return;

        ItemStack[] contents = player.getInventory().getContents();
        boolean changed = false;

        for (ItemStack item : contents) {
            if (item != null && (item.getType() == Material.WOOL ||
                    item.getType() == Material.STAINED_GLASS_PANE ||
                    item.getType() == Material.STAINED_CLAY ||
                    item.getType() == Material.STAINED_GLASS ||
                    item.getType() == Material.CARPET)) {
                item.setDurability((short) teamColor.getDurability());
                changed = true;
            }
        }

        if (changed) {
            player.getInventory().setContents(contents);
        }

        ItemStack[] armor = player.getInventory().getArmorContents();
        boolean armorChanged = false;
        for (ItemStack item : armor) {
            if (item != null && item.getType().name().contains("LEATHER")) {
                LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
                meta.setColor(Color.fromRGB(teamColor.getRgb()));
                item.setItemMeta(meta);
                armorChanged = true;
            }
        }

        if (armorChanged) {
            player.getInventory().setArmorContents(armor);
        }

        player.updateInventory();
    }

    private void setArmorButton(Map<Integer, Button> buttons, int slot, ItemStack item, TeamColor teamColor) {
        BasicConfigFile config = Eden.INSTANCE.getMenusConfig().getConfig();

        // Handle empty armor slots with placeholder
        if (item == null || item.getType() == Material.AIR) {
            String placeholderMaterial = config.getString("kit-editor-menu.items.armor.empty-placeholder.material");

            // If material is empty string or not set, don't show placeholder
            if (placeholderMaterial == null || placeholderMaterial.trim().isEmpty()) {
                return;
            }

            // Create placeholder item
            ItemStack placeholder = new ItemBuilder(Material.valueOf(placeholderMaterial))
                    .durability(config.getInt("kit-editor-menu.items.armor.empty-placeholder.data"))
                    .name(config.getString("kit-editor-menu.items.armor.empty-placeholder.name"))
                    .lore(config.getStringList("kit-editor-menu.items.armor.empty-placeholder.lore"))
                    .build();

            buttons.put(slot, new Button() {
                @Override
                public ItemStack getButtonItem(Player player) {
                    return placeholder;
                }
            });
            return;
        }

        ItemStack finalItem = item.clone();
        if (teamColor != null) {
            // Colorize leather armor
            if (finalItem.getType().name().contains("LEATHER")) {
                LeatherArmorMeta meta = (LeatherArmorMeta) finalItem.getItemMeta();
                meta.setColor(Color.fromRGB(teamColor.getRgb()));
                finalItem.setItemMeta(meta);
            }
            // Colorize blocks (wool, stained glass pane, stained clay)
            else if (finalItem.getType() == Material.WOOL ||
                    finalItem.getType() == Material.STAINED_GLASS_PANE ||
                    finalItem.getType() == Material.STAINED_CLAY) {
                finalItem.setDurability((short) teamColor.getDurability());
            }
        }

        buttons.put(slot, new Button() {
            @Override
            public ItemStack getButtonItem(Player player) {
                return finalItem;
            }
        });
    }
}
