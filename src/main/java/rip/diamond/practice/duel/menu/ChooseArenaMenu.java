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
import rip.diamond.practice.util.menu.MenuUtil;

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
        int itemsPerPage = MenuUtil.getItemsPerPage(config, "duel-choose-arena-menu");
        List<Arena> arenas = getFilteredArenas();
        return MenuUtil.calculateDynamicSize(config, "duel-choose-arena-menu", page, itemsPerPage, arenas.size());
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        BasicConfigFile config = Eden.INSTANCE.getMenusConfig().getConfig();
        int itemsPerPage = MenuUtil.getItemsPerPage(config, "duel-choose-arena-menu");

        
        MenuUtil.addFillerButtons(buttons, config, "duel-choose-arena-menu", getSize());
        MenuUtil.addBorderButtons(buttons, config, "duel-choose-arena-menu", getSize());

        
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

        
        List<Arena> allArenas = getFilteredArenas();
        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, allArenas.size());

        List<Arena> arenasOnThisPage = allArenas.subList(startIndex, endIndex);

        boolean hasBorder = config.getBoolean("duel-choose-arena-menu.border.enabled");
        int arenaIndex = 0;
        for (int slot = 0; slot < getSize() && arenaIndex < arenasOnThisPage.size(); slot++) {
            
            if (hasBorder && (slot < 9 || slot >= getSize() - 9 || slot % 9 == 0 || slot % 9 == 8)) {
                continue;
            }
            
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

        
        MenuUtil.addPreviousPageButton(buttons, config, "duel-choose-arena-menu", page,
            p -> new ChooseArenaMenu(targetUUID, kit, page - 1).openMenu(p));
        MenuUtil.addNextPageButton(buttons, config, "duel-choose-arena-menu", endIndex < allArenas.size(),
            p -> new ChooseArenaMenu(targetUUID, kit, page + 1).openMenu(p));

        return buttons;
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
