package rip.diamond.practice.kiteditor;

import lombok.RequiredArgsConstructor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import rip.diamond.practice.Eden;
import rip.diamond.practice.config.Language;
import rip.diamond.practice.kiteditor.menu.KitEditorExtraItemsMenu;
import rip.diamond.practice.kiteditor.menu.KitEditorSaveMenu;
import rip.diamond.practice.kiteditor.menu.KitEditorSelectKitMenu;
import rip.diamond.practice.kits.Kit;
import rip.diamond.practice.profile.procedure.Procedure;

@RequiredArgsConstructor
public class KitEditorListener implements Listener {

    private final Eden plugin;

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerQuitEvent(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (plugin.getKitEditorManager().isEditing(player)) {
            plugin.getKitEditorManager().leaveKitEditor(player, true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();

        
        if (!plugin.getConfigFile().getString("kit-editor-mode").equalsIgnoreCase("GUI")) {
            return;
        }

        if (!plugin.getKitEditorManager().isEditing(player)) {
            return;
        }

        
        if (Procedure.getProcedures().containsKey(player.getUniqueId())) {
            return;
        }

        
        
        
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (plugin.getKitEditorManager().isEditing(player)) {
                InventoryType type = player.getOpenInventory().getType();
                if (type == InventoryType.CRAFTING || type == InventoryType.CREATIVE) {
                    
                    plugin.getKitEditorManager().leaveKitEditor(player, true);
                    plugin.getServer().getScheduler().runTaskLater(plugin,
                            () -> new KitEditorSelectKitMenu().openMenu(player), 3L);
                }
            }
        }, 2L);
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (plugin.getKitEditorManager().isEditing(player)) {
            Language.KIT_EDITOR_CANNOT_USE_COMMAND_WHILE_EDITING.sendMessage(player);
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!plugin.getKitEditorManager().isEditing(player)) {
            return;
        }

        
        event.setCancelled(true);

        Action action = event.getAction();
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        if (action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Kit kit = plugin.getKitEditorManager().getEditingProfile(player).getKit();
        switch (block.getType()) {
            case CHEST:
                if (kit.getKitExtraItems().isEmpty()) {
                    Language.KIT_EDITOR_DISALLOW_EXTRA_ITEM.sendMessage(player);
                    break;
                }
                new KitEditorExtraItemsMenu(kit).openMenu(player);
                break;
            case ANVIL:
                new KitEditorSaveMenu(kit).openMenu(player);
                break;
            case WALL_SIGN:
            case SIGN_POST:
                plugin.getKitEditorManager().leaveKitEditor(player, true);
                break;
            default:
                break;
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (!plugin.getKitEditorManager().isEditing(player)) {
            return;
        }
        if (event.getSlotType() == InventoryType.SlotType.OUTSIDE) {
            player.setItemOnCursor(null);
            return;
        }
        if (event.getSlotType() == InventoryType.SlotType.ARMOR
                || event.getSlotType() == InventoryType.SlotType.CRAFTING) {
            event.setCancelled(true);
        }
    }

    
    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (!plugin.getKitEditorManager().isEditing(player)) {
            return;
        }
        if (event.getInventorySlots().stream().anyMatch(i -> i > 36)) {
            event.setCancelled(true);
        }
    }

}
