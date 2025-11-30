package rip.diamond.practice.events.games.dropper;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;
import rip.diamond.practice.Eden;
import rip.diamond.practice.events.EventCountdownTask;
import rip.diamond.practice.events.EventState;
import rip.diamond.practice.events.PracticeEvent;
import rip.diamond.practice.util.CC;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Getter
public class DropperEvent extends PracticeEvent<DropperPlayer> implements Listener {

    private final Map<UUID, DropperPlayer> players = new HashMap<>();
    private DropperCountdown eventCountdown;
    private DropperGameTask gameTask;

    public DropperEvent() {
        super("Dropper");
        this.eventCountdown = new DropperCountdown(this, 2);
    }

    @Override
    public Map<UUID, DropperPlayer> getPlayers() {
        return players;
    }

    @Override
    public EventCountdownTask getCountdownTask() {
        
        if (eventCountdown == null || eventCountdown.isEnded()) {
            eventCountdown = new DropperCountdown(this, 2);
        }
        return eventCountdown;
    }

    @Override
    public List<Location> getSpawnLocations() {
        return Collections.singletonList(plugin.getSpawnManager().getDropperLocation());
    }

    @Override
    public void onStart() {
        gameTask = new DropperGameTask();
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
        return player -> players.put(player.getUniqueId(), new DropperPlayer(player.getUniqueId(), this));
    }

    @Override
    public Consumer<Player> onDeath() {
        return player -> {
            DropperPlayer data = getPlayer(player);
            if (data.getState() != DropperPlayer.DropperState.PLAYING) {
                return;
            }

            
            List<Location> maps = plugin.getSpawnManager().getDropperMaps();
            if (data.getMapIndex() < maps.size()) {
                player.teleport(maps.get(data.getMapIndex()));
            } else {
                player.teleport(getSpawnLocations().get(0));
            }
        };
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            DropperPlayer data = getPlayer(player);

            if (data != null && data.getState() == DropperPlayer.DropperState.PLAYING) {
                if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
                    event.setCancelled(true);
                    
                    Block block = player.getLocation().getBlock();
                    if (block.getType() == Material.STATIONARY_WATER || block.getType() == Material.WATER) {
                        handleLevelComplete(player, data);
                    } else {
                        onDeath().accept(player);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        DropperPlayer data = getPlayer(player);

        if (data != null && data.getState() == DropperPlayer.DropperState.PLAYING) {
            
            Block block = player.getLocation().getBlock();
            if ((block.getType() == Material.STATIONARY_WATER || block.getType() == Material.WATER)
                    && player.getVelocity().getY() < -0.1) {
                handleLevelComplete(player, data);
            }
        }
    }

    private void handleLevelComplete(Player player, DropperPlayer data) {
        
        if (data.getState() != DropperPlayer.DropperState.PLAYING) {
            return;
        }

        data.setMapIndex(data.getMapIndex() + 1);
        List<Location> maps = plugin.getSpawnManager().getDropperMaps();

        if (data.getMapIndex() >= maps.size()) {
            data.setState(DropperPlayer.DropperState.FINISHED);
            sendMessage(CC.GREEN + player.getName() + " has finished the Dropper!");
            handleWin(player);
            end();
        } else {
            player.teleport(maps.get(data.getMapIndex()));
            player.sendMessage(CC.GREEN + "Level completed! Moving to next level...");
        }
    }

    @Getter
    @RequiredArgsConstructor
    public class DropperGameTask extends BukkitRunnable {

        private int time = 300;

        @Override
        public void run() {
            if (time == 300) {
                sendMessage(plugin.getLanguageFile().getConfiguration()
                        .getString("EVENT.STARTED"));

                
                setState(rip.diamond.practice.events.EventState.PLAYING);

                List<Location> maps = plugin.getSpawnManager().getDropperMaps();
                if (!maps.isEmpty()) {
                    getPlayers().forEach((uuid, player) -> {
                        player.setState(DropperPlayer.DropperState.PLAYING);
                        Bukkit.getPlayer(uuid).teleport(maps.get(0));
                    });
                }
            } else if (time <= 0) {
                end();
                cancel();
                return;
            }

            time--;
        }
    }

    @Override
    public List<String> getScoreboard(Player player) {
        List<String> lines;

        if (getState() == EventState.WAITING || getState() == EventState.STARTING) {
            lines = new ArrayList<>(Eden.INSTANCE.getScoreboardFile().getLines("DROPPER", "WAITING"));
        } else {
            
            lines = new ArrayList<>(Eden.INSTANCE.getScoreboardFile().getLines("DROPPER", "PLAYING"));
        }

        
        List<String> replaced = new ArrayList<>();
        for (String line : lines) {
            
            line = line.replace("{event_name}", getName());
            line = line.replace("{event_players}", String.valueOf(getPlayers().size()));
            line = line.replace("{event_limit}", String.valueOf(getLimit()));
            line = line.replace("{event_host}", getHost() != null ? getHost().getName() : "None");

            
            if (getState() == EventState.PLAYING) {
                DropperPlayer data = getPlayer(player);
                if (data != null) {
                    line = line.replace("{map_number}", String.valueOf(data.getMapIndex() + 1));
                } else {
                    line = line.replace("{map_number}", "1");
                }
                line = line.replace("{players_finished}",
                        String.valueOf(getByState(DropperPlayer.DropperState.FINISHED).size()));
                line = line.replace("{players_playing}",
                        String.valueOf(getByState(DropperPlayer.DropperState.PLAYING).size()));
            } else {
                line = line.replace("{map_number}", "1");
                line = line.replace("{players_finished}", "0");
                line = line.replace("{players_playing}", String.valueOf(getPlayers().size()));
            }

            replaced.add(CC.translate(line));
        }

        return replaced;
    }

    public List<DropperPlayer> getByState(DropperPlayer.DropperState state) {
        return players.values().stream().filter(player -> player.getState() == state)
                .collect(Collectors.toList());
    }

    public class DropperCountdown extends EventCountdownTask {
        private final int requiredPlayers;

        public DropperCountdown(PracticeEvent<?> event, int requiredPlayers) {
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
