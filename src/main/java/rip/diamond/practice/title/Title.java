package rip.diamond.practice.title;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import rip.diamond.practice.util.CC;

/**
 * Represents a title that can be earned by winning matches.
 * Titles are loaded from titles.yml and looked up by win count.
 * 
 * Supports separate thresholds for kit-specific vs overall titles.
 */
@Getter
@RequiredArgsConstructor
public class Title {

    private final String id;
    private final String displayName;
    private final String shortDisplayName;
    private final int minWins;      // Overall title threshold
    private final int maxWins;
    private final int kitMinWins;   // Kit-specific threshold (usually lower)
    private final int kitMaxWins;
    private final int priority;

    /**
     * Check if this title applies for the given overall win count.
     */
    public boolean appliesTo(int wins) {
        return wins >= minWins && wins <= maxWins;
    }

    /**
     * Check if this title applies for the given kit-specific win count.
     */
    public boolean appliesToKit(int wins) {
        return wins >= kitMinWins && wins <= kitMaxWins;
    }

    /**
     * Get the colored display name.
     */
    public String getFormattedDisplay() {
        return CC.translate(displayName);
    }

    /**
     * Get the colored short display name (for scoreboards/holograms).
     */
    public String getFormattedShortDisplay() {
        return CC.translate(shortDisplayName);
    }
}
