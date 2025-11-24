package rip.diamond.practice.config;

import lombok.Getter;
import rip.diamond.practice.Eden;
import rip.diamond.practice.util.BasicConfigFile;

@Getter
public class MenusConfig {

    private final BasicConfigFile configFile;

    public MenusConfig(Eden plugin) {
        this.configFile = new BasicConfigFile(plugin, "menus.yml");
    }

    public void reload() {
        this.configFile.load();
    }

    public BasicConfigFile getConfig() {
        return configFile;
    }
}
