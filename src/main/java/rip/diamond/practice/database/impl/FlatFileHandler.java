package rip.diamond.practice.database.impl;

import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
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
    private final File customItemsFile;
    private final Object fileLock = new Object();

    private final Map<String, UUID> nameIndex = new ConcurrentHashMap<>();

    public FlatFileHandler() {
        this.folder = new File(Eden.INSTANCE.getDataFolder(), "data");
        this.customItemsFile = new File(this.folder, "custom_items.json");
    }

    @Override
    public void init() {
        if (!folder.exists()) {
            folder.mkdirs();
        }

        Tasks.runAsync(() -> {
            long start = System.currentTimeMillis();
            File[] files = folder.listFiles((dir, name) -> name.endsWith(".json"));
            if (files != null) {
                for (File file : files) {
                    try {

                        String uuidStr = file.getName().replace(".json", "");
                        UUID uuid = UUID.fromString(uuidStr);

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
            Files.move(tempFile.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            tempFile.delete();
        }
    }

    private String tryRepairJson(String content) {

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
    public void saveProfile(PlayerProfile profile, boolean async) {
        Document document = profile.toBson();
        String json = document.toJson();
        nameIndex.put(profile.getUsername().toLowerCase(), profile.getUniqueId());

        if (async) {
            Tasks.runAsync(() -> {
                File file = new File(folder, profile.getUniqueId().toString() + ".json");
                writeJsonAtomically(file, json);
            });
        } else {
            File file = new File(folder, profile.getUniqueId().toString() + ".json");
            writeJsonAtomically(file, json);
        }
    }

    @Override
    public List<Document> getAllProfiles() {
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null)
            return new ArrayList<>();

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

    @Override
    public void saveCustomItem(String key, org.bukkit.inventory.ItemStack item) {
        Tasks.runAsync(() -> {
            synchronized (fileLock) {
                Document document;
                if (customItemsFile.exists()) {
                    try {
                        String content = new String(Files.readAllBytes(customItemsFile.toPath()),
                                StandardCharsets.UTF_8);
                        document = Document.parse(content);
                    } catch (Exception e) {
                        document = new Document();
                    }
                } else {
                    document = new Document();
                }

                document.put(key, rip.diamond.practice.util.serialization.BukkitSerialization.itemStackToBase64(item));
                writeJsonAtomically(customItemsFile, document.toJson());
            }
        });
    }

    @Override
    public void loadAllCustomItems(Consumer<java.util.Map<String, org.bukkit.inventory.ItemStack>> callback) {
        Tasks.runAsync(() -> {
            synchronized (fileLock) {
                java.util.Map<String, org.bukkit.inventory.ItemStack> map = new java.util.HashMap<>();
                if (customItemsFile.exists()) {
                    try {
                        String content = new String(Files.readAllBytes(customItemsFile.toPath()),
                                StandardCharsets.UTF_8);
                        Document document = Document.parse(content);
                        for (String key : document.keySet()) {
                            try {
                                map.put(key, rip.diamond.practice.util.serialization.BukkitSerialization
                                        .itemStackFromBase64(document.getString(key)));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    } catch (Exception e) {
                        Eden.INSTANCE.getLogger().log(java.util.logging.Level.SEVERE, "Failed to load custom items", e);
                    }
                }
                callback.accept(map);
            }
        });
    }

    @Override
    public void deleteCustomItem(String key) {
        Tasks.runAsync(() -> {
            synchronized (fileLock) {
                if (customItemsFile.exists()) {
                    try {
                        String content = new String(Files.readAllBytes(customItemsFile.toPath()),
                                StandardCharsets.UTF_8);
                        Document document = Document.parse(content);
                        if (document.containsKey(key)) {
                            document.remove(key);
                            writeJsonAtomically(customItemsFile, document.toJson());
                        }
                    } catch (Exception e) {
                        Eden.INSTANCE.getLogger().log(java.util.logging.Level.SEVERE,
                                "Failed to delete custom item " + key, e);
                    }
                }
            }
        });
    }
}
