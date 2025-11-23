package rip.diamond.practice.duel.menu;

import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import rip.diamond.practice.Eden;
import rip.diamond.practice.arenas.Arena;
import rip.diamond.practice.arenas.ArenaDetail;
import rip.diamond.practice.config.Language;
import rip.diamond.practice.kits.Kit;
import rip.diamond.practice.util.BasicConfigFile;
import rip.diamond.practice.util.CC;
import rip.diamond.practice.util.Common;
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
public class ChooseArenaMenu extends Menu {
    private final UUID targetUUID;
    private final Kit kit;
    private final int page;

    public ChooseArenaMenu(UUID targetUUID, Kit kit) {
        this(targetUUID, kit, 1);
    }

    @Override
    public String getTitle(Player player) {
        BasicConfigFile config = Eden.INSTANCE.getMenusConfig().getConfig();
        return CC.translate(config.getString("duel-choose-arena-menu.title"));
    }

    @Override
    public int getSize() {
        BasicConfigFile config = Eden.INSTANCE.getMenusConfig().getConfig();
        String sizeStr = config.getString("duel-choose-arena-menu.size");

        if ("dynamic".equalsIgnoreCase(sizeStr)) {
            if (page > 1) {
                return config.getInt("duel-choose-arena-menu.max-size");
            }

            int itemsPerPage = getItemsPerPage(config);
            List<Arena> arenas = getFilteredArenas();
            int itemsOnThisPage = Math.min(arenas.size() - ((page - 1) * itemsPerPage), itemsPerPage);

            boolean hasBorder = config.getBoolean("duel-choose-arena-menu.border.enabled");
            int contentSlots = itemsOnThisPage;
            int rowsNeeded = (int) Math.ceil(contentSlots / 7.0);
            int totalRows = rowsNeeded + (hasBorder ? 2 : 0);

            int maxSize = config.getInt("duel-choose-arena-menu.max-size");
            int calculatedSize = Math.max(27, Math.min(totalRows * 9, maxSize));

            return ((calculatedSize + 8) / 9) * 9;
        } else {
            return config.getInt("duel-choose-arena-menu.size");
        }
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        BasicConfigFile config = Eden.INSTANCE.getMenusConfig().getConfig();
        int itemsPerPage = getItemsPerPage(config);

        // Filler
        if (config.getBoolean("duel-choose-arena-menu.filler.enabled")) {
            ItemStack filler = new ItemBuilder(
                    org.bukkit.Material.valueOf(config.getString("duel-choose-arena-menu.filler.material")))
                    .durability(config.getInt("duel-choose-arena-menu.filler.data"))
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
        if (config.getBoolean("duel-choose-arena-menu.border.enabled")) {
            ItemStack border = new ItemBuilder(
                    org.bukkit.Material.valueOf(config.getString("duel-choose-arena-menu.border.material")))
                    .durability(config.getInt("duel-choose-arena-menu.border.data"))
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

        // Random Arena Button
        int randomSlot = config.getInt("duel-choose-arena-menu.items.random-arena-button.slot");
        buttons.put(randomSlot, new Button() {
            @Override
            public ItemStack getButtonItem(Player player) {
                return new ItemBuilder(
                        org.bukkit.Material
                                .valueOf(config.getString("duel-choose-arena-menu.items.random-arena-button.material")))
                        .name(config.getString("duel-choose-arena-menu.items.random-arena-button.name"))
                        .lore(config.getStringList("duel-choose-arena-menu.items.random-arena-button.lore"))
                        .build();
            }

            @Override
            public void clicked(Player player, ClickType clickType) {
                Arena arena = Arena.getEnabledArena(kit);
                if (arena == null) {
                    Common.log("[Eden] There's no available arenas for kit " + kit.getName()
                            + ", consider add more arenas.");
                    return;
                }
                player.closeInventory();
                Eden.INSTANCE.getDuelRequestManager().sendDuelRequest(player, Bukkit.getPlayer(targetUUID), kit, arena);
            }
        });

        // Go Back Button
        if (config.getBoolean("duel-choose-arena-menu.items.go-back.enabled")) {
            int goBackSlot = config.getInt("duel-choose-arena-menu.items.go-back.slot");
            buttons.put(goBackSlot, new Button() {
                @Override
                public ItemStack getButtonItem(Player player) {
                    return new ItemBuilder(
                            org.bukkit.Material
                                    .valueOf(config.getString("duel-choose-arena-menu.items.go-back.material")))
                            .name(config.getString("duel-choose-arena-menu.items.go-back.name"))
                            .lore(config.getStringList("duel-choose-arena-menu.items.go-back.lore"))
                            .build();
                }

                @Override
                public void clicked(Player player, ClickType clickType) {
                    new rip.diamond.practice.duel.menu.ChooseKitMenu(targetUUID, false).openMenu(player);
                }
            });
        }

        // Arenas
        List<Arena> allArenas = getFilteredArenas();
        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, allArenas.size());

        List<Arena> arenasOnThisPage = allArenas.subList(startIndex, endIndex);

        boolean hasBorder = config.getBoolean("duel-choose-arena-menu.border.enabled");
        int arenaIndex = 0;
        for (int slot = 0; slot < getSize() && arenaIndex < arenasOnThisPage.size(); slot++) {
            // Skip border slots and the random arena button slot
            if (hasBorder && (slot < 9 || slot >= getSize() - 9 || slot % 9 == 0 || slot % 9 == 8)) {
                continue;
            }
            // Skip if this slot is the random arena button
            if (slot == randomSlot) {
                continue;
            }

            Arena arena = arenasOnThisPage.get(arenaIndex);
            buttons.put(slot, new Button() {
                @Override
                public ItemStack getButtonItem(Player player) {
                    return new ItemBuilder(arena.getIcon().clone())
                            .name(config.getString("duel-choose-arena-menu.items.arena-button.name")
                                    .replace("{arena-name}", arena.getDisplayName()))
                            .lore(config.getStringList("duel-choose-arena-menu.items.arena-button.lore"))
                            .build();
                }

                @Override
                public void clicked(Player player, ClickType clickType) {
                    player.closeInventory();
                    Eden.INSTANCE.getDuelRequestManager().sendDuelRequest(player, Bukkit.getPlayer(targetUUID), kit,
                            arena);
                }
            });
            arenaIndex++;
        }

        // Pagination
        if (page > 1) {
            int prevSlot = config.getInt("duel-choose-arena-menu.items.previous-page.slot");
            buttons.put(prevSlot, new Button() {
                @Override
                public ItemStack getButtonItem(Player player) {
                    return new ItemBuilder(
                            org.bukkit.Material
                                    .valueOf(config.getString("duel-choose-arena-menu.items.previous-page.material")))
                            .name(config.getString("duel-choose-arena-menu.items.previous-page.name"))
                            .lore(config.getStringList("duel-choose-arena-menu.items.previous-page.lore"))
                            .build();
                }

                @Override
                public void clicked(Player player, ClickType clickType) {
                    new ChooseArenaMenu(targetUUID, kit, page - 1).openMenu(player);
                }
            });
        }

        if (endIndex < allArenas.size()) {
            int nextSlot = config.getInt("duel-choose-arena-menu.items.next-page.slot");
            buttons.put(nextSlot, new Button() {
                @Override
                public ItemStack getButtonItem(Player player) {
                    return new ItemBuilder(
                            org.bukkit.Material
                                    .valueOf(config.getString("duel-choose-arena-menu.items.next-page.material")))
                            .name(config.getString("duel-choose-arena-menu.items.next-page.name"))
                            .lore(config.getStringList("duel-choose-arena-menu.items.next-page.lore"))
                            .build();
                }

                @Override
                public void clicked(Player player, ClickType clickType) {
                    new ChooseArenaMenu(targetUUID, kit, page + 1).openMenu(player);
                }
            });
        }

        return buttons;
    }

    private int getItemsPerPage(BasicConfigFile config) {
        int size;
        if (config.getString("duel-choose-arena-menu.size").equalsIgnoreCase("dynamic")) {
            size = config.getInt("duel-choose-arena-menu.max-size");
        } else {
            size = config.getInt("duel-choose-arena-menu.size");
        }

        if (config.getBoolean("duel-choose-arena-menu.border.enabled")) {
            int rows = size / 9;
            return (rows - 2) * 7;
        } else {
            return size - 9;
        }
    }

    private List<Arena> getFilteredArenas() {
        return Arena.getArenas().stream()
                .filter(arena -> {
                    return arena.isEnabled() && !arena.isLocked() && !arena.getArenaDetails().isEmpty()
                            && arena.getAllowedKits().contains(kit.getName());
                })
                .collect(Collectors.toList());
    }
}
