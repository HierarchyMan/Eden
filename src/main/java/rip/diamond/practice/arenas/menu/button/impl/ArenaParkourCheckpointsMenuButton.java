package rip.diamond.practice.arenas.menu.button.impl;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import rip.diamond.practice.arenas.Arena;
import rip.diamond.practice.arenas.menu.button.ArenaButton;
import rip.diamond.practice.util.ItemBuilder;
import rip.diamond.practice.util.menu.Menu;

import java.util.Arrays;
import java.util.List;

/**
 * Lightweight menu button (non-mutating) for parkour checkpoints.
 *
 * This exists so we can have multiple entries in the arena GUI without duplicating
 * the full logic of the editing buttons.
 */
@Deprecated
public class ArenaParkourCheckpointsMenuButton extends ArenaButton {

    private final Menu backMenu;

    public ArenaParkourCheckpointsMenuButton(Arena arena, Menu backMenu) {
        super(arena);
        this.backMenu = backMenu;
    }

    @Override
    public String getName() {
        return "Parkour Checkpoints";
    }

    @Override
    public Material getIcon() {
        return Material.DEAD_BUSH;
    }

    @Override
    public String getDescription() {
        int countA = getArena().getParkourCheckpoints(true).size();
        int countB = getArena().getParkourCheckpoints(false).size();
        return "Team A: " + countA + " | Team B: " + countB;
    }

    @Override
    public String getActionDescription() {
        return "";
    }

    @Override
    public ItemStack getButtonItem(Player player) {
        List<String> lore = Arrays.asList(
                getDescription(),
                " ",
                "§7Use the two buttons to edit:",
                "§aTeam A checkpoints §7and §cTeam B checkpoints",
                " ",
                "§cCompletely optional§7 - only set if you plan",
                "§7to use this arena with a parkour kit.",
                " ",
                getActionDescription()
        );

        return new ItemBuilder(getIcon())
                .name(getName())
                .lore(lore)
                .build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarSlot) {
        // No-op for now. We keep it as informational.
        // (If you want later we can open a dedicated submenu.)
    }
}
