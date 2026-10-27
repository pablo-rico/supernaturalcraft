package org.papiricoh.supernaturalcraft.structure.legacy;

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
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import org.papiricoh.supernaturalcraft.registry.AllStructures;

/**
 * The three pieces of a Hymnal Spire generated before the remake ({@code spire_summit}, {@code spire_ascent},
 * {@code spire_temple}), read back from old worlds and built exactly as they always were ({@link LegacySpireBuilder}). Nothing
 * creates them any more: new spires use {@link org.papiricoh.supernaturalcraft.structure.SpirePiece}.
 */
public abstract class LegacySpirePiece extends StructurePiece {

    protected final LegacySpirePlan plan;

    protected LegacySpirePiece(StructurePieceType type, CompoundTag tag) {
        super(type, tag);
        this.plan = LegacySpirePlan.read(tag);
        setOrientation(null);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        plan.save(tag);
    }

    public LegacySpirePlan plan() {
        return plan;
    }

    public static class Summit extends LegacySpirePiece {
        public Summit(CompoundTag tag) {
            super(AllStructures.SPIRE_SUMMIT.get(), tag);
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                                BoundingBox box, ChunkPos chunk, BlockPos pivot) {
            LegacySpireBuilder.summit(level, box, plan);
        }
    }

    public static class Ascent extends LegacySpirePiece {
        public Ascent(CompoundTag tag) {
            super(AllStructures.SPIRE_ASCENT.get(), tag);
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                                BoundingBox box, ChunkPos chunk, BlockPos pivot) {
            LegacySpireBuilder.ascent(level, box, plan);
        }
    }

    public static class Temple extends LegacySpirePiece {
        public Temple(CompoundTag tag) {
            super(AllStructures.SPIRE_TEMPLE.get(), tag);
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                                BoundingBox box, ChunkPos chunk, BlockPos pivot) {
            LegacySpireBuilder.temple(level, box, plan);
        }
    }
}
