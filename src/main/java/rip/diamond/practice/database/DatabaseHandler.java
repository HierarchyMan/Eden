package rip.diamond.practice.database;

import org.bson.Document;
import rip.diamond.practice.profile.PlayerProfile;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public interface DatabaseHandler {
    void init();

    void shutdown();

    // Load a profile by UUID
    void loadProfile(UUID uuid, Consumer<Document> callback);

    // Find a profile by Name (for offline lookups/stats)
    void findProfileByName(String name, Consumer<Document> callback);

    // Save a profile
    void saveProfile(PlayerProfile profile, boolean async);

    // Get all documents (For leaderboards/resets) - Heavy operation
    List<Document> getAllProfiles();

    // Save raw document (For migration)
    void saveDocumentRaw(Document document);
}
