package rip.diamond.practice.kiteditor.menu;

import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import rip.diamond.practice.config.Language;
import rip.diamond.practice.kits.Kit;
import rip.diamond.practice.kits.KitExtraItem;
import rip.diamond.practice.util.ItemBuilder;
import rip.diamond.practice.util.menu.Button;
import rip.diamond.practice.util.menu.Menu;
import rip.diamond.practice.util.menu.pagination.PaginatedMenu;

import java.util.HashMap;
import java.util.Map;

public class KitEditorExtraItemsMenu extends PaginatedMenu {
    private final Kit kit;
    private final Menu backMenu;

    public KitEditorExtraItemsMenu(Kit kit) {
        this(kit, null);
    }

    public KitEditorExtraItemsMenu(Kit kit, Menu backMenu) {
        this.kit = kit;
        this.backMenu = backMenu;
    }

    @Override
    public String getPrePaginatedTitle(Player player) {
        return Language.KIT_EDITOR_EXTRA_ITEM_MENU_NAME.toString();
    }

    @Override
    public Map<Integer, Button> getGlobalButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();

        
        if (backMenu != null) {
            buttons.put(4, new Button() {
                @Override
                public ItemStack getButtonItem(Player player) {
                    return new ItemBuilder(Material.ARROW)
                            .name("&aGo Back")
                            .lore("&7Click to return to kit editor")
                            .build();
                }

                @Override
                public void clicked(Player player, ClickType clickType) {
                    backMenu.openMenu(player);
                }
            });
        }

        return buttons;
    }

    @Override
    public Map<Integer, Button> getAllPagesButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        for (KitExtraItem item : kit.getKitExtraItems()) {
            ItemBuilder builder = new ItemBuilder(item.getMaterial()).amount(item.getAmount())
                    .durability(item.getData()).enchantments(item.getEnchantments());
            if (item.getName() != null) {
                builder.name(item.getName());
            }
            if (item.isUnbreakable()) {
                builder.unbreakable();
            }

            buttons.put(buttons.size(), new Button() {
                @Override
                public ItemStack getButtonItem(Player player) {
                    return builder.build();
                }

                @Override
                public void clicked(Player player, ClickType clickType) {
                    
                    player.setItemOnCursor(builder.build());
                }

                @Override
                public boolean shouldCancel(Player player, ClickType clickType) {
                    
                    return true;
                }
            });
        }
        return buttons;
    }

    @Override
    public void onClose(Player player) {
        if (backMenu != null) {
            
            rip.diamond.practice.Eden.INSTANCE.getServer().getScheduler()
                    .runTaskLater(rip.diamond.practice.Eden.INSTANCE, () -> {
                        backMenu.openMenu(player);
                    }, 1L);
        }
    }
}
