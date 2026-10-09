package org.papiricoh.supernaturalcraft.legacy.bunker;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Which of the {@link BunkerSite} candidates holds the bunker, worked out the same way by the world generator (in
 * {@link BunkerStructure}) and by the server (from the live world): the first candidate on quiet ground in a
 * {@link BunkerStructure#BIOMES} biome. Remembered per {@link RandomState}.
 */
public final class BunkerLocator {

    private static final int REACH = 8;
    private static final Map<RandomState, Integer> CHOSEN = new WeakHashMap<>();

    /** Where the bunker stands: the hut's floor centre and its turn. */
    public record Site(BlockPos origin, int rotation) {
    }

    private BunkerLocator() {
    }

    public static int chosen(long seed, ChunkGenerator generator, BiomeSource biomes, RandomState random, LevelHeightAccessor heights) {
        synchronized (CHOSEN) {
            Integer known = CHOSEN.get(random);
            if (known != null) return known;
        }
        int[][] chunks = BunkerPlacement.candidates(seed);
        int k = BunkerSite.choose(i -> quiet(chunks[i], generator, biomes, random, heights));
        synchronized (CHOSEN) {
            CHOSEN.put(random, k);
        }
        return k;
    }

    static boolean quiet(int[] chunk, ChunkGenerator generator, BiomeSource biomes, RandomState random, LevelHeightAccessor heights) {
        int x = BunkerSite.middle(chunk[0]), z = BunkerSite.middle(chunk[1]);
        int[][] at = {{0, 0}, {-REACH, -REACH}, {REACH, -REACH}, {-REACH, REACH}, {REACH, REACH}};
        int[] tops = new int[at.length], floors = new int[at.length];
        for (int i = 0; i < at.length; i++) {
            tops[i] = generator.getBaseHeight(x + at[i][0], z + at[i][1], Heightmap.Types.WORLD_SURFACE_WG, heights, random);
            floors[i] = generator.getBaseHeight(x + at[i][0], z + at[i][1], Heightmap.Types.OCEAN_FLOOR_WG, heights, random);
        }
        if (!BunkerSite.quiet(tops, floors, generator.getSeaLevel())) return false;
        Holder<Biome> biome = biomes.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(tops[0]), QuartPos.fromBlock(z), random.sampler());
        return biome.is(BunkerStructure.BIOMES);
    }

    /** Where the world generator puts (or would put) the bunker, from the live overworld. */
    public static Site compute(ServerLevel level) {
        var source = level.getChunkSource();
        ChunkGenerator generator = source.getGenerator();
        RandomState random = source.randomState();
        long seed = level.getSeed();
        int k = chosen(seed, generator, generator.getBiomeSource(), random, level);
        int[] chunk = BunkerPlacement.candidates(seed)[k];
        int x = BunkerSite.middle(chunk[0]), z = BunkerSite.middle(chunk[1]);
        // The plan's y 0 is the ground's top block (the base height is the first block of air over it).
        int y = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, random) - 1;
        return new Site(new BlockPos(x, y, z), BunkerSite.rotation(seed));
    }

    /** The bunker's site for this world, worked out once and remembered in {@link BunkerSavedData}. */
    public static Site of(ServerLevel level) {
        BunkerSavedData data = BunkerSavedData.get(level);
        if (data.origin() == null) {
            Site s = compute(level.getServer().overworld());
            data.setBunker(s.origin(), s.rotation(), false);
        }
        return new Site(data.origin(), data.rotation());
    }
}
