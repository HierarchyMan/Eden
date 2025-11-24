package rip.diamond.practice.util;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Setter
public class BasicConfigFile {
    private JavaPlugin plugin;
    private String fileName;
    private YamlConfiguration configuration;

    private File file;
    private final Map<String, Object> cache = new ConcurrentHashMap<>();

    public BasicConfigFile(JavaPlugin plugin, String fileName) {
        this.plugin = plugin;
        this.fileName = fileName;
        this.file = new File(plugin.getDataFolder(), fileName);
        if (!this.file.exists()) {
            plugin.saveResource(fileName, false);
        }
        this.configuration = YamlConfiguration.loadConfiguration(this.file);
        rebuildCache();
    }

    public boolean getBoolean(String path) {
        Object value = cache.get(path);
        return value instanceof Boolean && (Boolean) value;
    }

    public double getDouble(String path) {
        Object value = cache.get(path);
        return value instanceof Number ? ((Number) value).doubleValue() : 0.0D;
    }

    public int getInt(String path) {
        Object value = cache.get(path);
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    public String getString(String path) {
        if (cache.containsKey(path)) {
            Object value = cache.get(path);
            if (value == null) {
                return path;
            }
            // REMOVED ColorUtil.colorize here.
            // If you need colors in raw config access, wrap the call in ColorUtil.colorize() in the calling class.
            return String.valueOf(value);
        }
        return path;
    }

    public List<String> getStringList(String path) {
        Object value = cache.get(path);
        if (value instanceof List) {
            List<String> strings = new ArrayList<>();
            for (Object entry : (List<?>) value) {
                if (entry != null) {
                    // REMOVED ColorUtil.colorize here too
                    strings.add(entry.toString());
                }
            }
            return strings;
        }
        return Collections.singletonList(path);
    }

    public String getStringRaw(String path) {
        Object value = cache.get(path);
        return value == null ? path : value.toString();
    }



    public List<String> getRawStringList(String path) {
        Object value = cache.get(path);
        if (value instanceof List) {
            List<String> strings = new ArrayList<>();
            for (Object entry : (List<?>) value) {
                if (entry != null) {
                    strings.add(entry.toString());
                }
            }
            return strings;
        }
        return Collections.singletonList(path);
    }

    public void load() {
        this.file = new File(plugin.getDataFolder(), fileName);
        if (!this.file.exists()) {
            plugin.saveResource(fileName, false);
        }
        this.configuration = YamlConfiguration.loadConfiguration(this.file);
        rebuildCache();
    }

    public void save() {
        try {
            this.configuration.save(this.file);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            rebuildCache();
        }
    }

    public void set(String path, Object value) {
        this.configuration.set(path, value);
        if (value == null) {
            cache.remove(path);
        } else {
            cache.put(path, value);
        }
    }

    private void rebuildCache() {
        cache.clear();
        if (this.configuration == null) {
            return;
        }
        for (String key : this.configuration.getKeys(true)) {
            if (this.configuration.isConfigurationSection(key)) {
                continue;
            }
            cache.put(key, this.configuration.get(key));
        }
    }
}
