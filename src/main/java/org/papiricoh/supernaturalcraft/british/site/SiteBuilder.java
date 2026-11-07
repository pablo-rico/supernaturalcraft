package org.papiricoh.supernaturalcraft.british.site;

import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.apocalypse.warehouse.WarehousePlan;
import org.papiricoh.supernaturalcraft.chain.seat.GeneratedChunks;
import org.papiricoh.supernaturalcraft.chain.seat.SeatPlacement;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.ArenaCell;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Season 12's unique places in the world (v0.41), the way Alastair's warehouse is raised ({@code WarehouseWorld}): one of each
 * per world, hidden from {@code /locate} (they are no structures), raised the first time they are needed ({@link #ensure}), in
 * country nobody has explored if there is any ({@link GeneratedChunks}), dry and level. Written in batches: their chunks
 * forced, the land above the pad cleared, footings under it, then the plan. A restart drops the work; the next
 * {@link #ensure} begins it again at the same origin.
 */
public final class SiteBuilder {

    public static final int BATCH = 6000, LOAD_TIMEOUT = 1200, SAMPLES = 5;

    private static final Map<String, Job> JOBS = new ConcurrentHashMap<>();

    private SiteBuilder() {
    }

    private static final class Job {
        final SiteSpec spec;
        final BlockPos origin;
        final int q;
        final List<ArenaCell> cells;
        final Map<String, BlockState> states = new HashMap<>();
        final List<long[]> chunks = new ArrayList<>();
        int phase = -1, index, waited;

        Job(SiteSpec spec, BlockPos origin, int q) {
            this.spec = spec;
            this.origin = origin.immutable();
            this.q = q;
            this.cells = PlacedSite.plan(spec, q).cells();
            for (ArenaCell c : cells) states.computeIfAbsent(c.block(), SeatPlacement::parse);
        }
    }

    /** The site's origin, worked out (and remembered) the first time; it is built if it is not yet. */
    public static BlockPos ensure(ServerLevel overworld, SiteSpec spec) {
        BritishSitesData data = BritishSitesData.get(overworld.getServer());
        BritishSitesData.Entry e = data.get(spec.id());
        if (e == null) {
            int[] at = pick(overworld, spec);
            int q = (int) Math.floorMod((overworld.getSeed() ^ spec.salt()) >>> 13, 4L);
            var source = overworld.getChunkSource();
            int y = source.getGenerator().getBaseHeight(at[0], at[1], Heightmap.Types.WORLD_SURFACE_WG, overworld, source.randomState());
            y = Math.clamp(y - spec.groundY() - 1, overworld.getMinBuildHeight() - spec.bottomY() + 1, overworld.getMaxBuildHeight() - spec.topY() - 1);
            data.set(spec.id(), new BlockPos(at[0], y, at[1]), q, false);
            e = data.get(spec.id());
        }
        if (!e.built() && !JOBS.containsKey(spec.id())) start(overworld, spec, e.origin(), e.q());
        return e.origin();
    }

    /** Where the site stands (built or not), or null if it was never placed. */
    public static @Nullable PlacedSite site(MinecraftServer server, SiteSpec spec) {
        BritishSitesData.Entry e = BritishSitesData.get(server).get(spec.id());
        return e == null ? null : new PlacedSite(spec, e.origin(), e.q());
    }

    public static boolean built(MinecraftServer server, SiteSpec spec) {
        BritishSitesData.Entry e = BritishSitesData.get(server).get(spec.id());
        return e != null && e.built();
    }

    public static boolean building(SiteSpec spec) {
        return JOBS.containsKey(spec.id());
    }

    /** Builds now, in one go (commands, tests). */
    public static PlacedSite buildNow(ServerLevel level, SiteSpec spec) {
        ensure(level, spec);
        Job j;
        while ((j = JOBS.get(spec.id())) != null) step(level, j, Integer.MAX_VALUE);
        return site(level.getServer(), spec);
    }

    /** Builds the site at {@code origin} in one go, forgetting any other (commands, tests, previews). */
    public static PlacedSite placeAt(ServerLevel level, SiteSpec spec, BlockPos origin, int q) {
        JOBS.remove(spec.id());
        BritishSitesData.get(level.getServer()).set(spec.id(), origin, q, false);
        start(level, spec, origin, q);
        Job j;
        while ((j = JOBS.get(spec.id())) != null) step(level, j, Integer.MAX_VALUE);
        return new PlacedSite(spec, origin, q);
    }

    /** Forgets the site (tests): the world has none of it again. */
    public static void forget(MinecraftServer server, SiteSpec spec) {
        JOBS.remove(spec.id());
        BritishSitesData.get(server).set(spec.id(), null, 0, false);
    }

    /** Once a tick: a batch of whichever sites are being raised. */
    public static void tick(ServerLevel overworld) {
        for (Job j : List.copyOf(JOBS.values())) step(overworld, j, BATCH);
    }

    static int[] pick(ServerLevel level, SiteSpec spec) {
        Random r = new Random(level.getSeed() ^ spec.salt());
        List<int[]> candidates = new ArrayList<>();
        for (int i = 0; i < spec.candidates(); i++) {
            double a = r.nextDouble() * Math.PI * 2;
            int d = spec.ringMin() + i * spec.ringStep();
            candidates.add(new int[]{(int) Math.round(Math.cos(a) * d), (int) Math.round(Math.sin(a) * d)});
        }
        GeneratedChunks generated = new GeneratedChunks(level);
        int[] fallback = null;
        int reach = spec.reach() + 16;
        for (int[] c : candidates) {
            boolean untouched = !generated.any(c[0] - reach, c[1] - reach, c[0] + reach, c[1] + reach);
            if (untouched && judge(level, spec, c)) return c;
            if (fallback == null && untouched) fallback = c;
        }
        return fallback != null ? fallback : candidates.getFirst();
    }

    static boolean judge(ServerLevel level, SiteSpec spec, int[] c) {
        var source = level.getChunkSource();
        var generator = source.getGenerator();
        int sea = generator.getSeaLevel(), low = Integer.MAX_VALUE, high = Integer.MIN_VALUE;
        int reach = spec.reach();
        for (int i = 0; i < SAMPLES; i++) for (int j = 0; j < SAMPLES; j++) {
            int x = c[0] - reach + i * 2 * reach / (SAMPLES - 1);
            int z = c[1] - reach + j * 2 * reach / (SAMPLES - 1);
            int surface = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, source.randomState());
            int floor = generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, source.randomState());
            var biome = generator.getBiomeSource().getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(surface), QuartPos.fromBlock(z),
                    source.randomState().sampler());
            if (biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_RIVER) || biome.is(BiomeTags.IS_BEACH) || surface > floor || surface <= sea) return false;
            low = Math.min(low, floor);
            high = Math.max(high, floor);
            if (high - low > spec.maxRelief()) return false;
        }
        return true;
    }

    static void start(ServerLevel level, SiteSpec spec, BlockPos origin, int q) {
        Job j = new Job(spec, origin, q);
        int[] a = WarehousePlan.rotate(spec.minX(), spec.minZ(), q), b = WarehousePlan.rotate(spec.maxX(), spec.maxZ(), q);
        int x0 = origin.getX() + Math.min(a[0], b[0]), x1 = origin.getX() + Math.max(a[0], b[0]);
        int z0 = origin.getZ() + Math.min(a[1], b[1]), z1 = origin.getZ() + Math.max(a[1], b[1]);
        for (int cx = x0 >> 4; cx <= x1 >> 4; cx++)
            for (int cz = z0 >> 4; cz <= z1 >> 4; cz++) {
                level.setChunkForced(cx, cz, true);
                j.chunks.add(new long[]{cx, cz});
            }
        JOBS.put(spec.id(), j);
    }

    private static void step(ServerLevel level, Job j, int budget) {
        SiteSpec s = j.spec;
        int q = j.q;
        if (j.phase < 0) {
            boolean loaded = j.chunks.stream().allMatch(c -> level.getChunkSource().hasChunk((int) c[0], (int) c[1]));
            if (loaded || ++j.waited > LOAD_TIMEOUT || budget == Integer.MAX_VALUE) j.phase = 0;
            return;
        }
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        if (j.phase == 0) {
            int width = s.maxX() - s.minX();
            do {
                int lx = s.minX() + j.index;
                for (int lz = s.minZ(); lz <= s.maxZ(); lz++) {
                    int[] w = WarehousePlan.rotate(lx, lz, q);
                    for (int y = s.groundY() + 1; y <= s.topY() + 8; y++) {
                        p.set(j.origin.getX() + w[0], j.origin.getY() + y, j.origin.getZ() + w[1]);
                        if (!level.getBlockState(p).isAir()) {
                            level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                            budget--;
                        }
                    }
                }
                if (++j.index > width) {
                    j.phase = 1;
                    j.index = 0;
                    break;
                }
            } while (budget > 0);
            return;
        }
        if (j.phase == 1) {
            for (int lx = s.minX(); lx <= s.maxX(); lx++)
                for (int lz = s.minZ(); lz <= s.maxZ(); lz++) {
                    int[] w = WarehousePlan.rotate(lx, lz, q);
                    p.set(j.origin.getX() + w[0], j.origin.getY() + s.bottomY(), j.origin.getZ() + w[1]);
                    for (int y = p.getY(); y >= level.getMinBuildHeight(); y--) {
                        p.setY(y);
                        BlockState old = level.getBlockState(p);
                        if (old.isSolidRender(level, p) && old.getFluidState().isEmpty()) break;
                        level.setBlock(p, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                    for (int y = s.bottomY() + 1; y <= s.groundY(); y++) {
                        p.set(j.origin.getX() + w[0], j.origin.getY() + y, j.origin.getZ() + w[1]);
                        BlockState old = level.getBlockState(p);
                        if (old.isAir() || !old.getFluidState().isEmpty()) {
                            level.setBlock(p, y == s.groundY() ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
                        }
                    }
                }
            j.phase = 2;
            return;
        }
        if (j.phase == 2) {
            while (budget-- > 0 && j.index < j.cells.size()) {
                ArenaCell cell = j.cells.get(j.index++);
                level.setBlock(j.origin.offset(cell.dx(), cell.dy(), cell.dz()), j.states.get(cell.block()), Block.UPDATE_CLIENTS);
            }
            if (j.index >= j.cells.size()) j.phase = 3;
            return;
        }
        for (long[] c : j.chunks) level.setChunkForced((int) c[0], (int) c[1], false);
        BritishSitesData.get(level.getServer()).set(s.id(), j.origin, q, true);
        JOBS.remove(s.id());
    }
}
