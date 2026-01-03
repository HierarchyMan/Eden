package rip.diamond.practice.config;

import com.google.common.collect.ImmutableList;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.Material;
import rip.diamond.practice.Eden;
import rip.diamond.practice.util.CC;
import rip.diamond.practice.util.BasicConfigFile;

import rip.diamond.practice.util.Util;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@AllArgsConstructor
public enum Config {

    DEBUG("debug", false),
    ARENA_KIT_AUTO_SAVE("arena-kit-auto-save", false),
    DISABLE_SAVE_WORLD("disable-save-world", true),
    LOBBY_ONLY_COMMANDS("lobby-only-commands", ImmutableList.of()),

    FANCY_TABLIST_ENABLED("fancy-tablist.enabled", true),
    FANCY_TABLIST_FORMAT("fancy-tablist.format", "&a{player-name}"),
    FANCY_TABLIST_UPDATE_TICKS("fancy-tablist.update-ticks", 20),

    NAMETAG_ENABLED("nametag.enabled", true),
    NAMETAG_PREFIX_LOBBY("nametag.prefix.lobby", "&9"),
    NAMETAG_PREFIX_SPECTATOR("nametag.prefix.spectator", "&7"),
    NAMETAG_PREFIX_TEAMMATE("nametag.prefix.teammate", "&a"),
    NAMETAG_PREFIX_OPPONENT("nametag.prefix.opponent", "&c"),
    NAMETAG_PREFIX_OTHER("nametag.prefix.other", "&e"),

    PARTY_DEFAULT_MAX_SIZE("party.default-max-size", 30),
    PARTY_ANNOUNCE_COOLDOWN("party.announce-cooldown", 10),

    LOBBY_DISPLAY_PLAYERS("lobby.display-players", true),

    QUEUE_RANKED_REQUIRED_WINS("queue.ranked-required-wins", 10),

    MATCH_ALLOW_PREFIRE("match.allow-prefire", true),
    MATCH_ALLOW_REQUEUE("match.allow-requeue", true),
    MATCH_OUTSIDE_CUBOID_INSTANT_DEATH("match.outside-cuboid-instant-death", true),
    MATCH_ABOVE_BUILDHEIGHT_DAMAGE_ENABLED("match.above-buildheight-damage.enabled", true),
    MATCH_ABOVE_BUILDHEIGHT_DAMAGE_THRESHOLD("match.above-buildheight-damage.threshold", 3),
    MATCH_ABOVE_BUILDHEIGHT_DAMAGE_DELAY("match.above-buildheight-damage.delay", 20),
    MATCH_ABOVE_BUILDHEIGHT_DAMAGE_AMOUNT("match.above-buildheight-damage.amount", 1.0),
    MATCH_REMOVE_CACTUS_SUGAR_CANE_PHYSICS("match.remove-cactus-sugar-cane-physics", true),
    MATCH_DEATH_LIGHTNING("match.death-lightning", true),
    MATCH_DEATH_ANIMATION("match.death-animation", true),
    MATCH_TP_2_BLOCKS_UP_WHEN_DIE("match.tp-2-blocks-up-when-die", false),
    MATCH_RESPAWN_TELEPORT_TO_SPAWN_WHEN_DIE("match.respawn-teleport-to-spawn-when-die", true),
    MATCH_WIN_COMMANDS("match.win-commands", ImmutableList.of()),
    MATCH_LOSE_COMMANDS("match.lose-commands", ImmutableList.of()),
    MATCH_START_SATURATION("match.start-saturation", 15),
    MATCH_SPECTATE_EXPEND_CUBOID("match.spectate-expend-cuboid", 50),
    MATCH_GOLDEN_APPLE_INSTANT_GAPPLE_EFFECTS("match.golden-apple.instant-gapple-effects", true),
    MATCH_GOLDEN_APPLE_GIVE_ABSORPTION_HEARTS_EVERYTIME("match.golden-apple.give-absorption-hearts-everytime", false),
    MATCH_SNOW_SNOWBALL_DROP_CHANCE("match.snow.snowball-drop-chance", 50),
    MATCH_SNOW_SNOWBALL_DROP_AMOUNT("match.snow.snowball-drop-amount", 4),
    MATCH_TITLE_SCORE("match.title.score", true),
    MATCH_TITLE_END("match.title.end", true),
    MATCH_END_DURATION("match.end-duration", 100),
    MATCH_ALLOW_BREAKING_BLOCKS("match.allow-breaking-blocks",
            ImmutableList.of("DEAD_BUSH", "GRASS", "LONG_GRASS", "CACTUS")),
    MATCH_FIREBALL_ENABLED("match.fireball.enabled", true),
    MATCH_FIREBALL_MAX_DAMAGE_SELF("match.fireball.max-damage-self", 2.0),
    MATCH_FIREBALL_MAX_DAMAGE_OTHERS("match.fireball.max-damage-others", 4.0),
    MATCH_FIREBALL_SPEED("match.fireball.speed", 2.0),
    MATCH_FIREBALL_YIELD("match.fireball.yield", 2.0),
    MATCH_FIREBALL_ALLOWED_BREAKING_BLOCKS("match.fireball.allowed-breaking-blocks",
            ImmutableList.of("WOOD", "BED_BLOCK")),
    MATCH_FIREBALL_PLACED_ONLY_BREAKING_BLOCKS("match.fireball.placed-only-breaking-blocks",
            ImmutableList.of("WOOL", "LADDER")),
    MATCH_FIREBALL_KNOCKBACK_ENABLED("match.fireball.knockback.enabled", true),
    MATCH_FIREBALL_KNOCKBACK_VERTICAL("match.fireball.knockback.vertical", 1.1),
    MATCH_FIREBALL_KNOCKBACK_HORIZONTAL("match.fireball.knockback.horizontal", 1.2),
    MATCH_TNT_ENABLED("match.tnt.enabled", true),
    MATCH_TNT_MAX_DAMAGE_SELF("match.tnt.max-damage-self", 2.0),
    MATCH_TNT_MAX_DAMAGE_OTHERS("match.tnt.max-damage-others", 6.0),

    MATCH_TNT_YIELD("match.tnt.yield", 2.0),
    MATCH_TNT_FUSE_TICKS("match.tnt.fuse-ticks", 50),
    MATCH_TNT_ALLOWED_BREAKING_BLOCKS("match.tnt.allowed-breaking-blocks", ImmutableList.of("WOOD", "BED_BLOCK")),
    MATCH_TNT_PLACED_ONLY_BREAKING_BLOCKS("match.tnt.placed-only-breaking-blocks", ImmutableList.of("WOOL", "LADDER")),
    MATCH_TNT_KNOCKBACK_ENABLED("match.tnt.knockback.enabled", true),
    MATCH_TNT_KNOCKBACK_VERTICAL("match.tnt.knockback.vertical", 1.1),
    MATCH_TNT_KNOCKBACK_HORIZONTAL("match.tnt.knockback.horizontal", 1.2),
    MATCH_INSTA_TNT_ENABLED("match.insta-boom-tnt.enabled", true),
    MATCH_INSTA_TNT_YIELD("match.insta-boom-tnt.yield", 3.0),
    MATCH_INSTA_TNT_MAX_DAMAGE_SELF("match.insta-boom-tnt.max-damage-self", 0.0),
    MATCH_INSTA_TNT_MAX_DAMAGE_OTHERS("match.insta-boom-tnt.max-damage-others", 4.0),
    MATCH_INSTA_TNT_ALLOWED_BREAKING_BLOCKS("match.insta-boom-tnt.allowed-breaking-blocks", 
            ImmutableList.of("WOOL")),
    MATCH_INSTA_TNT_PLACED_ONLY_BREAKING_BLOCKS("match.insta-boom-tnt.placed-only-breaking-blocks", 
            ImmutableList.of("STAINED_CLAY")),
    MATCH_INSTA_TNT_KNOCKBACK_ENABLED("match.insta-boom-tnt.knockback.enabled", true),
    MATCH_INSTA_TNT_KNOCKBACK_VERTICAL("match.insta-boom-tnt.knockback.vertical", 1.5),
    MATCH_INSTA_TNT_KNOCKBACK_HORIZONTAL("match.insta-boom-tnt.knockback.horizontal", 1.5),
    MATCH_INSTA_TNT_KNOCKBACK_GROUND_VERTICAL("match.insta-boom-tnt.knockback.ground-vertical", 0.15),
    MATCH_INSTA_TNT_KNOCKBACK_GROUND_HORIZONTAL("match.insta-boom-tnt.knockback.ground-horizontal", 0.5),
    MATCH_INSTA_TNT_KNOCKBACK_Y_LIMIT("match.insta-boom-tnt.knockback.y-limit", 256.0),
    MATCH_INSTA_TNT_KNOCKBACK_DISTANCE_THRESHOLD("match.insta-boom-tnt.knockback.distance-threshold", 3.0),
    MATCH_INSTA_TNT_KNOCKBACK_CONSTANT_HORIZONTAL("match.insta-boom-tnt.knockback.constant-horizontal", 0.8),
    MATCH_INSTA_TNT_RADIUS("match.insta-boom-tnt.radius", 5.0),
    MATCH_INSTA_TNT_PLACEMENT_COOLDOWN_MS("match.insta-boom-tnt.placement-cooldown-ms", 200),
    MATCH_GOLDEN_HEAD_EFFECTS("match.golden-head.effects",
            ImmutableList.of("REGENERATION;200;2", "ABSORPTION;2400;0", "SPEED;200;0")),
    MATCH_GOLDEN_HEAD_FOOD_LEVEL("match.golden-head.food-level", 6),
    MATCH_GOLDEN_HEAD_SATURATION_LEVEL("match.golden-head.saturation-level", 6),

    EVENT_SUMO_EVENT_ARENAS("event.sumo-event.arenas", ImmutableList.of("sumoevent")),
    EVENT_SUMO_EVENT_KIT("event.sumo-event.kit", "sumo"),

    CHAT_FORMAT_ENABLED("chat-format.enabled", true),
    CHAT_FORMAT_FORMAT("chat-format.format", "&a%1$s&f: %2$s"),

    PROFILE_DEFAULT_ELO("profile.default-elo", 1000),
    PROFILE_SAVE_ON_SERVER_STOP("profile.save-on-server-stop", true),
    PROFILE_DEFAULT_SETTINGS_TIME_CHANGER("profile.default-settings.time-changer", "normal"),
    PROFILE_DEFAULT_SETTINGS_ARENA_SELECTION("profile.default-settings.arena-selection", false),
    PROFILE_DEFAULT_SETTINGS_MATCH_SCOREBOARD("profile.default-settings.match-scoreboard", true),
    PROFILE_DEFAULT_SETTINGS_ALLOW_DUEL_REQUEST("profile.default-settings.allow-duel-request", true),
    PROFILE_DEFAULT_SETTINGS_ALLOW_PARTY_INVITE("profile.default-settings.allow-party-invite", true),
    PROFILE_DEFAULT_SETTINGS_SPECTATOR_VISIBILITY("profile.default-settings.spectator-visibility", true),
    PROFILE_DEFAULT_SETTINGS_SPECTATOR_JOIN_LEAVE_MESSAGE("profile.default-settings.spectator-join-leave-message",
            true),
    PROFILE_DEFAULT_SETTINGS_EVENT_ANNOUNCEMENT("profile.default-settings.event-announcement", true),
    PROFILE_DEFAULT_SETTINGS_PING_RANGE("profile.default-settings.ping-range", "infinite"),

    CRAFTING_ENABLED("crafting.enabled", false),
    CRAFTING_WHITELISTED_ITEMS("crafting.whitelisted-items", ImmutableList.of("MUSHROOM_SOUP")),

    IMANITY_TELEPORT_ASYNC("imanity.teleport-async", true),

    OPTIMIZATION_SET_BLOCK_FAST("optimization.set-block-fast", true),

    EXPERIMENT_DISABLE_ORIGINAL_ARENA("experiment.disable-original-arena", false),
    EXPERIMENT_K_FACTOR("experiment.k-factor", 32),
    ;

    @Getter
    private final String path;
    @Getter
    private final Object defaultValue;
    private static final Map<Config, Object> CACHE = new ConcurrentHashMap<>();

    public String toString() {
        return CC.translate(toStringRaw());
    }

    private String toStringRaw() {
        if (CACHE.containsKey(this)) {
            return String.valueOf(CACHE.get(this));
        }
        String value = Eden.INSTANCE.getConfigFile().getStringRaw(path);
        if (value.equals(path)) {
            value = defaultValue.toString();
        }
        CACHE.put(this, value);
        return value;
    }

    public List<String> toStringList() {
        if (CACHE.containsKey(this)) {
            return (List<String>) CACHE.get(this);
        }
        List<String> result = Eden.INSTANCE.getConfigFile().getRawStringList(path);
        // Only use default if the config key is MISSING (getRawStringList returns [path] when key doesn't exist)
        // An intentionally empty list [] should NOT fall back to default
        if (result.size() == 1 && result.get(0).equals(path)) {
            result = (List<String>) defaultValue;
        }
        List<String> colored = result.stream().map(CC::translate).collect(Collectors.toList());
        List<String> finalResult = ImmutableList.copyOf(colored);
        CACHE.put(this, finalResult);
        return finalResult;
    }

    public boolean toBoolean() {
        return Boolean.parseBoolean(toStringRaw());
    }

    public int toInteger() {
        return Integer.parseInt(toStringRaw());
    }

    public double toDouble() {
        return Double.parseDouble(toStringRaw());
    }

    public static void invalidateCache() {
        CACHE.clear();
    }

    public static void loadDefault() {
        BasicConfigFile configFile = Eden.INSTANCE.getConfigFile();

        boolean changed = ConfigUtil.addMissingKeys(configFile, Config.values());

        if (changed) {
            Eden.INSTANCE.getLogger().info("Updating config.yml with new keys...");
            configFile.load();
            invalidateCache();
        }
    }

}
