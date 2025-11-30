package rip.diamond.practice.events.menu;

import lombok.AllArgsConstructor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import rip.diamond.practice.Eden;
import rip.diamond.practice.events.EventState;
import rip.diamond.practice.events.PracticeEvent;
import rip.diamond.practice.util.CC;
import rip.diamond.practice.util.ItemBuilder;
import rip.diamond.practice.util.menu.Button;
import rip.diamond.practice.util.menu.Menu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventManagerMenu extends Menu {

    private final Eden plugin = Eden.INSTANCE;

    @Override
    public String getTitle(Player player) {
        return CC.translate("&bEvent Manager");
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();

        PracticeEvent<?> event = plugin.getEventManager().getOngoingEvent();

        if (event == null) {
            buttons.put(4, new EventManageButton(
                    CC.translate("&c&lNo Active Event"),
                    Material.BARRIER,
                    Arrays.asList(
                            CC.MENU_BAR,
                            "&7There is no event currently active.",
                            CC.MENU_BAR)));
            return buttons;
        }

        buttons.put(2, new EventManageButton(
                CC.translate("&a&lForce-Start Event &7▸ &f" + event.getName()),
                Material.EMERALD,
                Arrays.asList(
                        CC.MENU_BAR,
                        "&7Clicking here will reduce",
                        "&7the event countdown down to 5",
                        " ",
                        "&eClick here to force-start the &a" + event.getName() + "&e event.",
                        CC.MENU_BAR)));

        buttons.put(4, new EventManageButton(
                CC.translate("&e&lEvent Status &7▸ &f" + event.getName()),
                Material.GLOWSTONE_DUST,
                Arrays.asList(
                        CC.MENU_BAR,
                        " &9&l▸ &fHost: &b" + (event.getHost() == null ? "&cNone" : event.getHost().getName()),
                        " &9&l▸ &fEvent: &b" + event.getName(),
                        " &9&l▸ &fPlayers: &b" + event.getPlayers().size() + "&7 out of &b" + event.getLimit(),
                        " &9&l▸ &fState: &b" + capitalizeFirst(event.getState().name()),
                        CC.MENU_BAR)));

        buttons.put(6, new EventManageButton(
                CC.translate("&c&lStop Event &7▸ &f" + event.getName()),
                Material.REDSTONE,
                Arrays.asList(
                        CC.MENU_BAR,
                        "&7Clicking here will end",
                        "&7the event that is ongoing",
                        "",
                        "&eClick here to stop the &a" + event.getName() + "&e event.",
                        CC.MENU_BAR)));

        return buttons;
    }

    @Override
    public int getSize() {
        return 9;
    }

    private String capitalizeFirst(String text) {
        if (text == null || text.isEmpty())
            return text;
        return text.charAt(0) + text.substring(1).toLowerCase();
    }

    @AllArgsConstructor
    public static class EventManageButton extends Button {

        private final Eden plugin = Eden.INSTANCE;
        private final String name;
        private final Material material;
        private final List<String> lore;

        @Override
        public ItemStack getButtonItem(Player player) {
            List<String> lines = new ArrayList<>(lore);
            return new ItemBuilder(material)
                    .name(name)
                    .lore(lines)
                    .build();
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
            PracticeEvent<?> event = plugin.getEventManager().getOngoingEvent();

            if (event == null) {
                player.sendMessage(CC.RED + "The event is not active!");
                player.closeInventory();
                return;
            }

            if (slot == 2) {
                
                if (event.getState() != EventState.UNANNOUNCED) {
                    Button.playSuccess(player);
                    event.getCountdownTask().setTimeUntilStart(5);
                    player.sendMessage(CC.GREEN + "Successfully force-started the " + event.getName() + " event!");
                } else {
                    player.sendMessage(CC.RED + "The event is not active!");
                }
                player.closeInventory();
            } else if (slot == 6) {
                
                if (event.getState() != EventState.UNANNOUNCED) {
                    event.end();
                    Button.playFail(player);
                    player.sendMessage(CC.GREEN + event.getName() + " event successfully stopped.");
                } else {
                    player.sendMessage(CC.RED + "The event is not active!");
                }
                player.closeInventory();
            }
        }
    }
}
