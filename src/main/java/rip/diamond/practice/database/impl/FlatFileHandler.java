package rip.diamond.practice.database.impl;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
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
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
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

    private Document readDocument(File file) {
        try {
            if (!file.exists()) {
                return null;
            }

            byte[] bytes = Files.readAllBytes(file.toPath());
            if (bytes.length == 0) {
                Common.log("&c[FlatFile] Detected empty JSON file: " + file.getName());
                return null;
            }

            String content = new String(bytes, StandardCharsets.UTF_8).trim();
            if (content.isEmpty()) {
                Common.log("&c[FlatFile] Detected blank JSON content: " + file.getName());
                return null;
            }

            try {
                return Document.parse(content);
            } catch (Exception primaryParse) {
                Common.log("&c[FlatFile] Failed to parse JSON (" + primaryParse.getMessage()
                        + ") in file: " + file.getName() + ". Attempting repair...");
                String repaired = tryRepairJson(content);
                if (repaired != null) {
                    try {
                        Document repairedDoc = Document.parse(repaired);
                        Common.log("&a[FlatFile] Repair succeeded for " + file.getName());
                        return repairedDoc;
                    } catch (Exception ignore) {
                        // fallthrough to quarantine
                    }
                }
                quarantineFile(file, content);
                return null;
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return null;
        }
    }

    private void writeJsonAtomically(File target, String json) {
        File tempFile = new File(target.getParentFile(), target.getName() + ".tmp");
        try {
            Files.write(tempFile.toPath(), json.getBytes(StandardCharsets.UTF_8));
            Files.move(tempFile.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            tempFile.delete();
        }
    }

    private String tryRepairJson(String content) {
        // Basic repair to close an unterminated string or document
        StringBuilder builder = new StringBuilder(content);
        boolean needsClosingQuote = builder.chars().filter(ch -> ch == '\"').count() % 2 != 0;
        if (needsClosingQuote) {
            builder.append('\"');
        }
        long openBraces = builder.chars().filter(ch -> ch == '{').count();
        long closeBraces = builder.chars().filter(ch -> ch == '}').count();
        while (closeBraces < openBraces) {
            builder.append('}');
            closeBraces++;
        }
        String repaired = builder.toString();
        // Guard against unreasonable growth
        if (repaired.length() > content.length() + 10) {
            return null;
        }
        return repaired;
    }

    private void quarantineFile(File file, String content) {
        try {
            File corruptedDir = new File(folder, "corrupted");
            corruptedDir.mkdirs();
            File backup = new File(corruptedDir, file.getName() + ".corrupt");
            Files.write(backup.toPath(), content.getBytes(StandardCharsets.UTF_8));
            Common.log("&c[FlatFile] Quarantined corrupt file: " + file.getName() + " -> " + backup.getName());
        } catch (IOException ignored) {
        }
    }

    @Override
    public void saveProfile(PlayerProfile profile) {
        Document document = profile.toBson();
        String json = document.toJson();
        nameIndex.put(profile.getUsername().toLowerCase(), profile.getUniqueId());

        Tasks.runAsync(() -> {
            File file = new File(folder, profile.getUniqueId().toString() + ".json");
            writeJsonAtomically(file, json);
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

    public void saveDocumentRaw(Document document) {
        String uuid = document.getString("uuid");
        String json = document.toJson();
        if (document.containsKey("lowerCaseUsername")) {
            nameIndex.put(document.getString("lowerCaseUsername"), UUID.fromString(uuid));
        }
        File file = new File(folder, uuid + ".json");
        writeJsonAtomically(file, json);
    }
}
