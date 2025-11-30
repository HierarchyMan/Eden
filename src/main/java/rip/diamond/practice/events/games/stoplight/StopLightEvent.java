package rip.diamond.practice.events.games.stoplight;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;
import rip.diamond.practice.Eden;
import rip.diamond.practice.events.EventCountdownTask;
import rip.diamond.practice.events.EventState;
import rip.diamond.practice.events.PracticeEvent;
import rip.diamond.practice.util.CC;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Getter
public class StopLightEvent extends PracticeEvent<StopLightPlayer> implements Listener {

    private final Map<UUID, StopLightPlayer> players = new HashMap<>();
    private StopLightCountdown eventCountdown;
    private StopLightGameTask gameTask;
    private boolean greenLight = false;
    private long lastStateChange;

    public StopLightEvent() {
        super("Stop Light");
        this.eventCountdown = new StopLightCountdown(this, 2);
    }

    @Override
    public Map<UUID, StopLightPlayer> getPlayers() {
        return players;
    }

    @Override
    public EventCountdownTask getCountdownTask() {
        
        if (eventCountdown == null || eventCountdown.isEnded()) {
            eventCountdown = new StopLightCountdown(this, 2);
        }
        return eventCountdown;
    }

    @Override
    public List<Location> getSpawnLocations() {
        return Collections.singletonList(plugin.getSpawnManager().getStoplightLocation());
    }

    @Override
    public void onStart() {
        setState(rip.diamond.practice.events.EventState.PLAYING);
        gameTask = new StopLightGameTask();
        gameTask.runTaskTimer(plugin, 0, 20L);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void end() {
        
        if (gameTask != null) {
            gameTask.cancel();
        }

        
        super.end();
    }

    @Override
    public Consumer<Player> onJoin() {
        return player -> players.put(player.getUniqueId(), new StopLightPlayer(player.getUniqueId(), this));
    }

    @Override
    public Consumer<Player> onDeath() {
        return player -> {
            StopLightPlayer data = getPlayer(player);
            if (data.getState() != StopLightPlayer.StopLightState.PLAYING) {
                return;
            }

            data.setState(StopLightPlayer.StopLightState.ELIMINATED);
            getPlayers().remove(player.getUniqueId());
            plugin.getEventManager().addSpectator(player,
                    rip.diamond.practice.profile.PlayerProfile.get(player), this);

            checkWin();
        };
    }

    private void checkWin() {
        List<StopLightPlayer> playing = getByState(StopLightPlayer.StopLightState.PLAYING);
        if (playing.size() <= 1) {
            if (!playing.isEmpty()) {
                Player winner = Bukkit.getPlayer(playing.get(0).getUuid());
                if (winner != null) {
                    handleWin(winner);
                }
            }
            end();
            if (gameTask != null)
                gameTask.cancel();
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!greenLight) {
            
            if (System.currentTimeMillis() - lastStateChange < 100) {
                return;
            }

            Player player = event.getPlayer();
            StopLightPlayer data = getPlayer(player);

            if (data != null && data.getState() == StopLightPlayer.StopLightState.PLAYING) {
                if (event.getFrom().getX() != event.getTo().getX() || event.getFrom().getZ() != event.getTo().getZ()) {
                    onDeath().accept(player);
                    player.sendMessage(CC.RED + "You moved during Red Light!");
                }
            }
        }
    }

    public List<StopLightPlayer> getByState(StopLightPlayer.StopLightState state) {
        return players.values().stream().filter(player -> player.getState() == state)
                .collect(Collectors.toList());
    }

    @Getter
    @RequiredArgsConstructor
    public class StopLightGameTask extends BukkitRunnable {

        private int time = 10;

        @Override
        public void run() {
            if (time <= 0) {
                greenLight = !greenLight;
                
                time = greenLight ? ThreadLocalRandom.current().nextInt(3, 7)
                        : ThreadLocalRandom.current().nextInt(2, 5);

                if (greenLight) {
                    sendMessage(CC.GREEN + "GREEN LIGHT! GO!");
                } else {
                    sendMessage(CC.RED + "RED LIGHT! STOP!");
                    lastStateChange = System.currentTimeMillis();
                }
            }

            time--;
        }
    }

    @Override
    public List<String> getScoreboard(Player player) {
        List<String> lines;

        if (getState() == EventState.WAITING || getState() == EventState.STARTING) {
            lines = new ArrayList<>(Eden.INSTANCE.getScoreboardFile().getLines("STOPLIGHT", "WAITING"));
        } else {
            
            lines = new ArrayList<>(Eden.INSTANCE.getScoreboardFile().getLines("STOPLIGHT", "PLAYING"));
        }

        
        List<String> replaced = new ArrayList<>();
        for (String line : lines) {
            
            line = line.replace("{event_name}", getName());
            line = line.replace("{event_players}", String.valueOf(getPlayers().size()));
            line = line.replace("{event_limit}", String.valueOf(getLimit()));
            line = line.replace("{event_host}", getHost() != null ? getHost().getName() : "None");

            
            if (getState() == EventState.PLAYING) {
                line = line.replace("{light_state}", greenLight ? "&aGreen Light" : "&cRed Light");
                line = line.replace("{players_remaining}",
                        String.valueOf(getByState(StopLightPlayer.StopLightState.PLAYING).size()));
                line = line.replace("{players_eliminated}",
                        String.valueOf(getByState(StopLightPlayer.StopLightState.ELIMINATED).size()));
            } else {
                line = line.replace("{light_state}", "Waiting...");
                line = line.replace("{players_remaining}", String.valueOf(getPlayers().size()));
                line = line.replace("{players_eliminated}", "0");
            }

            replaced.add(CC.translate(line));
        }

        return replaced;
    }

    public class StopLightCountdown extends EventCountdownTask {
        private final int requiredPlayers;

        public StopLightCountdown(PracticeEvent<?> event, int requiredPlayers) {
            super(event, 60);
            this.requiredPlayers = requiredPlayers;
        }

        @Override
        public boolean shouldAnnounce(int timeUntilStart) {
            return Arrays.asList(60, 45, 30, 15, 10, 5).contains(timeUntilStart);
        }

        @Override
        public boolean canStart() {
            return this.getEvent().getPlayers().size() >= requiredPlayers;
        }

        @Override
        public void onCancel() {
            this.getEvent().sendMessage(CC.RED + "There were not enough players to start the event.");
            this.getEvent().end();
        }
    }
}
