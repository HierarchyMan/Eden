package io.github.epicgo.sconey;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import rip.diamond.practice.util.Util;

public class SconeyThread extends Thread {

    private final SconeyHandler sconeyHandler;

    public SconeyThread(final SconeyHandler sconeyHandler) {
        super("Board - Thread tick");
        this.sconeyHandler = sconeyHandler;

        this.setDaemon(true);
    }

    private volatile boolean running = true;

    @Override
    public void run() {
        while (this.running) {
            this.tick();
            try {
                Thread.sleep(50L);
            } catch (InterruptedException e) {
                this.running = false;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void shutdown() {
        this.running = false;
        this.interrupt();
    }

    /**
     * Tick logic for thread.
     */
    private void tick() {
        for (final Player player : Util.getOnlinePlayers()) {
            try {
                final SconeyPlayer sconeyPlayer = this.sconeyHandler.getScoreboard(player);
                if (sconeyPlayer == null)
                    return;

                sconeyPlayer.handleUpdate();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
