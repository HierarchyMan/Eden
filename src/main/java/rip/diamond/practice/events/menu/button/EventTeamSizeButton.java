package rip.diamond.practice.events.menu.button;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import rip.diamond.practice.Eden;
import rip.diamond.practice.events.menu.EventSettingsMenu;
import rip.diamond.practice.util.BasicConfigFile;
import rip.diamond.practice.util.CC;
import rip.diamond.practice.util.ItemBuilder;
import rip.diamond.practice.util.menu.Button;

import java.util.List;
import java.util.stream.Collectors;

public class EventTeamSizeButton extends Button {

    private final EventSettingsMenu parent;

    public EventTeamSizeButton(EventSettingsMenu parent) {
        this.parent = parent;
    }

    @Override
    public ItemStack getButtonItem(Player player) {
        BasicConfigFile config = Eden.INSTANCE.getMenusConfig().getConfig();
        String path = "event-settings-menu.items.party-size-button";

        Material material = Material.valueOf(config.getString(path + ".material"));
        int data = config.getInt(path + ".data");
        String name = config.getString(path + ".name");
        List<String> lore = config.getStringList(path + ".lore");

        
        name = name.replace("{team-size}", String.valueOf(parent.getTeamSize()));
        lore = lore.stream()
                .map((String line) -> line.replace("{team-size}", String.valueOf(parent.getTeamSize())))
                .collect(Collectors.toList());

        return new ItemBuilder(material)
                .durability(data)
                .name(CC.translate(name))
                .lore(lore)
                .build();
    }

    @Override
    public void clicked(Player player, ClickType clickType) {
        if (clickType.isLeftClick()) {
            
            if (parent.getTeamSize() < 5) {
                parent.setTeamSize(parent.getTeamSize() + 1);
            }
        } else if (clickType.isRightClick()) {
            
            if (parent.getTeamSize() > 1) {
                parent.setTeamSize(parent.getTeamSize() - 1);
            }
        }
    }

    @Override
    public boolean shouldUpdate(Player player, ClickType clickType) {
        return true;
    }
}
