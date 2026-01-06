package io.github.epicgo.sconey;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;

public class SconeyListener implements Listener {

    private final SconeyHandler sconeyHandler;
    private final Plugin owningPlugin;

    public SconeyListener(final SconeyHandler sconeyHandler, final Plugin owningPlugin) {
        this.sconeyHandler = sconeyHandler;
        this.owningPlugin = owningPlugin;
    }

    @EventHandler
    public void onJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        this.sconeyHandler.addScoreboard(player);
    }

    @EventHandler
    public void onQuit(final PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        this.sconeyHandler.removeScoreboard(player);
    }

    @EventHandler
    public void onPluginDisable(final PluginDisableEvent event) {
        // Only stop Sconey when the plugin that owns this handler is disabling.
        if (event.getPlugin().equals(this.owningPlugin)) {
            this.sconeyHandler.stopThread();
        }
    }
}
