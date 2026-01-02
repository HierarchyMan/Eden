package rip.diamond.practice.managers;

import org.bukkit.inventory.ItemStack;
import rip.diamond.practice.Eden;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CustomItemManager {

    private final Map<String, ItemStack> customItems = new ConcurrentHashMap<>();

    public CustomItemManager() {
        loadItems();
    }

    public void loadItems() {
        Eden.INSTANCE.getDatabaseManager().getHandler().loadAllCustomItems(items -> {
            customItems.clear();
            customItems.putAll(items);
            Eden.INSTANCE.getLogger().info("Loaded " + items.size() + " custom items.");
        });
    }

    public ItemStack getItem(String key) {
        if (customItems.containsKey(key)) {
            return customItems.get(key);
        }
        DefaultItem defaultItem = DefaultItem.getByName(key);
        if (defaultItem != null) {
            return defaultItem.toItemStack();
        }
        return null;
    }

    public void setItem(String key, ItemStack item) {
        if (item == null) {
            resetItem(key);
            return;
        }
        customItems.put(key, item);
        Eden.INSTANCE.getDatabaseManager().getHandler().saveCustomItem(key, item);
    }

    public void resetItem(String key) {
        customItems.remove(key);
        Eden.INSTANCE.getDatabaseManager().getHandler().deleteCustomItem(key);
    }
}
