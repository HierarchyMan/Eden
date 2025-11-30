package rip.diamond.practice.managers;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import rip.diamond.practice.Eden;
import rip.diamond.practice.util.serialization.LocationSerialization;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class SpawnManager {

    private final Eden plugin = Eden.INSTANCE;

    private Location sumoLocation;
    private Location sumoFirst;
    private Location sumoSecond;

    private Location bracketsLocation;
    private Location bracketsFirst;
    private Location bracketsSecond;
    private Location bracketsMin;
    private Location bracketsMax;

    private Location gulagLocation;
    private Location gulagFirework;
    private Location gulagFirst;
    private Location gulagSecond;
    private Location gulagMin;
    private Location gulagMax;

    private List<Location> lmsLocations;
    private Location lmsLocation;
    private Location lmsMin;
    private Location lmsMax;

    private List<Location> knockoutLocations;
    private Location knockoutLocation;
    private Location knockoutMin;
    private Location knockoutMax;

    private List<Location> skywarsLocations;
    private Location skywarsLocation;
    private Location skywarsMin;
    private Location skywarsMax;

    private List<Location> dropperMaps;
    private Location dropperLocation;

    private Location spleefLocation;
    private Location spleefMin;
    private Location spleefMax;

    private Location tntTagLocation;
    private Location tntTagGameLocation;

    private Location cornersLocation;
    private Location cornersMin;
    private Location cornersMax;

    private Location thimbleGameLocation;
    private Location thimbleLocation;

    private List<Location> stoplightLocations;
    private Location stoplightLocation;

    private List<Location> oitcSpawnpoints;
    private Location oitcLocation;
    private Location oitcSpectatorLocation;

    private Location parkourLocation;
    private Location parkourGameLocation;
    private List<Location> parkourCheckpoints;

    public SpawnManager() {
        this.dropperMaps = new ArrayList<>();
        this.lmsLocations = new ArrayList<>();
        this.oitcSpawnpoints = new ArrayList<>();
        this.skywarsLocations = new ArrayList<>();
        this.knockoutLocations = new ArrayList<>();
        this.stoplightLocations = new ArrayList<>();
        this.parkourCheckpoints = new ArrayList<>();

        this.loadConfig();
    }

    public void reload() {
        this.dropperMaps.clear();
        this.lmsLocations.clear();
        this.oitcSpawnpoints.clear();
        this.skywarsLocations.clear();
        this.knockoutLocations.clear();
        this.stoplightLocations.clear();
        this.parkourCheckpoints.clear();

        this.loadConfig();
    }

    private void loadConfig() {
        FileConfiguration config = plugin.getLocationFile().getConfiguration();

        if (config.contains("cornersLocation")) {
            this.cornersLocation = LocationSerialization.deserializeLocation(config.getString("cornersLocation"));
            this.cornersMin = LocationSerialization.deserializeLocation(config.getString("cornersMin"));
            this.cornersMax = LocationSerialization.deserializeLocation(config.getString("cornersMax"));
        }

        if (config.contains("thimbleLocation")) {
            this.thimbleGameLocation = LocationSerialization
                    .deserializeLocation(config.getString("thimbleGameLocation"));
            this.thimbleLocation = LocationSerialization.deserializeLocation(config.getString("thimbleLocation"));
        }

        if (config.contains("sumoLocation")) {
            this.sumoLocation = LocationSerialization.deserializeLocation(config.getString("sumoLocation"));
            this.sumoFirst = LocationSerialization.deserializeLocation(config.getString("sumoFirst"));
            this.sumoSecond = LocationSerialization.deserializeLocation(config.getString("sumoSecond"));
        }

        if (config.contains("bracketsLocation")) {
            this.bracketsLocation = LocationSerialization.deserializeLocation(config.getString("bracketsLocation"));
            this.bracketsFirst = LocationSerialization.deserializeLocation(config.getString("bracketsFirst"));
            this.bracketsSecond = LocationSerialization.deserializeLocation(config.getString("bracketsSecond"));
            this.bracketsMin = LocationSerialization.deserializeLocation(config.getString("bracketsMin"));
            this.bracketsMax = LocationSerialization.deserializeLocation(config.getString("bracketsMax"));
        }

        if (config.contains("gulagLocation")) {
            this.gulagLocation = LocationSerialization.deserializeLocation(config.getString("gulagLocation"));
            this.gulagFirework = LocationSerialization.deserializeLocation(config.getString("gulagFirework"));
            this.gulagFirst = LocationSerialization.deserializeLocation(config.getString("gulagFirst"));
            this.gulagSecond = LocationSerialization.deserializeLocation(config.getString("gulagSecond"));
            this.gulagMin = LocationSerialization.deserializeLocation(config.getString("gulagMin"));
            this.gulagMax = LocationSerialization.deserializeLocation(config.getString("gulagMax"));
        }

        if (config.contains("oitcLocation")) {
            this.oitcLocation = LocationSerialization.deserializeLocation(config.getString("oitcLocation"));
            for (String spawnpoint : config.getStringList("oitc")) {
                this.oitcSpawnpoints.add(LocationSerialization.deserializeLocation(spawnpoint));
            }
        }

        if (config.contains("oitcSpectatorLocation")) {
            this.oitcSpectatorLocation = LocationSerialization
                    .deserializeLocation(config.getString("oitcSpectatorLocation"));
        }

        if (config.contains("dropperLocation")) {
            this.dropperLocation = LocationSerialization.deserializeLocation(config.getString("dropperLocation"));
            for (String map : config.getStringList("dropperMaps")) {
                this.dropperMaps.add(LocationSerialization.deserializeLocation(map));
            }
        }

        if (config.contains("lmsLocation")) {
            this.lmsLocation = LocationSerialization.deserializeLocation(config.getString("lmsLocation"));
            this.lmsMin = LocationSerialization.deserializeLocation(config.getString("lmsMin"));
            this.lmsMax = LocationSerialization.deserializeLocation(config.getString("lmsMax"));
            for (String spawnpoint : config.getStringList("lms")) {
                this.lmsLocations.add(LocationSerialization.deserializeLocation(spawnpoint));
            }
        }

        if (config.contains("knockoutLocation")) {
            this.knockoutLocation = LocationSerialization.deserializeLocation(config.getString("knockoutLocation"));
            this.knockoutMin = LocationSerialization.deserializeLocation(config.getString("knockoutMin"));
            this.knockoutMax = LocationSerialization.deserializeLocation(config.getString("knockoutMax"));
            for (String spawnpoint : config.getStringList("knockout")) {
                this.knockoutLocations.add(LocationSerialization.deserializeLocation(spawnpoint));
            }
        }

        if (config.contains("skywarsLocation")) {
            this.skywarsLocation = LocationSerialization.deserializeLocation(config.getString("skywarsLocation"));
            this.skywarsMin = LocationSerialization.deserializeLocation(config.getString("skywarsMin"));
            this.skywarsMax = LocationSerialization.deserializeLocation(config.getString("skywarsMax"));
            for (String spawnpoint : config.getStringList("skywars")) {
                this.skywarsLocations.add(LocationSerialization.deserializeLocation(spawnpoint));
            }
        }

        if (config.contains("stoplightLocation")) {
            this.stoplightLocation = LocationSerialization.deserializeLocation(config.getString("stoplightLocation"));
            for (String spawnpoint : config.getStringList("stoplight")) {
                this.stoplightLocations.add(LocationSerialization.deserializeLocation(spawnpoint));
            }
        }

        if (config.contains("spleefLocation")) {
            this.spleefLocation = LocationSerialization.deserializeLocation(config.getString("spleefLocation"));
            this.spleefMin = LocationSerialization.deserializeLocation(config.getString("spleefMin"));
            this.spleefMax = LocationSerialization.deserializeLocation(config.getString("spleefMax"));
        }

        if (config.contains("tntTagLocation")) {
            this.tntTagLocation = LocationSerialization.deserializeLocation(config.getString("tntTagLocation"));
            this.tntTagGameLocation = LocationSerialization.deserializeLocation(config.getString("tntTagGameLocation"));
        }

        if (config.contains("parkourLocation")) {
            this.parkourLocation = LocationSerialization.deserializeLocation(config.getString("parkourLocation"));
            this.parkourGameLocation = LocationSerialization
                    .deserializeLocation(config.getString("parkourGameLocation"));
            for (String checkpoint : config.getStringList("parkourCheckpoints")) {
                this.parkourCheckpoints.add(LocationSerialization.deserializeLocation(checkpoint));
            }
        }
    }

    public void saveConfig() {
        FileConfiguration config = plugin.getLocationFile().getConfiguration();

        config.set("cornersLocation", LocationSerialization.serializeLocation(this.cornersLocation));
        config.set("cornersMin", LocationSerialization.serializeLocation(this.cornersMin));
        config.set("cornersMax", LocationSerialization.serializeLocation(this.cornersMax));

        config.set("thimbleGameLocation", LocationSerialization.serializeLocation(this.thimbleGameLocation));
        config.set("thimbleLocation", LocationSerialization.serializeLocation(this.thimbleLocation));

        config.set("sumoLocation", LocationSerialization.serializeLocation(this.sumoLocation));
        config.set("sumoFirst", LocationSerialization.serializeLocation(this.sumoFirst));
        config.set("sumoSecond", LocationSerialization.serializeLocation(this.sumoSecond));

        config.set("bracketsLocation", LocationSerialization.serializeLocation(this.bracketsLocation));
        config.set("bracketsFirst", LocationSerialization.serializeLocation(this.bracketsFirst));
        config.set("bracketsSecond", LocationSerialization.serializeLocation(this.bracketsSecond));
        config.set("bracketsMin", LocationSerialization.serializeLocation(this.bracketsMin));
        config.set("bracketsMax", LocationSerialization.serializeLocation(this.bracketsMax));

        config.set("gulagLocation", LocationSerialization.serializeLocation(this.gulagLocation));
        config.set("gulagFirework", LocationSerialization.serializeLocation(this.gulagFirework));
        config.set("gulagFirst", LocationSerialization.serializeLocation(this.gulagFirst));
        config.set("gulagSecond", LocationSerialization.serializeLocation(this.gulagSecond));
        config.set("gulagMin", LocationSerialization.serializeLocation(this.gulagMin));
        config.set("gulagMax", LocationSerialization.serializeLocation(this.gulagMax));

        config.set("oitcLocation", LocationSerialization.serializeLocation(this.oitcLocation));
        config.set("oitc", this.fromLocations(this.oitcSpawnpoints));
        config.set("oitcSpectatorLocation", LocationSerialization.serializeLocation(this.oitcSpectatorLocation));

        config.set("dropperLocation", LocationSerialization.serializeLocation(this.dropperLocation));
        config.set("dropperMaps", this.fromLocations(this.dropperMaps));

        config.set("lmsLocation", LocationSerialization.serializeLocation(this.lmsLocation));
        config.set("lmsMin", LocationSerialization.serializeLocation(this.lmsMin));
        config.set("lmsMax", LocationSerialization.serializeLocation(this.lmsMax));
        config.set("lms", this.fromLocations(this.lmsLocations));

        config.set("knockoutLocation", LocationSerialization.serializeLocation(this.knockoutLocation));
        config.set("knockoutMin", LocationSerialization.serializeLocation(this.knockoutMin));
        config.set("knockoutMax", LocationSerialization.serializeLocation(this.knockoutMax));
        config.set("knockout", this.fromLocations(this.knockoutLocations));

        config.set("stoplightLocation", LocationSerialization.serializeLocation(this.stoplightLocation));
        config.set("stoplight", this.fromLocations(this.stoplightLocations));

        config.set("skywarsLocation", LocationSerialization.serializeLocation(this.skywarsLocation));
        config.set("skywarsMin", LocationSerialization.serializeLocation(this.skywarsMin));
        config.set("skywarsMax", LocationSerialization.serializeLocation(this.skywarsMax));
        config.set("skywars", this.fromLocations(this.skywarsLocations));

        config.set("spleefLocation", LocationSerialization.serializeLocation(this.spleefLocation));
        config.set("spleefMin", LocationSerialization.serializeLocation(this.spleefMin));
        config.set("spleefMax", LocationSerialization.serializeLocation(this.spleefMax));

        config.set("tntTagLocation", LocationSerialization.serializeLocation(this.tntTagLocation));
        config.set("tntTagGameLocation", LocationSerialization.serializeLocation(this.tntTagGameLocation));

        config.set("parkourLocation", LocationSerialization.serializeLocation(this.parkourLocation));
        config.set("parkourGameLocation", LocationSerialization.serializeLocation(this.parkourGameLocation));
        config.set("parkourCheckpoints", this.fromLocations(this.parkourCheckpoints));

        plugin.getLocationFile().save();
    }

    public List<String> fromLocations(List<Location> locations) {
        List<String> toReturn = new ArrayList<>();
        for (Location location : locations) {
            toReturn.add(LocationSerialization.serializeLocation(location));
        }
        return toReturn;
    }

    public Location getEventLocation(String eventName) {
        Location toReturn = null;

        switch (eventName.toLowerCase()) {
            case "4corners":
                toReturn = this.cornersLocation;
                break;
            case "thimble":
                toReturn = this.thimbleLocation;
                break;
            case "sumo":
                toReturn = this.sumoLocation;
                break;
            case "brackets":
                toReturn = this.bracketsLocation;
                break;
            case "gulag":
                toReturn = this.gulagLocation;
                break;
            case "oitc":
                toReturn = this.oitcLocation;
                break;
            case "dropper":
                toReturn = this.dropperLocation;
                break;
            case "lms":
                toReturn = this.lmsLocation;
                break;
            case "knockout":
                toReturn = this.knockoutLocation;
                break;
            case "stoplight":
                toReturn = this.stoplightLocation;
                break;
            case "skywars":
                toReturn = this.skywarsLocation;
                break;
            case "spleef":
                toReturn = this.spleefLocation;
                break;
            case "tnttag":
                toReturn = this.tntTagLocation;
                break;
            case "parkour":
                toReturn = this.parkourLocation;
                break;
        }

        return toReturn;
    }

    public void copyEventChunks() {
        if (this.cornersMin != null && this.cornersMax != null) {
            plugin.getChunkRestorationManager()
                    .copy(new rip.diamond.practice.util.cuboid.Cuboid(this.cornersMin, this.cornersMax));
        }
        if (this.bracketsMin != null && this.bracketsMax != null) {
            plugin.getChunkRestorationManager()
                    .copy(new rip.diamond.practice.util.cuboid.Cuboid(this.bracketsMin, this.bracketsMax));
        }
        if (this.gulagMin != null && this.gulagMax != null) {
            plugin.getChunkRestorationManager()
                    .copy(new rip.diamond.practice.util.cuboid.Cuboid(this.gulagMin, this.gulagMax));
        }
        if (this.lmsMin != null && this.lmsMax != null) {
            plugin.getChunkRestorationManager()
                    .copy(new rip.diamond.practice.util.cuboid.Cuboid(this.lmsMin, this.lmsMax));
        }
        if (this.knockoutMin != null && this.knockoutMax != null) {
            plugin.getChunkRestorationManager()
                    .copy(new rip.diamond.practice.util.cuboid.Cuboid(this.knockoutMin, this.knockoutMax));
        }
        if (this.skywarsMin != null && this.skywarsMax != null) {
            plugin.getChunkRestorationManager()
                    .copy(new rip.diamond.practice.util.cuboid.Cuboid(this.skywarsMin, this.skywarsMax));
        }
        if (this.spleefMin != null && this.spleefMax != null) {
            plugin.getChunkRestorationManager()
                    .copy(new rip.diamond.practice.util.cuboid.Cuboid(this.spleefMin, this.spleefMax));
        }
    }
}
