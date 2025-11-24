package rip.diamond.practice.database;

import lombok.Getter;
import rip.diamond.practice.Eden;
import rip.diamond.practice.config.DatabaseConfig;
import rip.diamond.practice.database.impl.FlatFileHandler;
import rip.diamond.practice.database.impl.MongoHandler;
import rip.diamond.practice.database.impl.MySqlHandler;
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
        String type = DatabaseConfig.STORAGE_TYPE.toString().toUpperCase();

        if (type.equalsIgnoreCase("FLATFILE")) {
            this.handler = new FlatFileHandler();
            Common.log("&a[Database] Selected Storage: FlatFile (JSON)");
        } else if (type.equalsIgnoreCase("MYSQL")) {
            this.handler = new MySqlHandler();
            Common.log("&a[Database] Selected Storage: MySQL (HikariCP)");
        } else if (type.equalsIgnoreCase("MONGODB")) {
            this.handler = new MongoHandler();
            Common.log("&a[Database] Selected Storage: MongoDB");
        } else {
            // Default to FlatFile if invalid type
            this.handler = new FlatFileHandler();
            Common.log("&c[Database] Invalid storage type '" + type + "', defaulting to FlatFile (JSON)");
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
