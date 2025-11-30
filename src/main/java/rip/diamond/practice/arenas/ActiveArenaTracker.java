package rip.diamond.practice.arenas;

import com.boydti.fawe.util.TaskManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.experimental.UtilityClass;
import rip.diamond.practice.Eden;
import rip.diamond.practice.util.Common;

import java.io.*;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

@UtilityClass
public class ActiveArenaTracker {

    private final File file = new File(Eden.INSTANCE.getDataFolder(), "cache/active_arenas.json");
    private final Gson gson = new Gson();
    private Set<String> activeArenas = new HashSet<>();

    public void init() {
        if (!file.exists()) {
            return;
        }

        try (FileReader reader = new FileReader(file)) {
            Type type = new TypeToken<HashSet<String>>() {
            }.getType();
            activeArenas = gson.fromJson(reader, type);
            if (activeArenas == null) {
                activeArenas = new HashSet<>();
            }
        } catch (Exception e) {
            Common.log("&cFailed to load active arenas tracker. File may be corrupted.");
            e.printStackTrace();
            if (file.exists()) {
                file.delete();
                Common.log("&eDeleted corrupted active arenas file.");
            }
            activeArenas = new HashSet<>();
        }
    }

    public void add(ArenaDetail detail) {
        String id = getIdentifier(detail);
        activeArenas.add(id);
        saveAsync();
    }

    public void remove(ArenaDetail detail) {
        String id = getIdentifier(detail);
        activeArenas.remove(id);
        saveAsync();
    }

    public boolean wasActive(ArenaDetail detail) {
        return activeArenas.contains(getIdentifier(detail));
    }

    private String getIdentifier(ArenaDetail detail) {
        int index = detail.getArena().getArenaDetails().indexOf(detail);
        return detail.getArena().getName() + ":" + index;
    }

    private final java.util.concurrent.ExecutorService saveExecutor = java.util.concurrent.Executors
            .newSingleThreadExecutor();

    private void saveAsync() {
        // Create a snapshot on the main thread to avoid ConcurrentModificationException
        // and data corruption when writing to disk asynchronously.
        Set<String> snapshot = new HashSet<>(activeArenas);

        saveExecutor.submit(() -> {
            try {
                if (!file.exists()) {
                    file.getParentFile().mkdirs();
                    file.createNewFile();
                }

                try (FileWriter writer = new FileWriter(file)) {
                    gson.toJson(snapshot, writer);
                }
            } catch (IOException e) {
                Common.log("&cFailed to save active arenas tracker.");
                e.printStackTrace();
            }
        });
    }
}
