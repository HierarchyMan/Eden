package rip.diamond.practice.leaderboard.menu.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import rip.diamond.practice.Eden;
import rip.diamond.practice.kits.Kit;
import rip.diamond.practice.leaderboard.menu.LeaderboardMenu;
import rip.diamond.practice.profile.PlayerProfile;
import rip.diamond.practice.profile.data.ProfileKitData;
import rip.diamond.practice.util.BasicConfigFile;
import rip.diamond.practice.util.CC;
import rip.diamond.practice.util.ItemBuilder;
import rip.diamond.practice.util.menu.Button;

import java.util.*;

@RequiredArgsConstructor
public class KitStatsMenu extends LeaderboardMenu {

    private final PlayerProfile profile;

    @Override
    public String getPrePaginatedTitle(Player player) {
        String title = Eden.INSTANCE.getMenusConfig().getConfig().getString("kit-stats-menu.title");
        return CC.translate(title.replace("{player}", profile.getUsername()));
    }

    @Override
    public Map<Integer, Button> getGlobalButtons(Player player) {
        final Map<Integer, Button> buttons = super.getGlobalButtons(player);
        BasicConfigFile config = Eden.INSTANCE.getMenusConfig().getConfig();
        if (config.getConfiguration().contains("kit-stats-menu.items.global-stats")) {
            buttons.put(config.getInt("kit-stats-menu.items.global-stats.slot"), new GlobalStatsButton());
        }
        return buttons;
    }

    @Override
    public Map<Integer, Button> getAllPagesButtons(Player player) {
        final Map<Integer, Button> buttons = new HashMap<>();

        profile.getKitData().keySet().stream()
                .map(Kit::getByName)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(Kit::getPriority))
                .forEach(kit -> buttons.put(buttons.size(), new KitStatsButton(kit.getName())));

        return buttons;
    }

    private class GlobalStatsButton extends Button {
        @Override
        public ItemStack getButtonItem(Player player) {
            BasicConfigFile config = Eden.INSTANCE.getMenusConfig().getConfig();

            int rankedWon = profile.getKitData().values().stream().mapToInt(ProfileKitData::getRankedWon).sum();
            int rankedLost = profile.getKitData().values().stream().mapToInt(ProfileKitData::getRankedLost).sum();
            int unrankedWon = profile.getKitData().values().stream().mapToInt(ProfileKitData::getUnrankedWon).sum();
            int unrankedLost = profile.getKitData().values().stream().mapToInt(ProfileKitData::getUnrankedLost).sum();
            int avgElo = profile.getKitData().values().stream().mapToInt(ProfileKitData::getElo).sum() / (profile.getKitData().size() == 0 ? 1 : profile.getKitData().size());

            List<String> lore = config.getStringList("kit-stats-menu.items.global-stats.lore");
            lore = replacePlaceholders(lore,
                    unrankedWon, unrankedLost,
                    Eden.DECIMAL.format((double) unrankedWon / (double) (unrankedLost == 0 ? 1 : unrankedLost)),
                    avgElo, rankedWon, rankedLost,
                    Eden.DECIMAL.format((double) rankedWon / (double) (rankedLost == 0 ? 1 : rankedLost)));

            return new ItemBuilder(
                    Material.valueOf(config.getString("kit-stats-menu.items.global-stats.material")))
                    .durability(config.getInt("kit-stats-menu.items.global-stats.data"))
                    .name(CC.translate(config.getString("kit-stats-menu.items.global-stats.name")))
                    .lore(CC.translate(lore))
                    .build();
        }

        private List<String> replacePlaceholders(List<String> lore, int unrankedWins, int unrankedLosses, String unrankedWL,
                                                  int rankedElo, int rankedWins, int rankedLosses, String rankedWL) {
            List<String> replaced = new ArrayList<>();
            for (String line : lore) {
                replaced.add(line
                        .replace("{unranked-wins}", String.valueOf(unrankedWins))
                        .replace("{unranked-losses}", String.valueOf(unrankedLosses))
                        .replace("{unranked-wl}", unrankedWL)
                        .replace("{ranked-elo}", String.valueOf(rankedElo))
                        .replace("{ranked-wins}", String.valueOf(rankedWins))
                        .replace("{ranked-losses}", String.valueOf(rankedLosses))
                        .replace("{ranked-wl}", rankedWL));
            }
            return replaced;
        }
    }

    @Getter
    @RequiredArgsConstructor
    private class KitStatsButton extends Button {
        private final String kitName;

        @Override
        public ItemStack getButtonItem(Player player) {
            Kit kit = Kit.getByName(kitName);
            if (kit == null) {
                return new ItemStack(Material.AIR);
            }

            BasicConfigFile config = Eden.INSTANCE.getMenusConfig().getConfig();
            ProfileKitData kitData = profile.getKitData().get(kitName);

            int rankedWon = kitData.getRankedWon();
            int rankedLost = kitData.getRankedLost();
            int unrankedWon = kitData.getUnrankedWon();
            int unrankedLost = kitData.getUnrankedLost();
            int winstreak = kitData.getWinstreak();
            int bestWinstreak = kitData.getBestWinstreak();
            int elo = kitData.getElo();
            int peakElo = kitData.getPeakElo();

            List<String> lore = config.getStringList("kit-stats-menu.items.kit-stats.lore");
            lore = replacePlaceholders(lore, unrankedWon, unrankedLost, winstreak, bestWinstreak,
                    Eden.DECIMAL.format((double) unrankedWon / (double) (unrankedLost == 0 ? 1 : unrankedLost)),
                    elo, peakElo, rankedWon, rankedLost,
                    Eden.DECIMAL.format((double) rankedWon / (double) (rankedLost == 0 ? 1 : rankedLost)));

            String name = config.getString("kit-stats-menu.items.kit-stats.name")
                    .replace("{kit-name}", kit.getDisplayName());

            return new ItemBuilder(kit.getDisplayIcon().clone())
                    .name(CC.translate(name))
                    .lore(CC.translate(lore))
                    .build();
        }

        private List<String> replacePlaceholders(List<String> lore, int unrankedWins, int unrankedLosses,
                                                  int winstreak, int bestWinstreak, String unrankedWL,
                                                  int elo, int peakElo, int rankedWins, int rankedLosses, String rankedWL) {
            List<String> replaced = new ArrayList<>();
            for (String line : lore) {
                replaced.add(line
                        .replace("{unranked-wins}", String.valueOf(unrankedWins))
                        .replace("{unranked-losses}", String.valueOf(unrankedLosses))
                        .replace("{winstreak}", String.valueOf(winstreak))
                        .replace("{best-winstreak}", String.valueOf(bestWinstreak))
                        .replace("{unranked-wl}", unrankedWL)
                        .replace("{elo}", String.valueOf(elo))
                        .replace("{peak-elo}", String.valueOf(peakElo))
                        .replace("{ranked-wins}", String.valueOf(rankedWins))
                        .replace("{ranked-losses}", String.valueOf(rankedLosses))
                        .replace("{ranked-wl}", rankedWL));
            }
            return replaced;
        }
    }
}
