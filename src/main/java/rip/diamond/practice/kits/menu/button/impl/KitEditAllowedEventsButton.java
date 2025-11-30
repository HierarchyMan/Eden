package rip.diamond.practice.kits.menu.button.impl;

import lombok.AllArgsConstructor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import rip.diamond.practice.kits.Kit;
import rip.diamond.practice.kits.menu.KitAllowedEventsMenu;
import rip.diamond.practice.util.ItemBuilder;
import rip.diamond.practice.util.menu.Button;
import rip.diamond.practice.util.menu.Menu;

@AllArgsConstructor
public class KitEditAllowedEventsButton extends Button {

    private final Kit kit;
    private final Menu parent;

    @Override
    public ItemStack getButtonItem(Player player) {
        return new ItemBuilder(Material.REDSTONE_COMPARATOR)
                .name("&b&lAllowed Events")
                .lore(
                        "&7Click to configure which events",
                        "&7this kit can be used in.")
                .build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
        new KitAllowedEventsMenu(kit, parent).openMenu(player);
    }
}
