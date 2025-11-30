package rip.diamond.practice.events.games.gulag;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import rip.diamond.practice.Eden;
import rip.diamond.practice.events.EventCountdownTask;
import rip.diamond.practice.events.EventState;
import rip.diamond.practice.events.PracticeEvent;
import rip.diamond.practice.util.CC;
import rip.diamond.practice.util.cuboid.Cuboid;
import rip.diamond.practice.util.CC;
import rip.diamond.practice.util.CC;
import rip.diamond.practice.util.PlayerUtil;
import rip.diamond.practice.events.EventLoadout;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class GulagEvent extends PracticeEvent<GulagPlayer> implements Listener {

    private final Map<UUID, GulagPlayer> players = new HashMap<>();
    private GulagCountdown eventCountdown;

    @Getter
    final List<Player> fighting = new ArrayList<>();
    @Getter
    @Setter
    private int round;
    @Getter
    private Player playerA;
    @Getter
    private Player playerB;
    private Cuboid arenaCuboid;

    public GulagEvent() {
        super("Gulag");
        this.eventCountdown = new GulagCountdown(this, 2);
    }

    @Override
    public Map<UUID, GulagPlayer> getPlayers() {
        return players;
    }

    @Override
    public EventCountdownTask getCountdownTask() {

        if (eventCountdown == null || eventCountdown.isEnded()) {
            eventCountdown = new GulagCountdown(this, 2);
        }
        return eventCountdown;
    }

    @Override
    public List<Location> getSpawnLocations() {
        return Collections.singletonList(plugin.getSpawnManager().getGulagLocation());
    }

    @Override
    public List<String> getScoreboard(Player player) {
        List<String> lines;

        if (getState() == EventState.WAITING || getState() == EventState.STARTING) {
            lines = new ArrayList<>(Eden.INSTANCE.getScoreboardFile().getLines("GULAG", "WAITING"));
        } else {

            lines = new ArrayList<>(Eden.INSTANCE.getScoreboardFile().getLines("GULAG", "PLAYING"));
        }


        List<String> replaced = new ArrayList<>();
        for (String line : lines) {

            line = line.replace("{event_name}", getName());
            line = line.replace("{event_players}", String.valueOf(getPlayers().size()));
            line = line.replace("{event_limit}", String.valueOf(getLimit()));
            line = line.replace("{event_host}", getHost() != null ? getHost().getName() : "None");


            if (getState() == EventState.PLAYING) {
                line = line.replace("{round}", String.valueOf(round));
                line = line.replace("{round}", String.valueOf(round));
                line = line.replace("{players_remaining}", String.valueOf(getAlivePlayers().size()));

                GulagPlayer data = getPlayer(player);
                if (data != null) {
                    line = line.replace("{player_wins}", String.valueOf(data.getWins()));

                    if (data.getState() == GulagPlayer.GulagState.FIGHTING
                            || data.getState() == GulagPlayer.GulagState.PREPARING) {
                        GulagPlayer opponent = data.getFighting();
                        if (opponent != null) {
                            Player oppPlayer = Bukkit.getPlayer(opponent.getUuid());
                            line = line.replace("{opponent_wins}", String.valueOf(opponent.getWins()));
                            line = line.replace("{next_match}",
                                    "You vs " + (oppPlayer != null ? oppPlayer.getName() : "Unknown"));
                        } else {
                            line = line.replace("{opponent_wins}", "0");
                            line = line.replace("{next_match}", "None");
                        }
                    } else {
                        line = line.replace("{opponent_wins}", "N/A");

                        if (playerA != null && playerB != null) {
                            line = line.replace("{next_match}", playerA.getName() + " vs " + playerB.getName());
                        } else {
                            line = line.replace("{next_match}", "Pending...");
                        }
                    }
                } else {
                    line = line.replace("{player_wins}", "0");
                    line = line.replace("{opponent_wins}", "0");
                    line = line.replace("{next_match}", "None");
                }

                if (playerA != null && playerB != null) {
                    line = line.replace("{player_a}", playerA.getName());
                    line = line.replace("{player_b}", playerB.getName());
                    line = line.replace("{ongoing_matches}", "1");
                } else {
                    line = line.replace("{player_a}", "None");
                    line = line.replace("{player_b}", "None");
                    line = line.replace("{ongoing_matches}", "0");
                }









                int time = 0;
                if (playerA != null) {
                    GulagPlayer pA = getPlayer(playerA);
                    if (pA != null && pA.getFightTask() != null) {







                    }
                }
                line = line.replace("{time}", "0:00");
            } else {

                line = line.replace("{round}", String.valueOf(round));
                GulagPlayer playerData = getPlayer(player);
                if (playerData != null) {
                    line = line.replace("{player_wins}", String.valueOf(playerData.getWins()));
                } else {
                    line = line.replace("{player_wins}", "0");
                }

                if (playerA != null && playerB != null) {
                    line = line.replace("{next_match}", playerA.getName() + " vs " + playerB.getName());
                    line = line.replace("{ongoing_matches}", "1");
                } else {
                    line = line.replace("{next_match}", "Pending...");
                    line = line.replace("{ongoing_matches}", "0");
                }
            }

            replaced.add(CC.translate(line));
        }

        return replaced;
    }

    @Override
    public void onStart() {
        round = 0;
        setState(EventState.PLAYING);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);


        if (plugin.getSpawnManager().getGulagMin() != null && plugin.getSpawnManager().getGulagMax() != null) {
            this.arenaCuboid = new Cuboid(plugin.getSpawnManager().getGulagMin(),
                    plugin.getSpawnManager().getGulagMax());
            plugin.getChunkRestorationManager().copy(this.arenaCuboid);
        }

        selectPlayers();
    }

    @Override
    public Consumer<Player> onJoin() {
        return player -> {
            players.put(player.getUniqueId(), new GulagPlayer(player.getUniqueId(), this));


            for (Player other : getBukkitPlayers()) {
                if (!other.equals(player)) {
                    player.showPlayer(other);
                    other.showPlayer(player);
                }
            }
        };
    }

    @Override
    public Consumer<Player> onDeath() {
        return player -> {
            GulagPlayer data = getPlayer(player);
            if (data == null || data.getFighting() == null) {
                return;
            }

            if (data.getState() == GulagPlayer.GulagState.FIGHTING
                    || data.getState() == GulagPlayer.GulagState.PREPARING) {
                GulagPlayer killerData = data.getFighting();
                Player killer = plugin.getServer().getPlayer(killerData.getUuid());

                data.getFightTask().cancel();
                killerData.getFightTask().cancel();

                data.setState(GulagPlayer.GulagState.ELIMINATED);
                killerData.setState(GulagPlayer.GulagState.WAITING);
                killerData.setWins(killerData.getWins() + 1);


                playerA = null;
                playerB = null;


                player.teleport(getSpawnLocations().get(0));

                rip.diamond.practice.profile.PlayerProfile.get(player).giveItems();

                if (killer != null) {
                    PlayerUtil.reset(killer);

                    rip.diamond.practice.profile.PlayerProfile.get(killer).giveItems();

                    killer.teleport(getSpawnLocations().get(0));

                    sendMessage(plugin.getLanguageFile().getConfiguration()
                            .getString("EVENT.PLAYER-ELIMINATED-BY-KILLER")
                            .replace("<eventName>", this.getName())
                            .replace("<round>", String.valueOf(round))
                            .replace("<player>", player.getName())
                            .replace("<killer>", killer.getName()));
                } else {
                    sendMessage(plugin.getLanguageFile().getConfiguration()
                            .getString("EVENT.PLAYER-ELIMINATED")
                            .replace("<eventName>", this.getName())
                            .replace("<round>", String.valueOf(round))
                            .replace("<player>", player.getName()));
                }

                player.sendMessage(CC.translate(plugin.getLanguageFile().getConfiguration()
                        .getString("EVENT.ELIMINATED")
                        .replace("<eventName>", this.getName())));

                if (getAlivePlayers().size() <= 1) {
                    Player winner = getAlivePlayers().isEmpty() ? null : getAlivePlayers().get(0);
                    handleWin(winner);
                    fighting.clear();
                    end();
                } else {
                    plugin.getServer().getScheduler()
                            .runTaskLater(plugin, this::selectPlayers, 60L);
                }
            }
        };
    }

    private Location[] getGulagLocations() {
        Location[] array = new Location[2];
        array[0] = plugin.getSpawnManager().getGulagFirst();
        array[1] = plugin.getSpawnManager().getGulagSecond();
        return array;
    }

    private void selectPlayers() {

        if (this.arenaCuboid != null) {
            plugin.getChunkRestorationManager().reset(this.arenaCuboid);
        }

        if (getAlivePlayers().size() <= 1) {
            Player winner = getAlivePlayers().isEmpty() ? null : getAlivePlayers().get(0);
            handleWin(winner);
            fighting.clear();
            end();
            return;
        }


        List<UUID> waitingUUIDs = getByState(GulagPlayer.GulagState.WAITING);
        List<GulagPlayer> waitingPlayers = waitingUUIDs.stream()
                .map(this::getPlayer)
                .collect(Collectors.toList());

        if (waitingPlayers.size() < 2) {
            return;
        }


        Collections.shuffle(waitingPlayers);

        GulagPlayer player1 = null;
        GulagPlayer player2 = null;


        Map<Integer, List<GulagPlayer>> winsMap = new HashMap<>();
        for (GulagPlayer p : waitingPlayers) {
            winsMap.computeIfAbsent(p.getWins(), k -> new ArrayList<>()).add(p);
        }

        for (List<GulagPlayer> group : winsMap.values()) {
            if (group.size() >= 2) {
                player1 = group.get(0);
                player2 = group.get(1);
                break;
            }
        }


        if (player1 == null) {
            waitingPlayers.sort(Comparator.comparingInt(GulagPlayer::getWins));
            player1 = waitingPlayers.get(0);
            player2 = waitingPlayers.get(1);
        }

        Player picked1 = Bukkit.getPlayer(player1.getUuid());
        Player picked2 = Bukkit.getPlayer(player2.getUuid());

        if (picked1 == null || picked2 == null) {
            selectPlayers();
            return;
        }


        playerA = picked1;
        playerB = picked2;


        sendMessage(CC.translate("&eNext match: &a" + picked1.getName() + " &evs &a" + picked2.getName()));
        sendMessage(CC.translate("&eStarting in &a3 seconds..."));


        final GulagPlayer finalPlayer1 = player1;
        final GulagPlayer finalPlayer2 = player2;
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> startRound(finalPlayer1, finalPlayer2), 60L);
    }

    private void startRound(GulagPlayer player1, GulagPlayer player2) {
        Player picked1 = Bukkit.getPlayer(player1.getUuid());
        Player picked2 = Bukkit.getPlayer(player2.getUuid());

        if (picked1 == null || picked2 == null) {

            selectPlayers();
            return;
        }

        fighting.clear();


        player1.setState(GulagPlayer.GulagState.PREPARING);
        player2.setState(GulagPlayer.GulagState.PREPARING);

        player1.setFighting(player2);
        player2.setFighting(player1);
        fighting.add(picked1);
        fighting.add(picked2);



        Set<Player> allInEvent = new HashSet<>(getBukkitPlayers());
        allInEvent.addAll(getSpectators());

        for (Player other : allInEvent) {
            if (!other.equals(picked1) && !other.equals(picked2)) {

                PlayerUtil.hideOrShowPlayer(picked1, other, true);
                PlayerUtil.hideOrShowPlayer(picked2, other, true);

                PlayerUtil.hideOrShowPlayer(other, picked1, false);
                PlayerUtil.hideOrShowPlayer(other, picked2, false);
            }
        }


        PlayerUtil.hideOrShowPlayer(picked2, picked1, false);
        PlayerUtil.hideOrShowPlayer(picked1, picked2, false);



        allInEvent.addAll(getSpectators());

        for (Player other : allInEvent) {
            if (!other.equals(picked1) && !other.equals(picked2)) {

                PlayerUtil.hideOrShowPlayer(picked1, other, true);
                PlayerUtil.hideOrShowPlayer(picked2, other, true);

                PlayerUtil.hideOrShowPlayer(other, picked1, false);
                PlayerUtil.hideOrShowPlayer(other, picked2, false);
            }
        }


        PlayerUtil.hideOrShowPlayer(picked2, picked1, false);
        PlayerUtil.hideOrShowPlayer(picked1, picked2, false);

        picked1.teleport(getGulagLocations()[0]);
        picked2.teleport(getGulagLocations()[1]);

        round++;

        sendMessage(plugin.getLanguageFile().getConfiguration()
                .getString("EVENT.ROUND-STARTING")
                .replace("<round>", String.valueOf(round))
                .replace("<playerA>", picked1.getName())
                .replace("<playerB>", picked2.getName()));

        BukkitTask task = new GulagFightTask(picked1, picked2, player1, player2)
                .runTaskTimer(plugin, 0, 20);
        player1.setFightTask(task);
        player2.setFightTask(task);




        EventLoadout loadout = new EventLoadout("Gulag");
        if (loadout.exists()) {
            loadout.apply(picked1);
            loadout.apply(picked2);
        } else {

            getKitOptional().ifPresent(kit -> {

                kit.getKitLoadout().apply(kit, null, picked1);
                kit.getKitLoadout().apply(kit, null, picked2);


                picked1.addPotionEffects(kit.getEffects());
                picked2.addPotionEffects(kit.getEffects());


                plugin.getSpigotAPI().getKnockback().applyKnockback(picked1, kit.getGameRules().getKnockbackName());
                plugin.getSpigotAPI().getKnockback().applyKnockback(picked2, kit.getGameRules().getKnockbackName());
            });
        }
    }

    public List<UUID> getByState(GulagPlayer.GulagState state) {
        return players.values().stream().filter(player -> player.getState() == state)
                .map(GulagPlayer::getUuid).collect(Collectors.toList());
    }

    public List<Player> getAlivePlayers() {
        return players.values().stream()
                .filter(player -> player.getState() != GulagPlayer.GulagState.ELIMINATED)
                .map(player -> Bukkit.getPlayer(player.getUuid()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public void leave(Player player) {
        GulagPlayer gulagPlayer = getPlayer(player);



        if (gulagPlayer != null) {

            if (gulagPlayer.getState() == GulagPlayer.GulagState.FIGHTING
                    || gulagPlayer.getState() == GulagPlayer.GulagState.PREPARING) {
                GulagPlayer opponent = gulagPlayer.getFighting();


                if (gulagPlayer.getFightTask() != null)
                    gulagPlayer.getFightTask().cancel();
                if (opponent != null && opponent.getFightTask() != null)
                    opponent.getFightTask().cancel();

                if (opponent != null) {
                    Player oppPlayer = Bukkit.getPlayer(opponent.getUuid());
                    if (oppPlayer != null) {
                        opponent.setState(GulagPlayer.GulagState.WAITING);
                        opponent.setWins(opponent.getWins() + 1);

                        sendMessage(CC.translate(
                                "&c" + player.getName() + " disconnected. &a" + oppPlayer.getName()
                                        + " wins the round!"));

                        PlayerUtil.reset(oppPlayer);
                        oppPlayer.teleport(getSpawnLocations().get(0));
                    }
                }

                fighting.clear();
                playerA = null;
                playerB = null;
            }
        }

        super.leave(player);


        if (getState() == EventState.PLAYING) {
            if (getAlivePlayers().size() <= 1) {
                Player winner = getAlivePlayers().isEmpty() ? null : getAlivePlayers().get(0);
                handleWin(winner);
                fighting.clear();
                end();
            } else if (fighting.isEmpty() && getAlivePlayers().size() > 1) {

                plugin.getServer().getScheduler().runTaskLater(plugin, this::selectPlayers, 60L);
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        GulagPlayer data = getPlayer(player);


        if (data != null && data.getState() == GulagPlayer.GulagState.PREPARING) {
            if (event.getFrom().getX() != event.getTo().getX() || event.getFrom().getZ() != event.getTo().getZ()) {

                Location spawn = (player.equals(playerA)) ? getGulagLocations()[0] : getGulagLocations()[1];


                spawn.setYaw(player.getLocation().getYaw());
                spawn.setPitch(player.getLocation().getPitch());

                player.teleport(spawn);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        leave(event.getPlayer());
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();

            if (getState() != EventState.PLAYING) {
                if (getPlayers().containsKey(player.getUniqueId())) {
                    event.setCancelled(true);
                }
                return;
            }

            GulagPlayer gulagPlayer = getPlayer(player);
            if (gulagPlayer != null) {
                if (gulagPlayer.getState() != GulagPlayer.GulagState.FIGHTING) {
                    event.setCancelled(true);
                    return;
                }

                if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
                    return;
                }
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player))
            return;

        if (getState() != EventState.PLAYING) {
            if (getPlayers().containsKey(event.getEntity().getUniqueId())) {
                event.setCancelled(true);
            }
            return;
        }

        Player victim = (Player) event.getEntity();
        Player attacker = (Player) event.getDamager();

        GulagPlayer victimData = getPlayer(victim);
        GulagPlayer attackerData = getPlayer(attacker);

        if (victimData != null && attackerData != null) {
            if (victimData.getState() == GulagPlayer.GulagState.FIGHTING
                    && attackerData.getState() == GulagPlayer.GulagState.FIGHTING) {
                event.setDamage(0);
                return;
            }
        }

        if (victimData != null)
            event.setCancelled(true);
    }

    @Override
    public void end() {

        getPlayers().values().forEach(gulagPlayer -> {
            if (gulagPlayer.getFightTask() != null) {
                gulagPlayer.getFightTask().cancel();
            }
        });

        fighting.clear();


        if (this.arenaCuboid != null) {
            plugin.getChunkRestorationManager().reset(this.arenaCuboid);
        }


        super.end();
    }

    @Getter
    @RequiredArgsConstructor
    public class GulagFightTask extends BukkitRunnable {

        private final Player player;
        private final Player other;
        private final GulagPlayer playerGulag;
        private final GulagPlayer otherGulag;

        private int time = 90;

        @Override
        public void run() {
            if (player == null || other == null || !player.isOnline() || !other.isOnline()) {
                cancel();
                return;
            }

            if (time == 90) {
                sendMessage(plugin.getLanguageFile().getConfiguration()
                        .getString("EVENT.ROUND-STARTING-THREE"));
            } else if (time == 89) {
                sendMessage(plugin.getLanguageFile().getConfiguration()
                        .getString("EVENT.ROUND-STARTING-TWO"));
            } else if (time == 88) {
                sendMessage(plugin.getLanguageFile().getConfiguration()
                        .getString("EVENT.ROUND-STARTING-ONE"));
            } else if (time == 87) {
                sendMessage(plugin.getLanguageFile().getConfiguration()
                        .getString("EVENT.ROUND-STARTED"));
                otherGulag.setState(GulagPlayer.GulagState.FIGHTING);
                playerGulag.setState(GulagPlayer.GulagState.FIGHTING);
            } else if (time <= 0) {
                List<Player> players = Arrays.asList(player, other);
                Player winner = players.get(ThreadLocalRandom.current().nextInt(players.size()));
                players.stream().filter(pl -> !pl.equals(winner)).forEach(pl -> onDeath().accept(pl));
                cancel();
                return;
            }

            if (Arrays.asList(30, 25, 20, 15, 10, 5, 4, 3, 2, 1).contains(time)) {
                sendMessage(plugin.getLanguageFile().getConfiguration()
                        .getString("EVENT.ROUND-ENDING")
                        .replace("<countdown>", String.valueOf(time)));
            }

            time--;
        }
    }

    public class GulagCountdown extends EventCountdownTask {

        private final int requiredPlayers;

        public GulagCountdown(PracticeEvent<?> event, int requiredPlayers) {
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
