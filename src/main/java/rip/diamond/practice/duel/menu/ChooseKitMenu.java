package rip.diamond.practice.duel.menu;

import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import rip.diamond.practice.Eden;
import rip.diamond.practice.arenas.Arena;
import rip.diamond.practice.config.Language;
import rip.diamond.practice.kits.Kit;
import rip.diamond.practice.kits.KitMatchType;
import rip.diamond.practice.profile.PlayerProfile;
import rip.diamond.practice.profile.ProfileSettings;
import rip.diamond.practice.util.BasicConfigFile;
import rip.diamond.practice.util.CC;
import rip.diamond.practice.util.ItemBuilder;
import rip.diamond.practice.util.menu.Button;
import rip.diamond.practice.util.menu.Menu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ChooseKitMenu extends Menu {
    private final UUID targetUUID;
    private final boolean party;
    private final int page;

    public ChooseKitMenu(UUID targetUUID, boolean party) {
        this(targetUUID, party, 1);
    }

    @Override
    public String getTitle(Player player) {
        BasicConfigFile config = Eden.INSTANCE.getMenusConfig().getConfig();
        return CC.translate(config.getString("duel-choose-kit-menu.title"));
    }

    @Override
    public int getSize() {
        BasicConfigFile config = Eden.INSTANCE.getMenusConfig().getConfig();
        String sizeStr = config.getString("duel-choose-kit-menu.size");

        if ("dynamic".equalsIgnoreCase(sizeStr)) {
            if (page > 1) {
                return config.getInt("duel-choose-kit-menu.max-size");
            }

            int itemsPerPage = getItemsPerPage(config);
            List<Kit> kits = getFilteredKits();
            int kitsOnThisPage = Math.min(kits.size() - ((page - 1) * itemsPerPage), itemsPerPage);

            boolean hasBorder = config.getBoolean("duel-choose-kit-menu.border.enabled");
            int contentSlots = kitsOnThisPage;
            int rowsNeeded = (int) Math.ceil(contentSlots / 7.0);
            int totalRows = rowsNeeded + (hasBorder ? 2 : 0);

            int maxSize = config.getInt("duel-choose-kit-menu.max-size");
            int calculatedSize = Math.max(27, Math.min(totalRows * 9, maxSize));

            return ((calculatedSize + 8) / 9) * 9;
        } else {
            return config.getInt("duel-choose-kit-menu.size");
        }
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        BasicConfigFile config = Eden.INSTANCE.getMenusConfig().getConfig();
        int itemsPerPage = getItemsPerPage(config);

        // Filler
        if (config.getBoolean("duel-choose-kit-menu.filler.enabled")) {
            ItemStack filler = new ItemBuilder(
                    org.bukkit.Material.valueOf(config.getString("duel-choose-kit-menu.filler.material")))
                    .durability(config.getInt("duel-choose-kit-menu.filler.data"))
                    .name(" ")
                    .build();
            for (int i = 0; i < getSize(); i++) {
                buttons.put(i, new Button() {
                    @Override
                    public ItemStack getButtonItem(Player player) {
                        return filler;
                    }
                });
            }
        }

        // Border
        if (config.getBoolean("duel-choose-kit-menu.border.enabled")) {
            ItemStack border = new ItemBuilder(
                    org.bukkit.Material.valueOf(config.getString("duel-choose-kit-menu.border.material")))
                    .durability(config.getInt("duel-choose-kit-menu.border.data"))
                    .name(" ")
                    .build();
            int size = getSize();
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

        // Kit buttons
        List<Kit> allKits = getFilteredKits();
        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, allKits.size());

        List<Kit> kitsOnThisPage = allKits.subList(startIndex, endIndex);

        boolean hasBorder = config.getBoolean("duel-choose-kit-menu.border.enabled");
        int kitIndex = 0;
        for (int slot = 0; slot < getSize() && kitIndex < kitsOnThisPage.size(); slot++) {
            // Skip border slots
            if (hasBorder && (slot < 9 || slot >= getSize() - 9 || slot % 9 == 0 || slot % 9 == 8)) {
                continue;
            }

            Kit kit = kitsOnThisPage.get(kitIndex);
            buttons.put(slot, new Button() {
                @Override
                public ItemStack getButtonItem(Player player) {
                    String name = config.getString("duel-choose-kit-menu.items.kit-button.name");
                    if (name == null)
                        name = "&b" + kit.getDisplayName();

                    return new ItemBuilder(kit.getDisplayIcon().getType())
                            .durability(kit.getDisplayIcon().getDurability())
                            .name(name.replace("{kit-name}", kit.getDisplayName()))
                            .lore(config.getStringList("duel-choose-kit-menu.items.kit-button.lore"))
                            .build();
                }

                @Override
                public void clicked(Player player, ClickType clickType) {
                    if (party) {
                        new ChooseArenaMenu(targetUUID, kit).openMenu(player);
                    } else {
                        Player target = Bukkit.getPlayer(targetUUID);
                        if (target != null) {
                            PlayerProfile profile = PlayerProfile.get(player);
                            if (profile.getSettings().get(ProfileSettings.ARENA_SELECTION).isEnabled()) {
                                new ChooseArenaMenu(targetUUID, kit).openMenu(player);
                            } else {
                                player.closeInventory();
                                Eden.INSTANCE.getDuelRequestManager().sendDuelRequest(player, target, kit,
                                        Arena.getEnabledArena(kit));
                            }
                        }
                    }
                }
            });
            kitIndex++;
        }

        // Pagination
        if (page > 1) {
            int prevSlot = config.getInt("duel-choose-kit-menu.items.previous-page.slot");
            buttons.put(prevSlot, new Button() {
                @Override
                public ItemStack getButtonItem(Player player) {
                    return new ItemBuilder(
                            org.bukkit.Material
                                    .valueOf(config.getString("duel-choose-kit-menu.items.previous-page.material")))
                            .name(config.getString("duel-choose-kit-menu.items.previous-page.name"))
                            .lore(config.getStringList("duel-choose-kit-menu.items.previous-page.lore"))
                            .build();
                }

                @Override
                public void clicked(Player player, ClickType clickType) {
                    // The following line is from the provided diff, but 'target' and 'kitMatchType'
                    // are not defined in this class.
                    // Reverting to original logic for ChooseKitMenu constructor parameters.
                    new ChooseKitMenu(targetUUID, party, page - 1).openMenu(player);
                }
            });
        }

        if (endIndex < allKits.size()) {
            int nextSlot = config.getInt("duel-choose-kit-menu.items.next-page.slot");
            buttons.put(nextSlot, new Button() {
                @Override
                public ItemStack getButtonItem(Player player) {
                    return new ItemBuilder(
                            org.bukkit.Material
                                    .valueOf(config.getString("duel-choose-kit-menu.items.next-page.material")))
                            .name(config.getString("duel-choose-kit-menu.items.next-page.name"))
                            .lore(config.getStringList("duel-choose-kit-menu.items.next-page.lore"))
                            .build();
                }

                @Override
                public void clicked(Player player, ClickType clickType) {
                    // The following line is from the provided diff, but 'target' and 'kitMatchType'
                    // are not defined in this class.
                    // Reverting to original logic for ChooseKitMenu constructor parameters.
                    new ChooseKitMenu(targetUUID, party, page + 1).openMenu(player);
                }
            });
        }

        return buttons;
    }

    private int getItemsPerPage(BasicConfigFile config) {
        int size;
        if (config.getString("duel-choose-kit-menu.size").equalsIgnoreCase("dynamic")) {
            size = config.getInt("duel-choose-kit-menu.max-size");
        } else {
            size = config.getInt("duel-choose-kit-menu.size");
        }

        if (config.getBoolean("duel-choose-kit-menu.border.enabled")) {
            int rows = size / 9;
            return (rows - 2) * 7;
        } else {
            // If no border, assume a standard layout where the last row is for
            // pagination/close buttons
            // and the first row might also be for special buttons, leaving the middle for
            // items.
            // This calculation is a bit arbitrary without more context on the menu layout.
            // The original code used config.getInt("duel-choose-kit-menu.items-per-page")
            // directly.
            // The provided diff suggests 'size - 9' which implies one row is reserved.
            return size - 9;
        }
    }

    private List<Kit> getFilteredKits() {
        return Kit.getKits().stream()
                .filter(Kit::isEnabled)
                .filter(kit -> !party || kit.getKitMatchTypes().contains(KitMatchType.SPLIT))
                .collect(Collectors.toList());
    }
}
