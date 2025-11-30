package rip.diamond.practice.events.games.corners;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.Color;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import rip.diamond.practice.Eden;
import rip.diamond.practice.events.EventCountdownTask;
import rip.diamond.practice.events.EventState;
import rip.diamond.practice.events.PracticeEvent;
import rip.diamond.practice.util.CC;
import rip.diamond.practice.util.PlayerUtil;
import rip.diamond.practice.util.cuboid.Cuboid;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Getter
public class FourCornersEvent extends PracticeEvent<FourCornersPlayer> implements Listener {

    private final Map<UUID, FourCornersPlayer> players = new HashMap<>();
    private FourCornersCountdown eventCountdown;
    private final List<BukkitTask> scheduledTasks = new ArrayList<>();

    private CornersGameTask gameTask;
    private MoveTask moveTask;
    private RemoveBlocksTask removeBlocksTask;
    private Map<Location, ItemStack> blocks;
    private int seconds, randomWool, round;
    private boolean running = false;
    private Cuboid zone;

    public FourCornersEvent() {
        super("Four Corners");
        this.eventCountdown = new FourCornersCountdown(this, 2);
    }

    @Override
    public Map<UUID, FourCornersPlayer> getPlayers() {
        return players;
    }

    @Override
    public EventCountdownTask getCountdownTask() {
        
        if (eventCountdown == null || eventCountdown.isEnded()) {
            eventCountdown = new FourCornersCountdown(this, 2);
        }
        return eventCountdown;
    }

    @Override
    public List<Location> getSpawnLocations() {
        return Collections.singletonList(plugin.getSpawnManager().getCornersLocation());
    }

    @Override
    public void onStart() {
        seconds = 11;
        round = 1;
        gameTask = new CornersGameTask();
        gameTask.runTaskTimerAsynchronously(plugin, 0L, 20L);
        blocks = new HashMap<>();

        Location min = plugin.getSpawnManager().getCornersMin();
        Location max = plugin.getSpawnManager().getCornersMax();
        if (min != null && max != null) {
            zone = new Cuboid(min, max);
        }

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void cancelAll() {
        if (gameTask != null) {
            gameTask.cancel();
        }
        if (moveTask != null) {
            moveTask.cancel();
        }
        if (removeBlocksTask != null) {
            removeBlocksTask.cancel();
        }

        
        for (BukkitTask task : scheduledTasks) {
            if (task != null) {
                task.cancel();
            }
        }
        scheduledTasks.clear();

        running = false;
        zone = null;
    }

    @Override
    public Consumer<Player> onJoin() {
        return player -> players.put(player.getUniqueId(), new FourCornersPlayer(player.getUniqueId(), this));
    }

    @Override
    public Consumer<Player> onDeath() {
        return player -> {
            FourCornersPlayer data = getPlayer(player);

            if (data.getState() != FourCornersPlayer.FourCornersState.PLAYING) {
                return;
            }

            data.setState(FourCornersPlayer.FourCornersState.ELIMINATED);

            getPlayers().remove(player.getUniqueId());

            String eliminatedMsg = getLanguageString("EVENT.PLAYER-ELIMINATED",
                    "&c<player> has been eliminated from <eventName>!");
            sendMessage(eliminatedMsg
                    .replace("<eventName>", this.getName())
                    .replace("<player>", player.getName()));

            String playerEliminatedMsg = getLanguageString("EVENT.ELIMINATED",
                    "&cYou have been eliminated from <eventName>!");
            player.sendMessage(CC.translate(playerEliminatedMsg
                    .replace("<eventName>", this.getName())));

            if (getByState(FourCornersPlayer.FourCornersState.PLAYING).size() == 1) {
                Player winner = Bukkit.getPlayer(getByState(FourCornersPlayer.FourCornersState.PLAYING).get(0));
                if (winner != null) {
                    handleWin(winner);
                }

                end();
                cancelAll();

                BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (blocks != null) {
                        blocks.forEach(((location, stack) -> location.getBlock()
                                .setTypeIdAndData(stack.getTypeId(), (byte) stack.getDurability(), true)));
                        blocks.clear();
                    }
                }, 40L);
                scheduledTasks.add(task);
            }
        };
    }

    public List<UUID> getByState(FourCornersPlayer.FourCornersState state) {
        return players.values().stream().filter(player -> player.getState() == state)
                .map(FourCornersPlayer::getUuid).collect(Collectors.toList());
    }

    @Override
    public void end() {
        
        cancelAll();

        
        if (blocks != null && !blocks.isEmpty()) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                blocks.forEach(((location, stack) -> location.getBlock()
                        .setTypeIdAndData(stack.getTypeId(), (byte) stack.getDurability(), true)));
                blocks.clear();
            });
        }

        
        super.end();
    }

    @RequiredArgsConstructor
    public class CornersGameTask extends BukkitRunnable {

        private int time = 303;

        @Override
        public void run() {
            if (time == 303) {
                String message = getLanguageString("EVENT.STARTING-IN", "&aEvent starting in <countdown> seconds...");
                sendMessage(message.replace("<countdown>", String.valueOf(time - 300)));
            } else if (time == 300) {
                String message = getLanguageString("EVENT.STARTED", "&aEvent has started!");
                sendMessage(message);

                
                setState(rip.diamond.practice.events.EventState.PLAYING);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        getBukkitPlayers().stream()
                                .filter(player -> getPlayers().containsKey(player.getUniqueId())).forEach(
                                        player -> player.teleport(plugin.getSpawnManager().getCornersLocation()));
                    }
                }.runTask(plugin);

                getPlayers().values()
                        .forEach(player -> player.setState(FourCornersPlayer.FourCornersState.PLAYING));
                getBukkitPlayers().forEach(player -> player.getInventory().clear());

                moveTask = new MoveTask();
                moveTask.runTaskTimer(plugin, 0, 1L);

                removeBlocksTask = new RemoveBlocksTask();
                removeBlocksTask.runTaskTimer(plugin, 0L, 20L);
                running = true;
            } else if (time <= 0) {
                Player winner = getRandomPlayer();
                if (winner != null) {
                    handleWin(winner);
                }

                end();
                cancelAll();
                cancel();
                return;
            }

            if (getPlayers().size() == 1 && running) {
                Player winner = Bukkit.getPlayer(getByState(FourCornersPlayer.FourCornersState.PLAYING).get(0));
                if (winner != null) {
                    handleWin(winner);
                }

                end();
                cancelAll();
                cancel();
                return;
            }

            time--;
        }
    }

    private Player getRandomPlayer() {
        if (getByState(FourCornersPlayer.FourCornersState.PLAYING).size() == 0) {
            return null;
        }

        List<UUID> fighting = getByState(FourCornersPlayer.FourCornersState.PLAYING);
        Collections.shuffle(fighting);
        UUID uuid = fighting.get(ThreadLocalRandom.current().nextInt(fighting.size()));

        return plugin.getServer().getPlayer(uuid);
    }

    @RequiredArgsConstructor
    private class MoveTask extends BukkitRunnable {

        @Override
        public void run() {
            getBukkitPlayers().forEach(player -> {
                if (getPlayer(player.getUniqueId()) != null
                        && getPlayer(player.getUniqueId()).getState() == FourCornersPlayer.FourCornersState.PLAYING) {
                    if (getPlayers().size() <= 1) {
                        return;
                    }
                    if (getPlayers().containsKey(player.getUniqueId())) {
                        
                        Block blockBelow = player.getLocation().subtract(0, 1, 0).getBlock();

                        if (blockBelow.getType() == Material.WOOL) {
                            if (blockBelow.getData() == (byte) randomWool) {
                                
                                onDeath().accept(player);
                            } else {
                                
                                giveColoredBoots(player, blockBelow.getData());
                            }
                        } else {
                            
                            if (player.getInventory().getBoots() != null) {
                                player.getInventory().setBoots(null);
                                player.updateInventory();
                            }

                            if (PlayerUtil.isStandingOnLiquid(player)) {
                                
                                onDeath().accept(player);
                            }
                        }
                    }
                }
            });
        }
    }

    private void giveColoredBoots(Player player, byte data) {
        ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
        LeatherArmorMeta meta = (LeatherArmorMeta) boots.getItemMeta();
        meta.setColor(getColorFromWoolData(data));
        boots.setItemMeta(meta);

        
        ItemStack current = player.getInventory().getBoots();
        if (current == null || !current.isSimilar(boots)) {
            player.getInventory().setBoots(boots);
            player.updateInventory();
        }
    }

    private Color getColorFromWoolData(byte data) {
        switch (data) {
            case 14:
                return Color.RED;
            case 11:
                return Color.BLUE;
            case 5:
                return Color.LIME;
            case 4:
                return Color.YELLOW;
            default:
                return Color.WHITE;
        }
    }

    @RequiredArgsConstructor
    private class RemoveBlocksTask extends BukkitRunnable {

        @Override
        public void run() {
            if (!running) {
                return;
            }

            seconds--;

            if (seconds <= 0) {
                running = false;
                handleRemoveBridges(true);
                BukkitTask task1 = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    handleRemoveBridges(false);

                    BukkitTask task2 = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (blocks != null) {
                            blocks.forEach(((location, stack) -> location.getBlock()
                                    .setTypeIdAndData(stack.getTypeId(), (byte) stack.getDurability(), true)));
                            blocks.clear();
                        }
                    }, 60L);
                    scheduledTasks.add(task2);

                    BukkitTask task3 = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        round++;
                        seconds = 11;
                        running = true;
                    }, 100L);
                    scheduledTasks.add(task3);
                }, 60L);
                scheduledTasks.add(task1);

                return;
            }

            if (Arrays.asList(10, 5, 4, 3, 2, 1).contains(seconds)) {
                sendMessage(
                        CC.translate("&bBridges are dropping in &f" + seconds + " &bseconds... (Round " + round + ")"));
            }
        }
    }

    private void handleRemoveBridges(boolean bridges) {
        randomWool = getRandomWool();

        if (zone != null) {
            zone.forEach(block -> {
                if (bridges) {
                    if (!block.getType().equals(Material.WOOL)) {
                        blocks.put(block.getLocation(), new ItemStack(block.getType(), 1, block.getData()));
                        block.setType(Material.AIR);
                    }
                } else {
                    if (block.getType().equals(Material.WOOL) && block.getData() == (byte) randomWool) {
                        blocks.put(block.getLocation(), new ItemStack(block.getType(), 1, (short) randomWool));
                        block.setType(Material.AIR);
                        block.getLocation().getWorld().strikeLightningEffect(block.getLocation());
                    }
                }
            });
        }

        if (!bridges) {
            String color = (randomWool == 14 ? "&cRed"
                    : randomWool == 11 ? "&9Blue" : randomWool == 5 ? "&aGreen" : "&eYellow");
            sendMessage(CC.translate("&bThe " + color + " &bcorner has been dropped!"));
        }
    }

    private int getRandomWool() {
        List<Integer> wools = Arrays.asList(14, 11, 5, 4);
        return wools.get(ThreadLocalRandom.current().nextInt(wools.size()));
    }

    @Override
    public List<String> getScoreboard(Player player) {
        List<String> lines;

        if (getState() == EventState.WAITING || getState() == EventState.STARTING) {
            lines = new ArrayList<>(Eden.INSTANCE.getScoreboardFile().getLines("FOUR_CORNERS", "WAITING"));
        } else {
            
            lines = new ArrayList<>(Eden.INSTANCE.getScoreboardFile().getLines("FOUR_CORNERS", "PLAYING"));
        }

        
        List<String> replaced = new ArrayList<>();
        for (String line : lines) {
            
            line = line.replace("{event_name}", getName());
            line = line.replace("{event_players}", String.valueOf(getPlayers().size()));
            line = line.replace("{event_limit}", String.valueOf(getLimit()));
            line = line.replace("{event_host}", getHost() != null ? getHost().getName() : "None");

            
            if (getState() == EventState.PLAYING) {
                line = line.replace("{round}", String.valueOf(round));
                line = line.replace("{players_remaining}",
                        String.valueOf(getByState(FourCornersPlayer.FourCornersState.PLAYING).size()));
                line = line.replace("{players_eliminated}",
                        String.valueOf(getByState(FourCornersPlayer.FourCornersState.ELIMINATED).size()));

                if (gameTask != null) {
                    
                    
                    
                    line = line.replace("{time}", String.valueOf(seconds));
                } else {
                    line = line.replace("{time}", "0");
                }
            } else {
                line = line.replace("{round}", "0");
                line = line.replace("{players_remaining}", String.valueOf(getPlayers().size()));
                line = line.replace("{players_eliminated}", "0");
                line = line.replace("{time}", "0");
            }

            replaced.add(CC.translate(line));
        }

        return replaced;
    }

    public class FourCornersCountdown extends EventCountdownTask {
        private final int requiredPlayers;

        public FourCornersCountdown(PracticeEvent<?> event, int requiredPlayers) {
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
