package rip.diamond.practice.match.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import rip.diamond.practice.match.kit.DynamicKitManager;

public class DynamicKitEditListener implements Listener {

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            DynamicKitManager.getInstance().handleInventoryClose((Player) event.getPlayer());
        }
    }
}
