package rip.diamond.practice.config;

import lombok.Getter;
import rip.diamond.practice.Eden;
import rip.diamond.practice.util.BasicConfigFile;

import java.util.Arrays;
import java.util.List;

@Getter
public class ScoreboardFile extends BasicConfigFile {

    public ScoreboardFile(Eden plugin) {
        super(plugin, "scoreboard.yml");
    }

    public String getTitle(String eventName, String state) {
        String path = eventName.toUpperCase() + "." + state.toUpperCase() + ".TITLE";
        return getString(path).equals(path) ? "&b&l" + eventName : getString(path);
    }

    public List<String> getLines(String eventName, String state) {
        String path = eventName.toUpperCase() + "." + state.toUpperCase() + ".LINES";
        List<String> lines = getStringList(path);

        if (lines.size() == 1 && lines.get(0).equals(path)) {
            // Log warning once per path to avoid spamming, or just log it
            getPlugin().getLogger().warning("Missing config for path: " + path);
            getPlugin().getLogger().warning("Available root keys: " + getConfiguration().getKeys(false));

            return Arrays.asList(
                    "&7&m---------------------",
                    "&b" + eventName,
                    "&7State: &f" + state,
                    "",
                    "&7Loading...",
                    "&7&m---------------------");
        }
        return lines;
    }
}
