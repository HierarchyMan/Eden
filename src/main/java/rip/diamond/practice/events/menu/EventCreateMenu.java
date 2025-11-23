package rip.diamond.practice.events.menu;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import rip.diamond.practice.Eden;
import rip.diamond.practice.events.EventType;
import rip.diamond.practice.util.BasicConfigFile;
import rip.diamond.practice.util.CC;
import rip.diamond.practice.util.ItemBuilder;
import rip.diamond.practice.util.menu.Button;
import rip.diamond.practice.util.menu.Menu;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventCreateMenu extends Menu {
    @Override
    public String getTitle(Player player) {
        BasicConfigFile config = Eden.INSTANCE.getMenusConfig().getConfig();
        return CC.translate(config.getString("event-create-menu.title"));
    }

    @Override
    public int getSize() {
        BasicConfigFile config = Eden.INSTANCE.getMenusConfig().getConfig();
        return config.getInt("event-create-menu.size");
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        final Map<Integer, Button> buttons = new HashMap<>();
        BasicConfigFile config = Eden.INSTANCE.getMenusConfig().getConfig();

        // Filler
        if (config.getBoolean("event-create-menu.filler.enabled")) {
            ItemStack filler = new ItemBuilder(Material.valueOf(config.getString("event-create-menu.filler.material")))
                    .durability(config.getInt("event-create-menu.filler.data"))
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
        if (config.getBoolean("event-create-menu.border.enabled")) {
            ItemStack border = new ItemBuilder(Material.valueOf(config.getString("event-create-menu.border.material")))
                    .durability(config.getInt("event-create-menu.border.data"))
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

        // Event type buttons
        int slotIndex = 0;
        for (EventType eventType : EventType.values()) {
            // Find next available slot (skip border)
            int slot = -1;
            for (int i = 0; i < getSize(); i++) {
                if (!buttons.containsKey(i) || !(i < 9 || i >= getSize() - 9 || i % 9 == 0 || i % 9 == 8)) {
                    if (slotIndex == 0 || !buttons.containsKey(i)) {
                        slot = i;
                        slotIndex++;
                        break;
                    }
                }
            }

            if (slot == -1)
                continue; // No more space

            final int finalSlot = slot;
            buttons.put(finalSlot, new Button() {
                @Override
                public ItemStack getButtonItem(Player player) {
                    String name = config.getString("event-create-menu.items.event-button.name")
                            .replace("{event-name}", eventType.getName());

                    List<String> lore;
                    if (player.hasPermission(eventType.getPermission())) {
                        lore = config.getStringList("event-create-menu.items.event-button.lore-has-permission");
                    } else {
                        lore = config.getStringList("event-create-menu.items.event-button.lore-no-permission");
                    }

                    return new ItemBuilder(eventType.getLogo())
                            .name(name)
                            .lore(lore)
                            .build();
                }

                @Override
                public void clicked(Player player, ClickType clickType) {
                    if (!player.hasPermission(eventType.getPermission())) {
                        List<String> noPermLore = config
                                .getStringList("event-create-menu.items.event-button.lore-no-permission");
                        if (!noPermLore.isEmpty()) {
                            player.sendMessage(CC.translate(noPermLore.get(noPermLore.size() - 1)));
                        }
                        return;
                    }
                    new EventSettingsMenu(eventType).openMenu(player);
                }
            });
        }

        return buttons;
    }
}
