package rip.diamond.practice.util.scoreboard.element;


import lombok.Getter;

@Getter
public enum ScoreboardElementMode {

    DOWN(true, 15),
    NEGATIVE(true, -1),
    UP(false, 1),
    CUSTOM(false, 0);

    private boolean descending;
    private int startNumber;

    /**
     * Constructor a new ScoreboardElementMode instance
     *
     * @param descending  whether the positions are going down or up.
     * @param startNumber from where to loop from.
     */
    private ScoreboardElementMode(final boolean descending, final int startNumber) {
        this.descending = descending;
        this.startNumber = startNumber;
    }

    public ScoreboardElementMode reverse() {
        return descending(!this.descending);
    }

    public ScoreboardElementMode descending(boolean descending) {
        this.descending = descending;
        return this;
    }

    public ScoreboardElementMode startNumber(int startNumber) {
        this.startNumber = startNumber;
        return this;
    }
}
