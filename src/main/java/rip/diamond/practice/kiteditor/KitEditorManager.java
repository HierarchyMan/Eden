package rip.diamond.practice.kiteditor;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import rip.diamond.practice.Eden;
import rip.diamond.practice.config.Language;
import rip.diamond.practice.kits.Kit;
import rip.diamond.practice.profile.PlayerProfile;
import rip.diamond.practice.profile.PlayerState;
import rip.diamond.practice.util.Common;
import rip.diamond.practice.util.Util;
import rip.diamond.practice.util.serialization.LocationSerialization;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import rip.diamond.practice.match.team.TeamColor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
public class KitEditorManager {

    private final Eden plugin;
    private final Map<UUID, KitEditProfile> editing = new HashMap<>();
    @Setter
    private Location editorLocation = null;

    public KitEditorManager(Eden plugin) {
        this.plugin = plugin;
        try {
            this.editorLocation = LocationSerialization
                    .deserializeLocation(plugin.getLocationFile().getString("editor-location"));
        } catch (Exception e) {
            Common.log("Unable to deserialize editor-location from location file.");
        }
    }

    public void reload() {
        try {
            this.editorLocation = LocationSerialization
                    .deserializeLocation(plugin.getLocationFile().getString("editor-location"));
        } catch (Exception e) {
            Common.log("Unable to deserialize editor-location from location file.");
        }
    }

    public boolean isEditing(Player player) {
        PlayerProfile profile = PlayerProfile.get(player);

        if (profile == null) {
            return false;
        }
        return profile.getPlayerState() == PlayerState.IN_EDIT && editing.containsKey(player.getUniqueId());
    }

    public KitEditProfile getEditingProfile(Player player) {
        return editing.get(player.getUniqueId());
    }

    /**
     * Update Kit references in active KitEditProfiles after kit reload
     * This ensures players editing kits don't save to wrong kit instances
     */
    public void updateKitReferences() {
        for (KitEditProfile kProfile : editing.values()) {
            Kit oldKit = kProfile.getKit();
            Kit newKit = Kit.getKits().stream()
                    .filter(k -> k.getName().equals(oldKit.getName()))
                    .findFirst()
                    .orElse(null);

            if (newKit != null && newKit != oldKit) {
                try {
                    java.lang.reflect.Field kitField = KitEditProfile.class.getDeclaredField("kit");
                    kitField.setAccessible(true);
                    kitField.set(kProfile, newKit);
                } catch (Exception e) {
                    Common.log("&cFailed to update kit reference for edit profile");
                }
            }
        }
    }

    public void addKitEditor(Player player, Kit kit) {
        PlayerProfile profile = PlayerProfile.get(player);

        ItemStack[] contentsToUse;
        rip.diamond.practice.profile.data.ProfileKitData kitData = profile.getKitData().get(kit.getName());
        rip.diamond.practice.kits.KitLoadout savedLoadout = (kitData != null) ? kitData.getLoadout(0) : null;

        if (savedLoadout != null) {

            contentsToUse = savedLoadout.getContents();
        } else {

            contentsToUse = kit.getKitLoadout().getContents();
        }

        if (Eden.INSTANCE.getConfigFile().getString("kit-editor-mode").equalsIgnoreCase("GUI")) {
            profile.setPlayerState(PlayerState.IN_EDIT);

            KitEditProfile kProfile = new KitEditProfile(player.getUniqueId(), kit);
            editing.put(player.getUniqueId(), kProfile);

            player.getInventory().clear();
            player.getInventory().setContents(contentsToUse);
            colorize(player, kit);

            new rip.diamond.practice.kiteditor.menu.KitEditorMenu(kit, 1).openMenu(player);
            return;
        }

        if (editorLocation == null) {
            Language.KIT_EDITOR_CANNOT_FIND_EDITOR_LOCATION.sendMessage(player);
            return;
        }
        profile.setPlayerState(PlayerState.IN_EDIT);

        KitEditProfile kProfile = new KitEditProfile(player.getUniqueId(), kit);
        editing.put(player.getUniqueId(), kProfile);

        Util.teleport(player, editorLocation);
        player.getInventory().clear();
        player.getInventory().setContents(contentsToUse);
        colorize(player, kit);

        Language.KIT_EDITOR_EDITING.sendListOfMessage(player, kit.getDisplayName());
    }

    public void leaveKitEditor(Player player, boolean sendToSpawnAndReset) {
        editing.remove(player.getUniqueId());
        if (sendToSpawnAndReset) {

            if (Eden.INSTANCE.getConfigFile().getString("kit-editor-mode").equalsIgnoreCase("GUI")) {
                plugin.getLobbyManager().reset(player);
            } else {

                plugin.getLobbyManager().sendToSpawnAndReset(player);
            }
        }
    }

    private void colorize(Player player, Kit kit) {
        if (kit.getGameRules().isTeamProjectile()) {
            TeamColor teamColor = (player.getUniqueId().hashCode() % 2 == 0) ? TeamColor.RED : TeamColor.BLUE;

            ItemStack[] contents = player.getInventory().getContents();
            boolean changed = false;

            for (ItemStack item : contents) {
                if (item != null && (item.getType() == Material.WOOL ||
                        item.getType() == Material.STAINED_GLASS_PANE ||
                        item.getType() == Material.STAINED_CLAY ||
                        item.getType() == Material.STAINED_GLASS ||
                        item.getType() == Material.CARPET)) {
                    item.setDurability((short) teamColor.getDurability());
                    changed = true;
                }
            }

            if (changed) {
                player.getInventory().setContents(contents);
            }

            ItemStack[] armor = player.getInventory().getArmorContents();
            boolean armorChanged = false;
            for (ItemStack item : armor) {
                if (item != null && item.getType().name().contains("LEATHER")) {
                    LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
                    meta.setColor(Color.fromRGB(teamColor.getRgb()));
                    item.setItemMeta(meta);
                    armorChanged = true;
                }
            }

            if (armorChanged) {
                player.getInventory().setArmorContents(armor);
            }

            player.updateInventory();
        }
    }

}
