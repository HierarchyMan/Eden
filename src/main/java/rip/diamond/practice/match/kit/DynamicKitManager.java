package rip.diamond.practice.match.kit;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import rip.diamond.practice.Eden;
import rip.diamond.practice.config.Language;
import rip.diamond.practice.kits.Kit;
import rip.diamond.practice.kits.KitLoadout;
import rip.diamond.practice.match.Match;
import rip.diamond.practice.profile.PlayerProfile;
import rip.diamond.practice.profile.data.ProfileKitData;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DynamicKitManager {

    @Getter
    private static final DynamicKitManager instance = new DynamicKitManager();

    private final Map<UUID, KitSession> activeSessions = new ConcurrentHashMap<>();

    public void addSession(Player player, Match match, Kit kit, KitLoadout appliedLoadout) {
        UUID uuid = player.getUniqueId();
        activeSessions.put(uuid, new KitSession(match, kit, appliedLoadout));


        Bukkit.getScheduler().runTaskLater(Eden.INSTANCE, () -> {
            activeSessions.remove(uuid);
        }, 20 * 20L);
    }

    public void handleInventoryClose(Player player) {
        KitSession session = activeSessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }


        if (validateInventory(player.getInventory(), session.kit)) {
            if (isExactMatch(player.getInventory(), session.appliedLoadout)) {

                return;
            }

            activeSessions.remove(player.getUniqueId());
            saveKit(player, session.kit);
        } else {

            activeSessions.remove(player.getUniqueId());
        }
    }

    private boolean isExactMatch(PlayerInventory inventory, KitLoadout loadout) {
        if (!isContentEqual(inventory.getArmorContents(), loadout.getArmor()))
            return false;
        return isContentEqual(inventory.getContents(), loadout.getContents());
    }

    private boolean isContentEqual(ItemStack[] a, ItemStack[] b) {
        if (a.length != b.length)
            return false;
        for (int i = 0; i < a.length; i++) {
            ItemStack i1 = a[i];
            ItemStack i2 = b[i];
            boolean empty1 = (i1 == null || i1.getType() == Material.AIR);
            boolean empty2 = (i2 == null || i2.getType() == Material.AIR);

            if (empty1 && empty2)
                continue;
            if (empty1 || empty2)
                return false;
            if (!i1.equals(i2))
                return false;
        }
        return true;
    }

    private boolean validateInventory(PlayerInventory inventory, Kit kit) {

        ItemStack[] playerArmor = inventory.getArmorContents();
        ItemStack[] kitArmor = kit.getKitLoadout().getArmor();

        if (playerArmor.length != kitArmor.length)
            return false;

        for (int i = 0; i < playerArmor.length; i++) {
            ItemStack pItem = playerArmor[i];
            ItemStack kItem = kitArmor[i];

            if (pItem == null && kItem == null)
                continue;
            if (pItem == null || kItem == null)
                return false;
            if (pItem.getType() != kItem.getType())
                return false;
        }


        Map<Material, Integer> playerCounts = countItems(inventory.getContents());
        Map<Material, Integer> kitCounts = countItems(kit.getKitLoadout().getContents());

        if (playerCounts.size() != kitCounts.size())
            return false;

        for (Map.Entry<Material, Integer> entry : kitCounts.entrySet()) {
            if (!playerCounts.containsKey(entry.getKey()))
                return false;
            if (!playerCounts.get(entry.getKey()).equals(entry.getValue()))
                return false;
        }

        return true;
    }

    private Map<Material, Integer> countItems(ItemStack[] items) {
        Map<Material, Integer> counts = new HashMap<>();
        for (ItemStack item : items) {
            if (item == null || item.getType() == Material.AIR)
                continue;
            counts.put(item.getType(), counts.getOrDefault(item.getType(), 0) + item.getAmount());
        }
        return counts;
    }

    private void saveKit(Player player, Kit kit) {
        PlayerProfile profile = PlayerProfile.get(player);
        if (profile == null)
            return;

        ProfileKitData kitData = profile.getKitData().get(kit.getName());
        if (kitData == null)
            return;


        KitLoadout loadout = kitData.getLoadout(0);
        if (loadout == null) {
            loadout = new KitLoadout("Kit 1");
            kitData.replaceKit(0, loadout);
        }


        loadout.setArmor(player.getInventory().getArmorContents());
        loadout.setContents(player.getInventory().getContents());


        profile.save(true, (success) -> {
        });

        Language.MATCH_KIT_EDIT_SAVED.sendMessage(player, loadout.getCustomName());
    }

    private static class KitSession {
        final Match match;
        final Kit kit;
        final KitLoadout appliedLoadout;

        KitSession(Match match, Kit kit, KitLoadout appliedLoadout) {
            this.match = match;
            this.kit = kit;
            this.appliedLoadout = appliedLoadout;
        }
    }
}
