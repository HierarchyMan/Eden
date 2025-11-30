package rip.diamond.practice.events.games.oitc;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import rip.diamond.practice.Eden;
import rip.diamond.practice.config.EdenSound;
import rip.diamond.practice.events.EventCountdownTask;
import rip.diamond.practice.events.EventState;
import rip.diamond.practice.events.PracticeEvent;
import rip.diamond.practice.events.EventLoadout;
import rip.diamond.practice.util.CC;
import rip.diamond.practice.util.Common;
import rip.diamond.practice.util.PlayerUtil;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Getter
public class OITCEvent extends PracticeEvent<OITCPlayer> implements Listener {

    private final Map<UUID, OITCPlayer> players = new HashMap<>();
    private OITCCountdown eventCountdown;
    private OITCGameTask gameTask;
    private boolean running = false;
    private Player winner;
    private List<OITCPlayer> topPlayers;

    public OITCEvent() {
        super("OITC");
        this.eventCountdown = new OITCCountdown(this, 2);
    }

    public boolean isRunning() {
        return running;
    }

    @Override
    public Map<UUID, OITCPlayer> getPlayers() {
        return players;
    }

    @Override
    public EventCountdownTask getCountdownTask() {
        
        if (eventCountdown == null || eventCountdown.isEnded()) {
            eventCountdown = new OITCCountdown(this, 2);
        }
        return eventCountdown;
    }

    @Override
    public List<Location> getSpawnLocations() {
        
        Location lobbyLocation = plugin.getSpawnManager().getOitcLocation();
        if (lobbyLocation == null) {
            return new ArrayList<>();
        }
        return Collections.singletonList(lobbyLocation);
    }

    /**
     * Get the list of game spawn points where players spawn during the match
     */
    public List<Location> getGameSpawnLocations() {
        return new ArrayList<>(plugin.getSpawnManager().getOitcSpawnpoints());
    }

    /**
     * Get the spectator location where players are teleported when they
     * die/spectate
     * If not configured, returns null and players will spectate from where they
     * died
     */
    public Location getSpectatorLocation() {
        return plugin.getSpawnManager().getOitcSpectatorLocation();
    }

    private final Map<UUID, org.bukkit.Color> playerColors = new HashMap<>();

    @Override
    public void onStart() {
        running = true;
        assignColors();
        gameTask = new OITCGameTask();
        gameTask.runTaskTimer(plugin, 0, 20L);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void assignColors() {
        List<org.bukkit.Color> availableColors = new ArrayList<>();
        
        for (org.bukkit.DyeColor dyeColor : org.bukkit.DyeColor.values()) {
            availableColors.add(dyeColor.getColor());
        }
        
        for (org.bukkit.DyeColor dyeColor : org.bukkit.DyeColor.values()) {
            org.bukkit.Color base = dyeColor.getColor();
            availableColors.add(base.mixColors(org.bukkit.Color.BLACK));
        }

        Collections.shuffle(availableColors);

        int i = 0;
        for (UUID uuid : getPlayers().keySet()) {
            if (i < availableColors.size()) {
                playerColors.put(uuid, availableColors.get(i));
            } else {
                
                playerColors.put(uuid, org.bukkit.Color.WHITE);
            }
            i++;
        }
    }

    @Override
    public List<String> getScoreboard(Player player) {
        List<String> lines;

        if (getState() == EventState.WAITING || getState() == EventState.STARTING) {
            lines = new ArrayList<>(Eden.INSTANCE.getScoreboardFile().getLines("OITC", "WAITING"));
        } else if (getState() == EventState.ENDING) {
            lines = new ArrayList<>(Eden.INSTANCE.getScoreboardFile().getLines("OITC", "ENDING"));
        } else {
            
            lines = new ArrayList<>(Eden.INSTANCE.getScoreboardFile().getLines("OITC", "PLAYING"));
        }

        
        List<String> replaced = new ArrayList<>();
        for (String line : lines) {
            
            line = line.replace("{event_name}", getName());
            line = line.replace("{event_players}", String.valueOf(getPlayers().size()));
            line = line.replace("{event_limit}", String.valueOf(getLimit()));
            line = line.replace("{event_host}", getHost() != null ? getHost().getName() : "None");

            
            if (getState() == EventState.PLAYING) {
                OITCPlayer playerData = getPlayer(player);
                if (playerData != null) {
                    line = line.replace("{player_kills}", String.valueOf(playerData.getScore()));
                    line = line.replace("{player_streak}", String.valueOf(playerData.getCurrentStreak()));
                } else {
                    
                    line = line.replace("{player_kills}", "&7Spectating");
                    line = line.replace("{player_streak}", "&7Spectating");
                }

                if (gameTask != null) {
                    int minutes = gameTask.time / 60;
                    int seconds = gameTask.time % 60;
                    line = line.replace("{time}", String.format("%d:%02d", minutes, seconds));
                } else {
                    line = line.replace("{time}", "0:00");
                }

                
                List<OITCPlayer> topPlayers = getPlayers().values().stream()
                        .sorted(Comparator.comparingInt(OITCPlayer::getScore).reversed())
                        .limit(3)
                        .collect(Collectors.toList());

                for (int i = 0; i < 3; i++) {
                    if (i < topPlayers.size()) {
                        OITCPlayer topPlayer = topPlayers.get(i);
                        Player p = Bukkit.getPlayer(topPlayer.getUuid());
                        String name = p != null ? p.getName() : "Unknown";
                        line = line.replace("{top" + (i + 1) + "_name}", name);
                        line = line.replace("{top" + (i + 1) + "_score}", String.valueOf(topPlayer.getScore()));
                    } else {
                        line = line.replace("{top" + (i + 1) + "_name}", "None");
                        line = line.replace("{top" + (i + 1) + "_score}", "0");
                    }
                }
            } else if (getState() == EventState.ENDING) {
                line = line.replace("{winner_name}", winner != null ? winner.getName() : "None");

                for (int i = 0; i < 3; i++) {
                    if (topPlayers != null && i < topPlayers.size()) {
                        OITCPlayer topPlayer = topPlayers.get(i);
                        Player p = Bukkit.getPlayer(topPlayer.getUuid());
                        String name = p != null ? p.getName() : "Unknown";
                        line = line.replace("{top" + (i + 1) + "_name}", name);
                        line = line.replace("{top" + (i + 1) + "_score}", String.valueOf(topPlayer.getScore()));
                    } else {
                        line = line.replace("{top" + (i + 1) + "_name}", "None");
                        line = line.replace("{top" + (i + 1) + "_score}", "0");
                    }
                }
            }

            replaced.add(CC.translate(line));
        }

        return replaced;
    }

    @Override
    public Consumer<Player> onJoin() {
        return player -> {
            players.put(player.getUniqueId(), new OITCPlayer(player.getUniqueId(), this));

            
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
            OITCPlayer data = getPlayer(player);
            if (data.getState() != OITCPlayer.OITCState.FIGHTING) {
                return;
            }

            data.setState(OITCPlayer.OITCState.RESPAWNING);
            data.setCurrentStreak(0);

            
            EdenSound.OITC_DEATH.play(player);

            
            OITCPlayer killerData = data.getLastKiller();
            if (killerData != null) {
                Player killer = org.bukkit.Bukkit.getPlayer(killerData.getUuid());
                if (killer != null) {
                    killerData.setScore(killerData.getScore() + 1);
                    killerData.setCurrentStreak(killerData.getCurrentStreak() + 1);

                    
                    killer.setHealth(killer.getMaxHealth());

                    
                    String killMessage;
                    if (data.getLastKillType() == OITCPlayer.KillType.ARROW) {
                        killMessage = "&6" + killer.getName() + " &7shot &6" + player.getName();
                        EdenSound.OITC_ARROW_KILL.play(killer);
                    } else {
                        killMessage = "&6" + killer.getName() + " &7slayed &6" + player.getName();
                        EdenSound.OITC_MELEE_KILL.play(killer);
                    }

                    for (Player eventPlayer : getBukkitPlayers()) {
                        eventPlayer.sendMessage(CC.translate(killMessage));
                    }

                    
                    if (killerData.getScore() >= 20) {
                        preEnd(killer);
                        return;
                    }

                    
                    if (killer.getInventory().getItem(8) != null
                            && killer.getInventory().getItem(8).getType() == Material.ARROW) {
                        killer.getInventory().getItem(8).setAmount(killer.getInventory().getItem(8).getAmount() + 1);
                    } else if (killer.getInventory().getItem(8) == null
                            || killer.getInventory().getItem(8).getType() == Material.AIR) {
                        killer.getInventory().setItem(8, new ItemStack(Material.ARROW, 1));
                    } else {
                        killer.getInventory().addItem(new ItemStack(Material.ARROW, 1));
                    }
                    killer.updateInventory();
                }
            }

            
            data.setLastKiller(null);

            
            OITCPlayer oitcPlayer = data;
            BukkitTask respawnTask = new OITCRespawnTask(this, player, oitcPlayer).runTaskTimer(plugin, 0L, 20L);
            oitcPlayer.setRespawnTask(respawnTask);
        };
    }

    @Override
    public void end() {
        running = false;

        
        if (gameTask != null) {
            gameTask.cancel();
        }

        
        getPlayers().values().forEach(oitcPlayer -> {
            if (oitcPlayer.getRespawnTask() != null) {
                oitcPlayer.getRespawnTask().cancel();
            }
        });

        
        for (Player player : getBukkitPlayers()) {
            plugin.getScoreboardHandler().getScoreboard(player).unregisterHealthObjective();
        }

        
        super.end();
    }

    @Override
    public boolean canArrowPenetrate(Player shooter, Player target) {
        if (getState() != EventState.PLAYING) {
            return false;
        }

        
        OITCPlayer shooterData = getPlayer(shooter);
        OITCPlayer targetData = getPlayer(target);

        if (shooterData == null || targetData == null) {
            return false;
        }

        return shooterData.getState() == OITCPlayer.OITCState.FIGHTING
                && targetData.getState() == OITCPlayer.OITCState.FIGHTING;
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        if (getState() != EventState.PLAYING) {
            event.setCancelled(true);
            return;
        }

        Player victim = (Player) event.getEntity();
        OITCPlayer victimData = getPlayer(victim);

        
        if (victimData == null || victimData.getState() != OITCPlayer.OITCState.FIGHTING) {
            event.setCancelled(true);
            return;
        }

        if (event.getDamager() instanceof Arrow) {
            Arrow arrow = (Arrow) event.getDamager();
            if (arrow.getShooter() instanceof Player) {
                Player shooter = (Player) arrow.getShooter();
                OITCPlayer shooterData = getPlayer(shooter);

                
                if (shooterData == null || shooterData.getState() != OITCPlayer.OITCState.FIGHTING) {
                    event.setCancelled(true);
                    return;
                }

                
                victimData.setLastKiller(shooterData);
                victimData.setLastKillType(OITCPlayer.KillType.ARROW);

                
                if (shooter != victim) {
                    event.setDamage(0.0);
                    onDeath().accept(victim);
                }
            }
        } else {
            
            if (event.getDamager() instanceof Player) {
                Player damager = (Player) event.getDamager();
                OITCPlayer damagerData = getPlayer(damager);
                if (damagerData == null || damagerData.getState() != OITCPlayer.OITCState.FIGHTING) {
                    event.setCancelled(true);
                    return;
                }

                
                victimData.setLastKiller(damagerData);
                victimData.setLastKillType(OITCPlayer.KillType.MELEE);

                
            }
        }
    }

    @EventHandler
    public void onProjectileHitOverride(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow)) {
            return;
        }

        Arrow arrow = (Arrow) event.getEntity();

        
        
        if (arrow.getShooter() instanceof Player) {
            Player shooter = (Player) arrow.getShooter();
            OITCPlayer shooterData = getPlayer(shooter);

            
            if (shooterData == null || shooterData.getState() != OITCPlayer.OITCState.FIGHTING) {
                arrow.remove();
                return;
            }
        }

        
        
        event.getEntity().remove();
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (getPlayer(player) != null) {
            event.setDeathMessage(null);
            event.getDrops().clear();

            
            onDeath().accept(player);
        }
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        OITCPlayer oitcPlayer = getPlayer(player);

        
        if (oitcPlayer != null) {
            event.setFoodLevel(20);
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onItemDamage(org.bukkit.event.player.PlayerItemDamageEvent event) {
        if (getPlayer(event.getPlayer()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        OITCPlayer oitcPlayer = getPlayer(player);

        if (oitcPlayer == null) {
            return;
        }

        
        getPlayers().remove(player.getUniqueId());

        
        if (oitcPlayer.getRespawnTask() != null) {
            oitcPlayer.getRespawnTask().cancel();
        }

        
        checkForLastPlayerWin();
    }

    @Override
    public void leave(Player player) {
        OITCPlayer oitcPlayer = getPlayer(player);

        
        if (oitcPlayer != null && oitcPlayer.getRespawnTask() != null) {
            oitcPlayer.getRespawnTask().cancel();
        }

        
        super.leave(player);

        
        checkForLastPlayerWin();
    }

    /**
     * Helper method to check if only 1 player remains and award them the win.
     * Called when a player leaves (either via /leave command or disconnecting).
     */
    private void checkForLastPlayerWin() {
        
        if (getPlayers().size() == 1 && running) {
            
            UUID lastPlayerUUID = getPlayers().keySet().iterator().next();
            Player lastPlayer = Bukkit.getPlayer(lastPlayerUUID);
            if (lastPlayer != null) {
                preEnd(lastPlayer);
            } else {
                end();
            }
        }
        
    }

    public void respawnPlayer(Player player) {
        OITCPlayer data = getPlayer(player);
        if (data == null) {
            return;
        }

        data.setState(OITCPlayer.OITCState.FIGHTING);

        
        List<Location> gameSpawns = getGameSpawnLocations();
        if (!gameSpawns.isEmpty()) {
            Location spawn = gameSpawns.get(ThreadLocalRandom.current().nextInt(gameSpawns.size()));
            player.teleport(spawn);
        }

        giveRespawnItems(player);
    }

    public void giveRespawnItems(Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            
            org.bukkit.Color color = playerColors.getOrDefault(player.getUniqueId(), org.bukkit.Color.WHITE);

            
            EventLoadout loadout = new EventLoadout("OITC");
            if (loadout.exists()) {
                
                loadout.apply(player, color);
            } else {
                
                PlayerUtil.reset(player);

                ItemStack helmet = new ItemStack(Material.LEATHER_HELMET);
                org.bukkit.inventory.meta.LeatherArmorMeta helmetMeta = (org.bukkit.inventory.meta.LeatherArmorMeta) helmet
                        .getItemMeta();
                helmetMeta.setColor(color);
                helmet.setItemMeta(helmetMeta);

                ItemStack chestplate = new ItemStack(Material.LEATHER_CHESTPLATE);
                org.bukkit.inventory.meta.LeatherArmorMeta chestMeta = (org.bukkit.inventory.meta.LeatherArmorMeta) chestplate
                        .getItemMeta();
                chestMeta.setColor(color);
                chestplate.setItemMeta(chestMeta);

                player.getInventory().setHelmet(helmet);
                player.getInventory().setChestplate(chestplate);
                player.getInventory().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
                player.getInventory().setBoots(new ItemStack(Material.IRON_BOOTS));

                player.getInventory().setItem(0, new ItemStack(Material.STONE_SWORD));
                player.getInventory().setItem(1, new ItemStack(Material.BOW));
                player.getInventory().setItem(8, new ItemStack(Material.ARROW));
                player.updateInventory();
            }
        });
    }

    public void teleportNextLocation(Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            List<Location> gameSpawns = getGameSpawnLocations();
            if (gameSpawns != null && !gameSpawns.isEmpty()) {
                Location spawn = gameSpawns.get(ThreadLocalRandom.current().nextInt(gameSpawns.size()));
                player.teleport(spawn);
            }
        });
    }

    private void preEnd(Player winner) {
        if (getState() == EventState.ENDING)
            return;
        this.winner = winner;
        this.topPlayers = getPlayers().values().stream()
                .sorted(Comparator.comparingInt(OITCPlayer::getScore).reversed())
                .limit(3)
                .collect(Collectors.toList());

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

    @RequiredArgsConstructor
    public class OITCGameTask extends BukkitRunnable {

        private int time = 300;

        @Override
        public void run() {
            if (time == 300) {
                String message = getLanguageString("EVENT.STARTED", "&aEvent has started!");
                sendMessage(message);

                
                setState(EventState.PLAYING);

                
                for (Player player : getBukkitPlayers()) {
                    plugin.getScoreboardHandler().getScoreboard(player).registerHealthObjective();
                }

                getPlayers().forEach((uuid, oitcPlayer) -> {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null) {
                        respawnPlayer(player);
                    }
                });
            } else if (time <= 0) {
                
                OITCPlayer winnerData = players.values().stream()
                        .max(Comparator.comparingInt(OITCPlayer::getScore))
                        .orElse(null);

                if (winnerData != null) {
                    Player winner = Bukkit.getPlayer(winnerData.getUuid());
                    preEnd(winner);
                } else {
                    end();
                }
                cancel();
                return;
            }

            time--;
        }
    }

    public class OITCCountdown extends EventCountdownTask {
        private final int requiredPlayers;

        public OITCCountdown(PracticeEvent<?> event, int requiredPlayers) {
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
