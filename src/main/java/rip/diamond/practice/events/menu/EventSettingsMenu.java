package rip.diamond.practice.events.menu;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import rip.diamond.practice.Eden;
import rip.diamond.practice.events.PracticeEvent;
import rip.diamond.practice.kits.Kit;
import rip.diamond.practice.util.CC;
import rip.diamond.practice.util.ItemBuilder;
import rip.diamond.practice.util.menu.Button;
import rip.diamond.practice.util.menu.Menu;

import java.util.HashMap;
import java.util.Map;

public class EventSettingsMenu extends Menu {

    private final Eden plugin = Eden.INSTANCE;
    @Getter
    private final String eventName;
    @Getter
    @Setter
    private Kit kit;
    @Getter
    @Setter
    private int maxPlayers = 30;
    @Getter
    @Setter
    private int teamSize = 1;

    public EventSettingsMenu(String eventName) {
        this.eventName = eventName;
        
        
    }

    @Override
    public String getTitle(Player player) {
        return "&bHost " + eventName;
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();

        
        
        if (needsKit(eventName)) {
            buttons.put(11, new EventKitButton());
        }

        
        if (isTournamentEvent(eventName)) {
            buttons.put(12, new rip.diamond.practice.events.menu.button.EventTeamSizeButton(this));
        }

        
        buttons.put(13, new EventMaxPlayersButton());

        
        buttons.put(15, new EventStartButton());

        return buttons;
    }

    private boolean needsKit(String eventName) {
        return eventName.equalsIgnoreCase("Brackets") ||
                eventName.equalsIgnoreCase("LMS") ||
                eventName.equalsIgnoreCase("SkyWars") ||
                eventName.equalsIgnoreCase("Knockout");
    }

    private boolean isTournamentEvent(String eventName) {
        return eventName.equalsIgnoreCase("Brackets");
    }

    private class EventKitButton extends Button {
        @Override
        public ItemStack getButtonItem(Player player) {
            return new ItemBuilder(Material.DIAMOND_SWORD)
                    .name("&b&lSelect Kit")
                    .lore("&7Current: &f" + (kit != null ? kit.getName() : "None"), "&7Click to change kit.")
                    .build();
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
            new EventSelectKitMenu(EventSettingsMenu.this).openMenu(player);
            
        }
    }

    private class EventMaxPlayersButton extends Button {
        @Override
        public ItemStack getButtonItem(Player player) {
            return new ItemBuilder(Material.PAPER)
                    .name("&b&lMax Players")
                    .lore("&7Current: &f" + maxPlayers, "&7Left-Click to increase.", "&7Right-Click to decrease.")
                    .build();
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
            if (clickType.isLeftClick()) {
                maxPlayers += 5;
            } else if (clickType.isRightClick()) {
                maxPlayers -= 5;
            }
            if (maxPlayers < 2)
                maxPlayers = 2;
            if (maxPlayers > 100)
                maxPlayers = 100;

            
        }

        @Override
        public boolean shouldUpdate(Player player, ClickType clickType) {
            return true;
        }
    }

    private class EventStartButton extends Button {
        @Override
        public ItemStack getButtonItem(Player player) {
            return new ItemBuilder(Material.EMERALD_BLOCK)
                    .name("&a&lStart Event")
                    .lore("&7Click to start the event.")
                    .build();
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
            player.closeInventory();
            PracticeEvent<?> event = plugin.getEventManager().getByName(eventName);
            if (event == null) {
                player.sendMessage(CC.RED + "Event not found.");
                return;
            }

            if (event.getState() != rip.diamond.practice.events.EventState.UNANNOUNCED) {
                player.sendMessage(CC.RED + "Event is already running or waiting.");
                return;
            }

            if (needsKit(eventName) && kit == null) {
                player.sendMessage(CC.RED + "You must select a kit.");
                return;
            }

            if (needsKit(eventName)) {
                plugin.getEventManager().hostEvent(event, kit, maxPlayers, player);
            } else {
                plugin.getEventManager().hostEvent(event, player);
                event.setLimit(maxPlayers); 
            }

            player.sendMessage(CC.GREEN + "Started " + eventName + " event!");
        }
    }
}
