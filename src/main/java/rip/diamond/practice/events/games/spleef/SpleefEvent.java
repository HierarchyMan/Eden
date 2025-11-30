package rip.diamond.practice.events.games.spleef;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.scheduler.BukkitRunnable;
import rip.diamond.practice.events.EventCountdownTask;
import rip.diamond.practice.events.PracticeEvent;
import rip.diamond.practice.util.CC;
import rip.diamond.practice.util.cuboid.Cuboid;
import org.bukkit.inventory.ItemStack;
import rip.diamond.practice.events.EventLoadout;
import rip.diamond.practice.util.PlayerUtil;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import rip.diamond.practice.Eden;
import rip.diamond.practice.events.EventState;

@Getter
public class SpleefEvent extends PracticeEvent<SpleefPlayer> implements Listener {

    private final Map<UUID, SpleefPlayer> players = new HashMap<>();
    private SpleefCountdown eventCountdown;
    private SpleefGameTask gameTask;
    private Cuboid arenaCuboid;
    private Player winner;
    private List<SpleefPlayer> topPlayers = new ArrayList<>();

    public SpleefEvent() {
        super("Spleef");
        this.eventCountdown = new SpleefCountdown(this, 2);
    }

    @Override
    public Map<UUID, SpleefPlayer> getPlayers() {
        return players;
    }

    @Override
    public EventCountdownTask getCountdownTask() {
        
        if (eventCountdown == null || eventCountdown.isEnded()) {
            eventCountdown = new SpleefCountdown(this, 2);
        }
        return eventCountdown;
    }

    @Override
    public List<Location> getSpawnLocations() {
        return Collections.singletonList(plugin.getSpawnManager().getSpleefLocation());
    }

    @Override
    public List<String> getScoreboard(Player player) {
        List<String> lines;

        if (getState() == EventState.WAITING || getState() == EventState.STARTING) {
            lines = new ArrayList<>(Eden.INSTANCE.getScoreboardFile().getLines("SPLEEF", "WAITING"));
        } else if (getState() == EventState.ENDING) {
            lines = new ArrayList<>(Eden.INSTANCE.getScoreboardFile().getLines("SPLEEF", "ENDING"));
        } else {
            
            lines = new ArrayList<>(Eden.INSTANCE.getScoreboardFile().getLines("SPLEEF", "PLAYING"));
        }

        
        List<String> replaced = new ArrayList<>();
        for (String line : lines) {
            
            line = line.replace("{event_name}", getName());
            line = line.replace("{event_players}", String.valueOf(getPlayers().size()));
            line = line.replace("{event_limit}", String.valueOf(getLimit()));
            line = line.replace("{event_host}", getHost() != null ? getHost().getName() : "None");

            
            if (getState() == EventState.PLAYING) {
                line = line.replace("{players_remaining}",
                        String.valueOf(getByState(SpleefPlayer.SpleefState.FIGHTING).size()));
                line = line.replace("{players_eliminated}",
                        String.valueOf(getByState(SpleefPlayer.SpleefState.ELIMINATED).size()));

                if (gameTask != null) {
                    int minutes = gameTask.time / 60;
                    int seconds = gameTask.time % 60;
                    line = line.replace("{time}", String.format("%d:%02d", minutes, seconds));
                } else {
                    line = line.replace("{time}", "0:00");
                }
            } else if (getState() == EventState.ENDING) {
                line = line.replace("{winner_name}", winner != null ? winner.getName() : "None");
                line = line.replace("{players_remaining}", "0");
                line = line.replace("{players_eliminated}", String.valueOf(getPlayers().size()));
                line = line.replace("{time}", "0:00");
            } else {
                line = line.replace("{players_remaining}", String.valueOf(getPlayers().size()));
                line = line.replace("{players_eliminated}", "0");
                line = line.replace("{time}", "0:00");
            }

            replaced.add(CC.translate(line));
        }

        return replaced;
    }

    @Override
    public void onStart() {
        
        Location min = plugin.getSpawnManager().getSpleefMin();
        Location max = plugin.getSpawnManager().getSpleefMax();
        if (min != null && max != null) {
            this.arenaCuboid = new Cuboid(min, max);
            plugin.getChunkRestorationManager().copy(this.arenaCuboid);
        }

        gameTask = new SpleefGameTask();
        gameTask.runTaskTimer(plugin, 0, 20L);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void preEnd(Player winner) {
        if (getState() == EventState.ENDING)
            return;
        this.winner = winner;
        setState(EventState.ENDING);

        if (winner != null) {
            handleWin(winner);
        }

        
        Location lobby = getSpawnLocations().isEmpty() ? null : getSpawnLocations().get(0);
        if (lobby != null) {
            for (Player p : getBukkitPlayers()) {
                p.teleport(lobby);
                PlayerUtil.reset(p);
            }
        }

        Bukkit.getScheduler().runTaskLater(plugin, this::end, 60L);
    }

    @Override
    public void end() {
        if (gameTask != null) {
            gameTask.cancel();
        }

        super.end();
        if (this.arenaCuboid != null) {
            plugin.getChunkRestorationManager().reset(this.arenaCuboid);
        }
    }

    @Override
    public Consumer<Player> onJoin() {
        return player -> players.put(player.getUniqueId(), new SpleefPlayer(player.getUniqueId(), this));
    }

    @Override
    public Consumer<Player> onDeath() {
        return player -> {
            SpleefPlayer data = getPlayer(player);
            if (data.getState() != SpleefPlayer.SpleefState.FIGHTING) {
                return;
            }

            Player killer = player.getKiller();
            data.setState(SpleefPlayer.SpleefState.ELIMINATED);

            
            

            plugin.getEventManager().addSpectator(player,
                    rip.diamond.practice.profile.PlayerProfile.get(player), this);

            sendMessage(ChatColor.RED + player.getName() + ChatColor.GRAY + " was eliminated" +
                    (killer == null ? "." : " by " + ChatColor.RED + killer.getName()) + ChatColor.GRAY + ".");

            checkForWin();
        };
    }

    private void checkForWin() {
        List<UUID> fighting = getByState(SpleefPlayer.SpleefState.FIGHTING);
        if (fighting.size() <= 1) {
            Player winner = fighting.isEmpty() ? null : Bukkit.getPlayer(fighting.get(0));
            preEnd(winner);
        }
    }

    @Override
    public void leave(Player player) {
        SpleefPlayer spleefPlayer = getPlayer(player);
        if (spleefPlayer != null) {
            if (spleefPlayer.getState() == SpleefPlayer.SpleefState.FIGHTING) {
                spleefPlayer.setState(SpleefPlayer.SpleefState.ELIMINATED);
                sendMessage(ChatColor.RED + player.getName() + ChatColor.GRAY + " disconnected.");
            }
        }

        super.leave(player);

        if (getState() == EventState.PLAYING) {
            checkForWin();
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        SpleefPlayer data = getPlayer(player);

        if (data != null) {
            if (getState() != EventState.PLAYING) {
                event.setCancelled(true);
                return;
            }

            if (data.getState() == SpleefPlayer.SpleefState.FIGHTING) {
                if (event.getBlock().getType() == Material.SNOW_BLOCK) {
                    event.setCancelled(false);
                    event.getBlock().setType(Material.AIR);
                    player.getInventory().addItem(new ItemStack(Material.SNOW_BALL, 4));
                } else {
                    event.setCancelled(true);
                }
            } else {
                event.setCancelled(true);
            }
        }
    }

    public List<UUID> getByState(SpleefPlayer.SpleefState state) {
        return players.values().stream().filter(player -> player.getState() == state)
                .map(SpleefPlayer::getUuid).collect(Collectors.toList());
    }

    @Getter
    @RequiredArgsConstructor
    public class SpleefGameTask extends BukkitRunnable {

        private int time = 303;

        @Override
        public void run() {
            if (time == 303 || time == 302 || time == 301) {
                sendMessage(plugin.getLanguageFile().getConfiguration()
                        .getString("event.event-start-countdown.message")
                        .replace("{0}", getName())
                        .replace("{1}", String.valueOf(time - 300)));
            } else if (time == 300) {
                sendMessage(plugin.getLanguageFile().getConfiguration()
                        .getString("event.new-round-start.message"));

                
                setState(rip.diamond.practice.events.EventState.PLAYING);

                getPlayers().forEach((uuid, spleefPlayer) -> {
                    spleefPlayer.setState(SpleefPlayer.SpleefState.FIGHTING);
                    Player player = Bukkit.getPlayer(uuid);

                    
                    EventLoadout loadout = new EventLoadout("Spleef");
                    if (loadout.exists()) {
                        loadout.apply(player);
                    } else {
                        
                        getKitOptional().ifPresent(kit -> kit.getKitLoadout().apply(kit, null, player));
                    }
                });
            } else if (time <= 0) {
                preEnd(null);
                cancel();
                return;
            }

            if (getByState(SpleefPlayer.SpleefState.FIGHTING).size() <= 1) {
                checkForWin();
                cancel();
                return;
            }

            time--;
        }
    }

    public class SpleefCountdown extends EventCountdownTask {
        private final int requiredPlayers;

        public SpleefCountdown(PracticeEvent<?> event, int requiredPlayers) {
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
