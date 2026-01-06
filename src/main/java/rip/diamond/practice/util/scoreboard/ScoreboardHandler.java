package rip.diamond.practice.util.scoreboard;

import rip.diamond.practice.util.scoreboard.element.ScoreboardElementAdapter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ScoreboardHandler {

    private final Map<UUID, ScoreboardPlayer> players = new HashMap<>();

    private final ScoreboardElementAdapter adapter;
    private ScoreboardThread scoreboardThread;

    public ScoreboardHandler(final JavaPlugin plugin, final ScoreboardElementAdapter adapter) {
        this.adapter = adapter;

        plugin.getServer().getPluginManager().registerEvents(new ScoreboardListener(this, plugin), plugin);

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

        this.scoreboardThread = new ScoreboardThread(this);
        this.scoreboardThread.start();
    }

    /**
     * Stops the threads
     */
    public void stopThread() {
        if (this.scoreboardThread != null) {
            this.scoreboardThread.shutdown();
            this.scoreboardThread = null;
        }
    }

    /**
     * @return true if the update thread exists and is alive
     */
    public boolean isRunning() {
        return this.scoreboardThread != null && this.scoreboardThread.isAlive();
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
        this.players.put(player.getUniqueId(), new ScoreboardPlayer(player, adapter));
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
     * Get the {@link ScoreboardPlayer} board of a player scoreboard
     *
     * @param player the player to get the board by
     * @return the board
     */
    public ScoreboardPlayer getScoreboard(final Player player) {
        return this.players.get(player.getUniqueId());
    }
}
