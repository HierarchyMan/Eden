package io.github.epicgo.sconey;

import io.github.epicgo.sconey.element.SconeyElementAdapter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SconeyHandler {

    private final Map<UUID, SconeyPlayer> players = new HashMap<>();

    private final SconeyElementAdapter adapter;
    private SconeyThread sconeyThread;

    public SconeyHandler(final JavaPlugin plugin, final SconeyElementAdapter adapter) {
        this.adapter = adapter;

        plugin.getServer().getPluginManager().registerEvents(new SconeyListener(this, plugin), plugin);

        this.startThread();

        // Ensure already-online players get a board if the handler is created after they're online.
        for (Player player : Bukkit.getOnlinePlayers()) {
            this.ensureScoreboard(player);
        }
    }

    /**
     * Starts the threads
     */
    public void startThread() {
        this.stopThread();

        this.sconeyThread = new SconeyThread(this);
        this.sconeyThread.start();
    }

    /**
     * Stops the threads
     */
    public void stopThread() {
        if (this.sconeyThread != null) {
            this.sconeyThread.shutdown();
            this.sconeyThread = null;
        }
    }

    /**
     * @return true if the update thread exists and is alive
     */
    public boolean isRunning() {
        return this.sconeyThread != null && this.sconeyThread.isAlive();
    }

    /**
     * Ensure a player has a scoreboard registered in this handler.
     */
    public void ensureScoreboard(final Player player) {
        if (this.getScoreboard(player) == null) {
            this.addScoreboard(player);
        }
    }

    /**
     * Rebuild all boards (useful for reload recovery).
     */
    public void rebuildAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            this.removeScoreboard(player);
            this.addScoreboard(player);
        }
    }

    /**
     * Handle a board to send to a player scoreboard
     *
     * @param player the player scoreboard to display the board for
     */
    public void addScoreboard(final Player player) {
        this.players.put(player.getUniqueId(), new SconeyPlayer(player, adapter));
    }

    /**
     * Clear the board from a player's scoreboard
     *
     * @param player the player scoreboard to clear the board for
     */
    public void removeScoreboard(final Player player) {
        this.players.remove(player.getUniqueId());
    }

    /**
     * Get the {@link SconeyPlayer} board of a player scoreboard
     *
     * @param player the player to get the board by
     * @return the board
     */
    public SconeyPlayer getScoreboard(final Player player) {
        return this.players.get(player.getUniqueId());
    }
}
