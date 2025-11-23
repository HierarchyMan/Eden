package rip.diamond.practice.database;

import lombok.Getter;
import rip.diamond.practice.Eden;
import rip.diamond.practice.config.Config;
import rip.diamond.practice.database.impl.FlatFileHandler;
import rip.diamond.practice.database.impl.MongoHandler;
import rip.diamond.practice.util.Common;

@Getter
public class DatabaseManager {

    private final Eden plugin;
    private DatabaseHandler handler;

    public DatabaseManager(Eden plugin) {
        this.plugin = plugin;
        this.init();
    }

    public void init() {
        String type = Config.STORAGE_TYPE.toString();

        if (type.equalsIgnoreCase("FLATFILE")) {
            this.handler = new FlatFileHandler();
            Common.log("&a[Database] Selected Storage: FlatFile (JSON)");
        } else {
            // Default to Mongo if not FlatFile, or if Mongo is enabled in old config style
            this.handler = new MongoHandler();
            Common.log("&a[Database] Selected Storage: MongoDB");
        }

        try {
            this.handler.init();
        } catch (Exception e) {
            Common.log("&c[Database] Failed to initialize database!");
            e.printStackTrace();
        }
    }

    public void shutdown() {
        if (this.handler != null) {
            this.handler.shutdown();
        }
    }
}
