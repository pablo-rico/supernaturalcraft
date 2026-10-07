package org.papiricoh.supernaturalcraft.author;

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
 * Which of the {@link CabinSite} candidates holds the cabin, worked out the same way by the world generator (in
 * {@link AuthorCabinStructure}) and by "Find the Author" (from the live world): the first candidate on quiet, flat, dry
 * ground. Each answer costs up to a few hundred noise samples, so it is remembered per {@link RandomState}.
 */
public final class AuthorSite {

    /** How far from the middle the corners are sampled for flatness. */
    private static final int REACH = 6;
    private static final Map<RandomState, Integer> CHOSEN = new WeakHashMap<>();

    /** Where the cabin stands: its floor centre and turn. */
    public record Site(BlockPos origin, int rotation) {
    }

    private AuthorSite() {
    }

    /** The candidate index that holds the cabin. */
    public static int chosen(long seed, ChunkGenerator generator, BiomeSource biomes, RandomState random, LevelHeightAccessor heights) {
        synchronized (CHOSEN) {
            Integer known = CHOSEN.get(random);
            if (known != null) return known;
        }
        int[][] chunks = AuthorPlacement.candidates(seed);
        int k = CabinSite.choose(i -> quiet(chunks[i], generator, biomes, random, heights));
        synchronized (CHOSEN) {
            CHOSEN.put(random, k);
        }
        return k;
    }

    /** Whether a candidate chunk's middle is quiet land: a cabin biome, flat and dry. */
    static boolean quiet(int[] chunk, ChunkGenerator generator, BiomeSource biomes, RandomState random, LevelHeightAccessor heights) {
        int x = CabinSite.middle(chunk[0]), z = CabinSite.middle(chunk[1]);
        int[][] at = {{0, 0}, {-REACH, -REACH}, {REACH, -REACH}, {-REACH, REACH}, {REACH, REACH}};
        int[] tops = new int[at.length], floors = new int[at.length];
        for (int i = 0; i < at.length; i++) {
            tops[i] = generator.getBaseHeight(x + at[i][0], z + at[i][1], Heightmap.Types.WORLD_SURFACE_WG, heights, random);
            floors[i] = generator.getBaseHeight(x + at[i][0], z + at[i][1], Heightmap.Types.OCEAN_FLOOR_WG, heights, random);
        }
        if (!CabinSite.flat(tops, floors, generator.getSeaLevel())) return false;
        Holder<Biome> biome = biomes.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(tops[0]), QuartPos.fromBlock(z), random.sampler());
        return biome.is(AuthorCabinStructure.QUIET_BIOMES);
    }

    /** Where the world generator puts (or would put) the cabin, read from the live overworld. */
    public static Site compute(ServerLevel level) {
        var source = level.getChunkSource();
        ChunkGenerator generator = source.getGenerator();
        RandomState random = source.randomState();
        long seed = level.getSeed();
        int k = chosen(seed, generator, generator.getBiomeSource(), random, level);
        int[] chunk = AuthorPlacement.candidates(seed)[k];
        int x = CabinSite.middle(chunk[0]), z = CabinSite.middle(chunk[1]);
        int y = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, random);
        return new Site(new BlockPos(x, y, z), CabinSite.rotation(seed));
    }

    /** The cabin's site for this world, worked out once and remembered in {@link AuthorSavedData}. */
    public static Site of(ServerLevel level) {
        AuthorSavedData data = AuthorSavedData.get(level);
        if (data.cabin() == null) {
            Site s = compute(level.getServer().overworld());
            data.setCabin(s.origin(), s.rotation(), false);
        }
        return new Site(data.cabin(), data.rotation());
    }
}
