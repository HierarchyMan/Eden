package rip.diamond.practice.duel.menu;

import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
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
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ChooseKitMenu extends Menu {
    private final UUID targetUUID;
    private final boolean party;
    private final int page;

    public ChooseKitMenu(UUID targetUUID, boolean party) {
        this(targetUUID, party, 1);
    }

    @Override
    public String getTitle(Player player) {
        BasicConfigFile config = Eden.INSTANCE.getMenusConfig().getConfig();
        return CC.translate(config.getString("duel-choose-kit-menu.title"));
    }

    @Override
    public int getSize() {
        BasicConfigFile config = Eden.INSTANCE.getMenusConfig().getConfig();
        int itemsPerPage = MenuUtil.getItemsPerPage(config, "duel-choose-kit-menu");
        List<Kit> kits = getFilteredKits();
        return MenuUtil.calculateDynamicSize(config, "duel-choose-kit-menu", page, itemsPerPage, kits.size());
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        BasicConfigFile config = Eden.INSTANCE.getMenusConfig().getConfig();
        int itemsPerPage = MenuUtil.getItemsPerPage(config, "duel-choose-kit-menu");


        MenuUtil.addFillerButtons(buttons, config, "duel-choose-kit-menu", getSize());
        MenuUtil.addBorderButtons(buttons, config, "duel-choose-kit-menu", getSize());


        List<Kit> allKits = getFilteredKits();
        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, allKits.size());

        List<Kit> kitsOnThisPage = allKits.subList(startIndex, endIndex);

        boolean hasBorder = config.getBoolean("duel-choose-kit-menu.border.enabled");
        int kitIndex = 0;
        for (int slot = 0; slot < getSize() && kitIndex < kitsOnThisPage.size(); slot++) {

            if (hasBorder && (slot < 9 || slot >= getSize() - 9 || slot % 9 == 0 || slot % 9 == 8)) {
                continue;
            }

            Kit kit = kitsOnThisPage.get(kitIndex);
            buttons.put(slot, new Button() {
                @Override
                public ItemStack getButtonItem(Player player) {
                    String name = config.getString("duel-choose-kit-menu.items.kit-button.name");
                    if (name == null)
                        name = "&b" + kit.getDisplayName();

                    return new ItemBuilder(kit.getDisplayIcon().getType())
                            .durability(kit.getDisplayIcon().getDurability())
                            .name(name.replace("{kit-name}", kit.getDisplayName()))
                            .lore(config.getStringList("duel-choose-kit-menu.items.kit-button.lore"))
                            .build();
                }

                @Override
                public void clicked(Player player, ClickType clickType) {
                    if (party) {
                        new ChooseArenaMenu(targetUUID, kit).openMenu(player);
                    } else {
                        Player target = Bukkit.getPlayer(targetUUID);
                        if (target != null) {
                            PlayerProfile profile = PlayerProfile.get(player);
                            if (profile.getSettings().get(ProfileSettings.ARENA_SELECTION).isEnabled()) {
                                new ChooseArenaMenu(targetUUID, kit).openMenu(player);
                            } else {
                                player.closeInventory();
                                Eden.INSTANCE.getDuelRequestManager().sendDuelRequest(player, target, kit,
                                        Arena.getEnabledArena(kit));
                            }
                        }
                    }
                }
            });
            kitIndex++;
        }


        MenuUtil.addPreviousPageButton(buttons, config, "duel-choose-kit-menu", page,
            p -> new ChooseKitMenu(targetUUID, party, page - 1).openMenu(p));
        MenuUtil.addNextPageButton(buttons, config, "duel-choose-kit-menu", endIndex < allKits.size(),
            p -> new ChooseKitMenu(targetUUID, party, page + 1).openMenu(p));

        return buttons;
    }


    private List<Kit> getFilteredKits() {
        return Kit.getKits().stream()
                .filter(Kit::isEnabled)
                .filter(kit -> !party || kit.getKitMatchTypes().contains(KitMatchType.SPLIT))
                .collect(Collectors.toList());
    }
}
