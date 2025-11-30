package rip.diamond.practice.events.menu;

import lombok.AllArgsConstructor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import rip.diamond.practice.Eden;
import rip.diamond.practice.events.PracticeEvent;
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

public class EventHostMenu extends Menu {

    @Override
    public String getTitle(Player player) {
        BasicConfigFile config = Eden.INSTANCE.getMenusConfig().getConfig();
        return CC.translate(config.getString("event-host-menu.title"));
    }

    @Override
    public int getSize() {
        BasicConfigFile config = Eden.INSTANCE.getMenusConfig().getConfig();
        return config.getInt("event-host-menu.size");
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        BasicConfigFile config = Eden.INSTANCE.getMenusConfig().getConfig();

        
        MenuUtil.addFillerButtons(buttons, config, "event-host-menu", getSize());
        MenuUtil.addBorderButtons(buttons, config, "event-host-menu", getSize());

        
        String[] eventKeys = { "sumo", "oitc", "tnttag", "brackets", "lms", "knockout", "skywars",
                "parkour", "gulag", "4corners", "thimble", "dropper", "stoplight", "spleef" };

        for (String eventKey : eventKeys) {
            String path = "event-host-menu.events." + eventKey;

            
            if (!config.getBoolean(path + ".enabled")) {
                continue;
            }

            int slot = config.getInt(path + ".slot");
            String materialName = config.getString(path + ".material");
            String name = config.getString(path + ".name");
            List<String> lore = config.getStringList(path + ".lore");

            Material material;
            try {
                material = Material.valueOf(materialName);
            } catch (IllegalArgumentException e) {
                Eden.INSTANCE.getLogger()
                        .warning("[EventHostMenu] Invalid material '" + materialName + "' for event: " + eventKey);
                continue;
            }

            
            PracticeEvent<?> event = Eden.INSTANCE.getEventManager().getByName(eventKey);
            boolean isValid = false;
            if (event != null) {
                isValid = Eden.INSTANCE.getEventManager().validateEvent(event, null, false);
            }

            buttons.put(slot, new SelectEventButton(name, material, eventKey, lore, isValid));
        }

        return buttons;
    }

    @AllArgsConstructor
    private class SelectEventButton extends Button {

        private final String name;
        private final Material material;
        private final String eventName;
        private final List<String> lore;
        private final boolean isValid;

        @Override
        public ItemStack getButtonItem(Player player) {
            if (!isValid) {
                List<String> errorLore = new ArrayList<>(lore);
                errorLore.add(" ");
                errorLore.add("&c&lSETUP INCOMPLETE");
                errorLore.add("&cClick to view missing locations.");

                return new ItemBuilder(Material.STAINED_GLASS_PANE).durability(14) 
                        .name(CC.translate(name) + " &c(Invalid)")
                        .lore(errorLore)
                        .build();
            }

            return new ItemBuilder(material)
                    .name(CC.translate(name))
                    .lore(lore)
                    .build();
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
            player.closeInventory();

            PracticeEvent<?> event = Eden.INSTANCE.getEventManager().getByName(eventName);
            if (event == null) {
                player.sendMessage(CC.RED + "Event not found.");
                return;
            }

            if (!isValid) {
                
                Eden.INSTANCE.getEventManager().validateEvent(event, player, true);
                return;
            }

            new EventSettingsMenu(eventName).openMenu(player);
        }
    }
}
