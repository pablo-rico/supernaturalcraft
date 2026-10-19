package org.papiricoh.supernaturalcraft.cain;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import org.papiricoh.supernaturalcraft.coven.rowena.RowenaRing;
import org.papiricoh.supernaturalcraft.registry.AllWorldgen;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Where Cain's farm may stand (v0.21): the {@link RowenaRing#CANDIDATES} candidate chunks of {@link CainRing}, between
 * {@code min_distance} and {@code max_distance} blocks from the origin. Exactly one gets the farm ({@link CainFarmStructure}
 * decides which). {@code /locate} cannot find it (vanilla only searches its own placements): {@code /supernatural cain locate}
 * can, and his address points there.
 */
public class CainPlacement extends StructurePlacement {

    public static final MapCodec<CainPlacement> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.fieldOf("min_distance").forGetter(p -> p.minDistance),
            Codec.INT.fieldOf("max_distance").forGetter(p -> p.maxDistance)
    ).apply(i, CainPlacement::new));

    private static final Map<String, int[][]> CACHE = new ConcurrentHashMap<>();

    private final int minDistance, maxDistance;

    public CainPlacement(int minDistance, int maxDistance) {
        super(Vec3i.ZERO, FrequencyReductionMethod.DEFAULT, 1.0f, 0, Optional.empty());
        this.minDistance = minDistance;
        this.maxDistance = maxDistance;
    }

    /** The candidate chunks for a seed (on the default ring, which is the one the datapack registers). */
    public static int[][] candidates(long seed) {
        return candidates(seed, CainRing.MIN, CainRing.MAX);
    }

    static int[][] candidates(long seed, int min, int max) {
        if (CACHE.size() > 16) CACHE.clear();
        return CACHE.computeIfAbsent(seed + "/" + min + "/" + max, k -> CainRing.chunks(seed, min, max));
    }

    @Override
    protected boolean isPlacementChunk(ChunkGeneratorStructureState state, int x, int z) {
        long d2 = (long) x * x + (long) z * z;
        long lo = (minDistance >> 4) - 2, hi = (maxDistance >> 4) + 2;
        if (d2 < lo * lo || d2 > hi * hi) return false;
        return RowenaRing.indexOf(candidates(state.getLevelSeed(), minDistance, maxDistance), x, z) >= 0;
    }

    @Override
    public StructurePlacementType<?> type() {
        return AllWorldgen.CAIN_PLACEMENT.get();
    }
}
