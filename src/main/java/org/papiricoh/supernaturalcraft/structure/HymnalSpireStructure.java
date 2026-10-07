package org.papiricoh.supernaturalcraft.structure;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import org.papiricoh.supernaturalcraft.registry.AllStructures;

import java.util.Optional;

/**
 * The Hymnal Spire: found on the highest point near a mountain chunk. Finding that point costs
 * about 75 height samples (this runs for {@code /locate} and maps too, so it stays cheap); the
 * stair and temple are planned only when the spire is actually built.
 */
public class HymnalSpireStructure extends Structure {

    public static final MapCodec<HymnalSpireStructure> CODEC = simpleCodec(HymnalSpireStructure::new);
    /** How far above sea level a peak must rise to hold a spire. */
    public static final int MIN_RISE = 48;

    public HymnalSpireStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        ChunkPos cp = ctx.chunkPos();
        HeightSampler h = (x, z) -> ctx.chunkGenerator().getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(),
                ctx.randomState()) - 1;
        BlockPos peak = findPeak(h, cp.getMiddleBlockX(), cp.getMiddleBlockZ());
        if (peak.getY() < ctx.chunkGenerator().getSeaLevel() + MIN_RISE) return Optional.empty();
        long seed = ctx.seed() ^ cp.toLong() * 0x2545F4914F6CDD1DL;
        // The stub sits on the ground (the biome check looks there), not on the floating platform.
        return Optional.of(new GenerationStub(peak, builder -> {
            SpireLayout.Plan plan = SpireLayout.plan(h, peak.getX(), peak.getY(), peak.getZ(), seed);
            SpirePiece.all(plan).forEach(builder::addPiece);
        }));
    }

    /** The highest point on a coarse grid around (x, z), refined on a fine one. */
    public static BlockPos findPeak(HeightSampler h, int x, int z) {
        int bx = x, bz = z, by = h.top(x, z);
        for (int dx = -27; dx <= 27; dx += 9) {
            for (int dz = -27; dz <= 27; dz += 9) {
                int y = h.top(x + dx, z + dz);
                if (y > by) {
                    by = y;
                    bx = x + dx;
                    bz = z + dz;
                }
            }
        }
        int cx = bx, cz = bz;
        for (int dx = -6; dx <= 6; dx += 3) {
            for (int dz = -6; dz <= 6; dz += 3) {
                int y = h.top(cx + dx, cz + dz);
                if (y > by) {
                    by = y;
                    bx = cx + dx;
                    bz = cz + dz;
                }
            }
        }
        return new BlockPos(bx, by, bz);
    }

    @Override
    public StructureType<?> type() {
        return AllStructures.HYMNAL_SPIRE.get();
    }
}
