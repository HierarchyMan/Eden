package rip.diamond.practice.util.scoreboard;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;

public class ScoreboardListener implements Listener {

    private final ScoreboardHandler scoreboardHandler;
    private final Plugin owningPlugin;

    public ScoreboardListener(final ScoreboardHandler scoreboardHandler, final Plugin owningPlugin) {
        this.scoreboardHandler = scoreboardHandler;
        this.owningPlugin = owningPlugin;
    }

    @EventHandler
    public void onJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        this.scoreboardHandler.addScoreboard(player);
    }

    @EventHandler
    public void onQuit(final PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        this.scoreboardHandler.removeScoreboard(player);
    }

    @EventHandler
    public void onPluginDisable(final PluginDisableEvent event) {
        // Only stop Sconey when the plugin that owns this handler is disabling.
        if (event.getPlugin().equals(this.owningPlugin)) {
            this.scoreboardHandler.stopThread();
        }
    }
}
