package org.papiricoh.supernaturalcraft.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

import java.util.ArrayList;
import java.util.List;

/**
 * One part of a Hymnal Spire. Each piece carries the whole {@link SpireLayout.Plan} (it is small),
 * so it rebuilds exactly the same from its saved data in whichever chunk it is asked to fill.
 */
public abstract class SpirePiece extends StructurePiece {

    protected final SpireLayout.Plan plan;

    protected SpirePiece(StructurePieceType type, SpireLayout.Plan plan, BoundingBox box) {
        super(type, 0, box);
        this.plan = plan;
        setOrientation(null);
    }

    protected SpirePiece(StructurePieceType type, CompoundTag tag) {
        super(type, tag);
        this.plan = readPlan(tag);
        setOrientation(null);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putLong("Altar", plan.altar().asLong());
        tag.putByteArray("Melody", plan.melody());
        tag.putLongArray("Path", plan.path().stream().mapToLong(BlockPos::asLong).toArray());
        tag.putLong("Temple", plan.templeOrigin().asLong());
        tag.putInt("Facing", plan.templeFacing().get3DDataValue());
        tag.putLong("Seed", plan.seed());
    }

    static SpireLayout.Plan readPlan(CompoundTag tag) {
        List<BlockPos> path = new ArrayList<>();
        for (long l : tag.getLongArray("Path")) path.add(BlockPos.of(l));
        return new SpireLayout.Plan(BlockPos.of(tag.getLong("Altar")), tag.getByteArray("Melody"), List.copyOf(path),
                BlockPos.of(tag.getLong("Temple")), Direction.from3DDataValue(tag.getInt("Facing")), tag.getLong("Seed"));
    }

    public SpireLayout.Plan plan() {
        return plan;
    }

    // --- the three pieces -------------------------------------------------------------------

    public static class Summit extends SpirePiece {
        public Summit(SpireLayout.Plan plan) {
            super(AllStructures.SPIRE_SUMMIT.get(), plan, box(plan));
        }

        public Summit(CompoundTag tag) {
            super(AllStructures.SPIRE_SUMMIT.get(), tag);
        }

        static BoundingBox box(SpireLayout.Plan plan) {
            return SpireLayout.summitBox(plan);
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                                BoundingBox box, ChunkPos chunk, BlockPos pivot) {
            SpireBuilder.summit(level, box, plan);
        }
    }

    public static class Ascent extends SpirePiece {
        public Ascent(SpireLayout.Plan plan) {
            super(AllStructures.SPIRE_ASCENT.get(), plan, box(plan));
        }

        public Ascent(CompoundTag tag) {
            super(AllStructures.SPIRE_ASCENT.get(), tag);
        }

        static BoundingBox box(SpireLayout.Plan plan) {
            return SpireLayout.ascentBox(plan);
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                                BoundingBox box, ChunkPos chunk, BlockPos pivot) {
            SpireBuilder.ascent(level, box, plan);
        }
    }

    public static class Temple extends SpirePiece {
        public Temple(SpireLayout.Plan plan) {
            super(AllStructures.SPIRE_TEMPLE.get(), plan, box(plan));
        }

        public Temple(CompoundTag tag) {
            super(AllStructures.SPIRE_TEMPLE.get(), tag);
        }

        static BoundingBox box(SpireLayout.Plan plan) {
            return SpireLayout.templeBox(plan);
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                                BoundingBox box, ChunkPos chunk, BlockPos pivot) {
            SpireBuilder.temple(level, box, plan);
        }
    }

    /** All three, in the order they must be built (the temple and stair cut into what the summit leaves). */
    public static List<SpirePiece> all(SpireLayout.Plan plan) {
        return List.of(new Summit(plan), new Ascent(plan), new Temple(plan));
    }
}
