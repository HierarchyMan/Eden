package rip.diamond.practice.kits.menu;

import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import rip.diamond.practice.kits.Kit;
import rip.diamond.practice.util.CC;
import rip.diamond.practice.util.ItemBuilder;
import rip.diamond.practice.util.menu.Button;
import rip.diamond.practice.util.menu.Menu;
import rip.diamond.practice.util.menu.button.BackButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class KitAllowedEventsMenu extends Menu {

    private final Kit kit;
    private final Menu backMenu;

    
    private static final List<String> KIT_EVENTS = Arrays.asList(
            "Brackets",
            "LMS",
            "SkyWars",
            "Knockout",
            "Gulag",
            "OITC");

    @Override
    public String getTitle(Player player) {
        return "&bAllowed Events: " + kit.getName();
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();

        
        buttons.put(0, new BackButton(Material.STAINED_GLASS_PANE, 14, backMenu));

        
        int index = 10;
        for (String eventName : KIT_EVENTS) {
            buttons.put(index++, new EventToggleButton(eventName));
        }

        return buttons;
    }

    @RequiredArgsConstructor
    private class EventToggleButton extends Button {
        private final String eventName;

        @Override
        public ItemStack getButtonItem(Player player) {
            boolean allowed = isEventAllowed();
            
            Material material = Material.WOOL;
            short data = (short) (allowed ? 5 : 14); 
            String status = allowed ? "&a&lALLOWED" : "&c&lNOT ALLOWED";

            return new ItemBuilder(material)
                    .durability(data)
                    .name("&b&l" + eventName)
                    .lore(
                            "&7Status: " + status,
                            "",
                            "&7Click to " + (allowed ? "&cdisallow" : "&aallow"))
                    .build();
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
            if (kit.getAllowedEvents() == null) {
                kit.setAllowedEvents(new ArrayList<>());
            }

            if (isEventAllowed()) {
                
                kit.getAllowedEvents().removeIf(e -> e.equalsIgnoreCase(eventName));
                player.sendMessage(CC.translate("&cRemoved &e" + eventName + " &cfrom allowed events for &e" + kit.getName()));
            } else {
                
                kit.getAllowedEvents().add(eventName);
                player.sendMessage(CC.translate("&aAdded &e" + eventName + " &ato allowed events for &e" + kit.getName()));
            }

            
            kit.autoSave();

            
            KitAllowedEventsMenu.this.openMenu(player);
        }

        private boolean isEventAllowed() {
            
            
            if (kit.getAllowedEvents() == null || kit.getAllowedEvents().isEmpty()) {
                
                return true;
            }
            
            return kit.getAllowedEvents().stream()
                    .anyMatch(e -> e.equalsIgnoreCase(eventName));
        }
    }
}
