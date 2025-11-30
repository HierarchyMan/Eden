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
import rip.diamond.practice.util.menu.MenuUtil;

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
        int itemsPerPage = MenuUtil.getItemsPerPage(config, "party-other-parties-menu");
        int partiesCount = Party.getParties().values().stream()
                .filter(party -> party.getPrivacy() == PartyPrivacy.OPEN
                        && party.getAllPartyMembers().size() < party.getMaxSize())
                .collect(Collectors.toList()).size();
        return MenuUtil.calculateDynamicSize(config, "party-other-parties-menu", page, itemsPerPage, partiesCount);
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        BasicConfigFile config = Eden.INSTANCE.getMenusConfig().getConfig();
        int itemsPerPage = MenuUtil.getItemsPerPage(config, "party-other-parties-menu");

        
        MenuUtil.addFillerButtons(buttons, config, "party-other-parties-menu", getSize());
        MenuUtil.addBorderButtons(buttons, config, "party-other-parties-menu", getSize());

        
        List<Party> allParties = Party.getParties().values().stream()
                .filter(party -> party.getPrivacy() == PartyPrivacy.OPEN
                        && party.getAllPartyMembers().size() < party.getMaxSize())
                .collect(Collectors.toList());

        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, allParties.size());

        List<Party> partiesOnThisPage = allParties.subList(startIndex, endIndex);

        
        int partyIndex = 0;
        for (int slot = 0; slot < getSize() && partyIndex < partiesOnThisPage.size(); slot++) {
            
            if (buttons.containsKey(slot) && (slot < 9 || slot >= getSize() - 9 || slot % 9 == 0 || slot % 9 == 8)) {
                continue;
            }
            if (buttons.containsKey(slot))
                continue;

            Party party = partiesOnThisPage.get(partyIndex);
            buttons.put(slot, new PartyButton(party, config));
            partyIndex++;
        }

        
        MenuUtil.addPreviousPageButton(buttons, config, "party-other-parties-menu", page,
            p -> new OtherPartiesMenu(page - 1).openMenu(p));
        MenuUtil.addNextPageButton(buttons, config, "party-other-parties-menu", endIndex < allParties.size(),
            p -> new OtherPartiesMenu(page + 1).openMenu(p));

        return buttons;
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

            
            String memberFormat = config.getString("party-other-parties-menu.items.party-button.member-format");
            List<String> memberList = party.getAllPartyMembers().stream()
                    .map(partyMember -> {
                        String memberName = partyMember.getUsername();
                        return memberFormat.replace("{member}", memberName);
                    })
                    .collect(Collectors.toList());

            
            List<String> lore = config.getStringList("party-other-parties-menu.items.party-button.lore");
            lore = lore.stream()
                    .map(line -> line.replace("{leader}", leaderName))
                    .map(line -> line.replace("{members}", String.valueOf(members)))
                    .map(line -> line.replace("{max-size}", String.valueOf(maxSize)))
                    .map(line -> {
                        if (line.contains("{member-list}")) {
                            
                            return null; 
                        }
                        return line;
                    })
                    .collect(Collectors.toList());

            
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
