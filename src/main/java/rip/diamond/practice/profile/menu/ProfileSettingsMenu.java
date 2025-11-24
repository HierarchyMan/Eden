package rip.diamond.practice.profile.menu;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import rip.diamond.practice.Eden;
import rip.diamond.practice.profile.PlayerProfile;
import rip.diamond.practice.profile.ProfileSettings;
import rip.diamond.practice.util.BasicConfigFile;
import rip.diamond.practice.util.CC;
import rip.diamond.practice.util.ItemBuilder;
import rip.diamond.practice.util.menu.Button;
import rip.diamond.practice.util.menu.Menu;
import rip.diamond.practice.util.menu.MenuUtil;
import rip.diamond.practice.util.option.Option;
import rip.diamond.practice.util.option.TrueOption;
import rip.diamond.practice.util.option.FalseOption;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileSettingsMenu extends Menu {
    @Override
    public String getTitle(Player player) {
        BasicConfigFile config = Eden.INSTANCE.getMenusConfig().getConfig();
        return CC.translate(config.getString("profile-settings-menu.title"));
    }

    @Override
    public int getSize() {
        BasicConfigFile config = Eden.INSTANCE.getMenusConfig().getConfig();
        String sizeStr = config.getString("profile-settings-menu.size");

        if ("dynamic".equalsIgnoreCase(sizeStr)) {
            // Calculate dynamic size based on number of settings
            int settingsCount = ProfileSettings.values().length;
            boolean hasBorder = config.getBoolean("profile-settings-menu.border.enabled");

            // Calculate needed rows
            int slotsPerRow = hasBorder ? 7 : 9;
            int rowsNeeded = (int) Math.ceil((double) settingsCount / slotsPerRow);
            int totalRows = rowsNeeded + (hasBorder ? 2 : 0);

            // Ensure minimum of 3 rows
            int maxSize = config.getInt("profile-settings-menu.max-size");
            int calculatedSize = Math.max(27, Math.min(totalRows * 9, maxSize));

            // Round to valid inventory size
            return ((calculatedSize + 8) / 9) * 9;
        } else {
            return config.getInt("profile-settings-menu.size");
        }
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        final Map<Integer, Button> buttons = new HashMap<>();
        BasicConfigFile config = Eden.INSTANCE.getMenusConfig().getConfig();

        // Filler and Border
        MenuUtil.addFillerButtons(buttons, config, "profile-settings-menu", getSize());
        MenuUtil.addBorderButtons(buttons, config, "profile-settings-menu", getSize());

        // Settings buttons
        for (ProfileSettings settings : ProfileSettings.values()) {
            String configKey = settings.name().toLowerCase().replace('_', '-');
            String path = "profile-settings-menu.items." + configKey;

            if (config.getConfiguration().contains(path)) {
                int slot = config.getInt(path + ".slot");
                buttons.put(slot, new Button() {
                    @Override
                    public ItemStack getButtonItem(Player player) {
                        PlayerProfile profile = PlayerProfile.get(player);
                        Option currentOption = profile.getSettings().get(settings);
                        Option defaultOption = settings.getDefault();

                        String optionValue = currentOption.getValue().toLowerCase();
                        if (settings == ProfileSettings.PING_RANGE
                                && optionValue.equals(String.valueOf(Integer.MAX_VALUE))) {
                            optionValue = "unlimited";
                        }
                        String specificPath = path + "." + optionValue;
                        String statePath;

                        // Check if material exists at base path (for settings like ping-range)
                        if (config.getConfiguration().contains(path + ".material")) {
                            statePath = path;
                        }
                        // Check if specific configuration exists for this option value
                        else if (config.getConfiguration().contains(specificPath + ".material")) {
                            statePath = specificPath;
                        } else {
                            // Fallback to enabled/disabled logic
                            boolean isEnabledState;
                            if (settings.getOptions().size() == 2 &&
                                    (settings.getOptions().get(0) instanceof TrueOption ||
                                            settings.getOptions().get(0) instanceof FalseOption)) {
                                // Boolean setting
                                isEnabledState = Boolean.parseBoolean(currentOption.getValue());
                            } else {
                                // Multi-option setting: Non-default is considered "enabled"
                                isEnabledState = !currentOption.equals(defaultOption);
                            }
                            statePath = path + "." + (isEnabledState ? "enabled" : "disabled");
                        }

                        ItemBuilder builder = new ItemBuilder(
                                org.bukkit.Material.valueOf(config.getString(statePath + ".material")))
                                .durability(config.getInt(statePath + ".data"))
                                .name(config.getString(statePath + ".name"));

                        // Handle lore with placeholders for ping-range
                        List<String> lore = config.getStringList(statePath + ".lore");
                        if (settings == ProfileSettings.PING_RANGE) {
                            Option nextOption = settings.getNextOption(currentOption);
                            String currentDisplay = optionValue.equals("unlimited") ? "Unlimited" : optionValue + "ms";
                            String nextValue = nextOption.getValue().toLowerCase();
                            String nextDisplay = nextValue.equals(String.valueOf(Integer.MAX_VALUE)) ? "Unlimited"
                                    : nextValue + "ms";

                            lore = lore.stream()
                                    .map(line -> line.replace("{current}", currentDisplay).replace("{next}",
                                            nextDisplay))
                                    .collect(java.util.stream.Collectors.toList());
                        }
                        builder.lore(lore);

                        if (config.getBoolean(statePath + ".glow")) {
                            builder.glow();
                        }

                        return builder.build();
                    }

                    @Override
                    public void clicked(Player player, ClickType clickType) {
                        PlayerProfile profile = PlayerProfile.get(player);
                        Option currentOption = profile.getSettings().get(settings);

                        if (settings.getPermission() != null && !player.hasPermission(settings.getPermission())) {
                            player.sendMessage(CC.RED + "You don't have permission to change this setting.");
                            return;
                        }

                        // Special handling for PING_RANGE
                        if (settings == ProfileSettings.PING_RANGE) {
                            // Shift click resets to infinite
                            if (clickType.isShiftClick()) {
                                Option infiniteOption = settings.getOptions().get(0); // First option is infinite
                                profile.getSettings().replace(settings, infiniteOption);
                                infiniteOption.run(player);
                            }
                            // Left click: decrease ping range (50 -> 100 -> 150 -> 200 -> 300 -> infinite)
                            else if (clickType.isLeftClick()) {
                                Option last = settings.getLastOption(currentOption);
                                profile.getSettings().replace(settings, last);
                                last.run(player);
                            }
                            // Right click: increase ping range (infinite -> 300 -> 200 -> 150 -> 100 -> 50)
                            else if (clickType.isRightClick()) {
                                Option next = settings.getNextOption(currentOption);
                                profile.getSettings().replace(settings, next);
                                next.run(player);
                            }
                        } else {
                            // Default behavior for other settings
                            if (clickType.isLeftClick()) {
                                Option next = settings.getNextOption(currentOption);
                                profile.getSettings().replace(settings, next);
                                next.run(player);
                            } else if (clickType.isRightClick()) {
                                Option last = settings.getLastOption(currentOption);
                                profile.getSettings().replace(settings, last);
                                last.run(player);
                            }
                        }

                        settings.runSettingsChangeEvent(player, profile);
                    }

                    @Override
                    public boolean shouldUpdate(Player player, ClickType clickType) {
                        return true;
                    }
                });
            }
        }

        return buttons;
    }
}
