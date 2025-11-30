package rip.diamond.practice.events.games.parkour;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import rip.diamond.practice.Eden;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;

import rip.diamond.practice.events.EventCountdownTask;
import rip.diamond.practice.events.PracticeEvent;
import rip.diamond.practice.util.CC;

import java.util.*;
import java.util.function.Consumer;
import rip.diamond.practice.events.EventState;

@Getter
public class ParkourEvent extends PracticeEvent<ParkourPlayer> implements Listener {

    private final Map<UUID, ParkourPlayer> players = new HashMap<>();
    private ParkourCountdown eventCountdown;
    private ParkourGameTask gameTask;

    public ParkourEvent() {
        super("Parkour");
        this.eventCountdown = new ParkourCountdown(this, 2);
    }

    @Override
    public Map<UUID, ParkourPlayer> getPlayers() {
        return players;
    }

    @Override
    public EventCountdownTask getCountdownTask() {
        
        if (eventCountdown == null || eventCountdown.isEnded()) {
            eventCountdown = new ParkourCountdown(this, 2);
        }
        return eventCountdown;
    }

    @Override
    public List<Location> getSpawnLocations() {
        return Collections.singletonList(plugin.getSpawnManager().getParkourLocation());
    }

    @Override
    public List<String> getScoreboard(Player player) {
        List<String> lines;

        if (getState() == EventState.WAITING || getState() == EventState.STARTING) {
            lines = new ArrayList<>(Eden.INSTANCE.getScoreboardFile().getLines("PARKOUR", "WAITING"));
        } else {
            
            lines = new ArrayList<>(Eden.INSTANCE.getScoreboardFile().getLines("PARKOUR", "PLAYING"));
        }

        
        List<String> replaced = new ArrayList<>();
        for (String line : lines) {
            
            line = line.replace("{event_name}", getName());
            line = line.replace("{event_players}", String.valueOf(getPlayers().size()));
            line = line.replace("{event_limit}", String.valueOf(getLimit()));
            line = line.replace("{event_host}", getHost() != null ? getHost().getName() : "None");

            
            if (getState() == EventState.PLAYING) {
                ParkourPlayer data = getPlayer(player);
                if (data != null) {
                    line = line.replace("{checkpoint}", String.valueOf(data.getCurrentCheckpoint()));

                    
                    List<ParkourPlayer> sortedPlayers = new ArrayList<>(getPlayers().values());
                    sortedPlayers.sort((p1, p2) -> {
                        if (p1.getCurrentCheckpoint() != p2.getCurrentCheckpoint()) {
                            return Integer.compare(p2.getCurrentCheckpoint(), p1.getCurrentCheckpoint());
                        }
                        return Long.compare(p1.getCheckpointTime(), p2.getCheckpointTime());
                    });

                    int position = sortedPlayers.indexOf(data) + 1;
                    line = line.replace("{position}", String.valueOf(position) + getOrdinal(position));

                    
                    for (int i = 0; i < 3; i++) {
                        String placeholder = "{top_" + (i + 1) + "}";
                        if (line.contains(placeholder)) {
                            if (i < sortedPlayers.size()) {
                                ParkourPlayer topPlayer = sortedPlayers.get(i);
                                Player topBukkitPlayer = Bukkit.getPlayer(topPlayer.getUuid());
                                String name = topBukkitPlayer != null ? topBukkitPlayer.getName() : "Unknown";
                                line = line.replace(placeholder,
                                        name + " (CP " + topPlayer.getCurrentCheckpoint() + ")");
                            } else {
                                line = line.replace(placeholder, "None");
                            }
                        }
                    }
                } else {
                    line = line.replace("{checkpoint}", "0");
                    line = line.replace("{position}", "N/A");
                    line = line.replace("{top_1}", "None");
                    line = line.replace("{top_2}", "None");
                    line = line.replace("{top_3}", "None");
                }

                line = line.replace("{completions}", String.valueOf(getPlayers().values().stream()
                        .filter(p -> p.getState() == ParkourPlayer.ParkourState.FINISHED).count()));

                if (gameTask != null) {
                    int minutes = gameTask.time / 60;
                    int seconds = gameTask.time % 60;
                    line = line.replace("{time}", String.format("%d:%02d", minutes, seconds));
                } else {
                    line = line.replace("{time}", "0:00");
                }
            } else {
                
                line = line.replace("{checkpoint}", "0");
                line = line.replace("{position}", "N/A");
                line = line.replace("{top_1}", "None");
                line = line.replace("{top_2}", "None");
                line = line.replace("{top_3}", "None");
                line = line.replace("{completions}", "0");
                line = line.replace("{time}", "0:00");
            }

            replaced.add(CC.translate(line));
        }

        return replaced;
    }

    private String getOrdinal(int i) {
        String[] suffixes = new String[] { "th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th" };
        switch (i % 100) {
            case 11:
            case 12:
            case 13:
                return "th";
            default:
                return suffixes[i % 10];
        }
    }

    @Override
    public void onStart() {
        gameTask = new ParkourGameTask();
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
        return player -> players.put(player.getUniqueId(), new ParkourPlayer(player.getUniqueId(), this));
    }

    @Override
    public Consumer<Player> onDeath() {
        return player -> {
            ParkourPlayer data = getPlayer(player);
            if (data.getState() != ParkourPlayer.ParkourState.PLAYING) {
                return;
            }
            teleportToCheckpoint(player, data);
        };
    }

    private void teleportToCheckpoint(Player player, ParkourPlayer data) {
        List<Location> checkpoints = plugin.getSpawnManager().getParkourCheckpoints();
        if (checkpoints != null && !checkpoints.isEmpty()) {
            if (data.getCurrentCheckpoint() > 0 && data.getCurrentCheckpoint() <= checkpoints.size()) {
                player.teleport(checkpoints.get(data.getCurrentCheckpoint() - 1));
            } else {
                player.teleport(plugin.getSpawnManager().getParkourGameLocation());
            }
        } else {
            player.teleport(plugin.getSpawnManager().getParkourGameLocation());
        }
    }

    private void giveItems(Player player) {
        player.getInventory().clear();

        ItemStack checkpoint = new ItemStack(Material.WATCH);
        net.minecraft.server.v1_8_R3.ItemStack nmsCheckpoint = CraftItemStack.asNMSCopy(checkpoint);
        NBTTagCompound tag = nmsCheckpoint.hasTag() ? nmsCheckpoint.getTag() : new NBTTagCompound();
        tag.setString("command", "eden:parkour checkpoint");
        nmsCheckpoint.setTag(tag);
        checkpoint = CraftItemStack.asBukkitCopy(nmsCheckpoint);
        org.bukkit.inventory.meta.ItemMeta checkpointMeta = checkpoint.getItemMeta();
        checkpointMeta.setDisplayName(CC.GREEN + "Teleport to Checkpoint");
        checkpoint.setItemMeta(checkpointMeta);

        ItemStack hide = new ItemStack(Material.INK_SACK, 1, (short) 10); 
        net.minecraft.server.v1_8_R3.ItemStack nmsHide = CraftItemStack.asNMSCopy(hide);
        NBTTagCompound tagHide = nmsHide.hasTag() ? nmsHide.getTag() : new NBTTagCompound();
        tagHide.setString("command", "eden:parkour hide");
        nmsHide.setTag(tagHide);
        hide = CraftItemStack.asBukkitCopy(nmsHide);
        org.bukkit.inventory.meta.ItemMeta hideMeta = hide.getItemMeta();
        hideMeta.setDisplayName(CC.GREEN + "Hide Players");
        hide.setItemMeta(hideMeta);

        player.getInventory().setItem(0, checkpoint);
        player.getInventory().setItem(8, hide);
        player.updateInventory();
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        ParkourPlayer data = getPlayer(player);

        if (data != null && data.getState() == ParkourPlayer.ParkourState.PLAYING) {
            Block block = event.getTo().getBlock().getRelative(0, -1, 0);
            if (block.getType() == Material.GOLD_BLOCK || block.getType() == Material.EMERALD_BLOCK) {
                data.setState(ParkourPlayer.ParkourState.FINISHED);
                sendMessage(CC.GREEN + player.getName() + " has finished the Parkour!");
                handleWin(player);
                end();
            } else if (event.getTo().getY() < plugin.getSpawnManager().getParkourGameLocation().getY() - 10) {
                onDeath().accept(player);
            }

            
            List<Location> checkpoints = plugin.getSpawnManager().getParkourCheckpoints();
            if (checkpoints != null && !checkpoints.isEmpty()) {
                int nextCheckpointIndex = data.getCurrentCheckpoint();
                if (nextCheckpointIndex < checkpoints.size()) {
                    Location nextCheckpoint = checkpoints.get(nextCheckpointIndex);
                    if (player.getLocation().distanceSquared(nextCheckpoint) < 4) { 
                        data.setCurrentCheckpoint(nextCheckpointIndex + 1);
                        data.setCheckpointTime(System.currentTimeMillis());
                        player.sendMessage(CC.GREEN + "Reached Checkpoint #" + (nextCheckpointIndex + 1) + "!");
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ParkourPlayer data = getPlayer(player);

        if (data != null && data.getState() == ParkourPlayer.ParkourState.PLAYING) {
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                if (event.getItem() != null) {
                    net.minecraft.server.v1_8_R3.ItemStack nmsItem = CraftItemStack.asNMSCopy(event.getItem());
                    if (nmsItem != null && nmsItem.hasTag()) {
                        NBTTagCompound compound = nmsItem.getTag();
                        if (compound.hasKey("command")) {
                            String command = compound.getString("command");
                            if (command.equals("eden:parkour checkpoint")) {
                                event.setCancelled(true);
                                teleportToCheckpoint(player, data);
                                player.sendMessage(CC.GREEN + "Teleported back to the checkpoint!");
                            } else if (command.equals("eden:parkour hide")) {
                                event.setCancelled(true);
                                toggleVisibility(player);
                            }
                        }
                    }
                }
            }
        }
    }

    public void toggleVisibility(Player player) {
        
        
        boolean visible = !player.hasMetadata("hide_players"); 

        if (visible) {
            
            player.setMetadata("hide_players", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
            getBukkitPlayers().forEach(p -> {
                if (!p.getUniqueId().equals(player.getUniqueId())) {
                    player.hidePlayer(p);
                }
            });
            player.sendMessage(CC.YELLOW + "Players hidden.");

            
            ItemStack show = new ItemStack(Material.INK_SACK, 1, (short) 8); 
            net.minecraft.server.v1_8_R3.ItemStack nmsShow = CraftItemStack.asNMSCopy(show);
            NBTTagCompound tagShow = nmsShow.hasTag() ? nmsShow.getTag() : new NBTTagCompound();
            tagShow.setString("command", "eden:parkour hide"); 
            nmsShow.setTag(tagShow);
            show = CraftItemStack.asBukkitCopy(nmsShow);
            org.bukkit.inventory.meta.ItemMeta showMeta = show.getItemMeta();
            showMeta.setDisplayName(CC.GRAY + "Show Players");
            show.setItemMeta(showMeta);
            player.getInventory().setItem(8, show);

        } else {
            
            player.removeMetadata("hide_players", plugin);
            getBukkitPlayers().forEach(p -> {
                if (!p.getUniqueId().equals(player.getUniqueId())) {
                    player.showPlayer(p);
                }
            });
            player.sendMessage(CC.GREEN + "Players shown.");

            
            ItemStack hide = new ItemStack(Material.INK_SACK, 1, (short) 10); 
            net.minecraft.server.v1_8_R3.ItemStack nmsHide = CraftItemStack.asNMSCopy(hide);
            NBTTagCompound tagHide = nmsHide.hasTag() ? nmsHide.getTag() : new NBTTagCompound();
            tagHide.setString("command", "eden:parkour hide");
            nmsHide.setTag(tagHide);
            hide = CraftItemStack.asBukkitCopy(nmsHide);
            org.bukkit.inventory.meta.ItemMeta hideMeta = hide.getItemMeta();
            hideMeta.setDisplayName(CC.GREEN + "Hide Players");
            hide.setItemMeta(hideMeta);
            player.getInventory().setItem(8, hide);
        }
        player.updateInventory();
    }

    @Getter
    @RequiredArgsConstructor
    public class ParkourGameTask extends BukkitRunnable {

        private int time = 300;

        @Override
        public void run() {
            if (time == 300) {
                sendMessage(plugin.getLanguageFile().getConfiguration()
                        .getString("EVENT.STARTED"));

                
                setState(EventState.PLAYING);

                Location start = plugin.getSpawnManager().getParkourGameLocation();
                getPlayers().forEach((uuid, player) -> {
                    player.setState(ParkourPlayer.ParkourState.PLAYING);
                    player.setStartTime(System.currentTimeMillis());
                    Bukkit.getPlayer(uuid).teleport(start);
                    giveItems(Bukkit.getPlayer(uuid));
                });
            } else if (time <= 0) {
                end();
                cancel();
                return;
            }

            time--;
        }
    }

    public class ParkourCountdown extends EventCountdownTask {
        private final int requiredPlayers;

        public ParkourCountdown(PracticeEvent<?> event, int requiredPlayers) {
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
