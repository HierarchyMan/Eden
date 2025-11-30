package rip.diamond.practice.arenas.menu.button.impl;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import rip.diamond.practice.config.Language;
import rip.diamond.practice.arenas.Arena;
import rip.diamond.practice.arenas.menu.button.ArenaButton;
import rip.diamond.practice.util.CC;
import rip.diamond.practice.util.Util;

public class ArenaToggleButton extends ArenaButton {
    public ArenaToggleButton(Arena arena) {
        super(arena);
    }

    @Override
    public String getName() {
        return Language.ARENA_EDIT_MENU_TOGGLE_NAME.toString();
    }

    @Override
    public Material getIcon() {
        return getArena().isEnabled() ? Material.REDSTONE_TORCH_ON : Material.LEVER;
    }

    @Override
    public String getDescription() {
        return Language.ARENA_EDIT_MENU_TOGGLE_DESCRIPTION.toString((!getArena().isFinishedSetup() ? CC.YELLOW + "需要注意"
                : getArena().isEdited() ? CC.YELLOW + "有待處理的更改"
                        : getArena().isEnabled() ? CC.GREEN + Language.ENABLED.toString()
                                : CC.RED + Language.DISABLED.toString()));
    }

    @Override
    public String getActionDescription() {
        return !getArena().isFinishedSetup()
                ? Language.ARENA_EDIT_MENU_TOGGLE_ACTION_DESCRIPTION_NOT_FINISHED_SETUP.toString()
                : getArena().isEdited() ? CC.YELLOW + "點擊以重新快取場地區塊並啟用"
                        : Language.ARENA_EDIT_MENU_TOGGLE_ACTION_DESCRIPTION.toString();
    }

    @Override
    public void clicked(Player player, ClickType clickType) {
        Util.performCommand(player, "arena setup " + getArena().getName() + " toggle");
    }
}
