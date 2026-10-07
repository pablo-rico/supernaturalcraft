package org.papiricoh.supernaturalcraft.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/** Builds a Hymnal Spire straight into a loaded world: for commands, previews and tests. */
public final class HymnalSpire {

    private HymnalSpire() {
    }

    /** A sampler over a live level's terrain. */
    public static HeightSampler live(ServerLevel level) {
        return (x, z) -> level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
    }

    /**
     * Plans and builds a spire on the highest point near {@code near}.
     *
     * @return the plan (its altar is where the Chorus will be called)
     */
    public static SpireLayout.Plan placeDirect(ServerLevel level, BlockPos near, long seed) {
        HeightSampler h = live(level);
        // Make sure every chunk it might touch is loaded before anything is planned.
        int reach = SpireLayout.MAX_REACH;
        for (int cx = (near.getX() - reach) >> 4; cx <= (near.getX() + reach) >> 4; cx++) {
            for (int cz = (near.getZ() - reach) >> 4; cz <= (near.getZ() + reach) >> 4; cz++) level.getChunk(cx, cz);
        }
        BlockPos peak = HymnalSpireStructure.findPeak(h, near.getX(), near.getZ());
        return placeAt(level, peak, seed, h);
    }

    /** Builds a spire on exactly this peak. */
    public static SpireLayout.Plan placeAt(ServerLevel level, BlockPos peak, long seed, HeightSampler h) {
        SpireLayout.Plan plan = SpireLayout.plan(h, peak.getX(), peak.getY(), peak.getZ(), seed);
        RandomSource random = RandomSource.create(seed);
        for (SpirePiece piece : SpirePiece.all(plan)) {
            BoundingBox box = piece.getBoundingBox();
            piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), random, box,
                    new ChunkPos(peak), peak);
        }
        return plan;
    }
}
