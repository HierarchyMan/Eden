package rip.diamond.practice.title;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import rip.diamond.practice.Eden;
import rip.diamond.practice.kits.Kit;
import rip.diamond.practice.profile.PlayerProfile;
import rip.diamond.practice.profile.data.ProfileKitData;
import rip.diamond.practice.util.BasicConfigFile;
import rip.diamond.practice.util.CC;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Manages title loading from config and lookup by win count.
 * Titles are calculated real-time from existing win data.
 */
@Getter
public class TitleManager {

    private BasicConfigFile titlesFile;
    private final List<Title> titles = new ArrayList<>();
    private boolean enabled = false;
    private String format = "[{title}] {name}";

    public TitleManager(Eden plugin) {
        this.titlesFile = new BasicConfigFile(plugin, "titles.yml");
        loadTitles();
    }

    /**
     * Load or reload titles from titles.yml
     */
    public void loadTitles() {
        titles.clear();
        
        if (titlesFile == null || titlesFile.getConfiguration() == null) {
            return;
        }

        this.enabled = titlesFile.getBoolean("enabled");
        this.format = titlesFile.getString("format");

        ConfigurationSection titlesSection = titlesFile.getConfiguration().getConfigurationSection("titles");
        if (titlesSection == null) {
            return;
        }

        for (String id : titlesSection.getKeys(false)) {
            String path = "titles." + id + ".";
            String display = titlesFile.getString(path + "display");
            String shortDisplay = titlesFile.getString(path + "short-display");
            // Fallback to display if short-display not configured
            if (shortDisplay == null || shortDisplay.equals(path + "short-display")) {
                shortDisplay = display;
            }
            int minWins = titlesFile.getInt(path + "min-wins");
            int maxWins = titlesFile.getInt(path + "max-wins");
            // Kit thresholds - fallback to overall if not specified
            int kitMinWins = titlesFile.getConfiguration().contains(path + "kit-min-wins") 
                    ? titlesFile.getInt(path + "kit-min-wins") : minWins;
            int kitMaxWins = titlesFile.getConfiguration().contains(path + "kit-max-wins") 
                    ? titlesFile.getInt(path + "kit-max-wins") : maxWins;
            int priority = titlesFile.getInt(path + "priority");

            titles.add(new Title(id, display, shortDisplay, minWins, maxWins, kitMinWins, kitMaxWins, priority));
        }

        // Sort by priority descending (highest priority first for lookup)
        titles.sort(Comparator.comparingInt(Title::getPriority).reversed());
        
        this.rankedMatchEloFormat = titlesFile.getString("ranked-match-elo-format");
        if (this.rankedMatchEloFormat == null) {
            this.rankedMatchEloFormat = "&8[&b{elo}&8]";
        }
    }

    private String rankedMatchEloFormat;
    
    public String getEloDisplay(int elo) {
        return rip.diamond.practice.util.CC.translate(rankedMatchEloFormat.replace("{elo}", String.valueOf(elo)));
    }

    /**
     * Reload titles configuration
     */
    public void reload() {
        if (titlesFile != null) {
            titlesFile.load();
            loadTitles();
        }
    }

    /**
     * Get the title for a specific OVERALL win count.
     * Returns null if no matching title or titles disabled.
     */
    public Title getTitleFromWins(int wins) {
        if (!enabled || titles.isEmpty()) {
            return null;
        }

        for (Title title : titles) {
            if (title.appliesTo(wins)) {
                return title;
            }
        }

        return null;
    }

    /**
     * Get the title for a specific KIT win count (uses kit thresholds).
     */
    public Title getTitleFromKitWins(int wins) {
        if (!enabled || titles.isEmpty()) {
            return null;
        }

        for (Title title : titles) {
            if (title.appliesToKit(wins)) {
                return title;
            }
        }

        return null;
    }

    /**
     * Get player's overall title (total wins across all kits).
     */
    public Title getOverallTitle(PlayerProfile profile) {
        if (profile == null) {
            return null;
        }

        int totalWins = profile.getKitData().values().stream()
                .mapToInt(ProfileKitData::getWon)
                .sum();

        return getTitleFromWins(totalWins);
    }

    /**
     * Get player's title for a specific kit (uses kit thresholds).
     */
    public Title getKitTitle(PlayerProfile profile, Kit kit) {
        if (profile == null || kit == null) {
            return null;
        }

        ProfileKitData kitData = profile.getKitData().get(kit.getName());
        if (kitData == null) {
            return getTitleFromKitWins(0);
        }

        return getTitleFromKitWins(kitData.getWon());
    }

    /**
     * Get player's title for a specific kit by name (uses kit thresholds).
     */
    public Title getKitTitle(PlayerProfile profile, String kitName) {
        if (profile == null || kitName == null) {
            return null;
        }

        ProfileKitData kitData = profile.getKitData().get(kitName);
        if (kitData == null) {
            return getTitleFromKitWins(0);
        }

        return getTitleFromKitWins(kitData.getWon());
    }

    /**
     * Format a player name with their title using the configured format.
     * Returns just the name if titles disabled or no title found.
     */
    public String formatWithTitle(String playerName, Title title) {
        if (!enabled || title == null) {
            return playerName;
        }

        return CC.translate(format
                .replace("{title}", title.getFormattedDisplay())
                .replace("{name}", playerName));
    }

    /**
     * Get formatted title display string, or empty if no title.
     */
    public String getTitleDisplay(Title title) {
        if (!enabled || title == null) {
            return "";
        }
        return title.getFormattedDisplay();
    }

    /**
     * Get formatted SHORT title display string (for scoreboards/holograms).
     */
    public String getShortTitleDisplay(Title title) {
        if (!enabled || title == null) {
            return "";
        }
        return title.getFormattedShortDisplay();
    }
}
