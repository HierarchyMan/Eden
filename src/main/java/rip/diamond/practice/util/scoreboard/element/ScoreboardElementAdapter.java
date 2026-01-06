package rip.diamond.practice.util.scoreboard.element;

import org.bukkit.entity.Player;

/**
 *
 * Sconey Element Adapter interface
 * the adapter that will provide the player with scoreboard element
 */
public interface ScoreboardElementAdapter {

    /**
     * This method returns the scoreboard element used by this instance
     * @param player the player containing the provided scoreboard
     * @return the scoreboard element used by this instance
     */
    ScoreboardElement getElement(final Player player);
}
