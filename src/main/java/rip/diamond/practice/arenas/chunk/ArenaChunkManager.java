package rip.diamond.practice.arenas.chunk;

import com.boydti.fawe.util.TaskManager;
import lombok.experimental.UtilityClass;
import net.minecraft.server.v1_8_R3.NibbleArray;
import rip.diamond.practice.Eden;
import rip.diamond.practice.arenas.ArenaDetail;
import rip.diamond.practice.util.Common;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class ArenaChunkManager {

    private final int MAGIC_NUMBER = 0xCAFEBABE;
    private final int VERSION = 1;

    public void saveChunks(ArenaDetail detail) {
        if (detail.getCachedChunks().isEmpty()) {
            return;
        }

        TaskManager.IMP.async(() -> {
            File file = getChunkFile(detail);
            try {
                if (!file.exists()) {
                    file.getParentFile().mkdirs();
                    file.createNewFile();
                }

                try (DataOutputStream out = new DataOutputStream(
                        new BufferedOutputStream(new FileOutputStream(file)))) {
                    out.writeInt(MAGIC_NUMBER);
                    out.writeInt(VERSION);
                    out.writeInt(detail.getCachedChunks().size());

                    for (IArenaChunk chunk : detail.getCachedChunks()) {
                        if (chunk instanceof ArenaChunk) {
                            ArenaChunk arenaChunk = (ArenaChunk) chunk;
                            out.writeInt(arenaChunk.getX());
                            out.writeInt(arenaChunk.getZ());

                            // We need to access the private data array in ArenaChunk
                            // Since we can't easily change ArenaChunk to expose it without modifying it
                            // heavily,
                            // let's assume we modify ArenaChunk to expose 'data' or we use
                            // reflection/accessors.
                            // For now, I will assume I can add a getter to ArenaChunk or use what's
                            // available.
                            // Looking at ArenaChunk.java, 'data' is private and no getter.
                            // I will need to modify ArenaChunk to add a getter for 'data'.

                            // WAIT: I should modify ArenaChunk first to expose data.
                            // But since I am writing this file now, I will write the code assuming the
                            // getter exists.
                            // I will add the getter in the next step.

                            ArenaChunkSection[] sections = arenaChunk.getData();
                            out.writeInt(sections.length);

                            for (ArenaChunkSection section : sections) {
                                out.writeBoolean(section.isSetup());
                                if (section.isSetup()) {
                                    out.writeInt(section.getYPos());
                                    out.writeInt(section.getNonEmptyBlockCount());
                                    out.writeInt(section.getTickingBlockCount());
                                    out.writeBoolean(section.isDirty());

                                    writeCharArray(out, section.getBlockIds());
                                    writeNibbleArray(out, section.getEmittedLight());
                                    writeNibbleArray(out, section.getSkyLight());
                                }
                            }
                        }
                    }
                }
            } catch (IOException e) {
                Common.log("&cFailed to save chunks for arena " + detail.getArena().getName() + " (Detail "
                        + detail.getArena().getArenaDetails().indexOf(detail) + ")");
                e.printStackTrace();
            }
        });
    }

    public boolean loadChunks(ArenaDetail detail) {
        File file = getChunkFile(detail);
        if (!file.exists()) {
            return false;
        }

        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            int magic = in.readInt();
            if (magic != MAGIC_NUMBER) {
                Common.log("&cInvalid chunk file format for arena " + detail.getArena().getName());
                return false;
            }

            int version = in.readInt();
            if (version != VERSION) {
                Common.log("&cInvalid chunk file version for arena " + detail.getArena().getName());
                return false;
            }

            int chunkCount = in.readInt();
            List<IArenaChunk> loadedChunks = new ArrayList<>();

            for (int i = 0; i < chunkCount; i++) {
                int x = in.readInt();
                int z = in.readInt();
                int sectionCount = in.readInt();

                ArenaChunkSection[] sections = new ArenaChunkSection[sectionCount];
                for (int j = 0; j < sectionCount; j++) {
                    boolean setup = in.readBoolean();
                    if (setup) {
                        int yPos = in.readInt();
                        int nonEmptyBlockCount = in.readInt();
                        int tickingBlockCount = in.readInt();
                        boolean isDirty = in.readBoolean();

                        char[] blockIds = readCharArray(in);
                        NibbleArray emittedLight = readNibbleArray(in);
                        NibbleArray skyLight = readNibbleArray(in);

                        sections[j] = new ArenaChunkSection(true, yPos, nonEmptyBlockCount, tickingBlockCount, blockIds,
                                emittedLight, skyLight, isDirty);
                    } else {
                        sections[j] = new ArenaChunkSection(false, 0, 0, 0, null, null, null, false);
                    }
                }

                loadedChunks.add(new ArenaChunk(detail.getMin().getWorld(), x, z, sections));
            }

            detail.getCachedChunks().clear();
            detail.getCachedChunks().addAll(loadedChunks);
            return true;

        } catch (Exception e) {
            int index = detail.getArena().getArenaDetails().indexOf(detail);
            Common.log("&cFailed to load chunks for arena " + detail.getArena().getName() + " (Detail " + index + ")");
            e.printStackTrace();
            // Delete the corrupted file so we don't crash next time
            if (file.exists()) {
                file.delete();
                Common.log("&eDeleted corrupted chunk file for arena " + detail.getArena().getName());
            }
            return false;
        }
    }

    private File getChunkFile(ArenaDetail detail) {
        int index = detail.getArena().getArenaDetails().indexOf(detail);
        return new File(Eden.INSTANCE.getDataFolder(),
                "cache/chunks/" + detail.getArena().getName() + "_" + index + ".chunks");
    }

    private void writeCharArray(DataOutputStream out, char[] array) throws IOException {
        out.writeInt(array.length);
        for (char c : array) {
            out.writeChar(c);
        }
    }

    private char[] readCharArray(DataInputStream in) throws IOException {
        int length = in.readInt();
        char[] array = new char[length];
        for (int i = 0; i < length; i++) {
            array[i] = in.readChar();
        }
        return array;
    }

    private void writeNibbleArray(DataOutputStream out, NibbleArray nibbleArray) throws IOException {
        if (nibbleArray == null) {
            out.writeInt(0);
            return;
        }
        byte[] data = nibbleArray.a();
        out.writeInt(data.length);
        out.write(data);
    }

    private NibbleArray readNibbleArray(DataInputStream in) throws IOException {
        int length = in.readInt();
        if (length == 0) {
            return null;
        }
        byte[] data = new byte[length];
        in.readFully(data);
        return new NibbleArray(data);
    }
}
