package rip.diamond.practice.util.file;

import org.bukkit.configuration.ConfigurationSection;
import rip.diamond.practice.util.BasicConfigFile;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ConfigCursor {

    private final BasicConfigFile configFile;
    private final String path;

    public ConfigCursor(BasicConfigFile configFile, String path) {
        this.configFile = configFile;
        this.path = path;
    }

    public boolean exists() {
        return this.configFile.getConfiguration().contains(this.path);
    }

    public boolean exists(String subPath) {
        return this.configFile.getConfiguration().contains(this.path + "." + subPath);
    }

    public Set<String> getKeys() {
        if (!exists())
            return Collections.emptySet();
        ConfigurationSection section = this.configFile.getConfiguration().getConfigurationSection(this.path);
        return section != null ? section.getKeys(false) : Collections.emptySet();
    }

    public String getString(String subPath) {
        return this.configFile.getConfiguration().getString(this.path + "." + subPath);
    }

    public int getInt(String subPath) {
        return this.configFile.getConfiguration().getInt(this.path + "." + subPath);
    }

    public double getDouble(String subPath) {
        return this.configFile.getConfiguration().getDouble(this.path + "." + subPath);
    }

    public boolean getBoolean(String subPath) {
        return this.configFile.getConfiguration().getBoolean(this.path + "." + subPath);
    }

    public List<String> getStringList(String subPath) {
        return this.configFile.getConfiguration().getStringList(this.path + "." + subPath);
    }

    public ConfigurationSection getConfigurationSection() {
        return this.configFile.getConfiguration().getConfigurationSection(this.path);
    }
}
