package rip.diamond.practice.managers.chest;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.inventory.ItemStack;
import rip.diamond.practice.Eden;
import rip.diamond.practice.util.serialization.ItemSerialization;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Getter
@Setter
public class ChestManager {

    private final Eden plugin;
    private List<Chest> chests = new ArrayList<>();
    private final Random random = new Random();

    public ChestManager(Eden plugin) {
        this.plugin = plugin;

        if (plugin.getChestFile().getConfiguration().getConfigurationSection("CHESTS") == null) {
            return;
        }

        plugin.getChestFile().getConfiguration().getConfigurationSection("CHESTS").getKeys(false)
                .forEach(key -> {
                    if (plugin.getChestFile().getConfiguration().getConfigurationSection("CHESTS") != null) {
                        if (!plugin.getChestFile().getConfiguration().getConfigurationSection("CHESTS")
                                .contains(String.valueOf(key))) {
                            chests.add(new Chest(null, Integer.parseInt(key)));
                        }
                    } else {
                        chests.add(new Chest(null, Integer.parseInt(key)));
                    }
                });

        loadChestsFromConfig();
        sortChestsByNumber();
    }

    public Chest getChest(int chestNumber) {
        return chests.get(chestNumber - 1);
    }

    public void updateChestItems(int chestNumber, ItemStack[] items) {
        Chest chest = chests.get(chestNumber - 1);
        chest.setItems(items);
    }

    public void saveChestsToConfig() {
        plugin.getChestFile().getConfiguration().createSection("CHESTS");
        for (Chest chest : chests) {
            if (chest.getItems() != null) {
                plugin.getChestFile().getConfiguration()
                        .set("CHESTS." + chest.getNumber(), ItemSerialization.serializeInventory(chest.getItems()));
            }
        }

        plugin.getChestFile().save();
    }

    public void loadChestsFromConfig() {
        if (plugin.getChestFile().getConfiguration().getConfigurationSection("CHESTS") == null) {
            return;
        }

        plugin.getChestFile().getConfiguration().getConfigurationSection("CHESTS").getKeys(false)
                .forEach(key -> {
                    if (isInteger(key)) {
                        ItemStack[] items = ItemSerialization.deserializeInventory(
                                plugin.getChestFile().getConfiguration().getString("CHESTS." + key));
                        chests.add(new Chest(items, Integer.parseInt(key)));
                    }
                });
    }

    public ItemStack[] getRandomItemsFromChests() {
        if (plugin.getChestFile().getConfiguration().getConfigurationSection("CHESTS") == null) {
            return null;
        }

        List<Chest> availableChests = new ArrayList<>();
        plugin.getChestFile().getConfiguration().getConfigurationSection("CHESTS").getKeys(false)
                .forEach(key -> {
                    if (isInteger(key)) {
                        ItemStack[] items = ItemSerialization.deserializeInventory(
                                plugin.getChestFile().getConfiguration().getString("CHESTS." + key));
                        if (items.length >= 1) {
                            availableChests.add(new Chest(items, Integer.parseInt(key)));
                        }
                    }
                });

        if (availableChests.isEmpty()) {
            return null;
        }

        int randomNum = random.nextInt(9);
        if (this.chests.size() <= randomNum) {
            return null;
        }

        return availableChests.get(randomNum).getItems();
    }

    public void sortChestsByNumber() {
        List<Chest> fixed = chests.stream().sorted(Comparator.comparing(Chest::getNumber))
                .collect(Collectors.toList());

        chests.clear();
        chests.addAll(fixed);
    }

    public void reload() {
        this.chests.clear();
        loadChestsFromConfig();
        sortChestsByNumber();
    }

    public void populateChests(rip.diamond.practice.util.cuboid.Cuboid cuboid) {
        if (cuboid == null)
            return;

        for (org.bukkit.block.Block block : cuboid) {
            if (block.getType() == org.bukkit.Material.CHEST || block.getType() == org.bukkit.Material.TRAPPED_CHEST) {
                org.bukkit.block.Chest chest = (org.bukkit.block.Chest) block.getState();
                ItemStack[] items = getRandomItemsFromChests();
                if (items != null) {
                    chest.getInventory().setContents(items);
                    chest.update();
                }
            }
        }
    }

    private boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
