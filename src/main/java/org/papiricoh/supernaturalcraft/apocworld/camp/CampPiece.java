package org.papiricoh.supernaturalcraft.apocworld.camp;

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
import org.papiricoh.supernaturalcraft.apocworld.ApocWorldRegistry;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;

/**
 * The one piece of an outpost (v0.41). It carries only the origin and the seed: the plan is pure and decided by the seed, so every
 * chunk rebuilds its share of the same outpost.
 */
public class CampPiece extends StructurePiece {

    private final BlockPos origin;
    private final long seed;
    private volatile LayoutPlan plan;

    public CampPiece(BlockPos origin, long seed) {
        super(ApocWorldRegistry.CAMP_PIECE.get(), 0, box(origin, seed));
        this.origin = origin;
        this.seed = seed;
        setOrientation(null);
    }

    public CampPiece(CompoundTag tag) {
        super(ApocWorldRegistry.CAMP_PIECE.get(), tag);
        this.origin = BlockPos.of(tag.getLong("Origin"));
        this.seed = tag.getLong("Seed");
        setOrientation(null);
    }

    /** From the first air layer up (the beard levels the land under it). */
    private static BoundingBox box(BlockPos origin, long seed) {
        BoundingBox full = CampBuilder.fullBox(origin, CampPlan.turnOf(seed));
        return new BoundingBox(full.minX(), origin.getY() + CampAnchors.BOTTOM_Y, full.minZ(), full.maxX(), full.maxY(), full.maxZ());
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putLong("Origin", origin.asLong());
        tag.putLong("Seed", seed);
    }

    public BlockPos origin() {
        return origin;
    }

    private LayoutPlan plan() {
        LayoutPlan p = plan;
        if (p == null) {
            synchronized (this) {
                if (plan == null) plan = CampBuilder.plan(seed);
                p = plan;
            }
        }
        return p;
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunk, BlockPos pivot) {
        CampBuilder.build(level, box, origin, seed, plan());
    }
}
