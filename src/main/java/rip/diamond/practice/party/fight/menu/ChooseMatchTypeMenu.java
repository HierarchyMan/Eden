package rip.diamond.practice.party.fight.menu;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import rip.diamond.practice.Eden;
import rip.diamond.practice.kits.KitMatchType;
import rip.diamond.practice.util.BasicConfigFile;
import rip.diamond.practice.util.CC;
import rip.diamond.practice.util.ItemBuilder;
import rip.diamond.practice.util.menu.Button;
import rip.diamond.practice.util.menu.Menu;
import rip.diamond.practice.util.menu.MenuUtil;

import java.util.HashMap;
import java.util.Map;

public class ChooseMatchTypeMenu extends Menu {

    @Override
    public String getTitle(Player player) {
        BasicConfigFile config = Eden.INSTANCE.getMenusConfig().getConfig();
        return CC.translate(config.getString("party-events-menu.title"));
    }

    @Override
    public int getSize() {
        BasicConfigFile config = Eden.INSTANCE.getMenusConfig().getConfig();
        return config.getInt("party-events-menu.size");
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        final Map<Integer, Button> buttons = new HashMap<>();
        BasicConfigFile config = Eden.INSTANCE.getMenusConfig().getConfig();

        
        MenuUtil.addFillerButtons(buttons, config, "party-events-menu", getSize());
        MenuUtil.addBorderButtons(buttons, config, "party-events-menu", getSize());

        
        int splitSlot = config.getInt("party-events-menu.items.split-button.slot");
        buttons.put(splitSlot, new Button() {
            @Override
            public ItemStack getButtonItem(Player player) {
                return new ItemBuilder(
                        Material.valueOf(config.getString("party-events-menu.items.split-button.material")))
                        .durability(config.getInt("party-events-menu.items.split-button.data"))
                        .name(CC.translate(config.getString("party-events-menu.items.split-button.name")))
                        .lore(CC.translate(config.getStringList("party-events-menu.items.split-button.lore")))
                        .build();
            }

            @Override
            public void clicked(Player player, ClickType clickType) {
                new ChooseKitMenu(KitMatchType.SPLIT).openMenu(player);
            }
        });

        
        int ffaSlot = config.getInt("party-events-menu.items.ffa-button.slot");
        buttons.put(ffaSlot, new Button() {
            @Override
            public ItemStack getButtonItem(Player player) {
                return new ItemBuilder(
                        Material.valueOf(config.getString("party-events-menu.items.ffa-button.material")))
                        .durability(config.getInt("party-events-menu.items.ffa-button.data"))
                        .name(CC.translate(config.getString("party-events-menu.items.ffa-button.name")))
                        .lore(CC.translate(config.getStringList("party-events-menu.items.ffa-button.lore")))
                        .build();
            }

            @Override
            public void clicked(Player player, ClickType clickType) {
                new ChooseKitMenu(KitMatchType.FFA).openMenu(player);
            }
        });

        return buttons;
    }
}
