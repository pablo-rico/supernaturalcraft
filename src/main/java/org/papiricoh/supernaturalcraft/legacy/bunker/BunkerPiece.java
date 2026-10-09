package org.papiricoh.supernaturalcraft.legacy.bunker;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import org.papiricoh.supernaturalcraft.registry.AllStructures;

/** The bunker's one piece: its origin and turn are the whole plan, so every chunk builds its share the same. */
public class BunkerPiece extends StructurePiece {

    private final BlockPos origin;
    private final int rotation;

    public BunkerPiece(BlockPos origin, int rotation) {
        super(AllStructures.BUNKER_PIECE.get(), 0, BunkerBuilder.box(origin, rotation));
        this.origin = origin.immutable();
        this.rotation = rotation;
        setOrientation(null);
    }

    public BunkerPiece(CompoundTag tag) {
        super(AllStructures.BUNKER_PIECE.get(), tag);
        this.origin = BlockPos.of(tag.getLong("Origin"));
        this.rotation = tag.getInt("Turn");
        setOrientation(null);
    }

    public BlockPos origin() {
        return origin;
    }

    public int rotation() {
        return rotation;
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putLong("Origin", origin.asLong());
        tag.putInt("Turn", rotation);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunk, BlockPos pivot) {
        BunkerBuilder.build(level, box, origin, rotation);
    }
}
