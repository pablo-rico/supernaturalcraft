package org.papiricoh.supernaturalcraft.author;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import org.papiricoh.supernaturalcraft.registry.AllStructures;

/** The cabin's one piece: its floor centre and turn are the whole plan, so every chunk builds its share the same. */
public class CabinPiece extends StructurePiece {

    private final BlockPos origin;
    private final int rotation;

    public CabinPiece(BlockPos origin, int rotation) {
        super(AllStructures.AUTHOR_CABIN_PIECE.get(), 0, CabinBuilder.box(origin, rotation));
        this.origin = origin.immutable();
        this.rotation = rotation;
        setOrientation(null);
    }

    public CabinPiece(CompoundTag tag) {
        super(AllStructures.AUTHOR_CABIN_PIECE.get(), tag);
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
                            net.minecraft.world.level.levelgen.structure.BoundingBox box, ChunkPos chunk, BlockPos pivot) {
        CabinBuilder.build(level, box, origin, rotation);
    }
}
