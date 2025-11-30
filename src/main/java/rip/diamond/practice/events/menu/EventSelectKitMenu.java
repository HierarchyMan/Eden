package rip.diamond.practice.events.menu;

import lombok.AllArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import rip.diamond.practice.kits.Kit;
import rip.diamond.practice.util.ItemBuilder;
import rip.diamond.practice.util.menu.Button;
import rip.diamond.practice.util.menu.Menu;

import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
public class EventSelectKitMenu extends Menu {

    private final EventSettingsMenu parent;

    @Override
    public String getTitle(Player player) {
        return "&bSelect a Kit";
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();

        int index = 0;
        for (Kit kit : Kit.getKits()) {
            if (kit.isEnabled() && isKitAllowedForEvent(kit)) {
                buttons.put(index++, new SelectKitButton(kit));
            }
        }

        return buttons;
    }

    private boolean isKitAllowedForEvent(Kit kit) {
        
        if (kit.getAllowedEvents() == null || kit.getAllowedEvents().isEmpty()) {
            return true;
        }

        
        
        String eventName = parent.getEventName();
        return kit.getAllowedEvents().stream()
                .anyMatch(allowed -> allowed.equalsIgnoreCase(eventName));
    }

    @AllArgsConstructor
    private class SelectKitButton extends Button {

        private final Kit kit;

        @Override
        public ItemStack getButtonItem(Player player) {
            return new ItemBuilder(kit.getDisplayIcon())
                    .name("&b&l" + kit.getName())
                    .lore("§7Click to select this kit.")
                    .build();
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
            parent.setKit(kit);
            player.closeInventory();
            parent.openMenu(player);
        }
    }
}
