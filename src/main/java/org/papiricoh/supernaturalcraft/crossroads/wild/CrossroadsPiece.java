package org.papiricoh.supernaturalcraft.crossroads.wild;

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
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;
import org.papiricoh.supernaturalcraft.registry.AllStructures;

/**
 * The one piece of a natural crossroads. It carries only the origin, the road's direction and the seed: the plan is pure and
 * deterministic, so every chunk rebuilds its share of the same crossroads.
 */
public class CrossroadsPiece extends StructurePiece {

    private final BlockPos origin;
    private final int dir;
    private final long seed;
    private LayoutPlan plan;

    public CrossroadsPiece(BlockPos origin, int dir, long seed) {
        this(origin, dir, seed, CrossroadsLayout.plan(seed, dir));
    }

    private CrossroadsPiece(BlockPos origin, int dir, long seed, LayoutPlan plan) {
        super(AllStructures.CROSSROADS_PIECE.get(), 0, CrossroadsBuilder.pieceBox(origin, plan));
        this.origin = origin;
        this.dir = dir;
        this.seed = seed;
        this.plan = plan;
        setOrientation(null);
    }

    public CrossroadsPiece(CompoundTag tag) {
        super(AllStructures.CROSSROADS_PIECE.get(), tag);
        this.origin = BlockPos.of(tag.getLong("Origin"));
        this.dir = tag.getInt("Dir");
        this.seed = tag.getLong("Seed");
        setOrientation(null);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putLong("Origin", origin.asLong());
        tag.putInt("Dir", dir);
        tag.putLong("Seed", seed);
    }

    public BlockPos origin() {
        return origin;
    }

    private LayoutPlan plan() {
        if (plan == null) plan = CrossroadsLayout.plan(seed, dir);
        return plan;
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunk, BlockPos pivot) {
        CrossroadsBuilder.build(level, box, origin, plan());
    }
}
