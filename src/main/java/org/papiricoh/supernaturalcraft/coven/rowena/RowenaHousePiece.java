package org.papiricoh.supernaturalcraft.coven.rowena;

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
import org.papiricoh.supernaturalcraft.coven.PlanWriter;
import org.papiricoh.supernaturalcraft.registry.AllStructures;

/** The house's one piece: its origin and turn are the whole plan (the design is fixed), so every chunk builds its share the same. */
public class RowenaHousePiece extends StructurePiece {

    private final BlockPos origin;
    private final int rotation;

    public RowenaHousePiece(BlockPos origin, int rotation) {
        super(AllStructures.ROWENA_HOUSE_PIECE.get(), 0, RowenaHouse.pieceBox(origin, rotation));
        this.origin = origin.immutable();
        this.rotation = rotation;
        setOrientation(null);
    }

    public RowenaHousePiece(CompoundTag tag) {
        super(AllStructures.ROWENA_HOUSE_PIECE.get(), tag);
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
        PlanWriter.build(level, box, origin, RowenaHouse.plan(rotation));
    }
}
