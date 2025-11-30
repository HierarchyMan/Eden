package rip.diamond.practice.events;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import rip.diamond.practice.Eden;
import rip.diamond.practice.events.games.skywars.SkyWarsEvent;
import rip.diamond.practice.events.games.spleef.SpleefEvent;
import rip.diamond.practice.kits.Kit;
import rip.diamond.practice.profile.PlayerProfile;
import rip.diamond.practice.profile.PlayerState;
import rip.diamond.practice.profile.ProfileSettings;
import rip.diamond.practice.util.CC;
import rip.diamond.practice.util.Clickable;
import rip.diamond.practice.util.PlayerUtil;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Getter
@Setter
@RequiredArgsConstructor
public abstract class PracticeEvent<K extends EventPlayer> {

    protected final Eden plugin = Eden.INSTANCE;

    private final String name;

    private Player host;
    private int limit = 30;
    private Optional<Kit> kitOptional = Optional.empty();

    private List<UUID> playersX = new ArrayList<>();
    private EventState state = EventState.UNANNOUNCED;

    public void startCountdown() {
        EventCountdownTask task = getCountdownTask();

        task.setTimeUntilStart(task.getCountdownTime());
        task.setEnded(false);

        try {
            task.runTaskTimer(plugin, 0L, 20L);
        } catch (IllegalStateException e) {

            plugin.getLogger().warning("Event countdown task for " + getName()
                    + " is already scheduled! This may prevent the event from starting.");
        }
    }

    public void sendMessage(String message) {
        for (Player player : getBukkitPlayers()) {
            player.sendMessage(CC.translate(message));
        }
    }

    public Set<Player> getSpectators() {
        Set<Player> spectators = new HashSet<>();
        plugin.getEventManager().getSpectators().forEach((uuid, event) -> {
            if (event == this) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null)
                    spectators.add(p);
            }
        });
        return spectators;
    }

    public Set<Player> getBukkitPlayers() {
        return getPlayers().keySet().stream()
                .filter(uuid -> plugin.getServer().getPlayer(uuid) != null)
                .map(plugin.getServer()::getPlayer)
                .collect(Collectors.toSet());
    }

    public void broadcastToEvent(String message) {
        sendMessage(message);
    }

    public void join(Player player) {

        if (!(state == EventState.WAITING || state == EventState.STARTING)) {
            player.sendMessage(CC.RED + "This event is not accepting players.");
            return;
        }
        if (getPlayers().containsKey(player.getUniqueId())) {
            player.sendMessage(CC.RED + "You are already in this event.");
            return;
        }
        if (getPlayers().size() >= limit) {
            player.sendMessage(CC.RED + "Event is full.");
            return;
        }
        if (getSpawnLocations() == null || getSpawnLocations().isEmpty() || getSpawnLocations().get(0) == null) {
            player.sendMessage(CC.RED + "Event spawn not configured. Contact staff.");
            return;
        }

        if (getCountdownTask() != null && !getCountdownTask().isEnded()
                && getCountdownTask().getTimeUntilStart() <= 1) {
            player.sendMessage(CC.RED + "The event is starting too soon to join.");
            return;
        }

        playersX.add(player.getUniqueId());

        PlayerProfile profile = PlayerProfile.get(player);
        profile.setPlayerState(PlayerState.IN_EVENT);

        PlayerUtil.reset(player);

        if (onJoin() != null) {
            onJoin().accept(player);
        }

        if (getSpawnLocations().size() == 1) {
            player.teleport(getSpawnLocations().get(0));
        } else {
            player.teleport(getSpawnLocations().get(ThreadLocalRandom.current().nextInt(getSpawnLocations().size())));
        }

        profile.giveItems();

        rip.diamond.practice.util.VisibilityController.updateVisibility(player);

        String joinMsg = plugin.getLanguageFile().getConfiguration().getString("event.event-join.message");
        if (joinMsg != null) {
            broadcastToEvent(joinMsg
                    .replace("{0}", player.getName())
                    .replace("{1}", name)
                    .replace("{2}", String.valueOf(getPlayers().size()))
                    .replace("{3}", String.valueOf(limit)));
        }

        if (getPlayers().size() == limit && state == EventState.WAITING) {
            broadcastToEvent(plugin.getLanguageFile().getConfiguration().getString("event.starting-full"));
            getCountdownTask().setTimeUntilStart(5);
        }
    }

    public void leave(Player player) {
        if (!getPlayers().containsKey(player.getUniqueId())) {
            return;
        }

        getPlayers().remove(player.getUniqueId());
        playersX.remove(player.getUniqueId());

        plugin.getLobbyManager().sendToSpawnAndReset(player);

        String leaveMsg = plugin.getLanguageFile().getConfiguration().getString("event.event-leave.message");
        if (leaveMsg != null) {
            broadcastToEvent(leaveMsg
                    .replace("{0}", player.getName())
                    .replace("{1}", name)
                    .replace("{2}", String.valueOf(getPlayers().size()))
                    .replace("{3}", String.valueOf(limit)));
        }

        if (getPlayers().isEmpty() && (state == EventState.WAITING || state == EventState.STARTING)) {
            end();
        }
    }

    public void broadcastEventMessage(String raw) {
        if (raw == null)
            return;
        String message = CC.translate(raw);
        for (Player online : Bukkit.getOnlinePlayers()) {
            PlayerProfile prof = PlayerProfile.get(online);
            if (prof == null)
                continue;
            if (prof.getSettings().get(ProfileSettings.EVENT_ANNOUNCEMENT).isEnabled()) {
                online.sendMessage(message);
            }
        }
    }

    public void broadcastEventClickable(Clickable clickable) {
        if (clickable == null)
            return;
        for (Player online : Bukkit.getOnlinePlayers()) {
            PlayerProfile prof = PlayerProfile.get(online);
            if (prof == null)
                continue;

            if (prof.getSettings().get(ProfileSettings.EVENT_ANNOUNCEMENT).isEnabled()
                    && !getPlayers().containsKey(online.getUniqueId())) {
                clickable.sendToPlayer(online);
            } else if (getPlayers().containsKey(online.getUniqueId())) {

                online.sendMessage(clickable.getText());
            }
        }
    }

    public void start() {
        if (host == null) {
            Bukkit.getLogger().warning("Attempted to start event " + name + " without a host.");
            end();
            return;
        }

        if (state == EventState.STARTING || state == EventState.STARTED || state == EventState.PLAYING) {
            return;
        }

        if (getSpawnLocations().isEmpty() || getSpawnLocations().contains(null)) {
            host.sendMessage(CC.RED + "Event could not start because spawn locations are not configured.");
            end();
            return;
        }

        getCountdownTask().cancelCountdown();

        setState(EventState.STARTING);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            setState(EventState.STARTED);

            for (Player p : getBukkitPlayers()) {
                applyEventKnockback(p);

                if (kitOptional.isPresent() && kitOptional.get().getGameRules().isShowHealth()) {
                    plugin.getScoreboardHandler().getScoreboard(p).registerHealthObjective();
                }
            }
            onStart();

            for (Player p : getBukkitPlayers()) {
                rip.diamond.practice.profile.PlayerProfile profile = rip.diamond.practice.profile.PlayerProfile.get(p);
                if (profile != null) {
                    profile.setEventsPlayed(profile.getEventsPlayed() + 1);
                    profile.save(true, (success) -> {
                    });
                    Eden.INSTANCE.getLeaderboardManager().updateEventStats(profile);
                }
            }
        }, 1L);
    }

    public List<String> applyCommonPlaceholders(List<String> lines, Player player) {
        List<String> replaced = new ArrayList<>();
        for (String line : lines) {
            line = line.replace("%event_host%", host == null ? "None" : host.getName())
                    .replace("%event_players%", String.valueOf(getPlayers().size()))
                    .replace("%event_limit%", String.valueOf(limit))
                    .replace("%event_name%", name);
            replaced.add(CC.translate(line));
        }
        return replaced;
    }

    private void applyEventKnockback(Player player) {
        String kbProfile = plugin.getConfig().getString("event.default-event-kb", "default");

        if (kitOptional.isPresent()) {
            kbProfile = kitOptional.get().getGameRules().getKnockbackName();
        }

        plugin.getSpigotAPI().getKnockback().applyKnockback(player, kbProfile);
    }

    public void handleWin(Player winner) {
        String message = plugin.getLanguageFile().getConfiguration()
                .getString("event.winner-announce.message");
        if (message != null) {
            broadcastEventMessage(message.replace("{0}", winner.getName()));
        }

        rip.diamond.practice.profile.PlayerProfile profile = rip.diamond.practice.profile.PlayerProfile.get(winner);
        if (profile != null) {
            profile.incrementEventWins();
            profile.save(true, (success) -> {
            });
            Eden.INSTANCE.getLeaderboardManager().updateEventStats(profile);
        }
    }

    public void end() {
        if (this instanceof Listener) {
            HandlerList.unregisterAll((Listener) this);
        }

        if (this instanceof SkyWarsEvent) {
            if (((SkyWarsEvent) this).getArenaCuboid() != null) {
                plugin.getChunkRestorationManager().reset(((SkyWarsEvent) this).getArenaCuboid());
            }
        } else if (this instanceof SpleefEvent) {
            if (((SpleefEvent) this).getArenaCuboid() != null) {
                plugin.getChunkRestorationManager().reset(((SpleefEvent) this).getArenaCuboid());
            }
        }

        if (kitOptional.isPresent() && kitOptional.get().getGameRules().isShowHealth()) {
            for (Player player : getBukkitPlayers()) {
                plugin.getScoreboardHandler().getScoreboard(player).unregisterHealthObjective();
            }
        }

        if (getSpawnLocations() != null && !getSpawnLocations().isEmpty()) {
            Location center = getSpawnLocations().get(0);
            if (center != null && center.getWorld() != null) {
                center.getWorld().getEntities().stream()
                        .filter(entity -> entity instanceof Item)
                        .filter(entity -> entity.getLocation().distance(center) < 150)
                        .forEach(Entity::remove);
            }
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (UUID uuid : new ArrayList<>(playersX)) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) {
                    plugin.getLobbyManager().sendToSpawnAndReset(p);
                }
            }

            for (Player spec : new ArrayList<>(getSpectators())) {
                plugin.getLobbyManager().sendToSpawnAndReset(spec);
            }

            for (Player p : Bukkit.getOnlinePlayers()) {
                rip.diamond.practice.util.VisibilityController.updateVisibility(p);
            }

            playersX.clear();
            getPlayers().clear();
        }, 2L);

        plugin.getEventManager().setLastEvent(this);
        plugin.getEventManager().getSpectators().clear();

        setState(EventState.UNANNOUNCED);

        if (getCountdownTask() != null) {
            getCountdownTask().cancelCountdown();
        }
    }

    public Location getFirstLocation() {
        return this.getSpawnLocations().isEmpty() ? null : this.getSpawnLocations().get(0);
    }

    /**
     * Safely retrieves a language string from the configuration.
     * Returns the default value if the configuration is null or the key is not
     * found.
     *
     * @param path         The configuration path
     * @param defaultValue The default value to return if not found
     * @return The language string or default value
     */
    protected String getLanguageString(String path, String defaultValue) {
        if (plugin == null || plugin.getLanguageFile() == null || plugin.getLanguageFile().getConfiguration() == null) {
            return defaultValue;
        }
        return plugin.getLanguageFile().getConfiguration().getString(path, defaultValue);
    }

    public K getPlayer(Player player) {
        return getPlayer(player.getUniqueId());
    }

    public K getPlayer(UUID uuid) {
        return getPlayers().get(uuid);
    }

    /**
     * Determines if an arrow shot by a shooter can hit/damage the target player.
     * By default, arrows can always hit players in the event.
     * Subclasses can override this to prevent arrows from hitting spectators,
     * respawning players, or other non-combatant states.
     *
     * @param shooter the player who shot the arrow
     * @param target  the player being targeted
     * @return true if the arrow can penetrate/hit the target, false if it should
     *         pass through
     */
    public boolean canArrowPenetrate(Player shooter, Player target) {

        return true;
    }

    public boolean canDropItems() {
        return false;
    }

    public boolean canPickupItems() {
        return false;
    }

    public abstract Map<UUID, K> getPlayers();

    public abstract EventCountdownTask getCountdownTask();

    public abstract List<Location> getSpawnLocations();

    public abstract void onStart();

    public abstract Consumer<Player> onJoin();

    public abstract Consumer<Player> onDeath();

    public abstract List<String> getScoreboard(Player player);

    public void toggleVisibility(Player player) {
        boolean isVisible = PlayerProfile.get(player).getSettings().get(ProfileSettings.SPECTATOR_VISIBILITY)
                .isEnabled();
        for (Player spec : getSpectators()) {
            if (!spec.equals(player)) {
                PlayerUtil.hideOrShowPlayer(spec, player, !isVisible);
            }
        }
    }
}
