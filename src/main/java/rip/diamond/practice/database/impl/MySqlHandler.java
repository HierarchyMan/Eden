package rip.diamond.practice.database.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import org.bson.Document;
import rip.diamond.practice.Eden;
import rip.diamond.practice.config.DatabaseConfig;
import rip.diamond.practice.database.DatabaseHandler;
import rip.diamond.practice.profile.PlayerProfile;
import rip.diamond.practice.util.Common;
import rip.diamond.practice.util.Tasks;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;

@Getter
public class MySqlHandler implements DatabaseHandler {

    private final HikariDataSource dataSource;
    private final Gson gson = new GsonBuilder().create();
    private final String table;

    public MySqlHandler() {
        this.table = DatabaseConfig.MYSQL_TABLE.toString();

        String host = DatabaseConfig.MYSQL_HOST.toString();
        int port = DatabaseConfig.MYSQL_PORT.toInteger();
        String database = DatabaseConfig.MYSQL_DATABASE.toString();
        String username = DatabaseConfig.MYSQL_USERNAME.toString();
        String password = DatabaseConfig.MYSQL_PASSWORD.toString();
        boolean ssl = DatabaseConfig.MYSQL_SSL.toBoolean();

        HikariConfig config = new HikariConfig();

        // Build JDBC URL
        String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database +
                "?useSSL=" + ssl +
                "&allowPublicKeyRetrieval=true" +
                "&useUnicode=true&characterEncoding=UTF-8" +
                "&cachePrepStmts=true&prepStmtCacheSize=250&prepStmtCacheSqlLimit=2048" +
                "&rewriteBatchedStatements=true";
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);

        // Pool settings from config
        config.setMaximumPoolSize(DatabaseConfig.MYSQL_POOL_MAX_POOL_SIZE.toInteger());
        config.setMinimumIdle(DatabaseConfig.MYSQL_POOL_MIN_IDLE.toInteger());
        config.setConnectionTimeout(DatabaseConfig.MYSQL_POOL_CONNECTION_TIMEOUT_MS.toLong());
        config.setValidationTimeout(DatabaseConfig.MYSQL_POOL_VALIDATION_TIMEOUT_MS.toLong());
        config.setMaxLifetime(DatabaseConfig.MYSQL_POOL_MAX_LIFETIME_MS.toLong());
        config.setIdleTimeout(DatabaseConfig.MYSQL_POOL_IDLE_TIMEOUT_MS.toLong());
        config.setKeepaliveTime(DatabaseConfig.MYSQL_POOL_KEEPALIVE_TIME_MS.toLong());

        String testQuery = DatabaseConfig.MYSQL_POOL_TEST_QUERY.toString();
        if (testQuery != null && !testQuery.isEmpty() && !testQuery.equals("mysql.pool.test-query")) {
            config.setConnectionTestQuery(testQuery);
        }

        config.setPoolName("Eden-PlayerData");

        this.dataSource = new HikariDataSource(config);
    }

    @Override
    public void init() {
        try {
            ensureTable();
        } catch (SQLException e) {
            Common.log("&c[MySQL] Failed to create table!");
            e.printStackTrace();
        }
    }

    private void ensureTable() throws SQLException {
        try (Connection c = dataSource.getConnection(); Statement st = c.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS " + table + " (" +
                    "uuid VARCHAR(36) NOT NULL PRIMARY KEY," +
                    "json LONGTEXT NOT NULL" +
                    ")");
        }
    }

    @Override
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    @Override
    public void loadProfile(UUID uuid, Consumer<Document> callback) {
        Tasks.runAsync(() -> {
            String sql = "SELECT json FROM " + table + " WHERE uuid=?";
            try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String json = rs.getString(1);
                        Document document = Document.parse(json);
                        callback.accept(document);
                    } else {
                        callback.accept(null);
                    }
                }
            } catch (Exception e) {
                Eden.INSTANCE.getLogger().log(Level.WARNING, "Failed to load profile for " + uuid, e);
                callback.accept(null);
            }
        });
    }

    @Override
    public void findProfileByName(String name, Consumer<Document> callback) {
        Tasks.runAsync(() -> {
            String sql = "SELECT json FROM " + table;
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    try {
                        String json = rs.getString(1);
                        Document document = Document.parse(json);
                        if (document.containsKey("lowerCaseUsername") &&
                            document.getString("lowerCaseUsername").equalsIgnoreCase(name.toLowerCase())) {
                            callback.accept(document);
                            return;
                        }
                    } catch (Exception e) {
                        // Skip malformed documents
                    }
                }
                callback.accept(null);
            } catch (Exception e) {
                Eden.INSTANCE.getLogger().log(Level.WARNING, "Failed to find profile by name " + name, e);
                callback.accept(null);
            }
        });
    }

    @Override
    public void saveProfile(PlayerProfile profile) {
        Tasks.runAsync(() -> {
            Document document = profile.toBson();
            String json = document.toJson();
            String sql = "INSERT INTO " + table + " (uuid, json) VALUES (?, ?) ON DUPLICATE KEY UPDATE json=VALUES(json)";

            try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, profile.getUniqueId().toString());
                ps.setString(2, json);
                ps.executeUpdate();
            } catch (Exception e) {
                Eden.INSTANCE.getLogger().log(Level.SEVERE, "Failed to save profile for " + profile.getUniqueId(), e);
            }
        });
    }

    @Override
    public List<Document> getAllProfiles() {
        List<Document> profiles = new ArrayList<>();
        String sql = "SELECT json FROM " + table;

        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                try {
                    String json = rs.getString(1);
                    Document document = Document.parse(json);
                    profiles.add(document);
                } catch (Exception e) {
                    Eden.INSTANCE.getLogger().log(Level.WARNING, "Failed to parse profile row", e);
                }
            }
        } catch (Exception e) {
            Eden.INSTANCE.getLogger().log(Level.SEVERE, "Failed to load all profiles", e);
        }

        return profiles;
    }

    @Override
    public void saveDocumentRaw(Document document) {
        String uuid = document.getString("uuid");
        String json = document.toJson();
        String sql = "INSERT INTO " + table + " (uuid, json) VALUES (?, ?) ON DUPLICATE KEY UPDATE json=VALUES(json)";

        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, uuid);
            ps.setString(2, json);
            ps.executeUpdate();
        } catch (Exception e) {
            Eden.INSTANCE.getLogger().log(Level.SEVERE, "Failed to save raw document for " + uuid, e);
        }
    }
}

