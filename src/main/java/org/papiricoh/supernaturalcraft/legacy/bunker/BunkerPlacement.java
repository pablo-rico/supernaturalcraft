package org.papiricoh.supernaturalcraft.legacy.bunker;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.registry.AllWorldgen;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Where the bunker may stand: the {@link BunkerSite#CANDIDATES} candidate chunks of the world seed on the ring (the server
 * config's {@code bunkerMinDistance}/{@code bunkerMaxDistance} once it is loaded, else the datapack's). One of them gets the
 * bunker ({@link BunkerStructure} decides which). {@code /locate} cannot find it; {@code /supernatural legacy bunker locate} can.
 */
public class BunkerPlacement extends StructurePlacement {

    public static final MapCodec<BunkerPlacement> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.fieldOf("min_distance").forGetter(p -> p.minDistance),
            Codec.INT.fieldOf("max_distance").forGetter(p -> p.maxDistance)
    ).apply(i, BunkerPlacement::new));

    private static final Map<String, int[][]> CACHE = new ConcurrentHashMap<>();

    private final int minDistance, maxDistance;

    public BunkerPlacement(int minDistance, int maxDistance) {
        super(Vec3i.ZERO, FrequencyReductionMethod.DEFAULT, 1.0f, 0, Optional.empty());
        this.minDistance = minDistance;
        this.maxDistance = maxDistance;
    }

    /** The ring in use: the config's, if loaded and sensible, else the defaults. */
    public static int[] ring() {
        try {
            if (SNConfig.SPEC.isLoaded()) {
                int min = SNConfig.BUNKER_MIN_DISTANCE.get(), max = SNConfig.BUNKER_MAX_DISTANCE.get();
                if (BunkerSite.validRing(min, max)) return new int[]{min, max};
            }
        } catch (IllegalStateException ignored) {
            // Config not loaded yet (datagen, early worldgen): the defaults.
        }
        return new int[]{BunkerSite.MIN, BunkerSite.MAX};
    }

    /** The candidate chunks for a seed on the ring in use. */
    public static int[][] candidates(long seed) {
        int[] r = ring();
        if (CACHE.size() > 16) CACHE.clear();
        return CACHE.computeIfAbsent(seed + "/" + r[0] + "/" + r[1], k -> BunkerSite.chunks(seed, r[0], r[1]));
    }

    @Override
    protected boolean isPlacementChunk(ChunkGeneratorStructureState state, int x, int z) {
        int[] r = ring();
        long d2 = (long) x * x + (long) z * z;
        long lo = (r[0] >> 4) - 2, hi = (r[1] >> 4) + 2;
        if (d2 < lo * lo || d2 > hi * hi) return false;
        return BunkerSite.indexOf(candidates(state.getLevelSeed()), x, z) >= 0;
    }

    @Override
    public StructurePlacementType<?> type() {
        return AllWorldgen.BUNKER_PLACEMENT.get();
    }
}
