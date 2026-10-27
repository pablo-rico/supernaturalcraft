package org.papiricoh.supernaturalcraft.structure.legacy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;

/**
 * The plan of a Hymnal Spire generated before the remake, as its pieces saved it: altar (one above the platform), hymn, the
 * stair's nodes and the temple's door and facing. Only read back from saved pieces (see {@link LegacySpirePiece}).
 */
public record LegacySpirePlan(BlockPos altar, byte[] melody, List<BlockPos> path, BlockPos templeOrigin, Direction templeFacing, long seed) {

    public static final int PLATFORM_RADIUS = 22;
    public static final int TEMPLE_WIDTH = 11, TEMPLE_DEPTH = 13, TEMPLE_HEIGHT = 9;

    /** The floor of the summit platform (its top layer). */
    public int floorY() {
        return altar.getY() - 1;
    }

    void save(CompoundTag tag) {
        tag.putLong("Altar", altar.asLong());
        tag.putByteArray("Melody", melody);
        tag.putLongArray("Path", path.stream().mapToLong(BlockPos::asLong).toArray());
        tag.putLong("Temple", templeOrigin.asLong());
        tag.putInt("Facing", templeFacing.get3DDataValue());
        tag.putLong("Seed", seed);
    }

    static LegacySpirePlan read(CompoundTag tag) {
        List<BlockPos> path = new ArrayList<>();
        for (long l : tag.getLongArray("Path")) path.add(BlockPos.of(l));
        return new LegacySpirePlan(BlockPos.of(tag.getLong("Altar")), tag.getByteArray("Melody"), List.copyOf(path),
                BlockPos.of(tag.getLong("Temple")), Direction.from3DDataValue(tag.getInt("Facing")), tag.getLong("Seed"));
    }
}
