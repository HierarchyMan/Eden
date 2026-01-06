package rip.diamond.practice.util.scoreboard;

import org.bukkit.entity.Player;
import rip.diamond.practice.util.Util;

public class ScoreboardThread extends Thread {

    private final ScoreboardHandler scoreboardHandler;

    public ScoreboardThread(final ScoreboardHandler scoreboardHandler) {
        super("Board - Thread tick");
        this.scoreboardHandler = scoreboardHandler;

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
                final ScoreboardPlayer scoreboardPlayer = this.scoreboardHandler.getScoreboard(player);
                if (scoreboardPlayer == null) {
                    continue; // don't abort the entire tick if one player has no board
                }

                scoreboardPlayer.handleUpdate();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
