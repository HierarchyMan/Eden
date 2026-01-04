package rip.diamond.practice.arenas;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import rip.diamond.practice.Eden;
import rip.diamond.practice.config.Config;
import rip.diamond.practice.kits.Kit;
import rip.diamond.practice.util.Common;
import rip.diamond.practice.util.ItemBuilder;
import rip.diamond.practice.util.serialization.BukkitSerialization;
import rip.diamond.practice.util.serialization.LocationSerialization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
public class Arena {

    @Getter
    private static final List<Arena> arenas = new ArrayList<>();

    private final String name;
    private String displayName;
    private ItemStack icon = new ItemBuilder(Material.GRASS).build();
    private List<ArenaDetail> arenaDetails = new ArrayList<>();

    private int yLimit = 0;
    private int buildMax = -1;
    private int portalProtectionRadius = 3;
    private List<String> allowedKits = new ArrayList<>();
    private boolean enabled = false;
    private boolean edited = false;

    private boolean pendingReload = false;
    private Arena nextVersion = null;

    // Optional locations (extensible) - first feature: parkour checkpoints
    // NOTE: Parkour checkpoints are stored per-team so each side can have its own route.
    private List<Location> parkourCheckpointsA = new ArrayList<>();
    private List<Location> parkourCheckpointsB = new ArrayList<>();

    /**
     * Legacy (pre-team) checkpoints list.
     *
     * We keep it for one release cycle to avoid hard-breaking old arena.yml files.
     * On load, we migrate this list -> A list if A/B are missing.
     */
    @Deprecated
    private List<Location> parkourCheckpoints = new ArrayList<>();

    /**
     * @return parkour checkpoints for a specific team side.
     */
    public List<Location> getParkourCheckpoints(boolean teamA) {
        List<Location> src = teamA ? parkourCheckpointsA : parkourCheckpointsB;
        List<Location> copy = new ArrayList<>();
        for (Location l : src) {
            if (l != null) {
                copy.add(l.clone());
            }
        }
        return copy;
    }

    public void setParkourCheckpoints(boolean teamA, List<Location> checkpoints) {
        List<Location> dst = teamA ? parkourCheckpointsA : parkourCheckpointsB;
        dst.clear();
        if (checkpoints != null) {
            for (Location l : checkpoints) {
                if (l != null) {
                    dst.add(l.clone());
                }
            }
        }
    }

    public void addParkourCheckpoint(boolean teamA, Location location) {
        if (location == null) {
            return;
        }
        (teamA ? parkourCheckpointsA : parkourCheckpointsB).add(location.clone());
    }

    public boolean removeNearestParkourCheckpoint(boolean teamA, Location location, double radius) {
        List<Location> list = teamA ? parkourCheckpointsA : parkourCheckpointsB;
        if (location == null || list.isEmpty()) {
            return false;
        }
        Location nearest = null;
        double best = Double.MAX_VALUE;
        for (Location cp : list) {
            if (cp == null || cp.getWorld() == null || location.getWorld() == null) {
                continue;
            }
            if (!cp.getWorld().getName().equals(location.getWorld().getName())) {
                continue;
            }
            double dist = cp.distance(location);
            if (dist <= radius && dist < best) {
                best = dist;
                nearest = cp;
            }
        }
        if (nearest != null) {
            list.remove(nearest);
            return true;
        }
        return false;
    }

    public void clearParkourCheckpoints(boolean teamA) {
        (teamA ? parkourCheckpointsA : parkourCheckpointsB).clear();
    }

    public Location findCheckpointAt(boolean teamA, Location location, double radius) {
        List<Location> list = teamA ? parkourCheckpointsA : parkourCheckpointsB;
        if (location == null || list.isEmpty()) {
            return null;
        }
        for (Location cp : list) {
            if (cp == null || cp.getWorld() == null || location.getWorld() == null) {
                continue;
            }
            if (!cp.getWorld().getName().equals(location.getWorld().getName())) {
                continue;
            }
            if (cp.distance(location) <= radius) {
                return cp.clone();
            }
        }
        return null;
    }

    public boolean isNearCheckpoint(boolean teamA, Location location, int radiusBlocks) {
        List<Location> list = teamA ? parkourCheckpointsA : parkourCheckpointsB;
        if (location == null || list.isEmpty() || radiusBlocks <= 0) {
            return false;
        }
        double r = radiusBlocks;
        for (Location cp : list) {
            if (cp == null || cp.getWorld() == null || location.getWorld() == null) {
                continue;
            }
            if (!cp.getWorld().getName().equals(location.getWorld().getName())) {
                continue;
            }
            if (cp.distance(location) <= r) {
                return true;
            }
        }
        return false;
    }

    /**
     * Migration helper: if A/B are empty but legacy list has data, copy legacy -> A.
     */
    public void migrateLegacyParkourCheckpointsIfNeeded() {
        if (!parkourCheckpointsA.isEmpty() || !parkourCheckpointsB.isEmpty()) {
            return;
        }
        if (parkourCheckpoints == null || parkourCheckpoints.isEmpty()) {
            return;
        }
        setParkourCheckpoints(true, parkourCheckpoints);
    }

    /**
     * Legacy getter (merged).
     * Prefer getParkourCheckpoints(true/false).
     */
    @Deprecated
    public List<Location> getParkourCheckpoints() {
        // Keep old behavior: if only A is set, return A; else merge A+B.
        if (!parkourCheckpointsA.isEmpty() && parkourCheckpointsB.isEmpty()) {
            return getParkourCheckpoints(true);
        }
        List<Location> merged = new ArrayList<>();
        merged.addAll(getParkourCheckpoints(true));
        merged.addAll(getParkourCheckpoints(false));
        return merged;
    }

    /**
     * Legacy setter (writes to team A).
     */
    @Deprecated
    public void setParkourCheckpoints(List<Location> checkpoints) {
        setParkourCheckpoints(true, checkpoints);
    }

    /**
     * Legacy add (adds to team A).
     */
    @Deprecated
    public void addParkourCheckpoint(Location location) {
        addParkourCheckpoint(true, location);
    }

    @Deprecated
    public boolean removeNearestParkourCheckpoint(Location location, double radius) {
        return removeNearestParkourCheckpoint(true, location, radius);
    }

    @Deprecated
    public void clearParkourCheckpoints() {
        clearParkourCheckpoints(true);
    }

    @Deprecated
    public Location findCheckpointAt(Location location, double radius) {
        return findCheckpointAt(true, location, radius);
    }

    @Deprecated
    public boolean isNearCheckpoint(Location location, int radiusBlocks) {
        return isNearCheckpoint(true, location, radiusBlocks);
    }

    public boolean isUsing() {
        return arenaDetails.stream().anyMatch(ArenaDetail::isUsing);
    }

    public static void reload() {
        FileConfiguration fileConfig = Eden.INSTANCE.getArenaFile().getConfiguration();
        ConfigurationSection arenaSection = fileConfig.getConfigurationSection("arenas");

        List<Arena> loadedArenas = new ArrayList<>();

        if (arenaSection != null) {
            arenaSection.getKeys(false).forEach(name -> {
                String displayName = arenaSection.getString(name + ".display-name", name);
                ItemStack icon = BukkitSerialization.itemStackFromBase64(arenaSection.getString(name + ".icon"));
                int yLimit = arenaSection.getInt(name + ".y-limit");
                int buildMax = arenaSection.getInt(name + ".build-max");
                int portalProtectionRadius = arenaSection.getInt(name + ".portal-protection-radius");
                List<String> allowedKits = arenaSection.getStringList(name + ".kits");
                boolean enabled = arenaSection.getBoolean(name + ".enabled", false);

                Arena arena = new Arena(name);
                arena.setDisplayName(displayName);
                arena.setIcon(icon);
                arena.setYLimit(yLimit);
                arena.setBuildMax(buildMax);
                arena.setPortalProtectionRadius(portalProtectionRadius);
                arena.setAllowedKits(allowedKits);
                arena.setEnabled(enabled);

                ConfigurationSection details = arenaSection.getConfigurationSection(name + ".details");
                if (details != null) {
                    details.getKeys(false).forEach(id -> {
                        Location locCloneA = LocationSerialization.deserializeLocation(details.getString(id + ".a"));
                        Location locCloneB = LocationSerialization.deserializeLocation(details.getString(id + ".b"));
                        Location locCloneMin = LocationSerialization
                                .deserializeLocation(details.getString(id + ".min"));
                        Location locCloneMax = LocationSerialization
                                .deserializeLocation(details.getString(id + ".max"));
                        Location locCloneSpectator = LocationSerialization
                                .deserializeLocation(details.getString(id + ".spectator"));

                        ArenaDetail arenaDetail = new ArenaDetail(arena, locCloneA, locCloneB,
                                locCloneSpectator == null ? locCloneA : locCloneSpectator, locCloneMin, locCloneMax);
                        arenaDetail.copyChunk();
                        arena.getArenaDetails().add(arenaDetail);
                    });
                }

                // Optional parkour checkpoints (per-team)
                List<String> checkpointStringsA = arenaSection.getStringList(name + ".parkour-checkpoints-a");
                List<String> checkpointStringsB = arenaSection.getStringList(name + ".parkour-checkpoints-b");

                boolean hasAnyNew = (checkpointStringsA != null && !checkpointStringsA.isEmpty())
                        || (checkpointStringsB != null && !checkpointStringsB.isEmpty());

                if (hasAnyNew) {
                    if (checkpointStringsA != null && !checkpointStringsA.isEmpty()) {
                        List<Location> checkpointsA = new ArrayList<>();
                        for (String s : checkpointStringsA) {
                            Location l = LocationSerialization.deserializeLocation(s);
                            if (l != null) {
                                checkpointsA.add(l);
                            }
                        }
                        arena.setParkourCheckpoints(true, checkpointsA);
                    }

                    if (checkpointStringsB != null && !checkpointStringsB.isEmpty()) {
                        List<Location> checkpointsB = new ArrayList<>();
                        for (String s : checkpointStringsB) {
                            Location l = LocationSerialization.deserializeLocation(s);
                            if (l != null) {
                                checkpointsB.add(l);
                            }
                        }
                        arena.setParkourCheckpoints(false, checkpointsB);
                    }
                } else {
                    // Legacy fallback
                    List<String> checkpointStrings = arenaSection.getStringList(name + ".parkour-checkpoints");
                    if (checkpointStrings != null && !checkpointStrings.isEmpty()) {
                        List<Location> checkpoints = new ArrayList<>();
                        for (String s : checkpointStrings) {
                            Location l = LocationSerialization.deserializeLocation(s);
                            if (l != null) {
                                checkpoints.add(l);
                            }
                        }
                        arena.setParkourCheckpoints(checkpoints);
                        arena.migrateLegacyParkourCheckpointsIfNeeded();
                    }
                }

                loadedArenas.add(arena);
            });
        }

        List<Arena> toRemove = new ArrayList<>();
        for (Arena currentArena : new ArrayList<>(arenas)) {
            Arena newVersion = loadedArenas.stream().filter(a -> a.getName().equals(currentArena.getName())).findFirst()
                    .orElse(null);

            if (currentArena.isUsing()) {
                currentArena.setPendingReload(true);
                currentArena.setNextVersion(newVersion);
                Common.log("&cArena " + currentArena.getName() + " is in use. Queued for reload.");
            } else {

                toRemove.add(currentArena);
            }
        }

        arenas.removeAll(toRemove);

        for (Arena loadedArena : loadedArenas) {

            if (arenas.stream().noneMatch(a -> a.getName().equals(loadedArena.getName()))) {
                arenas.add(loadedArena);
            }
        }

        Common.log("&aReloaded arenas. " + arenas.size() + " arenas loaded.");
    }

    public static void processPendingReload(Arena arena) {
        if (arena.isPendingReload()) {
            if (arena.isUsing()) {
                return;
            }

            arenas.remove(arena);
            if (arena.getNextVersion() != null) {
                arenas.add(arena.getNextVersion());
                Common.log("&aArena " + arena.getName() + " has been reloaded (was pending).");
            } else {
                Common.log("&aArena " + arena.getName() + " has been removed (was pending).");
            }
        }
    }

    public static Arena getArena(String name) {
        return arenas.stream()
                .filter(arena -> arena.getName().equalsIgnoreCase(name))
                .findAny().orElse(null);
    }

    public static Arena getEnabledArena(String name, Kit kit) {
        Collections.shuffle(arenas);
        return arenas.stream()
                .filter(arena -> arena.isEnabled() &&
                        !arena.isLocked() &&
                        !arena.getArenaDetails().isEmpty() &&
                        arena.getAllowedKits().contains(kit.getName()) &&
                        arena.getName().equalsIgnoreCase(name))
                .findAny().orElse(null);
    }

    public static Arena getEnabledArena(Kit kit) {
        Collections.shuffle(arenas);
        return arenas.stream()
                .filter(Arena::isEnabled)
                .filter(arena -> arena.getAllowedKits().contains(kit.getName()))
                .findAny().orElse(null);
    }

    public static ArenaDetail getAvailableArenaDetail(Kit kit) {
        Collections.shuffle(arenas);
        Arena arena = arenas.stream()
                .filter(Arena::isEnabled)
                .filter(a -> a.getAllowedKits().contains(kit.getName()))
                .findAny().orElse(null);
        if (arena == null) {
            return null;
        }
        return getArenaDetail(arena);
    }

    public static ArenaDetail getArenaDetail(Arena arena) {
        return arena.getArenaDetails().stream()
                .filter(arenaDetail -> !arenaDetail.isUsing())
                .filter(arenaDetail -> {
                    if (Config.EXPERIMENT_DISABLE_ORIGINAL_ARENA.toBoolean()) {
                        return arena.getArenaDetails().get(0) != arenaDetail;
                    }
                    return true;
                })
                .findAny().orElse(null);
    }

    public Arena(String name) {
        this.name = name;
        this.displayName = name;
    }

    public ItemStack getIcon() {
        return icon.clone();
    }

    public boolean isFinishedSetup() {
        return arenaDetails.stream().allMatch(ArenaDetail::isFinishedSetup);
    }

    public boolean isEnabled() {
        return enabled && isFinishedSetup() && !edited;
    }

    public boolean isLocked() {
        return Config.EVENT_SUMO_EVENT_ARENAS.toStringList().contains(name);
    }

    public boolean hasClone() {
        return arenaDetails.size() > 1;
    }

    public Location getA() {
        return arenaDetails.get(0).getA();
    }

    public Location getB() {
        return arenaDetails.get(0).getB();
    }

    public Location getSpectator() {
        return arenaDetails.get(0).getSpectator();
    }

    public Location getMin() {
        return arenaDetails.get(0).getMin();
    }

    public Location getMax() {
        return arenaDetails.get(0).getMax();
    }

    public void setA(Location location) {
        arenaDetails.get(0).setA(location);
    }

    public void setB(Location location) {
        arenaDetails.get(0).setB(location);
    }

    public void setSpectator(Location location) {
        arenaDetails.get(0).setSpectator(location);
    }

    public void setMin(Location location) {
        arenaDetails.get(0).setMin(location);
    }

    public void setMax(Location location) {
        arenaDetails.get(0).setMax(location);
    }

    public void autoSave() {
        if (Config.ARENA_KIT_AUTO_SAVE.toBoolean()) {
            save();
        }
    }

    public static void init() {
        arenas.clear();
        FileConfiguration fileConfig = Eden.INSTANCE.getArenaFile().getConfiguration();
        ConfigurationSection arenaSection = fileConfig.getConfigurationSection("arenas");
        if (arenaSection == null) {
            return;
        }

        arenaSection.getKeys(false).forEach(name -> {
            String displayName = arenaSection.getString(name + ".display-name", name);

            ItemStack icon = BukkitSerialization.itemStackFromBase64(arenaSection.getString(name + ".icon"));
            int yLimit = arenaSection.getInt(name + ".y-limit");
            int buildMax = arenaSection.getInt(name + ".build-max");
            int portalProtectionRadius = arenaSection.getInt(name + ".portal-protection-radius");
            List<String> allowedKits = arenaSection.getStringList(name + ".kits");
            boolean enabled = arenaSection.getBoolean(name + ".enabled", false);

            Arena arena = new Arena(name);
            arena.setDisplayName(displayName);
            arena.setIcon(icon);
            arena.setYLimit(yLimit);
            arena.setBuildMax(buildMax);
            arena.setPortalProtectionRadius(portalProtectionRadius);
            arena.setAllowedKits(allowedKits);
            arena.setEnabled(enabled);

            ConfigurationSection details = arenaSection.getConfigurationSection(name + ".details");
            if (details != null) {
                details.getKeys(false).forEach(id -> {
                    Location locCloneA = LocationSerialization.deserializeLocation(details.getString(id + ".a"));
                    Location locCloneB = LocationSerialization.deserializeLocation(details.getString(id + ".b"));
                    Location locCloneMin = LocationSerialization.deserializeLocation(details.getString(id + ".min"));
                    Location locCloneMax = LocationSerialization.deserializeLocation(details.getString(id + ".max"));
                    Location locCloneSpectator = LocationSerialization
                            .deserializeLocation(details.getString(id + ".spectator"));

                    ArenaDetail arenaDetail = new ArenaDetail(arena, locCloneA, locCloneB,
                            locCloneSpectator == null ? locCloneA : locCloneSpectator, locCloneMin, locCloneMax);
                    arenaDetail.copyChunk();
                    arena.getArenaDetails().add(arenaDetail);
                });
            }

            // Optional parkour checkpoints (per-team)
            List<String> checkpointStringsA = arenaSection.getStringList(name + ".parkour-checkpoints-a");
            List<String> checkpointStringsB = arenaSection.getStringList(name + ".parkour-checkpoints-b");

            boolean hasAnyNew = (checkpointStringsA != null && !checkpointStringsA.isEmpty())
                    || (checkpointStringsB != null && !checkpointStringsB.isEmpty());

            if (hasAnyNew) {
                if (checkpointStringsA != null && !checkpointStringsA.isEmpty()) {
                    List<Location> checkpointsA = new ArrayList<>();
                    for (String s : checkpointStringsA) {
                        Location l = LocationSerialization.deserializeLocation(s);
                        if (l != null) {
                            checkpointsA.add(l);
                        }
                    }
                    arena.setParkourCheckpoints(true, checkpointsA);
                }

                if (checkpointStringsB != null && !checkpointStringsB.isEmpty()) {
                    List<Location> checkpointsB = new ArrayList<>();
                    for (String s : checkpointStringsB) {
                        Location l = LocationSerialization.deserializeLocation(s);
                        if (l != null) {
                            checkpointsB.add(l);
                        }
                    }
                    arena.setParkourCheckpoints(false, checkpointsB);
                }
            } else {
                // Legacy fallback
                List<String> checkpointStrings = arenaSection.getStringList(name + ".parkour-checkpoints");
                if (checkpointStrings != null && !checkpointStrings.isEmpty()) {
                    List<Location> checkpoints = new ArrayList<>();
                    for (String s : checkpointStrings) {
                        Location l = LocationSerialization.deserializeLocation(s);
                        if (l != null) {
                            checkpoints.add(l);
                        }
                    }
                    arena.setParkourCheckpoints(checkpoints);
                    arena.migrateLegacyParkourCheckpointsIfNeeded();
                }
            }

            arenas.add(arena);
            Common.log("Loaded " + arena.getArenaDetails().size() + " " + arena.getName() + " arenas");
        });

        ActiveArenaTracker.init();

        for (Arena arena : arenas) {
            for (ArenaDetail detail : arena.getArenaDetails()) {
                boolean wasActive = ActiveArenaTracker.wasActive(detail);
                boolean disableOriginal = Config.EXPERIMENT_DISABLE_ORIGINAL_ARENA.toBoolean();

                if (disableOriginal) {
                    // If disable original is on, we don't use cached chunks for index 0 (or any
                    // index really, based on copyChunk logic)
                    // But wait, copyChunk logic says: if index > 0 return. So only index 0 has
                    // chunks?
                    // No, "if index > 0 return" means ONLY index 0 is copied.
                    // So if disableOriginal is true, only index 0 has cached chunks.

                    if (wasActive) {
                        Common.log("&eArena " + arena.getName() + " (Detail " + arena.getArenaDetails().indexOf(detail)
                                + ") was active. Resetting via FAWE...");
                        detail.restoreChunk(false, true);
                        ActiveArenaTracker.remove(detail);
                    }
                } else {
                    // Normal mode: Try to load chunks from disk
                    boolean loaded = rip.diamond.practice.arenas.chunk.ArenaChunkManager.loadChunks(detail);

                    if (loaded) {
                        if (wasActive) {
                            Common.log("&eArena " + arena.getName() + " (Detail "
                                    + arena.getArenaDetails().indexOf(detail) + ") was active. Resetting from disk...");
                            detail.restoreChunk(false, true);
                            ActiveArenaTracker.remove(detail);
                        }
                    } else {
                        // If not loaded (file missing), copy from world and save
                        detail.copyChunk();
                    }
                }
            }
        }
    }

    public void save() {
        FileConfiguration fileConfig = Eden.INSTANCE.getArenaFile().getConfiguration();
        String arenaRoot = "arenas." + name;
        fileConfig.set(arenaRoot, null);

        fileConfig.set(arenaRoot + ".display-name", displayName);
        fileConfig.set(arenaRoot + ".icon", BukkitSerialization.itemStackToBase64(icon));
        fileConfig.set(arenaRoot + ".y-limit", yLimit);
        fileConfig.set(arenaRoot + ".build-max", buildMax);
        fileConfig.set(arenaRoot + ".portal-protection-radius", portalProtectionRadius);
        fileConfig.set(arenaRoot + ".kits", allowedKits);
        fileConfig.set(arenaRoot + ".enabled", enabled);

        if (!arenaDetails.isEmpty()) {
            for (int i = 0; i < arenaDetails.size(); i++) {
                ArenaDetail arenaDetail = arenaDetails.get(i);
                String arenaDetailsRoot = arenaRoot + ".details." + i;
                fileConfig.set(arenaDetailsRoot + ".a", LocationSerialization.serializeLocation(arenaDetail.getA()));
                fileConfig.set(arenaDetailsRoot + ".b", LocationSerialization.serializeLocation(arenaDetail.getB()));
                fileConfig.set(arenaDetailsRoot + ".spectator",
                        LocationSerialization.serializeLocation(arenaDetail.getSpectator()));
                fileConfig.set(arenaDetailsRoot + ".min",
                        LocationSerialization.serializeLocation(arenaDetail.getMin()));
                fileConfig.set(arenaDetailsRoot + ".max",
                        LocationSerialization.serializeLocation(arenaDetail.getMax()));
            }
        }

        // Optional parkour checkpoints (per-team)
        List<Location> cpsA = getParkourCheckpoints(true);
        List<Location> cpsB = getParkourCheckpoints(false);

        if (cpsA != null && !cpsA.isEmpty()) {
            List<String> serialized = new ArrayList<>();
            for (Location l : cpsA) {
                if (l != null) {
                    serialized.add(LocationSerialization.serializeLocation(l));
                }
            }
            fileConfig.set(arenaRoot + ".parkour-checkpoints-a", serialized);
        }

        if (cpsB != null && !cpsB.isEmpty()) {
            List<String> serialized = new ArrayList<>();
            for (Location l : cpsB) {
                if (l != null) {
                    serialized.add(LocationSerialization.serializeLocation(l));
                }
            }
            fileConfig.set(arenaRoot + ".parkour-checkpoints-b", serialized);
        }

        // Remove legacy key to avoid confusion (we can still read it for migration).
        fileConfig.set(arenaRoot + ".parkour-checkpoints", null);

        Eden.INSTANCE.getArenaFile().save();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Arena arena = (Arena) o;
        return java.util.Objects.equals(name, arena.name);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(name);
    }

}
