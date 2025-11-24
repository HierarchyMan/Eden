package rip.diamond.practice.leaderboard.menu;

import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import rip.diamond.practice.Eden;
import rip.diamond.practice.leaderboard.menu.impl.*;
import rip.diamond.practice.profile.PlayerProfile;
import rip.diamond.practice.util.BasicConfigFile;
import rip.diamond.practice.util.CC;
import rip.diamond.practice.util.ItemBuilder;
import rip.diamond.practice.util.menu.Button;
import rip.diamond.practice.util.menu.MenuUtil;
import rip.diamond.practice.util.menu.pagination.PageButton;
import rip.diamond.practice.util.menu.pagination.PaginatedMenu;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class LeaderboardMenu extends PaginatedMenu {

    private Integer[] getAllowedSlots() {
        List<String> slotStrings = Eden.INSTANCE.getMenusConfig().getConfig()
                .getStringList("leaderboard-menu.items.allowed-slots");
        return slotStrings.stream()
                .map(Integer::parseInt)
                .toArray(Integer[]::new);
    }

    @Override
    public int getSize() {
        return Eden.INSTANCE.getMenusConfig().getConfig().getInt("leaderboard-menu.size");
    }

    @Override
    public int getMaxItemsPerPage(Player player) {
        return getAllowedSlots().length;
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        BasicConfigFile config = Eden.INSTANCE.getMenusConfig().getConfig();
        int minIndex = (int) ((double) (getPage() - 1) * getMaxItemsPerPage(player));
        int maxIndex = (int) ((double) (getPage()) * getMaxItemsPerPage(player));
        int topIndex = 0;
        Integer[] allowedSlots = getAllowedSlots();

        HashMap<Integer, Button> buttons = new HashMap<>();

        // Add filler and border first
        MenuUtil.addFillerButtons(buttons, config, "leaderboard-menu", getSize());
        MenuUtil.addBorderButtons(buttons, config, "leaderboard-menu", getSize());

        for (Map.Entry<Integer, Button> entry : getAllPagesButtons(player).entrySet()) {
            int index = entry.getKey();

            if (index >= minIndex && index < maxIndex) {
                index -= (int) ((double) (getMaxItemsPerPage(player)) * (getPage() - 1));
                buttons.put(allowedSlots[index], entry.getValue());
                if (index > topIndex) {
                    topIndex = index;
                }
            }
        }

        buttons.put(0, new PageButton(-1, this));
        buttons.put(8, new PageButton(1, this));

        for (int i = 1; i < 8; i++) {
            buttons.put(i, getPlaceholderButton());
        }

        Map<Integer, Button> global = getGlobalButtons(player);

        if (global != null) {
            buttons.putAll(global);
        }


        return buttons;
    }

    @Override
    public Map<Integer, Button> getGlobalButtons(Player player) {
        BasicConfigFile config = Eden.INSTANCE.getMenusConfig().getConfig();
        Map<Integer, Button> buttons = new HashMap<>();

        // Wins button
        if (config.getConfiguration().contains("leaderboard-menu.items.switch-buttons.wins")) {
            buttons.put(
                config.getInt("leaderboard-menu.items.switch-buttons.wins.slot"),
                new SwitchLeaderboardButton(
                    Material.valueOf(config.getString("leaderboard-menu.items.switch-buttons.wins.material")),
                    config.getInt("leaderboard-menu.items.switch-buttons.wins.data"),
                    config.getString("leaderboard-menu.items.switch-buttons.wins.name"),
                    config.getStringList("leaderboard-menu.items.switch-buttons.wins.lore"),
                    WinsLeaderboardMenu.class
                )
            );
        }

        // ELO button
        if (config.getConfiguration().contains("leaderboard-menu.items.switch-buttons.elo")) {
            buttons.put(
                config.getInt("leaderboard-menu.items.switch-buttons.elo.slot"),
                new SwitchLeaderboardButton(
                    Material.valueOf(config.getString("leaderboard-menu.items.switch-buttons.elo.material")),
                    config.getInt("leaderboard-menu.items.switch-buttons.elo.data"),
                    config.getString("leaderboard-menu.items.switch-buttons.elo.name"),
                    config.getStringList("leaderboard-menu.items.switch-buttons.elo.lore"),
                    EloLeaderboardMenu.class
                )
            );
        }

        // View Stats button
        if (config.getConfiguration().contains("leaderboard-menu.items.switch-buttons.view-stats")) {
            buttons.put(
                config.getInt("leaderboard-menu.items.switch-buttons.view-stats.slot"),
                new SwitchLeaderboardButton(
                    Material.valueOf(config.getString("leaderboard-menu.items.switch-buttons.view-stats.material")),
                    config.getInt("leaderboard-menu.items.switch-buttons.view-stats.data"),
                    config.getString("leaderboard-menu.items.switch-buttons.view-stats.name"),
                    config.getStringList("leaderboard-menu.items.switch-buttons.view-stats.lore"),
                    KitStatsMenu.class
                )
            );
        }

        // Winstreak button
        if (config.getConfiguration().contains("leaderboard-menu.items.switch-buttons.winstreak")) {
            buttons.put(
                config.getInt("leaderboard-menu.items.switch-buttons.winstreak.slot"),
                new SwitchLeaderboardButton(
                    Material.valueOf(config.getString("leaderboard-menu.items.switch-buttons.winstreak.material")),
                    config.getInt("leaderboard-menu.items.switch-buttons.winstreak.data"),
                    config.getString("leaderboard-menu.items.switch-buttons.winstreak.name"),
                    config.getStringList("leaderboard-menu.items.switch-buttons.winstreak.lore"),
                    WinstreakLeaderboardMenu.class
                )
            );
        }

        // Best Winstreak button
        if (config.getConfiguration().contains("leaderboard-menu.items.switch-buttons.best-winstreak")) {
            buttons.put(
                config.getInt("leaderboard-menu.items.switch-buttons.best-winstreak.slot"),
                new SwitchLeaderboardButton(
                    Material.valueOf(config.getString("leaderboard-menu.items.switch-buttons.best-winstreak.material")),
                    config.getInt("leaderboard-menu.items.switch-buttons.best-winstreak.data"),
                    config.getString("leaderboard-menu.items.switch-buttons.best-winstreak.name"),
                    config.getStringList("leaderboard-menu.items.switch-buttons.best-winstreak.lore"),
                    BestWinstreakLeaderboardMenu.class
                )
            );
        }

        return buttons;
    }

    @RequiredArgsConstructor
    private class SwitchLeaderboardButton extends Button {
        private final Material material;
        private final int durability;
        private final String name;
        private final List<String> lore;
        private final Class<?> clazz;

        @Override
        public ItemStack getButtonItem(Player player) {
            ItemBuilder builder = new ItemBuilder(material)
                    .durability(durability)
                    .name(CC.translate(name))
                    .lore(CC.translate(lore));
            if (clazz.getName().equals(LeaderboardMenu.this.getClass().getName())) {
                builder.glow();
            }
            return builder.build();
        }

        @Override
        public void clicked(Player player, ClickType clickType) {
            if (clazz.equals(KitStatsMenu.class)) {
                new KitStatsMenu(PlayerProfile.get(player)).openMenu(player);
                return;
            }
            try {
                LeaderboardMenu menu = (LeaderboardMenu) clazz.newInstance();
                menu.openMenu(player);
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

}
