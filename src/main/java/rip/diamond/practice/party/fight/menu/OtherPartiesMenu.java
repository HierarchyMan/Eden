package rip.diamond.practice.party.fight.menu;

import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import rip.diamond.practice.Eden;
import rip.diamond.practice.party.Party;
import rip.diamond.practice.party.PartyPrivacy;
import rip.diamond.practice.util.BasicConfigFile;
import rip.diamond.practice.util.CC;
import rip.diamond.practice.util.Common;
import rip.diamond.practice.util.ItemBuilder;
import rip.diamond.practice.util.menu.Button;
import rip.diamond.practice.util.menu.Menu;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OtherPartiesMenu extends Menu {
    private final int page;

    public OtherPartiesMenu() {
        this(1);
    }

    public OtherPartiesMenu(int page) {
        this.page = page;
    }

    @Override
    public String getTitle(Player player) {
        BasicConfigFile config = Eden.INSTANCE.getMenusConfig().getConfig();
        return CC.translate(config.getString("party-other-parties-menu.title"));
    }

    @Override
    public int getSize() {
        BasicConfigFile config = Eden.INSTANCE.getMenusConfig().getConfig();
        String sizeStr = config.getString("party-other-parties-menu.size");

        if ("dynamic".equalsIgnoreCase(sizeStr)) {
            // Calculate dynamic size based on number of parties
            if (page > 1) {
                return config.getInt("party-other-parties-menu.max-size");
            }

            int partiesCount = Party.getParties().values().stream()
                    .filter(party -> party.getPrivacy() == PartyPrivacy.OPEN
                            && party.getAllPartyMembers().size() < party.getMaxSize())
                    .collect(Collectors.toList()).size();

            int itemsPerPage = getItemsPerPage(config);
            int partiesOnThisPage = Math.min(partiesCount - ((page - 1) * itemsPerPage), itemsPerPage);

            boolean hasBorder = config.getBoolean("party-other-parties-menu.border.enabled");
            int contentSlots = partiesOnThisPage;
            int rowsNeeded = (int) Math.ceil(contentSlots / 7.0);
            int totalRows = rowsNeeded + (hasBorder ? 2 : 0);

            int maxSize = config.getInt("party-other-parties-menu.max-size");
            int calculatedSize = Math.max(27, Math.min(totalRows * 9, maxSize));

            return ((calculatedSize + 8) / 9) * 9;
        } else {
            return config.getInt("party-other-parties-menu.size");
        }
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        BasicConfigFile config = Eden.INSTANCE.getMenusConfig().getConfig();
        int itemsPerPage = getItemsPerPage(config);

        // Filler
        if (config.getBoolean("party-other-parties-menu.filler.enabled")) {
            ItemStack filler = new ItemBuilder(
                    Material.valueOf(config.getString("party-other-parties-menu.filler.material")))
                    .durability(config.getInt("party-other-parties-menu.filler.data"))
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
        if (config.getBoolean("party-other-parties-menu.border.enabled")) {
            ItemStack border = new ItemBuilder(
                    Material.valueOf(config.getString("party-other-parties-menu.border.material")))
                    .durability(config.getInt("party-other-parties-menu.border.data"))
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

        // Party buttons with pagination
        List<Party> allParties = Party.getParties().values().stream()
                .filter(party -> party.getPrivacy() == PartyPrivacy.OPEN
                        && party.getAllPartyMembers().size() < party.getMaxSize())
                .collect(Collectors.toList());

        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, allParties.size());

        List<Party> partiesOnThisPage = allParties.subList(startIndex, endIndex);

        // Place parties in available slots (avoiding border)
        int partyIndex = 0;
        for (int slot = 0; slot < getSize() && partyIndex < partiesOnThisPage.size(); slot++) {
            // Skip border slots
            if (buttons.containsKey(slot) && (slot < 9 || slot >= getSize() - 9 || slot % 9 == 0 || slot % 9 == 8)) {
                continue;
            }
            if (buttons.containsKey(slot))
                continue;

            Party party = partiesOnThisPage.get(partyIndex);
            buttons.put(slot, new PartyButton(party, config));
            partyIndex++;
        }

        // Pagination buttons
        if (page > 1) {
            int prevSlot = config.getInt("party-other-parties-menu.items.previous-page.slot");
            buttons.put(prevSlot, new Button() {
                @Override
                public ItemStack getButtonItem(Player player) {
                    return new ItemBuilder(
                            Material.valueOf(config.getString("party-other-parties-menu.items.previous-page.material")))
                            .name(config.getString("party-other-parties-menu.items.previous-page.name"))
                            .lore(config.getStringList("party-other-parties-menu.items.previous-page.lore"))
                            .build();
                }

                @Override
                public void clicked(Player player, ClickType clickType) {
                    new OtherPartiesMenu(page - 1).openMenu(player);
                }
            });
        }

        if (endIndex < allParties.size()) {
            int nextSlot = config.getInt("party-other-parties-menu.items.next-page.slot");
            buttons.put(nextSlot, new Button() {
                @Override
                public ItemStack getButtonItem(Player player) {
                    return new ItemBuilder(
                            Material.valueOf(config.getString("party-other-parties-menu.items.next-page.material")))
                            .name(config.getString("party-other-parties-menu.items.next-page.name"))
                            .lore(config.getStringList("party-other-parties-menu.items.next-page.lore"))
                            .build();
                }

                @Override
                public void clicked(Player player, ClickType clickType) {
                    new OtherPartiesMenu(page + 1).openMenu(player);
                }
            });
        }

        return buttons;
    }

    private int getItemsPerPage(BasicConfigFile config) {
        int size;
        if (config.getString("party-other-parties-menu.size").equalsIgnoreCase("dynamic")) {
            size = config.getInt("party-other-parties-menu.max-size");
        } else {
            size = config.getInt("party-other-parties-menu.size");
        }

        if (config.getBoolean("party-other-parties-menu.border.enabled")) {
            int rows = size / 9;
            return (rows - 2) * 7;
        } else {
            return size - 9;
        }
    }

    @RequiredArgsConstructor
    private static class PartyButton extends Button {
        private final Party party;
        private final BasicConfigFile config;

        @Override
        public ItemStack getButtonItem(Player player) {
            String leaderName = party.getLeader().getUsername();
            int members = party.getAllPartyMembers().size();
            int maxSize = party.getMaxSize();

            // Build member list
            String memberFormat = config.getString("party-other-parties-menu.items.party-button.member-format");
            List<String> memberList = party.getAllPartyMembers().stream()
                    .map(partyMember -> {
                        String memberName = partyMember.getUsername();
                        return memberFormat.replace("{member}", memberName);
                    })
                    .collect(Collectors.toList());

            // Build lore with placeholders
            List<String> lore = config.getStringList("party-other-parties-menu.items.party-button.lore");
            lore = lore.stream()
                    .map(line -> line.replace("{leader}", leaderName))
                    .map(line -> line.replace("{members}", String.valueOf(members)))
                    .map(line -> line.replace("{max-size}", String.valueOf(maxSize)))
                    .map(line -> {
                        if (line.contains("{member-list}")) {
                            // Replace with actual member list
                            return null; // Mark for replacement
                        }
                        return line;
                    })
                    .collect(Collectors.toList());

            // Insert member list where placeholder was
            List<String> finalLore = lore.stream()
                    .flatMap(line -> {
                        if (line == null) {
                            return memberList.stream();
                        }
                        return java.util.stream.Stream.of(line);
                    })
                    .collect(Collectors.toList());

            String name = config.getString("party-other-parties-menu.items.party-button.name")
                    .replace("{leader}", leaderName);

            return new ItemBuilder(
                    Material.valueOf(config.getString("party-other-parties-menu.items.party-button.material")))
                    .durability(config.getInt("party-other-parties-menu.items.party-button.data"))
                    .name(name)
                    .lore(finalLore)
                    .build();
        }

        @Override
        public void clicked(Player player, ClickType clickType) {
            player.closeInventory();
            Common.sendMessage(player, "/party join " + party.getLeader().getUsername());
            player.performCommand("party join " + party.getLeader().getUsername());
        }
    }
}
