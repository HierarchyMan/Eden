package rip.diamond.practice.config;

import com.google.common.collect.ImmutableList;
import lombok.AllArgsConstructor;
import lombok.Getter;
import rip.diamond.practice.Eden;
import rip.diamond.practice.util.BasicConfigFile;
import rip.diamond.practice.util.CC;
import rip.diamond.practice.util.Common;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@AllArgsConstructor
public enum DatabaseConfig {

    // Storage Type
    STORAGE_TYPE("storage-type", "FLATFILE"),

    // MongoDB
    MONGODB_ENABLED("mongodb.enabled", true),
    MONGODB_URI_MODE("mongodb.uri-mode", false),
    MONGODB_NORMAL_HOST("mongodb.normal.host", "127.0.0.1"),
    MONGODB_NORMAL_PORT("mongodb.normal.port", 27017),
    MONGODB_NORMAL_AUTH_ENABLED("mongodb.normal.auth.enabled", false),
    MONGODB_NORMAL_AUTH_USERNAME("mongodb.normal.auth.username", ""),
    MONGODB_NORMAL_AUTH_PASSWORD("mongodb.normal.auth.password", ""),
    MONGODB_URI_DATABASE("mongodb.uri.database", "Practice"),
    MONGODB_URI_CONNECTION_STRING("mongodb.uri.connection-string", "mongodb://127.0.0.1:27017/Eden"),

    // MySQL
    MYSQL_HOST("mysql.host", "127.0.0.1"),
    MYSQL_PORT("mysql.port", 3306),
    MYSQL_DATABASE("mysql.database", "eden_practice"),
    MYSQL_USERNAME("mysql.username", "root"),
    MYSQL_PASSWORD("mysql.password", ""),
    MYSQL_SSL("mysql.ssl", false),
    MYSQL_TABLE("mysql.table", "profiles"),
    MYSQL_POOL_MAX_POOL_SIZE("mysql.pool.max-pool-size", 10),
    MYSQL_POOL_MIN_IDLE("mysql.pool.min-idle", 1),
    MYSQL_POOL_CONNECTION_TIMEOUT_MS("mysql.pool.connection-timeout-ms", 10000),
    MYSQL_POOL_VALIDATION_TIMEOUT_MS("mysql.pool.validation-timeout-ms", 3000),
    MYSQL_POOL_MAX_LIFETIME_MS("mysql.pool.max-lifetime-ms", 240000),
    MYSQL_POOL_IDLE_TIMEOUT_MS("mysql.pool.idle-timeout-ms", 120000),
    MYSQL_POOL_KEEPALIVE_TIME_MS("mysql.pool.keepalive-time-ms", 60000),
    MYSQL_POOL_TEST_QUERY("mysql.pool.test-query", "SELECT 1"),
    ;

    @Getter
    private final String path;
    @Getter
    private final Object defaultValue;
    private static final Map<DatabaseConfig, Object> CACHE = new ConcurrentHashMap<>();

    public String toString() {
        return CC.translate(toStringRaw());
    }

    private String toStringRaw() {
        if (CACHE.containsKey(this)) {
            return String.valueOf(CACHE.get(this));
        }
        String value = Eden.INSTANCE.getDatabaseFile().getStringRaw(path);
        if (value.equals(path)) {
            value = defaultValue.toString();
        }
        CACHE.put(this, value);
        return value;
    }

    public List<String> toStringList() {
        if (CACHE.containsKey(this)) {
            return (List<String>) CACHE.get(this);
        }
        List<String> result = Eden.INSTANCE.getDatabaseFile().getRawStringList(path);
        if (result.isEmpty() || result.get(0).equals(path)) {
            result = (List<String>) defaultValue;
        }
        List<String> colored = result.stream().map(CC::translate).collect(Collectors.toList());
        List<String> finalResult = ImmutableList.copyOf(colored);
        CACHE.put(this, finalResult);
        return finalResult;
    }

    public boolean toBoolean() {
        return Boolean.parseBoolean(toStringRaw());
    }

    public int toInteger() {
        return Integer.parseInt(toStringRaw());
    }

    public long toLong() {
        return Long.parseLong(toStringRaw());
    }

    public double toDouble() {
        return Double.parseDouble(toStringRaw());
    }

    public static void invalidateCache() {
        CACHE.clear();
    }

    public static void loadDefault() {
        BasicConfigFile databaseFile = Eden.INSTANCE.getDatabaseFile();

        for (DatabaseConfig config : DatabaseConfig.values()) {
            String path = config.getPath();
            String str = databaseFile.getStringRaw(path);
            if (str.equals(path)) {
                Common.debug("沒有找到 '" + path + "'... 正在加入到 database.yml");
                databaseFile.set(path, config.getDefaultValue());
            }
        }

        databaseFile.save();
        databaseFile.load();
        invalidateCache();
    }

}

