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
        String sizeStr = config.getString("queue-menu.size");

        if ("dynamic".equalsIgnoreCase(sizeStr)) {
            if (page > 1) {
                return config.getInt("queue-menu.max-size");
            }

            int itemsPerPage = getItemsPerPage(config);
            List<Kit> kits = getFilteredKits();
            int kitsOnThisPage = Math.min(kits.size() - ((page - 1) * itemsPerPage), itemsPerPage);

            boolean hasBorder = config.getBoolean("queue-menu.border.enabled");
            int contentSlots = kitsOnThisPage;
            int rowsNeeded = (int) Math.ceil(contentSlots / 7.0); // 7 slots per row (accounting for borders)
            int totalRows = rowsNeeded + (hasBorder ? 2 : 0); // Add top and bottom border rows

            // Ensure minimum of 3 rows and maximum as configured
            int maxSize = config.getInt("queue-menu.max-size");
            int calculatedSize = Math.max(27, Math.min(totalRows * 9, maxSize));

            // Round to valid inventory size
            return ((calculatedSize + 8) / 9) * 9;
        } else {
            return config.getInt("queue-menu.size");
        }
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        BasicConfigFile config = Eden.INSTANCE.getMenusConfig().getConfig();
        int itemsPerPage = getItemsPerPage(config);

        // Filler
        if (config.getBoolean("queue-menu.filler.enabled")) {
            ItemStack filler = new ItemBuilder(
                    org.bukkit.Material.valueOf(config.getString("queue-menu.filler.material")))
                    .durability(config.getInt("queue-menu.filler.data"))
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
        if (config.getBoolean("queue-menu.border.enabled")) {
            ItemStack border = new ItemBuilder(
                    org.bukkit.Material.valueOf(config.getString("queue-menu.border.material")))
                    .durability(config.getInt("queue-menu.border.data"))
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
        if (page > 1) {
            int prevSlot = config.getInt("queue-menu.items.previous-page.slot");
            buttons.put(prevSlot, new Button() {
                @Override
                public ItemStack getButtonItem(Player player) {
                    return new ItemBuilder(
                            org.bukkit.Material.valueOf(config.getString("queue-menu.items.previous-page.material")))
                            .name(config.getString("queue-menu.items.previous-page.name"))
                            .lore(config.getStringList("queue-menu.items.previous-page.lore"))
                            .build();
                }

                @Override
                public void clicked(Player player, ClickType clickType) {
                    new QueueMenu(queueType, page - 1).openMenu(player);
                }
            });
        }

        if (endIndex < allKits.size()) {
            int nextSlot = config.getInt("queue-menu.items.next-page.slot");
            buttons.put(nextSlot, new Button() {
                @Override
                public ItemStack getButtonItem(Player player) {
                    return new ItemBuilder(
                            org.bukkit.Material.valueOf(config.getString("queue-menu.items.next-page.material")))
                            .name(config.getString("queue-menu.items.next-page.name"))
                            .lore(config.getStringList("queue-menu.items.next-page.lore"))
                            .build();
                }

                @Override
                public void clicked(Player player, ClickType clickType) {
                    new QueueMenu(queueType, page + 1).openMenu(player);
                }
            });
        }

        return buttons;
    }

    private int getItemsPerPage(BasicConfigFile config) {
        int size;
        if (config.getString("queue-menu.size").equalsIgnoreCase("dynamic")) {
            size = config.getInt("queue-menu.max-size");
        } else {
            size = config.getInt("queue-menu.size");
        }

        if (config.getBoolean("queue-menu.border.enabled")) {
            int rows = size / 9;
            return (rows - 2) * 7;
        } else {
            return size - 9;
        }
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
                            String name = (entry != null) ? entry.getPlayerName() : "None";
                            String number = (entry != null) ? String.valueOf(entry.getData()) : "0";

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
