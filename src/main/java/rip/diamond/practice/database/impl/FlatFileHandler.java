package rip.diamond.practice.database.impl;

import com.google.common.collect.Lists;
import org.bson.Document;
import org.bukkit.Bukkit;
import rip.diamond.practice.Eden;
import rip.diamond.practice.database.DatabaseHandler;
import rip.diamond.practice.profile.PlayerProfile;
import rip.diamond.practice.util.Common;
import rip.diamond.practice.util.Tasks;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class FlatFileHandler implements DatabaseHandler {

    private final File folder;
    // Cache name to UUID mappings to make offline lookups fast
    private final Map<String, UUID> nameIndex = new ConcurrentHashMap<>();

    public FlatFileHandler() {
        this.folder = new File(Eden.INSTANCE.getDataFolder(), "data");
    }

    @Override
    public void init() {
        if (!folder.exists()) {
            folder.mkdirs();
        }

        // Build name index asynchronously on startup
        Tasks.runAsync(() -> {
            long start = System.currentTimeMillis();
            File[] files = folder.listFiles((dir, name) -> name.endsWith(".json"));
            if (files != null) {
                for (File file : files) {
                    try {
                        // We assume file name is UUID
                        String uuidStr = file.getName().replace(".json", "");
                        UUID uuid = UUID.fromString(uuidStr);

                        // We have to peek inside the file to get the name, or rely on file structure
                        // For performance, let's just read it.
                        Document doc = readDocument(file);
                        if (doc != null && doc.containsKey("lowerCaseUsername")) {
                            nameIndex.put(doc.getString("lowerCaseUsername"), uuid);
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
            Common.log("FlatFile Index built in " + (System.currentTimeMillis() - start) + "ms");
        });
    }

    @Override
    public void shutdown() {
        // Nothing to close for flat files
    }

    @Override
    public void loadProfile(UUID uuid, Consumer<Document> callback) {
        Tasks.runAsync(() -> {
            File file = new File(folder, uuid.toString() + ".json");
            if (!file.exists()) {
                callback.accept(null);
                return;
            }
            callback.accept(readDocument(file));
        });
    }

    @Override
    public void findProfileByName(String name, Consumer<Document> callback) {
        Tasks.runAsync(() -> {
            UUID uuid = nameIndex.get(name.toLowerCase());
            if (uuid == null) {
                callback.accept(null);
                return;
            }
            File file = new File(folder, uuid.toString() + ".json");
            if (!file.exists()) {
                callback.accept(null);
                return;
            }
            callback.accept(readDocument(file));
        });
    }

    @Override
    public void saveProfile(PlayerProfile profile) {
        // We perform serialization on the main thread to ensure data consistency,
        // but write IO on async thread.
        Document document = profile.toBson();
        String json = document.toJson();

        // Update index
        nameIndex.put(profile.getUsername().toLowerCase(), profile.getUniqueId());

        Tasks.runAsync(() -> {
            File file = new File(folder, profile.getUniqueId().toString() + ".json");
            try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
                writer.write(json);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public List<Document> getAllProfiles() {
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null)
            return new ArrayList<>();

        // Parallel stream for faster reading of many files
        return Arrays.stream(files)
                .parallel()
                .map(this::readDocument)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Document readDocument(File file) {
        try {
            String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            return Document.parse(content);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void saveDocumentRaw(Document document) {
        String uuid = document.getString("uuid");
        String json = document.toJson();

        // Update index if username exists
        if (document.containsKey("lowerCaseUsername")) {
            nameIndex.put(document.getString("lowerCaseUsername"), UUID.fromString(uuid));
        }

        File file = new File(folder, uuid + ".json");
        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            writer.write(json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
