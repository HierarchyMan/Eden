package rip.diamond.practice.events.games.skywars;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import rip.diamond.practice.Eden;
import rip.diamond.practice.events.EventCountdownTask;
import rip.diamond.practice.events.EventState;
import rip.diamond.practice.events.EventLoadout;
import rip.diamond.practice.events.PracticeEvent;
import rip.diamond.practice.profile.PlayerProfile;
import rip.diamond.practice.profile.PlayerState;
import rip.diamond.practice.util.CC;
import rip.diamond.practice.util.cuboid.Cuboid;
import rip.diamond.practice.util.PlayerUtil;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Getter
public class SkyWarsEvent extends PracticeEvent<SkyWarsPlayer> implements Listener {

    private final Map<UUID, SkyWarsPlayer> players = new HashMap<>();
    private SkyWarsCountdown eventCountdown;
    private SkyWarsGameTask gameTask;
    private Cuboid arenaCuboid;
    private List<Location> chestLocations = new ArrayList<>();
    private Player winner;
    private List<SkyWarsPlayer> topPlayers = new ArrayList<>();

    public SkyWarsEvent() {
        super("SkyWars");
        this.eventCountdown = new SkyWarsCountdown(this, 2);
    }

    @Override
    public Map<UUID, SkyWarsPlayer> getPlayers() {
        return players;
    }

    @Override
    public EventCountdownTask getCountdownTask() {

        if (eventCountdown == null || eventCountdown.isEnded()) {
            eventCountdown = new SkyWarsCountdown(this, 2);
        }
        return eventCountdown;
    }

    @Override
    public boolean canDropItems() {
        return true;
    }

    @Override
    public boolean canPickupItems() {
        return true;
    }

    @Override
    public List<Location> getSpawnLocations() {
        return new ArrayList<>(plugin.getSpawnManager().getSkywarsLocations());
    }

    @Override
    public void onStart() {

        Location min = plugin.getSpawnManager().getSkywarsMin();
        Location max = plugin.getSpawnManager().getSkywarsMax();
        if (min != null && max != null) {
            this.arenaCuboid = new Cuboid(min, max);

            plugin.getChunkRestorationManager().copy(this.arenaCuboid);
        }

        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        gameTask = new SkyWarsGameTask();
        gameTask.runTaskTimer(plugin, 0, 20L);
    }

    /**
     * Cache chest locations when event starts waiting
     * This scans blocks over multiple ticks to prevent lag spikes
     */
    public void cacheChestLocations() {
        if (arenaCuboid == null) {
            Location min = plugin.getSpawnManager().getSkywarsMin();
            Location max = plugin.getSpawnManager().getSkywarsMax();
            if (min != null && max != null) {
                this.arenaCuboid = new Cuboid(min, max);
            }
        }

        if (arenaCuboid == null) {
            return;
        }

        final List<Block> allBlocks = new ArrayList<>();
        for (Block block : arenaCuboid) {
            allBlocks.add(block);
        }

        final List<Location> chests = new ArrayList<>();
        final int blocksPerTick = 500;
        final int[] currentIndex = { 0 };

        new BukkitRunnable() {
            @Override
            public void run() {
                int endIndex = Math.min(currentIndex[0] + blocksPerTick, allBlocks.size());

                for (int i = currentIndex[0]; i < endIndex; i++) {
                    Block block = allBlocks.get(i);
                    if (block.getType() == Material.CHEST || block.getType() == Material.TRAPPED_CHEST) {
                        chests.add(block.getLocation());
                    }
                }

                currentIndex[0] = endIndex;

                if (currentIndex[0] >= allBlocks.size()) {
                    chestLocations = chests;
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Populate only the cached chest locations
     */
    private void populateCachedChests() {
        for (Location loc : chestLocations) {
            Block block = loc.getBlock();
            if (block.getType() == Material.CHEST || block.getType() == Material.TRAPPED_CHEST) {
                Chest chest = (Chest) block.getState();
                ItemStack[] items = plugin.getChestManager().getRandomItemsFromChests();
                if (items != null) {
                    chest.getInventory().setContents(items);
                    chest.update();
                }
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
        return player -> {
            players.put(player.getUniqueId(), new SkyWarsPlayer(player.getUniqueId(), this));

            if (players.size() == 1 && chestLocations.isEmpty()) {
                cacheChestLocations();
            }
        };
    }

    @Override
    public Consumer<Player> onDeath() {
        return player -> {
            SkyWarsPlayer data = getPlayer(player);
            if (data.getState() != SkyWarsPlayer.SkyWarsState.FIGHTING) {
                return;
            }

            Player killer = player.getKiller();
            data.setState(SkyWarsPlayer.SkyWarsState.ELIMINATED);

            if (killer != null) {
                SkyWarsPlayer killerData = getPlayer(killer);
                if (killerData != null) {
                    killerData.setKills(killerData.getKills() + 1);
                }
            }

            plugin.getEventManager().addSpectator(player,
                    rip.diamond.practice.profile.PlayerProfile.get(player), this);

            sendMessage(ChatColor.RED + player.getName() + ChatColor.GRAY + " was eliminated" +
                    (killer == null ? "." : " by " + ChatColor.RED + killer.getName()) + ChatColor.GRAY + ".");

            checkForWin();
        };
    }

    private void checkForWin() {
        List<UUID> fighting = getByState(SkyWarsPlayer.SkyWarsState.FIGHTING);
        if (fighting.size() <= 1) {
            Player winner = fighting.isEmpty() ? null : Bukkit.getPlayer(fighting.get(0));
            preEnd(winner);
        }
    }

    @Override
    public void leave(Player player) {
        SkyWarsPlayer skyWarsPlayer = getPlayer(player);
        if (skyWarsPlayer != null) {
            if (skyWarsPlayer.getState() == SkyWarsPlayer.SkyWarsState.FIGHTING) {

                skyWarsPlayer.setState(SkyWarsPlayer.SkyWarsState.ELIMINATED);
                sendMessage(ChatColor.RED + player.getName() + ChatColor.GRAY + " disconnected.");
            }
        }

        super.leave(player);

        if (getState() == EventState.PLAYING) {
            checkForWin();
        }
    }

    public List<UUID> getByState(SkyWarsPlayer.SkyWarsState state) {
        return players.values().stream().filter(player -> player.getState() == state)
                .map(SkyWarsPlayer::getUuid).collect(Collectors.toList());
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        PlayerProfile profile = PlayerProfile.get(player);

        if (profile.getPlayerState() == PlayerState.IN_EVENT && getPlayers().containsKey(player.getUniqueId())) {
            SkyWarsPlayer data = getPlayer(player);

            if (getState() != EventState.PLAYING) {
                event.setCancelled(true);
                player.updateInventory();
                return;
            }

            if (data.getState() != SkyWarsPlayer.SkyWarsState.FIGHTING) {
                event.setCancelled(true);
                player.updateInventory();
                return;
            }

            if (arenaCuboid != null && !arenaCuboid.contains(event.getBlock().getLocation())) {
                event.setCancelled(true);
                player.updateInventory();
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        PlayerProfile profile = PlayerProfile.get(player);

        if (profile.getPlayerState() == PlayerState.IN_EVENT && getPlayers().containsKey(player.getUniqueId())) {
            SkyWarsPlayer data = getPlayer(player);

            if (getState() != EventState.PLAYING) {
                event.setCancelled(true);
                player.updateInventory();
                return;
            }

            if (data.getState() != SkyWarsPlayer.SkyWarsState.FIGHTING) {
                event.setCancelled(true);
                player.updateInventory();
                return;
            }

            if (arenaCuboid != null && !arenaCuboid.contains(event.getBlock().getLocation())) {
                event.setCancelled(true);
                player.updateInventory();
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        PlayerProfile profile = PlayerProfile.get(player);

        if (profile.getPlayerState() == PlayerState.IN_EVENT && getPlayers().containsKey(player.getUniqueId())) {
            SkyWarsPlayer data = getPlayer(player);

            if (getState() != EventState.PLAYING) {
                event.setCancelled(true);
                return;
            }

            if (data.getState() != SkyWarsPlayer.SkyWarsState.FIGHTING) {

                if (event.getClickedBlock() != null &&
                        (event.getClickedBlock().getType() == Material.CHEST ||
                                event.getClickedBlock().getType() == Material.TRAPPED_CHEST)) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @Getter
    @RequiredArgsConstructor
    public class SkyWarsGameTask extends BukkitRunnable {

        private int time = 303;

        @Override
        public void run() {
            if (time == 303) {
                sendMessage(plugin.getLanguageFile().getConfiguration()
                        .getString("event.event-start-countdown.message")
                        .replace("{0}", getName())
                        .replace("{1}", String.valueOf(time - 300)));
            } else if (time == 300) {
                sendMessage(plugin.getLanguageFile().getConfiguration()
                        .getString("event.new-round-start.message"));

                setState(EventState.PLAYING);

                if (!chestLocations.isEmpty()) {
                    populateCachedChests();
                }

                List<Location> spawnList = new ArrayList<>(getSpawnLocations());
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    getPlayers().forEach((uuid, skyWarsPlayer) -> {
                        skyWarsPlayer.setState(SkyWarsPlayer.SkyWarsState.FIGHTING);

                        Player player = Bukkit.getPlayer(uuid);
                        if (!spawnList.isEmpty()) {
                            player.teleport(spawnList.remove(ThreadLocalRandom.current().nextInt(spawnList.size())));
                        }

                        EventLoadout loadout = new EventLoadout("SkyWars");
                        if (loadout.exists()) {
                            loadout.apply(player);
                        } else {

                            getKitOptional().ifPresent(kit -> kit.getKitLoadout().apply(kit, null, player));
                        }
                    });
                });
            } else if (time <= 0) {

                preEnd(null);
                cancel();
                return;
            }

            if (getPlayers().size() == 1) {
                checkForWin();
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
            lines = new ArrayList<>(Eden.INSTANCE.getScoreboardFile().getLines("SKYWARS", "WAITING"));
        } else if (getState() == EventState.ENDING) {
            lines = new ArrayList<>(Eden.INSTANCE.getScoreboardFile().getLines("SKYWARS", "ENDING"));
        } else {

            lines = new ArrayList<>(Eden.INSTANCE.getScoreboardFile().getLines("SKYWARS", "PLAYING"));
        }

        List<String> replaced = new ArrayList<>();
        for (String line : lines) {

            line = line.replace("{event_name}", getName());
            line = line.replace("{event_players}", String.valueOf(getPlayers().size()));
            line = line.replace("{event_limit}", String.valueOf(getLimit()));
            line = line.replace("{event_host}", getHost() != null ? getHost().getName() : "None");

            if (getState() == EventState.PLAYING) {
                SkyWarsPlayer data = getPlayer(player);
                if (data != null) {
                    line = line.replace("{player_kills}", String.valueOf(data.getKills()));
                } else {
                    line = line.replace("{player_kills}", "0");
                }
                line = line.replace("{players_remaining}",
                        String.valueOf(getByState(SkyWarsPlayer.SkyWarsState.FIGHTING).size()));
                line = line.replace("{players_eliminated}",
                        String.valueOf(getByState(SkyWarsPlayer.SkyWarsState.ELIMINATED).size()));
            } else if (getState() == EventState.ENDING) {
                line = line.replace("{winner_name}", winner != null ? winner.getName() : "None");
                line = line.replace("{player_kills}", "0");
                line = line.replace("{players_remaining}", "0");
                line = line.replace("{players_eliminated}", String.valueOf(getPlayers().size()));
            } else {
                line = line.replace("{player_kills}", "0");
                line = line.replace("{players_remaining}", String.valueOf(getPlayers().size()));
                line = line.replace("{players_eliminated}", "0");
            }

            replaced.add(CC.translate(line));
        }

        return replaced;
    }

    public class SkyWarsCountdown extends EventCountdownTask {
        private final int requiredPlayers;

        public SkyWarsCountdown(PracticeEvent<?> event, int requiredPlayers) {
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
