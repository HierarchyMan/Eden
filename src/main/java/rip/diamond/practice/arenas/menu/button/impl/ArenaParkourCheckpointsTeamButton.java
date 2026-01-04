package rip.diamond.practice.arenas.menu.button.impl;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import rip.diamond.practice.arenas.Arena;
import rip.diamond.practice.arenas.menu.button.ArenaButton;
import rip.diamond.practice.util.menu.Menu;

import java.util.ArrayList;
import java.util.List;

/**
 * Editor button for parkour checkpoints for a single team side (A or B).
 */
public class ArenaParkourCheckpointsTeamButton extends ArenaButton {

    private final Menu backMenu;
    private final boolean teamA;

    public ArenaParkourCheckpointsTeamButton(Arena arena, Menu backMenu, boolean teamA) {
        super(arena);
        this.backMenu = backMenu;
        this.teamA = teamA;
    }

    @Override
    public String getName() {
        return teamA ? "Parkour Checkpoints (Team A)" : "Parkour Checkpoints (Team B)";
    }

    @Override
    public Material getIcon() {
        return Material.DEAD_BUSH;
    }

    @Override
    public String getDescription() {
        int count = getArena().getParkourCheckpoints(teamA).size();
        return "Current: " + count + " checkpoint" + (count == 1 ? "" : "s");
    }

    @Override
    public String getActionDescription() {
        return "Left: add | Right: remove nearest | Shift: clear";
    }

    @Override
    public List<String> getActionDescriptions() {
        List<String> lore = new ArrayList<>();

        // Compressed guidance (replaces the removed info-only button)
        int countA = getArena().getParkourCheckpoints(true).size();
        int countB = getArena().getParkourCheckpoints(false).size();
        lore.add("");
        lore.add("§cCompletely optional§7 - only set if you plan");
        lore.add("§7to use this arena with a parkour kit.");
        lore.add("");
        lore.add("§7Total set: §fA=" + countA + "§7 | §fB=" + countB);
        lore.add("");
        lore.add("§7Checkpoint 1 comes after arena spawn.");
        lore.add("§7Last checkpoint acts as the finish.");
        lore.add("");

        // List checkpoints for this team (kept short)
        List<org.bukkit.Location> cps = getArena().getParkourCheckpoints(teamA);
        int maxLines = 6;
        for (int i = 0; i < cps.size() && i < maxLines; i++) {
            org.bukkit.Location l = cps.get(i);
            lore.add("§f#" + (i + 1) + " §7-> §f" + l.getWorld().getName() + " "
                    + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ());
        }
        if (cps.size() > maxLines) {
            lore.add("§7...and §f" + (cps.size() - maxLines) + "§7 more");
        }

        return lore;
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarSlot) {
        Arena arena = getArena();

        if (clickType.isShiftClick()) {
            arena.clearParkourCheckpoints(teamA);
            arena.autoSave();
            return;
        }

        if (clickType == ClickType.RIGHT) {
            arena.removeNearestParkourCheckpoint(teamA, player.getLocation(), 2.0);
            arena.autoSave();
            return;
        }

        if (clickType == ClickType.LEFT) {
            arena.addParkourCheckpoint(teamA, player.getLocation());
            arena.autoSave();
        }
    }

    @Override
    public boolean shouldUpdate(Player player, ClickType clickType) {
        return true;
    }
}
