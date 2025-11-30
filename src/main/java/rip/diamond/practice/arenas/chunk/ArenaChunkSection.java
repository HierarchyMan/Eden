package rip.diamond.practice.arenas.chunk;

import io.github.epicgo.sconey.reflection.Reflection;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.server.v1_8_R3.ChunkSection;
import net.minecraft.server.v1_8_R3.NibbleArray;

@Getter
@RequiredArgsConstructor
public class ArenaChunkSection extends Reflection {

    private final boolean setup;

    private int yPos;
    private int nonEmptyBlockCount;
    private int tickingBlockCount;
    private char[] blockIds;
    private NibbleArray emittedLight;
    private NibbleArray skyLight;
    private boolean isDirty;

    public ArenaChunkSection(ChunkSection section) {
        if (section == null) {
            this.setup = false;
            return;
        }
        this.yPos = section.getYPosition();
        this.nonEmptyBlockCount = (int) getDeclaredField(section, "nonEmptyBlockCount");
        this.tickingBlockCount = (int) getDeclaredField(section, "tickingBlockCount");
        this.blockIds = section.getIdArray().clone();
        this.emittedLight = clone(section.getEmittedLightArray());
        this.skyLight = clone(section.getSkyLightArray());
        this.isDirty = (boolean) getDeclaredField(section, "isDirty");

        this.setup = true;
    }

    public ArenaChunkSection(boolean setup, int yPos, int nonEmptyBlockCount, int tickingBlockCount, char[] blockIds,
            NibbleArray emittedLight, NibbleArray skyLight, boolean isDirty) {
        this.setup = setup;
        this.yPos = yPos;
        this.nonEmptyBlockCount = nonEmptyBlockCount;
        this.tickingBlockCount = tickingBlockCount;
        this.blockIds = blockIds;
        this.emittedLight = emittedLight;
        this.skyLight = skyLight;
        this.isDirty = isDirty;
    }

    public char[] getBlockIds() {
        return blockIds == null ? null : blockIds.clone();
    }

    public NibbleArray getEmittedLight() {
        return clone(emittedLight);
    }

    public NibbleArray getSkyLight() {
        return clone(skyLight);
    }

    private NibbleArray clone(NibbleArray array) {
        if (array == null) {
            return null;
        }
        return new NibbleArray(array.a().clone());
    }

    public ChunkSection toChunkSection() {
        if (setup) {
            ChunkSection section = new ChunkSection(yPos, getSkyLight() != null);

            setDeclaredField(section, "yPos", yPos);
            setDeclaredField(section, "nonEmptyBlockCount", nonEmptyBlockCount);
            setDeclaredField(section, "tickingBlockCount", tickingBlockCount);
            setDeclaredField(section, "blockIds", getBlockIds());
            setDeclaredField(section, "emittedLight", getEmittedLight());
            setDeclaredField(section, "skyLight", getSkyLight());
            setDeclaredField(section, "isDirty", isDirty);

            return section;
        }
        return null;
    }
}
