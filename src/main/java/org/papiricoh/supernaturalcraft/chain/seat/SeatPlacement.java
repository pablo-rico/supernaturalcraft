package org.papiricoh.supernaturalcraft.chain.seat;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.papiricoh.supernaturalcraft.chain.ChainFaction;
import org.papiricoh.supernaturalcraft.chain.seat.dungeon.DungeonAnchors;

/** World writing for the strongholds, deliberately absent from the structure registry and /locate. */
public final class SeatPlacement {
    private SeatPlacement() {}

    /** How many columns across the footprint {@link #suitable} samples on a side, and how uneven the ground may be. */
    public static final int SAMPLES = 5, MAX_RELIEF = 16;

    /** How far past its walls a stronghold wants untouched land (so it never rises at the edge of explored country). */
    public static final int MARGIN = 32;

    /**
     * The first of {@link SeatSite#candidates} on dry, fairly level land that no one has seen yet, judged from the generator
     * and the region files alone (no chunk is loaded): no ocean, river, beach or mushroom field, no water over the ground, above
     * sea level, relief within {@link #MAX_RELIEF}, and not one of its chunks ever generated ({@link #untouched}). In an older
     * world the stronghold so rises out in country nobody has explored. Failing that, dry land anywhere; failing that, the one
     * with the fewest wet columns.
     */
    public static SeatSite.Site site(ServerLevel level, ChainFaction faction) {
        GeneratedChunks generated = new GeneratedChunks(level);
        var candidates = SeatSite.candidates(level.getSeed(), faction);
        // Reading the region headers is cheap, sampling the generator is not: only unexplored candidates are sampled first.
        for (SeatSite.Site site : candidates) if (untouched(generated, site) && wetness(level, site, true) == 0) return site;
        SeatSite.Site best = null;
        int bestWet = Integer.MAX_VALUE;
        for (SeatSite.Site site : candidates) {
            int wet = wetness(level, site, false);
            if (wet == 0) return site;
            if (wet < bestWet) { bestWet = wet; best = site; }
        }
        return best;
    }

    /** Whether no chunk under the stronghold or within {@link #MARGIN} of its walls was ever generated. */
    public static boolean untouched(ServerLevel level, SeatSite.Site site) {
        return untouched(new GeneratedChunks(level), site);
    }

    private static boolean untouched(GeneratedChunks generated, SeatSite.Site site) {
        int r = DungeonAnchors.RADIUS + MARGIN;
        return !generated.any(site.x() - r, site.z() - r, site.x() + r, site.z() + r);
    }

    /**
     * 0 if the stronghold may stand here; otherwise how many sampled columns are wrong (water, shore, a cliff counts all), or
     * just 1 or more when {@code quick} (it stops at the first wrong column).
     */
    static int wetness(ServerLevel level, SeatSite.Site site, boolean quick) {
        var source = level.getChunkSource();
        var generator = source.getGenerator();
        int r = DungeonAnchors.RADIUS, sea = generator.getSeaLevel(), wet = 0, low = Integer.MAX_VALUE, high = Integer.MIN_VALUE;
        for (int i = 0; i < SAMPLES; i++) for (int j = 0; j < SAMPLES; j++) {
            int x = site.x() - r + i * 2 * r / (SAMPLES - 1), z = site.z() - r + j * 2 * r / (SAMPLES - 1);
            int surface = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, source.randomState());
            int floor = generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, source.randomState());
            var biome = generator.getBiomeSource().getNoiseBiome(net.minecraft.core.QuartPos.fromBlock(x),
                    net.minecraft.core.QuartPos.fromBlock(surface), net.minecraft.core.QuartPos.fromBlock(z), source.randomState().sampler());
            boolean shore = biome.is(net.minecraft.tags.BiomeTags.IS_OCEAN) || biome.is(net.minecraft.tags.BiomeTags.IS_DEEP_OCEAN)
                    || biome.is(net.minecraft.tags.BiomeTags.IS_RIVER) || biome.is(net.minecraft.tags.BiomeTags.IS_BEACH)
                    || biome.is(net.minecraft.world.level.biome.Biomes.MUSHROOM_FIELDS);
            if (shore || surface > floor || surface <= sea) wet++;
            low = Math.min(low, floor);
            high = Math.max(high, floor);
            // Asked only whether the site is good, the first bad column answers it (the generator is slow to sample).
            if (quick && (wet > 0 || high - low > MAX_RELIEF)) return Math.max(wet, 1);
        }
        return high - low > MAX_RELIEF ? Math.max(wet, SAMPLES * SAMPLES) : wet;
    }

    /** Uses generator heights so edited terrain cannot move a previously computed site. */
    public static BlockPos origin(ServerLevel level, ChainFaction faction, SeatSite.Site site) {
        var source = level.getChunkSource();
        int y = source.getGenerator().getBaseHeight(site.x(), site.z(), Heightmap.Types.WORLD_SURFACE_WG,
                level, source.randomState());
        if (faction == ChainFaction.DEMON) y -= 3;
        var bounds = SeatPlans.bounds(faction);
        y = Math.clamp(y, level.getMinBuildHeight() + bounds.depth() + 1, level.getMaxBuildHeight() - bounds.height() - 1);
        return new BlockPos(site.x(), y, site.z());
    }

    /** The walled court of 25×25 that versions before the strongholds built (its footprint, for clearing it away). */
    static final int OLD_RADIUS = 12, OLD_HEIGHT = 12, OLD_DEPTH = 4;

    /** Clears what is left of a seat an older version built where the stronghold no longer stands (water back below the sea). */
    static void clearOldSeat(ServerLevel level, BlockPos origin) {
        int r = OLD_RADIUS, sea = level.getSeaLevel();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) for (int y = -OLD_DEPTH; y <= OLD_HEIGHT; y++) {
            p.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
            if (y < 0 && p.getY() >= sea) continue;
            BlockState fill = p.getY() < sea ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
            if (level.getBlockState(p) != fill) level.setBlock(p, fill, Block.UPDATE_CLIENTS);
        }
    }

    /** Empties one north-south slice ({@code x = origin - radius + slice}) from the floor up. @return blocks changed */
    static int clearSlice(ServerLevel level, BlockPos origin, int slice, int radius, int height) {
        int changed = 0, x = origin.getX() - radius + slice;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int z = -radius; z <= radius; z++) for (int y = 0; y <= height; y++) {
            p.set(x, origin.getY() + y, origin.getZ() + z);
            if (level.getBlockState(p).isAir()) continue;
            level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            changed++;
        }
        return changed;
    }

    /** Solid footings bridge caves and water under the whole footprint, down to firm ground. */
    static void footings(ServerLevel level, BlockPos origin, ChainFaction faction, int radius, int depth) {
        BlockState foundation = (switch (faction) {
            case DEMON -> Blocks.BLACKSTONE;
            case ANGEL -> Blocks.SMOOTH_STONE;
            case LEVIATHAN -> Blocks.POLISHED_ANDESITE;
        }).defaultBlockState();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
            for (int y = origin.getY() - depth - 1; y >= level.getMinBuildHeight(); y--) {
                p.set(origin.getX() + x, y, origin.getZ() + z);
                BlockState old = level.getBlockState(p);
                if (old.isSolidRender(level, p) && old.getFluidState().isEmpty()) break;
                level.setBlock(p, foundation, Block.UPDATE_CLIENTS);
            }
        }
    }

    static BlockState parse(String state) {
        try {
            BlockState parsed = BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.asLookup(), state, false).blockState();
            if (parsed.hasBlockEntity()) throw new IllegalStateException("Block entity in seat: " + state);
            return parsed;
        } catch (CommandSyntaxException e) {
            throw new IllegalStateException("Invalid seat block: " + state, e);
        }
    }
}
