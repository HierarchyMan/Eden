package rip.diamond.practice.config;

import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;
import rip.diamond.practice.util.BasicConfigFile;

@Getter
public class EventLoadoutsFile extends BasicConfigFile {

    public EventLoadoutsFile(JavaPlugin plugin) {
        super(plugin, "eventloadouts.yml");
    }
}
