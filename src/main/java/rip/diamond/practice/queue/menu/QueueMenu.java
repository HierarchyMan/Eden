package rip.diamond.practice.queue.menu;

import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import rip.diamond.practice.Eden;
import rip.diamond.practice.kits.Kit;
import rip.diamond.practice.match.Match;
import rip.diamond.practice.queue.Queue;
import rip.diamond.practice.queue.QueueType;
import rip.diamond.practice.util.BasicConfigFile;
import rip.diamond.practice.util.CC;
import rip.diamond.practice.util.ItemBuilder;
import rip.diamond.practice.util.menu.Button;
import rip.diamond.practice.util.menu.Menu;
import rip.diamond.practice.util.menu.MenuUtil;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class QueueMenu extends Menu {

    private final QueueType queueType;
    private final int page;

    public QueueMenu(QueueType queueType) {
        this(queueType, 1);
    }

    @Override
    public String getTitle(Player player) {
        BasicConfigFile config = Eden.INSTANCE.getMenusConfig().getConfig();
        String title = config.getString("queue-menu.title");
        // Replace {queue-type} placeholder
        title = title.replace("{queue-type}", queueType.getReadable());
        return CC.translate(title);
    }

    @Override
    public int getSize() {
        BasicConfigFile config = Eden.INSTANCE.getMenusConfig().getConfig();
        int itemsPerPage = MenuUtil.getItemsPerPage(config, "queue-menu");
        List<Kit> kits = getFilteredKits();
        return MenuUtil.calculateDynamicSize(config, "queue-menu", page, itemsPerPage, kits.size());
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        BasicConfigFile config = Eden.INSTANCE.getMenusConfig().getConfig();
        int itemsPerPage = MenuUtil.getItemsPerPage(config, "queue-menu");

        // Filler and Border
        MenuUtil.addFillerButtons(buttons, config, "queue-menu", getSize());
        MenuUtil.addBorderButtons(buttons, config, "queue-menu", getSize());

        // Kit buttons with pagination
        List<Kit> allKits = getFilteredKits();
        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, allKits.size());

        List<Kit> kitsOnThisPage = allKits.subList(startIndex, endIndex);

        // Place kits in available slots (avoiding border)
        int kitIndex = 0;
        for (int slot = 0; slot < getSize() && kitIndex < kitsOnThisPage.size(); slot++) {
            // Skip border slots
            if (buttons.containsKey(slot) && (slot < 9 || slot >= getSize() - 9 || slot % 9 == 0 || slot % 9 == 8)) {
                continue;
            }

            Kit kit = kitsOnThisPage.get(kitIndex);
            buttons.put(slot, new KitButton(kit, queueType, config));
            kitIndex++;
        }

        // Pagination buttons
        MenuUtil.addPreviousPageButton(buttons, config, "queue-menu", page,
            p -> new QueueMenu(queueType, page - 1).openMenu(p));
        MenuUtil.addNextPageButton(buttons, config, "queue-menu", endIndex < allKits.size(),
            p -> new QueueMenu(queueType, page + 1).openMenu(p));

        return buttons;
    }


    private List<Kit> getFilteredKits() {
        return Kit.getKits().stream()
                .filter(Kit::isEnabled)
                .filter(kit -> queueType == QueueType.UNRANKED || kit.isRanked())
                .collect(Collectors.toList());
    }

    @RequiredArgsConstructor
    private static class KitButton extends Button {
        private final Kit kit;
        private final QueueType queueType;
        private final BasicConfigFile config;

        @Override
        public ItemStack getButtonItem(Player player) {
            // Get counts for placeholders
            long queueCount = Queue.getPlayers().values().stream()
                    .filter(profile -> profile.getKit() == kit && profile.getQueueType() == queueType)
                    .count();
            long fightingCount = Match.getMatches().values().stream()
                    .filter(match -> match.getKit() == kit && match.getQueueType() == queueType)
                    .mapToInt(match -> match.getMatchPlayers().size())
                    .sum();

            // Build lore with placeholders
            List<String> lore = config.getStringList("queue-menu.items.kit-button.lore");
            List<String> newLore = new ArrayList<>();

            for (String line : lore) {
                if (line.contains("{description}")) {
                    for (String descLine : kit.getDescription()) {
                        newLore.add(CC.translate(descLine));
                    }
                    continue;
                }

                line = line.replace("{queue-count}", String.valueOf(queueCount))
                        .replace("{fighting-count}", String.valueOf(fightingCount))
                        .replace("{kit-name}", kit.getDisplayName());

                if (line.contains("{top3_")) {
                    rip.diamond.practice.leaderboard.impl.KitLeaderboard leaderboard = Eden.INSTANCE
                            .getLeaderboardManager().getDailyWinstreakLeaderboard().get(kit);

                    if (leaderboard != null) {
                        for (int i = 1; i <= 3; i++) {
                            rip.diamond.practice.leaderboard.LeaderboardPlayerCache entry = leaderboard.getLeaderboard()
                                    .get(i);
                            String name = (entry != null) ? entry.getPlayerName() : "§7- ";
                            String number = (entry != null) ? String.valueOf(entry.getData()) : "§7-";

                            line = line.replace("{top3_name_" + i + "}", name)
                                    .replace("{top3_number_" + i + "}", number);
                        }
                    } else {
                        for (int i = 1; i <= 3; i++) {
                            line = line.replace("{top3_name_" + i + "}", "None")
                                    .replace("{top3_number_" + i + "}", "0");
                        }
                    }
                }

                newLore.add(line);
            }

            // Assign newLore to lore for final building (or just use newLore in builder)
            return new ItemBuilder(kit.getDisplayIcon().clone())
                    .name(kit.getDisplayName())
                    .lore(newLore)
                    .build();
        }

        @Override
        public void clicked(Player player, ClickType clickType) {
            player.closeInventory();
            Queue.joinQueue(player, kit, queueType);
        }
    }
}
