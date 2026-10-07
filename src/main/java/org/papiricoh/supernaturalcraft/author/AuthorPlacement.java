package org.papiricoh.supernaturalcraft.author;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import org.papiricoh.supernaturalcraft.registry.AllWorldgen;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Where the Author's cabin may stand: the {@link CabinSite#CANDIDATES} candidate chunks chosen from the world seed,
 * between {@code min_distance} and {@code max_distance} blocks from the origin. Exactly one of them gets the cabin
 * ({@link AuthorCabinStructure} decides which). {@code /locate} cannot find it (vanilla only searches its own
 * placements): {@code /supernatural author cabin locate} can.
 */
public class AuthorPlacement extends StructurePlacement {

    public static final MapCodec<AuthorPlacement> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.fieldOf("min_distance").forGetter(p -> p.minDistance),
            Codec.INT.fieldOf("max_distance").forGetter(p -> p.maxDistance)
    ).apply(i, AuthorPlacement::new));

    /** Candidates by seed and ring: computing them is cheap, but this is asked for every chunk of the world. */
    private static final Map<String, int[][]> CACHE = new ConcurrentHashMap<>();

    private final int minDistance, maxDistance;

    public AuthorPlacement(int minDistance, int maxDistance) {
        super(Vec3i.ZERO, FrequencyReductionMethod.DEFAULT, 1.0f, 0, Optional.empty());
        this.minDistance = minDistance;
        this.maxDistance = maxDistance;
    }

    public int minDistance() {
        return minDistance;
    }

    public int maxDistance() {
        return maxDistance;
    }

    /** The candidate chunks for a seed (on the default ring, which is the one the datapack registers). */
    public static int[][] candidates(long seed) {
        return candidates(seed, CabinSite.MIN, CabinSite.MAX);
    }

    static int[][] candidates(long seed, int min, int max) {
        if (CACHE.size() > 16) CACHE.clear();
        return CACHE.computeIfAbsent(seed + "/" + min + "/" + max, k -> CabinSite.chunks(seed, min, max));
    }

    @Override
    protected boolean isPlacementChunk(ChunkGeneratorStructureState state, int x, int z) {
        // Cheap rejection first: the ring is far from everything most chunks are.
        long d2 = (long) x * x + (long) z * z;
        long lo = (minDistance >> 4) - 2, hi = (maxDistance >> 4) + 2;
        if (d2 < lo * lo || d2 > hi * hi) return false;
        return CabinSite.indexOf(candidates(state.getLevelSeed(), minDistance, maxDistance), x, z) >= 0;
    }

    @Override
    public StructurePlacementType<?> type() {
        return AllWorldgen.AUTHOR_PLACEMENT.get();
    }
}
