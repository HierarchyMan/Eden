package rip.diamond.practice.events.menu;

import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import rip.diamond.practice.Eden;
import rip.diamond.practice.events.EdenEvent;
import rip.diamond.practice.events.EventType;
import rip.diamond.practice.events.impl.SumoEvent;
import rip.diamond.practice.events.impl.Tournament;
import rip.diamond.practice.kits.Kit;
import rip.diamond.practice.util.BasicConfigFile;
import rip.diamond.practice.util.CC;
import rip.diamond.practice.util.ItemBuilder;
import rip.diamond.practice.util.exception.PracticeUnexpectedException;
import rip.diamond.practice.util.menu.Button;
import rip.diamond.practice.util.menu.Menu;
import rip.diamond.practice.util.menu.MenuUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EventSettingsMenu extends Menu {
    private final EventType eventType;
    private int maxPlayers;
    private int minPlayers;
    private int teamSize = 1;
    @Setter
    private Kit kit = Kit.getKits().get(0);

    public EventSettingsMenu(EventType eventType) {
        this.eventType = eventType;
        this.maxPlayers = eventType.getDefaultMaxPlayers();
        this.minPlayers = eventType.getDefaultMinPlayers();
    }

    @Override
    public String getTitle(Player player) {
        BasicConfigFile config = Eden.INSTANCE.getMenusConfig().getConfig();
        return CC.translate(config.getString("event-settings-menu.title"));
    }

    @Override
    public int getSize() {
        BasicConfigFile config = Eden.INSTANCE.getMenusConfig().getConfig();
        return config.getInt("event-settings-menu.size");
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        final Map<Integer, Button> buttons = new HashMap<>();
        BasicConfigFile config = Eden.INSTANCE.getMenusConfig().getConfig();

        // Filler and Border
        MenuUtil.addFillerButtons(buttons, config, "event-settings-menu", getSize());
        MenuUtil.addBorderButtons(buttons, config, "event-settings-menu", getSize());

        // Max Players Button
        buttons.put(config.getInt("event-settings-menu.items.max-players-button.slot"), new Button() {
            @Override
            public ItemStack getButtonItem(Player player) {
                List<String> lore = config.getStringList("event-settings-menu.items.max-players-button.lore")
                        .stream()
                        .map(line -> line.replace("{max-players}", String.valueOf(maxPlayers)))
                        .collect(Collectors.toList());

                return new ItemBuilder(
                        Material.valueOf(config.getString("event-settings-menu.items.max-players-button.material")))
                        .name(config.getString("event-settings-menu.items.max-players-button.name"))
                        .lore(lore)
                        .build();
            }

            @Override
            public void clicked(Player player, ClickType clickType) {
                switch (clickType) {
                    case LEFT:
                        maxPlayers += 1;
                        break;
                    case SHIFT_LEFT:
                        maxPlayers += 10;
                        break;
                    case RIGHT:
                        maxPlayers -= 1;
                        break;
                    case SHIFT_RIGHT:
                        maxPlayers -= 10;
                        break;
                }
                if (maxPlayers < 2) {
                    maxPlayers = 2;
                }
                openMenu(player);
            }
        });

        // Party Size Button (only if event allows teams)
        if (eventType.isAllowTeams()) {
            buttons.put(config.getInt("event-settings-menu.items.party-size-button.slot"), new Button() {
                @Override
                public ItemStack getButtonItem(Player player) {
                    List<String> lore = config.getStringList("event-settings-menu.items.party-size-button.lore")
                            .stream()
                            .map(line -> line.replace("{team-size}", String.valueOf(teamSize)))
                            .collect(Collectors.toList());

                    return new ItemBuilder(
                            Material.valueOf(config.getString("event-settings-menu.items.party-size-button.material")))
                            .durability(config.getInt("event-settings-menu.items.party-size-button.data"))
                            .name(config.getString("event-settings-menu.items.party-size-button.name"))
                            .lore(lore)
                            .build();
                }

                @Override
                public void clicked(Player player, ClickType clickType) {
                    switch (clickType) {
                        case LEFT:
                            teamSize += 1;
                            break;
                        case RIGHT:
                            teamSize -= 1;
                            break;
                    }
                    if (teamSize < 1) {
                        teamSize = 1;
                    }
                    openMenu(player);
                }
            });
        }

        // Kit Button (only if event requires a kit)
        if (eventType.isKit()) {
            buttons.put(config.getInt("event-settings-menu.items.kit-button.slot"), new Button() {
                @Override
                public ItemStack getButtonItem(Player player) {
                    List<String> lore = config.getStringList("event-settings-menu.items.kit-button.lore")
                            .stream()
                            .map(line -> line.replace("{kit-name}", kit.getDisplayName()))
                            .collect(Collectors.toList());

                    return new ItemBuilder(
                            Material.valueOf(config.getString("event-settings-menu.items.kit-button.material")))
                            .name(config.getString("event-settings-menu.items.kit-button.name"))
                            .lore(lore)
                            .build();
                }

                @Override
                public void clicked(Player player, ClickType clickType) {
                    new EventSelectKitMenu(EventSettingsMenu.this).openMenu(player);
                }
            });
        }

        // Start Button
        buttons.put(config.getInt("event-settings-menu.items.start-button.slot"), new Button() {
            @Override
            public ItemStack getButtonItem(Player player) {
                List<String> lore = config.getStringList("event-settings-menu.items.start-button.lore")
                        .stream()
                        .map(line -> line.replace("{event-name}", eventType.getName()))
                        .map(line -> line.replace("{min-players}", String.valueOf(minPlayers)))
                        .map(line -> line.replace("{max-players}", String.valueOf(maxPlayers)))
                        .collect(Collectors.toList());

                return new ItemBuilder(
                        Material.valueOf(config.getString("event-settings-menu.items.start-button.material")))
                        .durability(config.getInt("event-settings-menu.items.start-button.data"))
                        .name(config.getString("event-settings-menu.items.start-button.name"))
                        .lore(lore)
                        .build();
            }

            @Override
            public void clicked(Player player, ClickType clickType) {
                player.closeInventory();

                EdenEvent event = EdenEvent.getOnGoingEvent();
                if (event != null) {
                    player.sendMessage(CC.translate("&cThere's already an active event!"));
                    return;
                }

                switch (eventType) {
                    case TOURNAMENT:
                        Tournament tournament = new Tournament(player.getName(), minPlayers, maxPlayers, kit, teamSize);
                        tournament.create();
                        return;
                    case SUMO_EVENT:
                        SumoEvent sumoEvent = new SumoEvent(player.getName(), minPlayers, maxPlayers, teamSize);
                        sumoEvent.create();
                        return;
                    default:
                        throw new PracticeUnexpectedException(
                                "Event type " + eventType.getName() + " is not initialized yet");
                }
            }
        });

        return buttons;
    }
}
