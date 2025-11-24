package rip.diamond.practice.database.impl;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import lombok.Getter;
import org.bson.Document;
import rip.diamond.practice.config.DatabaseConfig;
import rip.diamond.practice.database.DatabaseHandler;
import rip.diamond.practice.profile.PlayerProfile;
import rip.diamond.practice.util.Tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

@Getter
public class MongoHandler implements DatabaseHandler {

    private MongoClient client;
    private MongoDatabase database;
    private MongoCollection<Document> profiles;

    @Override
    public void init() {
        if (DatabaseConfig.MONGODB_URI_MODE.toBoolean()) {
            this.client = MongoClients.create(DatabaseConfig.MONGODB_URI_CONNECTION_STRING.toString());
        } else {
            String uri = "mongodb://" + DatabaseConfig.MONGODB_NORMAL_HOST.toString() + ":"
                    + DatabaseConfig.MONGODB_NORMAL_PORT.toInteger();
            if (DatabaseConfig.MONGODB_NORMAL_AUTH_ENABLED.toBoolean()) {
                String username = DatabaseConfig.MONGODB_NORMAL_AUTH_USERNAME.toString();
                String password = DatabaseConfig.MONGODB_NORMAL_AUTH_PASSWORD.toString()
                        .replaceAll("%(?![0-9a-fA-F]{2})", "%25")
                        .replaceAll("\\+", "%2B");
                uri = "mongodb://" + username + ":" + password + "@" + DatabaseConfig.MONGODB_NORMAL_HOST.toString() + ":"
                        + DatabaseConfig.MONGODB_NORMAL_PORT.toInteger();
            }
            this.client = MongoClients.create(uri);
        }
        this.database = client.getDatabase(DatabaseConfig.MONGODB_URI_DATABASE.toString());
        this.profiles = this.database.getCollection("profiles");
    }

    @Override
    public void shutdown() {
        if (this.client != null) {
            this.client.close();
        }
    }

    @Override
    public void loadProfile(UUID uuid, Consumer<Document> callback) {
        Tasks.runAsync(() -> {
            Document document = profiles.find(Filters.eq("uuid", uuid.toString())).first();
            callback.accept(document);
        });
    }

    @Override
    public void findProfileByName(String name, Consumer<Document> callback) {
        Tasks.runAsync(() -> {
            Document document = profiles.find(Filters.eq("lowerCaseUsername", name.toLowerCase())).first();
            callback.accept(document);
        });
    }

    @Override
    public void saveProfile(PlayerProfile profile) {
        Tasks.runAsync(() -> {
            profiles.replaceOne(Filters.eq("uuid", profile.getUniqueId().toString()), profile.toBson(),
                    new ReplaceOptions().upsert(true));
        });
    }

    @Override
    public List<Document> getAllProfiles() {
        return profiles.find().into(new ArrayList<>());
    }

    public void saveDocumentRaw(Document document) {
        String uuid = document.getString("uuid");
        profiles.replaceOne(Filters.eq("uuid", uuid), document, new ReplaceOptions().upsert(true));
    }
}
