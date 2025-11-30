package rip.diamond.practice.managers.chunk.restoration;

import rip.diamond.practice.util.cuboid.Cuboid;

public interface IChunkRestoration {

    void copy(Cuboid cuboid);

    void reset(Cuboid cuboid);
}
