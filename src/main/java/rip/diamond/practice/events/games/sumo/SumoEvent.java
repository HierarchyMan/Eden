package rip.diamond.practice.events.games.sumo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import rip.diamond.practice.Eden;
import rip.diamond.practice.events.EventCountdownTask;
import rip.diamond.practice.events.EventState;
import rip.diamond.practice.events.PracticeEvent;
import rip.diamond.practice.util.CC;
import rip.diamond.practice.util.PlayerUtil;
import rip.diamond.practice.events.EventLoadout;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class SumoEvent extends PracticeEvent<SumoPlayer> implements Listener {

    private final Map<UUID, SumoPlayer> players = new HashMap<>();
    private SumoCountdown eventCountdown;

    @Getter
    final List<Player> fighting = new ArrayList<>();
    @Getter
    private WaterCheckTask waterCheckTask;
    @Getter
    @Setter
    private int round;
    @Getter
    private Player playerA;
    @Getter
    private Player playerB;
    private boolean running = false;
    private Player winner;

    public SumoEvent() {
        super("Sumo");
        this.eventCountdown = new SumoCountdown(this, 2);
    }

    @Override
    public Map<UUID, SumoPlayer> getPlayers() {
        return players;
    }

    @Override
    public EventCountdownTask getCountdownTask() {
        
        if (eventCountdown == null || eventCountdown.isEnded()) {
            eventCountdown = new SumoCountdown(this, 2);
        }
        return eventCountdown;
    }

    @Override
    public List<Location> getSpawnLocations() {
        return Collections.singletonList(plugin.getSpawnManager().getSumoLocation());
    }

    @Override
    public List<String> getScoreboard(Player player) {
        List<String> lines;

        if (getState() == EventState.WAITING || getState() == EventState.STARTING) {
            lines = new ArrayList<>(Eden.INSTANCE.getScoreboardFile().getLines("SUMO", "WAITING"));
        } else if (getState() == EventState.PREPARING) {
            
            lines = new ArrayList<>(Eden.INSTANCE.getScoreboardFile().getLines("SUMO", "PREPARING"));
        } else if (getState() == EventState.ENDING) {
            lines = new ArrayList<>(Eden.INSTANCE.getScoreboardFile().getLines("SUMO", "ENDING"));
        } else {
            
            lines = new ArrayList<>(Eden.INSTANCE.getScoreboardFile().getLines("SUMO", "PLAYING"));
        }

        
        List<String> replaced = new ArrayList<>();
        for (String line : lines) {
            
            if (line.contains("{next_match}") && playerA == null && playerB == null) {
                continue; 
            }
            
            
            if (line.contains("{current_match}") && playerA == null && playerB == null
                    && getState() == EventState.PLAYING) {
                continue; 
            }

            
            line = line.replace("{event_name}", getName());
            line = line.replace("{event_players}", String.valueOf(getPlayers().size()));
            line = line.replace("{event_limit}", String.valueOf(getLimit()));
            line = line.replace("{event_host}", getHost() != null ? getHost().getName() : "None");

            
            if (getState() == EventState.PREPARING) {
                line = line.replace("{round}", String.valueOf(round));

                SumoPlayer playerData = getPlayer(player);
                if (playerData != null) {
                    line = line.replace("{player_wins}", String.valueOf(playerData.getWins()));
                } else {
                    line = line.replace("{player_wins}", "0");
                }

                
                if (playerA != null && playerB != null) {
                    if (player.equals(playerA)) {
                        line = line.replace("{next_match}", "You vs " + playerB.getName());
                    } else if (player.equals(playerB)) {
                        line = line.replace("{next_match}", "You vs " + playerA.getName());
                    } else {
                        line = line.replace("{next_match}", playerA.getName() + " vs " + playerB.getName());
                    }
                    line = line.replace("{ongoing_matches}", "1");
                } else {
                    line = line.replace("{ongoing_matches}", "0");
                }
            }
            
            else if (getState() == EventState.PLAYING) {
                line = line.replace("{round}", String.valueOf(round));
                line = line.replace("{players_remaining}", String.valueOf(getAlivePlayers().size()));

                SumoPlayer playerData = getPlayer(player);
                if (playerData != null) {
                    line = line.replace("{player_wins}", String.valueOf(playerData.getWins()));

                    if (playerData.getState() == SumoPlayer.SumoState.FIGHTING
                            || playerData.getState() == SumoPlayer.SumoState.PREPARING) {
                        SumoPlayer opponent = playerData.getFighting();
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
                    }
                } else {
                    line = line.replace("{player_wins}", "0");
                    line = line.replace("{opponent_wins}", "0");
                }

                
                if (playerA != null && playerB != null) {
                    line = line.replace("{player_a}", playerA.getName());
                    line = line.replace("{player_b}", playerB.getName());
                    line = line.replace("{ongoing_matches}", "1");
                    line = line.replace("{current_match}", playerA.getName() + " vs " + playerB.getName());
                } else {
                    line = line.replace("{player_a}", "None");
                    line = line.replace("{player_b}", "None");
                    line = line.replace("{ongoing_matches}", "0");
                }

                line = line.replace("{time}", "0:00"); 
            } else if (getState() == EventState.ENDING) {
                line = line.replace("{winner_name}", winner != null ? winner.getName() : "None");
                line = line.replace("{round}", String.valueOf(round));
            } else {
                
                line = line.replace("{round}", String.valueOf(round));
                SumoPlayer playerData = getPlayer(player);
                if (playerData != null) {
                    line = line.replace("{player_wins}", String.valueOf(playerData.getWins()));
                    
                    if (playerData.getState() == SumoPlayer.SumoState.ELIMINATED) {
                        line = line.replace("{player_status}", "&cELIMINATED");
                    } else {
                        line = line.replace("{player_status}", "&aCOMPETING");
                    }
                } else {
                    line = line.replace("{player_wins}", "0");
                    line = line.replace("{player_status}", "&aCOMPETING");
                }

                if (playerA != null && playerB != null) {
                    line = line.replace("{next_match}", playerA.getName() + " vs " + playerB.getName());
                    line = line.replace("{ongoing_matches}", "1");
                } else {
                    line = line.replace("{ongoing_matches}", "0");
                }
            }

            replaced.add(CC.translate(line));
        }

        return replaced;
    }

    @Override
    public void onStart() {
        running = true;
        round = 0;
        setState(EventState.PLAYING);
        waterCheckTask = new WaterCheckTask();
        waterCheckTask.runTaskTimer(plugin, 0, 10L);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        prepareNextRound();
    }

    @Override
    public Consumer<Player> onJoin() {
        return player -> {
            players.put(player.getUniqueId(), new SumoPlayer(player.getUniqueId(), this));

            
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
            SumoPlayer data = getPlayer(player);
            if (data == null || data.getFighting() == null) {
                return;
            }

            if (data.getState() == SumoPlayer.SumoState.FIGHTING
                    || data.getState() == SumoPlayer.SumoState.PREPARING) {
                SumoPlayer killerData = data.getFighting();
                Player killer = plugin.getServer().getPlayer(killerData.getUuid());

                data.getFightTask().cancel();
                killerData.getFightTask().cancel();

                data.setState(SumoPlayer.SumoState.ELIMINATED);
                killerData.setState(SumoPlayer.SumoState.WAITING);
                killerData.setWins(killerData.getWins() + 1);

                
                playerA = null;
                playerB = null;

                
                player.setGameMode(GameMode.SURVIVAL);
                player.setAllowFlight(false);
                player.setFlying(false);

                
                player.teleport(getSpawnLocations().get(0));
                
                rip.diamond.practice.profile.PlayerProfile.get(player).giveItems();

                if (killer != null) {
                    PlayerUtil.reset(killer);
                    
                    rip.diamond.practice.profile.PlayerProfile.get(killer).giveItems();

                    killer.teleport(getSpawnLocations().get(0));

                    player.getWorld().strikeLightningEffect(player.getLocation());

                    
                    killer.playSound(killer.getLocation(), org.bukkit.Sound.LEVEL_UP, 1f, 1f);
                    killer.sendMessage(CC.translate("&aYou won the round!"));

                    
                    player.playSound(player.getLocation(), org.bukkit.Sound.VILLAGER_NO, 1f, 1f);

                    String eliminatedMsg = plugin.getLanguageFile().getConfiguration()
                            .getString("EVENT.PLAYER-ELIMINATED-BY-KILLER");
                    if (eliminatedMsg != null) {
                        sendMessage(eliminatedMsg
                                .replace("<eventName>", this.getName())
                                .replace("<round>", String.valueOf(round))
                                .replace("<player>", player.getName())
                                .replace("<killer>", killer.getName()));
                    }
                } else {
                    player.getWorld().strikeLightningEffect(player.getLocation());
                    player.playSound(player.getLocation(), org.bukkit.Sound.VILLAGER_NO, 1f, 1f);

                    String eliminatedMsg = plugin.getLanguageFile().getConfiguration()
                            .getString("EVENT.PLAYER-ELIMINATED");
                    if (eliminatedMsg != null) {
                        sendMessage(eliminatedMsg
                                .replace("<eventName>", this.getName())
                                .replace("<round>", String.valueOf(round))
                                .replace("<player>", player.getName()));
                    }
                }

                String personalMsg = plugin.getLanguageFile().getConfiguration().getString("EVENT.ELIMINATED");
                if (personalMsg != null) {
                    player.sendMessage(CC.translate(personalMsg.replace("<eventName>", this.getName())));
                }

                
                if (getAlivePlayers().size() <= 1) {
                    Player winner = getAlivePlayers().isEmpty() ? null : getAlivePlayers().get(0);
                    preEnd(winner);
                } else {
                    plugin.getServer().getScheduler()
                            .runTaskLater(plugin, SumoEvent.this::prepareNextRound, 60L);
                }
            }
        };
    }

    private Location[] getSumoLocations() {
        Location[] array = new Location[2];
        array[0] = plugin.getSpawnManager().getSumoFirst();
        array[1] = plugin.getSpawnManager().getSumoSecond();
        return array;
    }

    private void prepareNextRound() {
        if (getState() == EventState.ENDING)
            return;

        if (getAlivePlayers().size() <= 1) {
            Player winner = getAlivePlayers().isEmpty() ? null : getAlivePlayers().get(0);
            preEnd(winner);
            return;
        }

        
        List<UUID> waitingUUIDs = getByState(SumoPlayer.SumoState.WAITING);
        List<SumoPlayer> waitingPlayers = waitingUUIDs.stream()
                .map(this::getPlayer)
                .collect(Collectors.toList());

        if (waitingPlayers.size() < 2) {
            return;
        }

        
        Collections.shuffle(waitingPlayers);

        SumoPlayer player1 = null;
        SumoPlayer player2 = null;

        
        Map<Integer, List<SumoPlayer>> winsMap = new HashMap<>();
        for (SumoPlayer p : waitingPlayers) {
            winsMap.computeIfAbsent(p.getWins(), k -> new ArrayList<>()).add(p);
        }

        for (List<SumoPlayer> group : winsMap.values()) {
            if (group.size() >= 2) {
                player1 = group.get(0);
                player2 = group.get(1);
                break;
            }
        }

        
        if (player1 == null) {
            waitingPlayers.sort(Comparator.comparingInt(SumoPlayer::getWins));
            player1 = waitingPlayers.get(0);
            player2 = waitingPlayers.get(1);
        }

        Player picked1 = Bukkit.getPlayer(player1.getUuid());
        Player picked2 = Bukkit.getPlayer(player2.getUuid());

        if (picked1 == null || picked2 == null) {
            prepareNextRound();
            return;
        }

        
        playerA = picked1;
        playerB = picked2;

        
        setState(EventState.PREPARING);
        sendMessage(CC.translate("&eNext match: &a" + picked1.getName() + " &evs &a" + picked2.getName()));
        sendMessage(CC.translate("&eStarting in &a3 seconds..."));

        
        picked1.playSound(picked1.getLocation(), org.bukkit.Sound.NOTE_PLING, 1f, 1.5f);
        picked2.playSound(picked2.getLocation(), org.bukkit.Sound.NOTE_PLING, 1f, 1.5f);

        
        final SumoPlayer finalPlayer1 = player1;
        final SumoPlayer finalPlayer2 = player2;
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> startRound(finalPlayer1, finalPlayer2), 60L);
    }

    private void startRound(SumoPlayer player1, SumoPlayer player2) {
        Player picked1 = Bukkit.getPlayer(player1.getUuid());
        Player picked2 = Bukkit.getPlayer(player2.getUuid());

        if (picked1 == null || picked2 == null) {
            
            prepareNextRound();
            return;
        }

        fighting.clear();

        
        player1.setState(SumoPlayer.SumoState.PREPARING);
        player2.setState(SumoPlayer.SumoState.PREPARING);

        player1.setFighting(player2);
        player2.setFighting(player1);
        fighting.add(picked1);
        fighting.add(picked2);

        
        setState(EventState.PLAYING);

        
        PlayerUtil.hideOrShowPlayer(picked2, picked1, true);
        PlayerUtil.hideOrShowPlayer(picked1, picked2, true);

        PlayerUtil.reset(picked1);
        PlayerUtil.reset(picked2);
        picked1.teleport(getSumoLocations()[0]);
        picked2.teleport(getSumoLocations()[1]);

        round++;

        String roundStartingMsg = plugin.getLanguageFile().getConfiguration().getString("EVENT.ROUND-STARTING");
        if (roundStartingMsg != null) {
            sendMessage(roundStartingMsg
                    .replace("<round>", String.valueOf(round))
                    .replace("<playerA>", picked1.getName())
                    .replace("<playerB>", picked2.getName()));
        }

        BukkitTask task = new SumoFightTask(picked1, picked2, player1, player2)
                .runTaskTimer(plugin, 0, 20);
        player1.setFightTask(task);
        player2.setFightTask(task);

        
        
        EventLoadout loadout = new EventLoadout("Sumo");
        if (loadout.exists()) {
            loadout.apply(picked1);
            loadout.apply(picked2);
        } else {
            
            
            
            
            
            
            
            getKitOptional().ifPresent(kit -> {
                picked1.addPotionEffects(kit.getEffects());
                picked2.addPotionEffects(kit.getEffects());
                plugin.getSpigotAPI().getKnockback().applyKnockback(picked1, kit.getGameRules().getKnockbackName());
                plugin.getSpigotAPI().getKnockback().applyKnockback(picked2, kit.getGameRules().getKnockbackName());
            });
        }
    }

    public List<UUID> getByState(SumoPlayer.SumoState state) {
        return players.values().stream().filter(player -> player.getState() == state)
                .map(SumoPlayer::getUuid).collect(Collectors.toList());
    }

    public List<Player> getAlivePlayers() {
        return players.values().stream()
                .filter(player -> player.getState() != SumoPlayer.SumoState.ELIMINATED)
                .map(player -> Bukkit.getPlayer(player.getUuid()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        SumoPlayer data = getPlayer(player);

        
        if (data != null && data.getState() == SumoPlayer.SumoState.PREPARING) {
            if (event.getFrom().getX() != event.getTo().getX() || event.getFrom().getZ() != event.getTo().getZ()) {
                
                Location spawn = (player.equals(playerA)) ? getSumoLocations()[0] : getSumoLocations()[1];

                
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

    @Override
    public void leave(Player player) {
        SumoPlayer sumoPlayer = getPlayer(player);
        
        

        if (sumoPlayer != null) {
            
            if (sumoPlayer.getState() == SumoPlayer.SumoState.FIGHTING
                    || sumoPlayer.getState() == SumoPlayer.SumoState.PREPARING) {
                SumoPlayer opponent = sumoPlayer.getFighting();

                
                if (sumoPlayer.getFightTask() != null)
                    sumoPlayer.getFightTask().cancel();
                if (opponent != null && opponent.getFightTask() != null)
                    opponent.getFightTask().cancel();

                if (opponent != null) {
                    Player oppPlayer = Bukkit.getPlayer(opponent.getUuid());
                    if (oppPlayer != null) {
                        opponent.setState(SumoPlayer.SumoState.WAITING);
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

        
        if (running && getState() == EventState.PLAYING) {
            if (getAlivePlayers().size() <= 1) {
                Player winner = getAlivePlayers().isEmpty() ? null : getAlivePlayers().get(0);
                preEnd(winner);
            } else if (fighting.isEmpty() && getAlivePlayers().size() > 1) {
                
                plugin.getServer().getScheduler().runTaskLater(plugin, this::prepareNextRound, 60L);
            }
        }
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

            SumoPlayer sumoPlayer = getPlayer(player);
            if (sumoPlayer != null) {
                if (sumoPlayer.getState() != SumoPlayer.SumoState.FIGHTING) {
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

        SumoPlayer victimData = getPlayer(victim);
        SumoPlayer attackerData = getPlayer(attacker);

        if (victimData != null && attackerData != null) {
            if (victimData.getState() == SumoPlayer.SumoState.FIGHTING
                    && attackerData.getState() == SumoPlayer.SumoState.FIGHTING) {
                event.setDamage(0);
                return;
            }
        }

        if (victimData != null)
            event.setCancelled(true);
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (getPlayer(player) != null) {
                event.setFoodLevel(20);
                event.setCancelled(true);
            }
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

        fighting.clear();

        
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
        running = false;

        
        if (waterCheckTask != null) {
            waterCheckTask.cancel();
        }

        
        getPlayers().values().forEach(sumoPlayer -> {
            if (sumoPlayer.getFightTask() != null) {
                sumoPlayer.getFightTask().cancel();
            }
        });

        fighting.clear();

        
        super.end();
    }

    @Getter
    @RequiredArgsConstructor
    public class SumoFightTask extends BukkitRunnable {

        private final Player player;
        private final Player other;
        private final SumoPlayer playerSumo;
        private final SumoPlayer otherSumo;

        private int time = 6;

        @Override
        public void run() {
            if (player == null || other == null || !player.isOnline() || !other.isOnline()) {
                cancel();
                return;
            }

            if (time == 6) {
                
            } else if (time == 5 || time == 4 || time == 3 || time == 2 || time == 1) {
                String color = (time == 1) ? "&a" : (time == 2) ? "&e" : (time == 3) ? "&6" : "&c";
                String title = color + time;
                rip.diamond.practice.util.TitleSender.sendTitle(player, title,
                        net.minecraft.server.v1_8_R3.PacketPlayOutTitle.EnumTitleAction.TITLE, 0, 20, 0);
                rip.diamond.practice.util.TitleSender.sendTitle(other, title,
                        net.minecraft.server.v1_8_R3.PacketPlayOutTitle.EnumTitleAction.TITLE, 0, 20, 0);
                player.playSound(player.getLocation(), org.bukkit.Sound.NOTE_PLING, 1f, 1f);
                other.playSound(other.getLocation(), org.bukkit.Sound.NOTE_PLING, 1f, 1f);
            } else if (time == 0) {
                rip.diamond.practice.util.TitleSender.sendTitle(player, "&aFIGHT!",
                        net.minecraft.server.v1_8_R3.PacketPlayOutTitle.EnumTitleAction.TITLE, 0, 20, 10);
                rip.diamond.practice.util.TitleSender.sendTitle(other, "&aFIGHT!",
                        net.minecraft.server.v1_8_R3.PacketPlayOutTitle.EnumTitleAction.TITLE, 0, 20, 10);
                player.playSound(player.getLocation(), org.bukkit.Sound.NOTE_PLING, 1f, 2f);
                other.playSound(other.getLocation(), org.bukkit.Sound.NOTE_PLING, 1f, 2f);

                sendMessage(plugin.getLanguageFile().getConfiguration()
                        .getString("EVENT.ROUND-STARTED"));
                otherSumo.setState(SumoPlayer.SumoState.FIGHTING);
                playerSumo.setState(SumoPlayer.SumoState.FIGHTING);

                
                
                
                
                
                
                time = 300; 
                return; 
            } else if (time < 0) {
                
                
            }

            
            if (time > 100) {
                if (time == 101) { 
                    List<Player> players = Arrays.asList(player, other);
                    Player winner = players.get(ThreadLocalRandom.current().nextInt(players.size()));
                    players.stream().filter(pl -> !pl.equals(winner)).forEach(pl -> onDeath().accept(pl));
                    cancel();
                    return;
                }

                
                
                
                
                
            }

            time--;
        }
    }

    @Getter
    @RequiredArgsConstructor
    public class WaterCheckTask extends BukkitRunnable {

        @Override
        public void run() {
            if (getPlayers().size() <= 1) {
                return;
            }

            getBukkitPlayers().forEach(player -> {
                SumoPlayer sumoPlayer = getPlayer(player);
                if (sumoPlayer == null)
                    return;

                
                if (sumoPlayer.getState() == SumoPlayer.SumoState.FIGHTING) {
                    if (PlayerUtil.isStandingOnLiquid(player) || player.getLocation().getY() < 0) {
                        onDeath().accept(player);
                    }
                }
                
                else {
                    if (PlayerUtil.isStandingOnLiquid(player) || player.getLocation().getY() < 0) {
                        player.teleport(getSpawnLocations().get(0));
                    }
                }
            });
        }
    }

    public class SumoCountdown extends EventCountdownTask {

        private final int requiredPlayers;

        public SumoCountdown(PracticeEvent<?> event, int requiredPlayers) {
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
