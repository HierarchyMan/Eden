package rip.diamond.practice.party.fight.menu;

import lombok.RequiredArgsConstructor;
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
import rip.diamond.practice.util.menu.MenuUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ChooseKitMenu extends Menu {
    private final KitMatchType kitMatchType;
    private final int page;

    public ChooseKitMenu(KitMatchType kitMatchType) {
        this(kitMatchType, 1);
    }

    @Override
    public String getTitle(Player player) {
        BasicConfigFile config = Eden.INSTANCE.getMenusConfig().getConfig();
        return CC.translate(config.getString("party-choose-kit-menu.title"));
    }

    @Override
    public int getSize() {
        BasicConfigFile config = Eden.INSTANCE.getMenusConfig().getConfig();
        int itemsPerPage = MenuUtil.getItemsPerPage(config, "party-choose-kit-menu");
        List<Kit> kits = getFilteredKits();
        return MenuUtil.calculateDynamicSize(config, "party-choose-kit-menu", page, itemsPerPage, kits.size());
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        BasicConfigFile config = Eden.INSTANCE.getMenusConfig().getConfig();
        int itemsPerPage = MenuUtil.getItemsPerPage(config, "party-choose-kit-menu");

        // Filler and Border
        MenuUtil.addFillerButtons(buttons, config, "party-choose-kit-menu", getSize());
        MenuUtil.addBorderButtons(buttons, config, "party-choose-kit-menu", getSize());

        // Kit buttons
        List<Kit> allKits = getFilteredKits();
        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, allKits.size());

        List<Kit> kitsOnThisPage = allKits.subList(startIndex, endIndex);

        boolean hasBorder = config.getBoolean("party-choose-kit-menu.border.enabled");
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
                    String name = config.getString("party-choose-kit-menu.items.kit-button.name");
                    if (name == null)
                        name = "&b" + kit.getDisplayName();

                    return new ItemBuilder(kit.getDisplayIcon().getType())
                            .durability(kit.getDisplayIcon().getDurability())
                            .name(name.replace("{kit-name}", kit.getDisplayName()))
                            .lore(config.getStringList("party-choose-kit-menu.items.kit-button.lore"))
                            .build();
                }

                @Override
                public void clicked(Player player, ClickType clickType) {
                    PlayerProfile profile = PlayerProfile.get(player);
                    if (profile.getSettings().get(ProfileSettings.ARENA_SELECTION).isEnabled()) {
                        new ChooseArenaMenu(kitMatchType, kit).openMenu(player);
                        return;
                    }
                    player.closeInventory();
                    Eden.INSTANCE.getPartyFightManager().startPartyEvent(player, kitMatchType, kit,
                            Arena.getEnabledArena(kit));
                }
            });
            kitIndex++;
        }

        // Pagination
        MenuUtil.addPreviousPageButton(buttons, config, "party-choose-kit-menu", page,
            p -> new ChooseKitMenu(kitMatchType, page - 1).openMenu(p));
        MenuUtil.addNextPageButton(buttons, config, "party-choose-kit-menu", endIndex < allKits.size(),
            p -> new ChooseKitMenu(kitMatchType, page + 1).openMenu(p));

        return buttons;
    }


    private List<Kit> getFilteredKits() {
        return Kit.getKits().stream()
                .filter(kit -> kit.getKitMatchTypes().contains(kitMatchType) && kit.isEnabled())
                .collect(Collectors.toList());
    }
}
