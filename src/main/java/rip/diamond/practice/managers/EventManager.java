package rip.diamond.practice.managers;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import rip.diamond.practice.Eden;
import rip.diamond.practice.events.EventState;
import rip.diamond.practice.events.PracticeEvent;
import rip.diamond.practice.events.games.brackets.BracketsEvent;
import rip.diamond.practice.events.games.corners.FourCornersEvent;
import rip.diamond.practice.events.games.dropper.DropperEvent;
import rip.diamond.practice.events.games.gulag.GulagEvent;
import rip.diamond.practice.events.games.knockout.KnockoutEvent;
import rip.diamond.practice.events.games.lms.LMSEvent;
import rip.diamond.practice.events.games.oitc.OITCEvent;
import rip.diamond.practice.events.games.parkour.ParkourEvent;
import rip.diamond.practice.events.games.skywars.SkyWarsEvent;
import rip.diamond.practice.events.games.spleef.SpleefEvent;
import rip.diamond.practice.events.games.stoplight.StopLightEvent;
import rip.diamond.practice.events.games.sumo.SumoEvent;
import rip.diamond.practice.events.games.thimble.ThimbleEvent;
import rip.diamond.practice.events.games.tnttag.TNTTagEvent;
import rip.diamond.practice.kits.Kit;
import rip.diamond.practice.profile.PlayerProfile;
import rip.diamond.practice.profile.PlayerState;
import rip.diamond.practice.util.CC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Getter
public class EventManager {

    private final Eden plugin = Eden.INSTANCE;

    private final Map<Class<? extends PracticeEvent>, PracticeEvent<?>> events = new HashMap<>();
    private final HashMap<UUID, PracticeEvent<?>> spectators;
    @Setter
    private PracticeEvent<?> lastEvent;
    private final World eventWorld;

    public EventManager() {

        events.put(SumoEvent.class, new SumoEvent());
        events.put(LMSEvent.class, new LMSEvent());
        events.put(KnockoutEvent.class, new KnockoutEvent());
        events.put(GulagEvent.class, new GulagEvent());
        events.put(BracketsEvent.class, new BracketsEvent());
        events.put(OITCEvent.class, new OITCEvent());
        events.put(TNTTagEvent.class, new TNTTagEvent());
        events.put(FourCornersEvent.class, new FourCornersEvent());
        events.put(StopLightEvent.class, new StopLightEvent());
        events.put(SkyWarsEvent.class, new SkyWarsEvent());
        events.put(SpleefEvent.class, new SpleefEvent());
        events.put(ThimbleEvent.class, new ThimbleEvent());
        events.put(DropperEvent.class, new DropperEvent());
        events.put(ParkourEvent.class, new ParkourEvent());

        boolean newWorld;

        if (plugin.getServer().getWorld("event") == null) {
            eventWorld = plugin.getServer()
                    .createWorld(new WorldCreator("event").type(WorldType.FLAT).generatorSettings("2;0;1;"));
            newWorld = true;
        } else {
            eventWorld = plugin.getServer().getWorld("event");
            newWorld = false;
        }

        this.spectators = new HashMap<>();

        if (eventWorld != null) {
            if (newWorld) {
                plugin.getServer().getWorlds().add(eventWorld);
            }
            eventWorld.setTime(6000L);
            eventWorld.setGameRuleValue("doDaylightCycle", "false");
            eventWorld.setGameRuleValue("doMobSpawning", "false");
            eventWorld.setStorm(false);
            eventWorld.getEntities().stream().filter(entity -> !(entity instanceof Player))
                    .forEach(Entity::remove);
        }
    }

    public PracticeEvent<?> getOngoingEvent() {
        return this.events.values().stream().filter(event -> event.getState() != EventState.UNANNOUNCED)
                .findFirst().orElse(null);
    }

    public PracticeEvent<?> getByName(String name) {
        return events.values().stream().filter(event -> event.getName().equalsIgnoreCase(name))
                .findFirst().orElse(null);
    }

    public void hostEvent(PracticeEvent<?> event, Player host) {
        if (!validateEvent(event, host, true)) {
            return;
        }
        event.setState(EventState.WAITING);
        event.setHost(host);
        event.startCountdown();

        announceEvent(event);
    }

    public void hostEvent(PracticeEvent<?> event, Kit kit, int limit, Player host) {
        if (!validateEvent(event, host, true)) {
            return;
        }
        event.setState(EventState.WAITING);
        event.setKitOptional(Optional.ofNullable(kit));
        event.setLimit(limit);
        event.setHost(host);
        event.startCountdown();

        announceEvent(event);
    }

    /**
     * Broadcasts an event announcement to all online players
     */
    private void announceEvent(PracticeEvent<?> event) {
        String message = plugin.getLanguageFile().getConfiguration()
                .getString("EVENT.ANNOUNCEMENT-MESSAGE",
                        "&e[Event] &a<eventName> &ehosted by &a<host> &eis waiting for players! &7(&a<players>&7/&a<maxPlayers>&7)")
                .replace("<host>", event.getHost().getName())
                .replace("<maxPlayers>", String.valueOf(event.getLimit()))
                .replace("<players>", String.valueOf(event.getPlayers().size()))
                .replace("<eventName>", event.getName());

        String hoverText = plugin.getLanguageFile().getConfiguration()
                .getString("EVENT.ANNOUNCEMENT-HOVER", "&eClick to join the event!");

        rip.diamond.practice.util.Clickable clickable = new rip.diamond.practice.util.Clickable(message);
        clickable.add("&a&l[CLICK TO JOIN]", hoverText, "/joinevent");

        event.broadcastEventClickable(clickable);
    }

    /**
     * Validates if an event is set up correctly.
     * 
     * @param event  The event to check
     * @param player The player to notify (can be null if notify is false)
     * @param notify Whether to send a message to the player
     * @return true if valid, false otherwise
     */
    public boolean validateEvent(PracticeEvent<?> event, Player player, boolean notify) {
        List<String> missing = new ArrayList<>();

        List<Location> spawns = event.getSpawnLocations();
        if (spawns == null || spawns.isEmpty()) {
            missing.add("Primary Spawn(s)");
        } else {
            if (spawns.contains(null)) {
                missing.add("A Null Spawn Location");
            }
        }

        if (event instanceof SumoEvent) {
            if (plugin.getSpawnManager().getSumoFirst() == null)
                missing.add("Sumo Pos 1");
            if (plugin.getSpawnManager().getSumoSecond() == null)
                missing.add("Sumo Pos 2");
        } else if (event instanceof SkyWarsEvent) {
            if (plugin.getSpawnManager().getSkywarsMin() == null || plugin.getSpawnManager().getSkywarsMax() == null) {
                missing.add("SkyWars Cuboid");
            }
        } else if (event instanceof SpleefEvent) {
            if (plugin.getSpawnManager().getSpleefMin() == null || plugin.getSpawnManager().getSpleefMax() == null) {
                missing.add("Spleef Cuboid");
            }
        }

        if (!missing.isEmpty()) {
            if (notify && player != null) {
                player.sendMessage(CC.translate("&c&lEvent Setup Error: &f" + event.getName()));
                player.sendMessage(CC.translate("&cMissing Locations: &f" + String.join(", ", missing)));
                player.sendMessage(CC.translate("&cPlease contact an administrator to set these locations."));
            }
            return false;
        }
        return true;
    }

    public void addSpectator(Player player, PlayerProfile profile, PracticeEvent<?> event) {
        if (event.getState() == EventState.UNANNOUNCED) {
            return;
        }

        this.spectators.put(player.getUniqueId(), event);
        event.sendMessage(
                CC.translate(plugin.getLanguageFile().getConfiguration().getString("MESSAGES.EVENT.SPECTATOR-JOIN")
                        .replace("<player>", player.getName())
                        .replace("<eventName>", event.getName())));

        profile.setPlayerState(PlayerState.IN_EVENT);
        player.teleport(event.getFirstLocation());

        player.setAllowFlight(true);
        player.setFlying(true);

        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.setHealth(player.getMaxHealth());
        player.setSaturation(20.0F);

        profile.giveItems();

        player.updateInventory();
        rip.diamond.practice.util.VisibilityController.updateVisibility(player);
    }

    public void removeSpectator(Player player, PracticeEvent<?> event) {
        event.sendMessage(
                CC.translate(plugin.getLanguageFile().getConfiguration().getString("MESSAGES.EVENT.SPECTATOR-LEAVE")
                        .replace("<player>", player.getName())
                        .replace("<eventName>", event.getName())));

        this.spectators.remove(player.getUniqueId(), event);
        plugin.getLobbyManager().resetPlayerOrSpawn(player, true);
    }

    public boolean isSpectating(Player player) {
        return this.spectators.containsKey(player.getUniqueId());
    }

    public boolean isPlaying(Player player, PracticeEvent<?> event) {
        return event.getPlayers().containsKey(player.getUniqueId());
    }

    public PracticeEvent<?> getEventPlaying(Player player) {
        return this.events.values().stream().filter(event -> this.isPlaying(player, event)).findFirst()
                .orElse(null);
    }
}
