package rip.diamond.practice.leaderboard.menu.impl;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import rip.diamond.practice.Eden;
import rip.diamond.practice.leaderboard.menu.LeaderboardMenu;
import rip.diamond.practice.util.CC;
import rip.diamond.practice.util.menu.Button;

import java.util.*;

public class BestWinstreakLeaderboardMenu extends LeaderboardMenu {

    @Override
    public String getPrePaginatedTitle(Player player) {
        return CC.translate(Eden.INSTANCE.getMenusConfig().getConfig().getString("leaderboard-menu.items.titles.best-winstreak"));
    }

    @Override
    public Map<Integer, Button> getAllPagesButtons(Player player) {
        final Map<Integer, Button> buttons = new HashMap<>();

        getPlugin().getLeaderboardManager().getBestWinstreakLeaderboard().values().stream()
                .filter(leaderboard -> leaderboard.getKit().isEnabled())
                .sorted(Comparator.comparingInt(leaderboard -> leaderboard.getKit().getPriority()))
                .forEach(leaderboard -> buttons.put(buttons.size(), new Button() {
                    @Override
                    public ItemStack getButtonItem(Player player) {
                        return leaderboard.getDisplayIcon();
                    }
                }));

        return buttons;
    }
}
