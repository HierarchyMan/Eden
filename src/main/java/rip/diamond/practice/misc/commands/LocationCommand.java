package rip.diamond.practice.misc.commands;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import rip.diamond.practice.Eden;
import rip.diamond.practice.config.Language;
import rip.diamond.practice.util.CC;
import rip.diamond.practice.util.Common;
import rip.diamond.practice.util.serialization.LocationSerialization;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import rip.diamond.practice.util.cuboid.Cuboid;

public class LocationCommand {

    private static final Eden plugin = Eden.INSTANCE;

    public static void handle(Player player, String[] args) {
        if (args.length < 2) {
            Common.sendMessage(player, CC.RED + "Usage: /eden location <type> [add/reset]");
            return;
        }

        LocationType type;
        try {
            type = LocationType.valueOf(args[1].toUpperCase());
        } catch (Exception e) {
            Common.sendMessage(player, CC.RED + "Invalid location type! Available types: " + Arrays
                    .stream(LocationType.values()).map(LocationType::name).collect(Collectors.joining(", ")));
            return;
        }

        Location location = player.getLocation();
        String action = args.length > 2 ? args[2].toLowerCase() : "set";

        switch (type) {

            case SPAWN:
                plugin.getLobbyManager().setSpawnLocation(location);
                saveLocation(player, "spawn-location", location);
                break;
            case EDITOR:
                plugin.getKitEditorManager().setEditorLocation(location);
                saveLocation(player, "editor-location", location);
                break;
            case SUMO_EVENT_A:
                plugin.getSpawnManager().setSumoFirst(location);
                saveLocation(player, "sumoFirst", location);
                break;
            case SUMO_EVENT_B:
                plugin.getSpawnManager().setSumoSecond(location);
                saveLocation(player, "sumoSecond", location);
                break;
            case SUMO_EVENT_SPECTATOR:
                plugin.getSpawnManager().setSumoLocation(location);
                saveLocation(player, "sumoLocation", location);
                break;
            case GULAG_1:
                plugin.getSpawnManager().setGulagFirst(location);
                saveLocation(player, "gulagFirst", location);
                break;
            case GULAG_2:
                plugin.getSpawnManager().setGulagSecond(location);
                saveLocation(player, "gulagSecond", location);
                break;
            case GULAG_FIREWORK:
                plugin.getSpawnManager().setGulagFirework(location);
                saveLocation(player, "gulagFirework", location);
                break;
            case BRACKETS_1:
                plugin.getSpawnManager().setBracketsFirst(location);
                saveLocation(player, "bracketsFirst", location);
                break;
            case BRACKETS_2:
                plugin.getSpawnManager().setBracketsSecond(location);
                saveLocation(player, "bracketsSecond", location);
                break;
            case PARKOUR:
                plugin.getSpawnManager().setParkourLocation(location);
                saveLocation(player, "parkourLocation", location);
                break;
            case PARKOUR_GAME:
                plugin.getSpawnManager().setParkourGameLocation(location);
                saveLocation(player, "parkourGameLocation", location);
                break;
            case SPLEEF:
                plugin.getSpawnManager().setSpleefLocation(location);
                saveLocation(player, "spleefLocation", location);
                break;
            case SPLEEF_MIN:
                plugin.getSpawnManager().setSpleefMin(location);
                saveLocation(player, "spleefMin", location);
                if (plugin.getSpawnManager().getSpleefMax() != null) {
                    plugin.getChunkRestorationManager()
                            .copy(new Cuboid(location, plugin.getSpawnManager().getSpleefMax()));
                    player.sendMessage(CC.GREEN + "Spleef chunks copied!");
                }
                break;
            case SPLEEF_MAX:
                plugin.getSpawnManager().setSpleefMax(location);
                saveLocation(player, "spleefMax", location);
                if (plugin.getSpawnManager().getSpleefMin() != null) {
                    plugin.getChunkRestorationManager()
                            .copy(new Cuboid(plugin.getSpawnManager().getSpleefMin(), location));
                    player.sendMessage(CC.GREEN + "Spleef chunks copied!");
                }
                break;
            case SKYWARS_MIN:
                plugin.getSpawnManager().setSkywarsMin(location);
                saveLocation(player, "skywarsMin", location);
                if (plugin.getSpawnManager().getSkywarsMax() != null) {
                    plugin.getChunkRestorationManager()
                            .copy(new Cuboid(location, plugin.getSpawnManager().getSkywarsMax()));
                    player.sendMessage(CC.GREEN + "SkyWars chunks copied!");
                }
                break;
            case SKYWARS_MAX:
                plugin.getSpawnManager().setSkywarsMax(location);
                saveLocation(player, "skywarsMax", location);
                if (plugin.getSpawnManager().getSkywarsMin() != null) {
                    plugin.getChunkRestorationManager()
                            .copy(new Cuboid(plugin.getSpawnManager().getSkywarsMin(), location));
                    player.sendMessage(CC.GREEN + "SkyWars chunks copied!");
                }
                break;
            case CORNERS:
                plugin.getSpawnManager().setCornersLocation(location);
                saveLocation(player, "cornersLocation", location);
                break;
            case CORNERS_MIN:
                plugin.getSpawnManager().setCornersMin(location);
                saveLocation(player, "cornersMin", location);
                if (plugin.getSpawnManager().getCornersMax() != null) {
                    plugin.getChunkRestorationManager()
                            .copy(new Cuboid(location, plugin.getSpawnManager().getCornersMax()));
                    player.sendMessage(CC.GREEN + "Corners chunks copied!");
                }
                break;
            case CORNERS_MAX:
                plugin.getSpawnManager().setCornersMax(location);
                saveLocation(player, "cornersMax", location);
                if (plugin.getSpawnManager().getCornersMin() != null) {
                    plugin.getChunkRestorationManager()
                            .copy(new Cuboid(plugin.getSpawnManager().getCornersMin(), location));
                    player.sendMessage(CC.GREEN + "Corners chunks copied!");
                }
                break;
            case THIMBLE:
                plugin.getSpawnManager().setThimbleLocation(location);
                saveLocation(player, "thimbleLocation", location);
                break;
            case THIMBLE_GAME:
                plugin.getSpawnManager().setThimbleGameLocation(location);
                saveLocation(player, "thimbleGameLocation", location);
                break;
            case TNT_TAG:
                plugin.getSpawnManager().setTntTagLocation(location);
                saveLocation(player, "tntTagLocation", location);
                break;
            case TNT_TAG_GAME:
                plugin.getSpawnManager().setTntTagGameLocation(location);
                saveLocation(player, "tntTagGameLocation", location);
                break;
            case BRACKETS_MIN:
                plugin.getSpawnManager().setBracketsMin(location);
                saveLocation(player, "bracketsMin", location);
                if (plugin.getSpawnManager().getBracketsMax() != null) {
                    plugin.getChunkRestorationManager()
                            .copy(new Cuboid(location, plugin.getSpawnManager().getBracketsMax()));
                    player.sendMessage(CC.GREEN + "Brackets chunks copied!");
                }
                break;
            case BRACKETS_MAX:
                plugin.getSpawnManager().setBracketsMax(location);
                saveLocation(player, "bracketsMax", location);
                if (plugin.getSpawnManager().getBracketsMin() != null) {
                    plugin.getChunkRestorationManager()
                            .copy(new Cuboid(plugin.getSpawnManager().getBracketsMin(), location));
                    player.sendMessage(CC.GREEN + "Brackets chunks copied!");
                }
                break;
            case GULAG_MIN:
                plugin.getSpawnManager().setGulagMin(location);
                saveLocation(player, "gulagMin", location);
                if (plugin.getSpawnManager().getGulagMax() != null) {
                    plugin.getChunkRestorationManager()
                            .copy(new Cuboid(location, plugin.getSpawnManager().getGulagMax()));
                    player.sendMessage(CC.GREEN + "Gulag chunks copied!");
                }
                break;
            case GULAG_MAX:
                plugin.getSpawnManager().setGulagMax(location);
                saveLocation(player, "gulagMax", location);
                if (plugin.getSpawnManager().getGulagMin() != null) {
                    plugin.getChunkRestorationManager()
                            .copy(new Cuboid(plugin.getSpawnManager().getGulagMin(), location));
                    player.sendMessage(CC.GREEN + "Gulag chunks copied!");
                }
                break;
            case LMS_MIN:
                plugin.getSpawnManager().setLmsMin(location);
                saveLocation(player, "lmsMin", location);
                if (plugin.getSpawnManager().getLmsMax() != null) {
                    plugin.getChunkRestorationManager()
                            .copy(new Cuboid(location, plugin.getSpawnManager().getLmsMax()));
                    player.sendMessage(CC.GREEN + "LMS chunks copied!");
                }
                break;
            case LMS_MAX:
                plugin.getSpawnManager().setLmsMax(location);
                saveLocation(player, "lmsMax", location);
                if (plugin.getSpawnManager().getLmsMin() != null) {
                    plugin.getChunkRestorationManager()
                            .copy(new Cuboid(plugin.getSpawnManager().getLmsMin(), location));
                    player.sendMessage(CC.GREEN + "LMS chunks copied!");
                }
                break;
            case KNOCKOUT_MIN:
                plugin.getSpawnManager().setKnockoutMin(location);
                saveLocation(player, "knockoutMin", location);
                if (plugin.getSpawnManager().getKnockoutMax() != null) {
                    plugin.getChunkRestorationManager()
                            .copy(new Cuboid(location, plugin.getSpawnManager().getKnockoutMax()));
                    player.sendMessage(CC.GREEN + "Knockout chunks copied!");
                }
                break;
            case KNOCKOUT_MAX:
                plugin.getSpawnManager().setKnockoutMax(location);
                saveLocation(player, "knockoutMax", location);
                if (plugin.getSpawnManager().getKnockoutMin() != null) {
                    plugin.getChunkRestorationManager()
                            .copy(new Cuboid(plugin.getSpawnManager().getKnockoutMin(), location));
                    player.sendMessage(CC.GREEN + "Knockout chunks copied!");
                }
                break;

            case LMS:
                handleListLocation(player, "lmsLocation", "lms", plugin.getSpawnManager().getLmsLocations(), action,
                        location);
                break;
            case KNOCKOUT:
                handleListLocation(player, "knockoutLocation", "knockout",
                        plugin.getSpawnManager().getKnockoutLocations(), action, location);
                break;
            case STOPLIGHT:
                handleListLocation(player, "stoplightLocation", "stoplight",
                        plugin.getSpawnManager().getStoplightLocations(), action, location);
                break;
            case SKYWARS:
                handleListLocation(player, "skywarsLocation", "skywars", plugin.getSpawnManager().getSkywarsLocations(),
                        action, location);
                break;
            case OITC:
                handleListLocation(player, "oitcLocation", "oitc", plugin.getSpawnManager().getOitcSpawnpoints(),
                        action, location);
                break;
            case OITC_SPECTATOR:
                plugin.getSpawnManager().setOitcSpectatorLocation(location);
                saveLocation(player, "oitcSpectatorLocation", location);
                break;
            case DROPPER:
                handleListLocation(player, "dropperLocation", "dropper", plugin.getSpawnManager().getDropperMaps(),
                        action, location);
                break;
            default:
                return;
        }
    }

    private static void saveLocation(Player player, String path, Location location) {
        plugin.getLocationFile().getConfiguration().set(path, LocationSerialization.serializeLocation(location));
        plugin.getLocationFile().save();
        Language.LOCATION_CHANGED.sendMessage(player, path);
    }

    private static void handleListLocation(Player player, String mainPath, String listPath, List<Location> list,
            String action, Location location) {
        if (action.equalsIgnoreCase("add")) {
            list.add(location);
            plugin.getSpawnManager().saveConfig();
            player.sendMessage(CC.GREEN + "Added a spawn point to " + listPath + " (Total: " + list.size() + ")");
        } else if (action.equalsIgnoreCase("reset")) {
            list.clear();
            plugin.getSpawnManager().saveConfig();
            player.sendMessage(CC.GREEN + "Reset all spawn points for " + listPath);
        } else {

            switch (listPath) {
                case "lms":
                    plugin.getSpawnManager().setLmsLocation(location);
                    break;
                case "knockout":
                    plugin.getSpawnManager().setKnockoutLocation(location);
                    break;
                case "stoplight":
                    plugin.getSpawnManager().setStoplightLocation(location);
                    break;
                case "skywars":
                    plugin.getSpawnManager().setSkywarsLocation(location);
                    break;
                case "oitc":
                    plugin.getSpawnManager().setOitcLocation(location);
                    break;
                case "dropper":
                    plugin.getSpawnManager().setDropperLocation(location);
                    break;
            }
            saveLocation(player, mainPath, location);
            player.sendMessage(CC.GREEN + "Set main spectator/lobby location for " + listPath);
            player.sendMessage(
                    CC.YELLOW + "To add game spawn points, use: /eden location " + listPath.toUpperCase() + " add");
        }
    }

    public static List<String> getTabComplete(String[] args) {
        if (args.length == 2) {
            return Arrays.stream(LocationType.values()).map(LocationType::name).collect(Collectors.toList());
        }
        if (args.length == 3) {
            return Arrays.asList("set", "add", "reset");
        }
        return null;
    }

    enum LocationType {
        SPAWN,
        EDITOR,
        SUMO_EVENT_A,
        SUMO_EVENT_B,
        SUMO_EVENT_SPECTATOR,
        LMS,
        KNOCKOUT,
        STOPLIGHT,
        SKYWARS_MIN,
        SKYWARS_MAX,
        SKYWARS,
        OITC,
        OITC_SPECTATOR,
        GULAG_1,
        GULAG_2,
        GULAG_FIREWORK,
        BRACKETS_1,
        BRACKETS_2,
        PARKOUR,
        PARKOUR_GAME,
        SPLEEF,
        SPLEEF_MIN,
        SPLEEF_MAX,
        TNT_TAG,
        TNT_TAG_GAME,
        CORNERS,
        CORNERS_MIN,
        CORNERS_MAX,
        THIMBLE,
        THIMBLE_GAME,
        DROPPER,
        BRACKETS_MIN,
        BRACKETS_MAX,
        GULAG_MIN,
        GULAG_MAX,
        LMS_MIN,
        LMS_MAX,
        KNOCKOUT_MIN,
        KNOCKOUT_MAX
    }
}
