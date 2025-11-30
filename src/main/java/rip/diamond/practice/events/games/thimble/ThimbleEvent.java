package rip.diamond.practice.events.games.thimble;

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
import rip.diamond.practice.events.EventLoadout;
import rip.diamond.practice.util.PlayerUtil;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Getter
public class ThimbleEvent extends PracticeEvent<ThimblePlayer> implements Listener {

    private final Map<UUID, ThimblePlayer> players = new HashMap<>();
    private ThimbleCountdown eventCountdown;
    private ThimbleGameTask gameTask;
    private final List<Block> placedBlocks = new ArrayList<>();
    private Player winner;
    private List<ThimblePlayer> topPlayers = new ArrayList<>();

    public ThimbleEvent() {
        super("Thimble");
        this.eventCountdown = new ThimbleCountdown(this, 2);
    }

    @Override
    public Map<UUID, ThimblePlayer> getPlayers() {
        return players;
    }

    @Override
    public EventCountdownTask getCountdownTask() {
        
        if (eventCountdown == null || eventCountdown.isEnded()) {
            eventCountdown = new ThimbleCountdown(this, 2);
        }
        return eventCountdown;
    }

    @Override
    public List<Location> getSpawnLocations() {
        return Collections.singletonList(plugin.getSpawnManager().getThimbleLocation());
    }

    @Override
    public void onStart() {
        gameTask = new ThimbleGameTask();
        gameTask.runTaskTimer(plugin, 0, 20L);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void end() {
        
        if (gameTask != null) {
            gameTask.cancel();
        }

        super.end();
        
        for (Block block : placedBlocks) {
            block.setType(Material.WATER);
        }
        placedBlocks.clear();
    }

    @Override
    public Consumer<Player> onJoin() {
        return player -> players.put(player.getUniqueId(), new ThimblePlayer(player.getUniqueId(), this));
    }

    @Override
    public Consumer<Player> onDeath() {
        return player -> {
            ThimblePlayer data = getPlayer(player);
            if (data.getState() != ThimblePlayer.ThimbleState.JUMPING) {
                return;
            }

            data.setFails(data.getFails() + 1);
            data.setState(ThimblePlayer.ThimbleState.ELIMINATED);
            getPlayers().remove(player.getUniqueId());
            plugin.getEventManager().addSpectator(player,
                    rip.diamond.practice.profile.PlayerProfile.get(player), this);

            checkWin();
        };
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

    private void checkWin() {
        List<ThimblePlayer> playing = getByState(ThimblePlayer.ThimbleState.JUMPING);
        playing.addAll(getByState(ThimblePlayer.ThimbleState.WAITING));

        if (playing.size() <= 1) {
            Player winner = playing.isEmpty() ? null : Bukkit.getPlayer(playing.get(0).getUuid());
            preEnd(winner);
        }
    }

    @Override
    public void leave(Player player) {
        ThimblePlayer thimblePlayer = getPlayer(player);
        if (thimblePlayer != null) {
            if (thimblePlayer.getState() == ThimblePlayer.ThimbleState.JUMPING
                    || thimblePlayer.getState() == ThimblePlayer.ThimbleState.WAITING) {
                thimblePlayer.setState(ThimblePlayer.ThimbleState.ELIMINATED);
                sendMessage(CC.translate("&c" + player.getName() + " &edisconnected."));

                
                if (gameTask != null && gameTask.jumper != null
                        && gameTask.jumper.getUniqueId().equals(player.getUniqueId())) {
                    gameTask.jumper = null; 
                }
            }
        }

        super.leave(player);

        if (getState() == EventState.PLAYING) {
            checkWin();
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        ThimblePlayer data = getPlayer(player);

        if (getState() != EventState.PLAYING) {
            return;
        }

        if (data != null && data.getState() == ThimblePlayer.ThimbleState.JUMPING) {
            
            Block block = player.getLocation().getBlock();
            if ((block.getType() == Material.STATIONARY_WATER || block.getType() == Material.WATER)
                    && player.getVelocity().getY() < -0.1) {
                handleWaterLanding(player, data);
            }
        }
    }

    private void handleWaterLanding(Player player, ThimblePlayer data) {
        
        Block block = player.getLocation().getBlock();
        if (block.getType() == Material.STATIONARY_WATER || block.getType() == Material.WATER) {
            
            if (data.getState() != ThimblePlayer.ThimbleState.JUMPING) {
                return;
            }

            data.setJumps(data.getJumps() + 1);
            block.setType(Material.WOOL);
            placedBlocks.add(block);
            player.teleport(getSpawnLocations().get(0));
            data.setState(ThimblePlayer.ThimbleState.WAITING);
            sendMessage(CC.GREEN + player.getName() + " landed successfully!");
            gameTask.nextTurn();
        } else {
            onDeath().accept(player);
        }
    }

    public List<ThimblePlayer> getByState(ThimblePlayer.ThimbleState state) {
        return players.values().stream().filter(player -> player.getState() == state)
                .collect(Collectors.toList());
    }

    @Getter
    @RequiredArgsConstructor
    public class ThimbleGameTask extends BukkitRunnable {

        private int time = 10;
        private int round = 1;
        private Player jumper;

        public int getRound() {
            return round;
        }

        @Override
        public void run() {
            if (jumper == null) {
                nextTurn();
                return;
            }

            if (time <= 0) {
                
                if (jumper.isOnline()) {
                    jumper.setVelocity(jumper.getLocation().getDirection().multiply(0.5).setY(0.3));
                    jumper.sendMessage(CC.RED + "Time's up! You were pushed!");
                }
                
                onDeath().accept(jumper);
                nextTurn();
                return;
            }

            if (time <= 3) {
                jumper.sendMessage(CC.RED + "You have " + time + " seconds to jump!");
            }

            time--;
        }

        public void nextTurn() {
            List<ThimblePlayer> waiting = getByState(ThimblePlayer.ThimbleState.WAITING);
            if (waiting.isEmpty()) {
                checkWin();
                return;
            }

            ThimblePlayer next = waiting.get(0);
            next.setState(ThimblePlayer.ThimbleState.JUMPING);
            jumper = Bukkit.getPlayer(next.getUuid());
            round++;

            
            if (round == 1) {
                setState(rip.diamond.practice.events.EventState.PLAYING);
            }

            if (jumper != null) {
                jumper.teleport(plugin.getSpawnManager().getThimbleGameLocation());
                sendMessage(CC.YELLOW + "It's " + jumper.getName() + "'s turn to jump!");
                time = 15;

                
                EventLoadout loadout = new EventLoadout("Thimble");
                if (loadout.exists()) {
                    loadout.apply(jumper);
                } else {
                    getKitOptional().ifPresent(kit -> kit.getKitLoadout().apply(kit, null, jumper));
                }
            } else {
                next.setState(ThimblePlayer.ThimbleState.ELIMINATED);
                nextTurn();
            }
        }
    }

    @Override
    public List<String> getScoreboard(Player player) {
        List<String> lines;

        if (getState() == EventState.WAITING || getState() == EventState.STARTING) {
            lines = new ArrayList<>(Eden.INSTANCE.getScoreboardFile().getLines("THIMBLE", "WAITING"));
        } else if (getState() == EventState.ENDING) {
            lines = new ArrayList<>(Eden.INSTANCE.getScoreboardFile().getLines("THIMBLE", "ENDING"));
        } else {
            
            lines = new ArrayList<>(Eden.INSTANCE.getScoreboardFile().getLines("THIMBLE", "PLAYING"));
        }

        
        List<String> replaced = new ArrayList<>();
        for (String line : lines) {
            
            line = line.replace("{event_name}", getName());
            line = line.replace("{event_players}", String.valueOf(getPlayers().size()));
            line = line.replace("{event_limit}", String.valueOf(getLimit()));
            line = line.replace("{event_host}", getHost() != null ? getHost().getName() : "None");

            
            if (getState() == EventState.PLAYING) {
                line = line.replace("{round}", String.valueOf(gameTask != null ? gameTask.getRound() : 1));
                line = line.replace("{players_remaining}",
                        String.valueOf(getByState(ThimblePlayer.ThimbleState.WAITING).size()
                                + getByState(ThimblePlayer.ThimbleState.JUMPING).size()));
                line = line.replace("{players_eliminated}",
                        String.valueOf(getByState(ThimblePlayer.ThimbleState.ELIMINATED).size()));

                ThimblePlayer playerData = getPlayer(player);
                if (playerData != null) {
                    line = line.replace("{player_jumps}", String.valueOf(playerData.getJumps()));
                    line = line.replace("{player_fails}", String.valueOf(playerData.getFails()));
                } else {
                    line = line.replace("{player_jumps}", "0");
                    line = line.replace("{player_fails}", "0");
                }
            } else if (getState() == EventState.ENDING) {
                line = line.replace("{winner_name}", winner != null ? winner.getName() : "None");
                line = line.replace("{round}", String.valueOf(gameTask != null ? gameTask.getRound() : 1));
                line = line.replace("{players_remaining}", "0");
                line = line.replace("{players_eliminated}", String.valueOf(getPlayers().size()));
                line = line.replace("{player_jumps}", "0");
                line = line.replace("{player_fails}", "0");
            } else {
                line = line.replace("{round}", "0");
                line = line.replace("{players_remaining}", String.valueOf(getPlayers().size()));
                line = line.replace("{players_eliminated}", "0");
                line = line.replace("{player_jumps}", "0");
                line = line.replace("{player_fails}", "0");
            }

            replaced.add(CC.translate(line));
        }

        return replaced;
    }

    public class ThimbleCountdown extends EventCountdownTask {
        private final int requiredPlayers;

        public ThimbleCountdown(PracticeEvent<?> event, int requiredPlayers) {
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
