package rip.diamond.practice.events.games.knockout;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import rip.diamond.practice.events.EventCountdownTask;
import rip.diamond.practice.Eden;
import rip.diamond.practice.events.EventCountdownTask;
import rip.diamond.practice.events.EventState;
import rip.diamond.practice.events.PracticeEvent;
import rip.diamond.practice.util.CC;
import rip.diamond.practice.util.cuboid.Cuboid;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Getter
public class KnockoutEvent extends PracticeEvent<KnockoutPlayer> {

    private final Map<UUID, KnockoutPlayer> players = new HashMap<>();
    private KnockoutCountdown eventCountdown;

    private KnockoutGameTask gameTask;
    private Player winner;
    private List<KnockoutPlayer> topPlayers = new ArrayList<>();

    public KnockoutEvent() {
        super("Knockout");
        this.eventCountdown = new KnockoutCountdown(this, 2);
    }

    @Override
    public Map<UUID, KnockoutPlayer> getPlayers() {
        return players;
    }

    @Override
    public EventCountdownTask getCountdownTask() {

        if (eventCountdown == null || eventCountdown.isEnded()) {
            eventCountdown = new KnockoutCountdown(this, 2);
        }
        return eventCountdown;
    }

    @Override
    public List<Location> getSpawnLocations() {
        return new ArrayList<>(plugin.getSpawnManager().getKnockoutLocations());
    }

    @Override
    public List<String> getScoreboard(Player player) {
        List<String> lines;

        if (getState() == EventState.WAITING || getState() == EventState.STARTING) {
            lines = new ArrayList<>(Eden.INSTANCE.getScoreboardFile().getLines("KNOCKOUT", "WAITING"));
        } else if (getState() == EventState.ENDING) {
            lines = new ArrayList<>(Eden.INSTANCE.getScoreboardFile().getLines("KNOCKOUT", "ENDING"));
        } else {

            lines = new ArrayList<>(Eden.INSTANCE.getScoreboardFile().getLines("KNOCKOUT", "PLAYING"));
        }


        List<String> replaced = new ArrayList<>();
        for (String line : lines) {

            line = line.replace("{event_name}", getName());
            line = line.replace("{event_players}", String.valueOf(getPlayers().size()));
            line = line.replace("{event_limit}", String.valueOf(getLimit()));
            line = line.replace("{event_host}", getHost() != null ? getHost().getName() : "None");


            if (getState() == EventState.PLAYING) {
                line = line.replace("{players_remaining}",
                        String.valueOf(getByState(KnockoutPlayer.KnockoutState.FIGHTING).size()));
                line = line.replace("{players_eliminated}",
                        String.valueOf(getByState(KnockoutPlayer.KnockoutState.ELIMINATED).size()));

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

    private Cuboid arenaCuboid;

    @Override
    public void onStart() {
        gameTask = new KnockoutGameTask();
        gameTask.runTaskTimerAsynchronously(plugin, 0, 20L);


        if (plugin.getSpawnManager().getKnockoutMin() != null && plugin.getSpawnManager().getKnockoutMax() != null) {
            this.arenaCuboid = new Cuboid(plugin.getSpawnManager().getKnockoutMin(),
                    plugin.getSpawnManager().getKnockoutMax());
            plugin.getChunkRestorationManager().copy(this.arenaCuboid);
        }
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
                rip.diamond.practice.util.PlayerUtil.reset(p);
            }
        }

        Bukkit.getScheduler().runTaskLater(plugin, this::end, 60L);
    }

    public void cancelAll() {
        if (gameTask != null) {
            gameTask.cancel();
        }
    }

    @Override
    public Consumer<Player> onJoin() {
        return player -> players.put(player.getUniqueId(), new KnockoutPlayer(player.getUniqueId(), this));
    }

    @Override
    public Consumer<Player> onDeath() {
        return player -> {
            KnockoutPlayer data = getPlayer(player);
            if (data.getState() != KnockoutPlayer.KnockoutState.FIGHTING) {
                return;
            }

            Player killer = player.getKiller();
            data.setState(KnockoutPlayer.KnockoutState.ELIMINATED);

            getPlayers().remove(player.getUniqueId());
            plugin.getEventManager().addSpectator(player,
                    rip.diamond.practice.profile.PlayerProfile.get(player), this);

            sendMessage(ChatColor.RED + player.getName() + ChatColor.GRAY + " was eliminated" +
                    (killer == null ? "." : " by " + ChatColor.RED + killer.getName()) + ChatColor.GRAY + ".");
            player.sendMessage(" ");
            player.sendMessage(ChatColor.RED + "You have been eliminated from the event. Better luck next time!");
            player.sendMessage(" ");

            if (getByState(KnockoutPlayer.KnockoutState.FIGHTING).size() <= 1) {
                checkForWin();
            }
        };
    }

    private void checkForWin() {
        List<UUID> fighting = getByState(KnockoutPlayer.KnockoutState.FIGHTING);
        if (fighting.size() <= 1) {
            Player winner = fighting.isEmpty() ? null : Bukkit.getPlayer(fighting.get(0));
            preEnd(winner);
        }
    }

    @Override
    public void leave(Player player) {
        KnockoutPlayer knockoutPlayer = getPlayer(player);
        if (knockoutPlayer != null) {
            if (knockoutPlayer.getState() == KnockoutPlayer.KnockoutState.FIGHTING) {
                knockoutPlayer.setState(KnockoutPlayer.KnockoutState.ELIMINATED);
                sendMessage(CC.translate("&c" + player.getName() + " &edisconnected."));
            }
        }

        super.leave(player);

        if (getState() == EventState.PLAYING) {
            checkForWin();
        }
    }

    private Player getRandomPlayer() {
        if (getByState(KnockoutPlayer.KnockoutState.FIGHTING).size() == 0) {
            return null;
        }

        List<UUID> fighting = getByState(KnockoutPlayer.KnockoutState.FIGHTING);
        Collections.shuffle(fighting);
        UUID uuid = fighting.get(ThreadLocalRandom.current().nextInt(fighting.size()));

        return plugin.getServer().getPlayer(uuid);
    }

    public List<UUID> getByState(KnockoutPlayer.KnockoutState state) {
        return players.values().stream().filter(player -> player.getState() == state)
                .map(KnockoutPlayer::getUuid).collect(Collectors.toList());
    }

    @Override
    public void end() {

        cancelAll();


        if (this.arenaCuboid != null) {
            plugin.getChunkRestorationManager().reset(this.arenaCuboid);
        }


        super.end();
    }

    @Getter
    @RequiredArgsConstructor
    public class KnockoutGameTask extends BukkitRunnable {

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


                setState(EventState.PLAYING);

                List<Location> spawnList = new ArrayList<>(getSpawnLocations());
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    getPlayers().forEach((uuid, knockoutPlayer) -> {
                        knockoutPlayer.setState(KnockoutPlayer.KnockoutState.FIGHTING);

                        Player player = Bukkit.getPlayer(uuid);
                        if (!spawnList.isEmpty()) {
                            player.teleport(spawnList.remove(ThreadLocalRandom.current().nextInt(spawnList.size())));
                        }

                        rip.diamond.practice.events.EventLoadout loadout = new rip.diamond.practice.events.EventLoadout(
                                "Knockout");
                        if (loadout.exists()) {
                            loadout.apply(player);
                        } else {
                            getKitOptional().ifPresent(kit -> kit.getKitLoadout().apply(kit, null, player));
                        }
                    });
                });
            } else if (time <= 0) {
                Player winner = getRandomPlayer();
                preEnd(winner);
                cancel();
                return;
            }

            if (getPlayers().size() == 1) {
                checkForWin();
                cancel();
                return;
            }

            if (Arrays.asList(60, 50, 40, 30, 25, 20, 15, 10, 5, 4, 3, 2, 1).contains(time)) {
                sendMessage(CC.RED + "Event ending in " + time + " seconds!");
            }

            time--;
        }
    }

    public class KnockoutCountdown extends EventCountdownTask {

        private final int requiredPlayers;

        public KnockoutCountdown(PracticeEvent<?> event, int requiredPlayers) {
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
            for (Map.Entry<UUID, ?> entry : getEvent().getPlayers().entrySet()) {
                UUID uuid = entry.getKey();
                if (uuid != null) {
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        Player player = Bukkit.getPlayer(uuid);
                        if (player != null) {
                            plugin.getLobbyManager().resetPlayerOrSpawn(player, true);
                        }
                    }, 1L);
                }
            }
            this.getEvent().end();
        }
    }
}
