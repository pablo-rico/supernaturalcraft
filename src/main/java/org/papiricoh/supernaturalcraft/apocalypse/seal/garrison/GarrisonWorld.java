package org.papiricoh.supernaturalcraft.apocalypse.seal.garrison;

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
import org.papiricoh.supernaturalcraft.apocalypse.seal.SealSavedData;
import org.papiricoh.supernaturalcraft.chain.seat.GeneratedChunks;
import org.papiricoh.supernaturalcraft.chain.seat.SeatPlacement;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.ArenaCell;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Uriel's garrison in the world (v0.40): one per world, hidden from {@code /locate} (it is no structure), raised the first time
 * Castiel tells a hunter of the traitor ({@link #ensure}), in country nobody has explored if there is any, dry and level. Written
 * in batches like Crowley's lab ({@code PactWorld}): its chunks forced, the land above the pad cleared, footings, the plan. A
 * restart drops the work; the next {@link #ensure} begins it again at the same origin.
 */
public final class GarrisonWorld {

    public static final int BATCH = 6000, LOAD_TIMEOUT = 1200;
    public static final int RING_MIN = 900, RING_STEP = 160, CANDIDATES = 16;
    public static final int MAX_RELIEF = 5, SAMPLES = 5;

    private static @Nullable Job job;

    private GarrisonWorld() {
    }

    private static final class Job {
        final BlockPos origin;
        final int q;
        final List<ArenaCell> cells;
        final Map<String, BlockState> states = new HashMap<>();
        final List<long[]> chunks = new ArrayList<>();
        int phase = -1, index, waited;

        Job(BlockPos origin, int q, LayoutPlan plan) {
            this.origin = origin.immutable();
            this.q = q;
            this.cells = plan.cells();
            for (ArenaCell c : cells) states.computeIfAbsent(c.block(), SeatPlacement::parse);
        }
    }

    /** The garrison's origin, worked out (and remembered) the first time; it is built if it is not yet. */
    public static BlockPos ensure(ServerLevel overworld) {
        SealSavedData data = SealSavedData.get(overworld.getServer());
        if (data.garrison() == null) {
            int[] site = site(overworld);
            int q = (int) Math.floorMod(overworld.getSeed() >>> 13, 4L);
            var source = overworld.getChunkSource();
            int y = source.getGenerator().getBaseHeight(site[0], site[1], Heightmap.Types.WORLD_SURFACE_WG, overworld, source.randomState());
            y = Math.clamp(y - GarrisonAnchors.GROUND_Y - 1, overworld.getMinBuildHeight() - GarrisonAnchors.BOTTOM_Y + 1,
                    overworld.getMaxBuildHeight() - GarrisonAnchors.TOP_Y - 1);
            data.setGarrison(new BlockPos(site[0], y, site[1]), q, false);
        }
        if (!data.garrisonBuilt() && job == null) start(overworld, data.garrison(), data.garrisonQ());
        return data.garrison();
    }

    /** The garrison, if it has a site (built or being built). */
    public static @Nullable GarrisonSite site(MinecraftServer server) {
        SealSavedData data = SealSavedData.get(server);
        return data.garrison() == null ? null : new GarrisonSite(data.garrison(), data.garrisonQ());
    }

    public static boolean building() {
        return job != null;
    }

    private static int reach() {
        return Math.max(Math.max(-GarrisonAnchors.MIN_X, GarrisonAnchors.MAX_X), Math.max(-GarrisonAnchors.MIN_Z, GarrisonAnchors.MAX_Z));
    }

    static int[] site(ServerLevel level) {
        Random r = new Random(level.getSeed() ^ 0x5EA1_0040L);
        List<int[]> candidates = new ArrayList<>();
        for (int i = 0; i < CANDIDATES; i++) {
            double a = r.nextDouble() * Math.PI * 2;
            int d = RING_MIN + i * RING_STEP;
            candidates.add(new int[]{(int) Math.round(Math.cos(a) * d), (int) Math.round(Math.sin(a) * d)});
        }
        GeneratedChunks generated = new GeneratedChunks(level);
        int[] fallback = null;
        for (int[] c : candidates) {
            int reach = reach() + 16;
            boolean untouched = !generated.any(c[0] - reach, c[1] - reach, c[0] + reach, c[1] + reach);
            if (untouched && judge(level, c)) return c;
            if (fallback == null && untouched) fallback = c;
        }
        return fallback != null ? fallback : candidates.getFirst();
    }

    static boolean judge(ServerLevel level, int[] c) {
        var source = level.getChunkSource();
        var generator = source.getGenerator();
        int sea = generator.getSeaLevel(), low = Integer.MAX_VALUE, high = Integer.MIN_VALUE;
        int reach = reach();
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
            if (high - low > MAX_RELIEF) return false;
        }
        return true;
    }

    static void start(ServerLevel level, BlockPos origin, int q) {
        Job j = new Job(origin, q, GarrisonSite.plan(q));
        int[] a = GarrisonPlan.rotate(GarrisonAnchors.MIN_X, GarrisonAnchors.MIN_Z, q), b = GarrisonPlan.rotate(GarrisonAnchors.MAX_X, GarrisonAnchors.MAX_Z, q);
        int x0 = origin.getX() + Math.min(a[0], b[0]), x1 = origin.getX() + Math.max(a[0], b[0]);
        int z0 = origin.getZ() + Math.min(a[1], b[1]), z1 = origin.getZ() + Math.max(a[1], b[1]);
        for (int cx = x0 >> 4; cx <= x1 >> 4; cx++)
            for (int cz = z0 >> 4; cz <= z1 >> 4; cz++) {
                level.setChunkForced(cx, cz, true);
                j.chunks.add(new long[]{cx, cz});
            }
        job = j;
    }

    /** Builds now, in one go (commands, tests). */
    public static void buildNow(ServerLevel level) {
        ensure(level);
        while (job != null) step(level, Integer.MAX_VALUE);
    }

    /** Builds the garrison at {@code origin} in one go, forgetting any other (commands, tests, previews). */
    public static GarrisonSite placeAt(ServerLevel level, BlockPos origin, int q) {
        job = null;
        SealSavedData.get(level.getServer()).setGarrison(origin, q, false);
        start(level, origin, q);
        while (job != null) step(level, Integer.MAX_VALUE);
        return new GarrisonSite(origin, q);
    }

    public static void tick(ServerLevel overworld) {
        if (job != null) step(overworld, BATCH);
    }

    private static void step(ServerLevel level, int budget) {
        Job j = job;
        if (j == null) return;
        int q = j.q;
        if (j.phase < 0) {
            boolean loaded = j.chunks.stream().allMatch(c -> level.getChunkSource().hasChunk((int) c[0], (int) c[1]));
            if (loaded || ++j.waited > LOAD_TIMEOUT || budget == Integer.MAX_VALUE) j.phase = 0;
            return;
        }
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        if (j.phase == 0) {
            int width = GarrisonAnchors.MAX_X - GarrisonAnchors.MIN_X;
            do {
                int lx = GarrisonAnchors.MIN_X + j.index;
                for (int lz = GarrisonAnchors.MIN_Z; lz <= GarrisonAnchors.MAX_Z; lz++) {
                    int[] w = GarrisonPlan.rotate(lx, lz, q);
                    for (int y = GarrisonAnchors.GROUND_Y + 1; y <= GarrisonAnchors.TOP_Y + 8; y++) {
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
            for (int lx = GarrisonAnchors.MIN_X; lx <= GarrisonAnchors.MAX_X; lx++)
                for (int lz = GarrisonAnchors.MIN_Z; lz <= GarrisonAnchors.MAX_Z; lz++) {
                    int[] w = GarrisonPlan.rotate(lx, lz, q);
                    p.set(j.origin.getX() + w[0], j.origin.getY() + GarrisonAnchors.BOTTOM_Y, j.origin.getZ() + w[1]);
                    for (int y = p.getY(); y >= level.getMinBuildHeight(); y--) {
                        p.setY(y);
                        BlockState old = level.getBlockState(p);
                        if (old.isSolidRender(level, p) && old.getFluidState().isEmpty()) break;
                        level.setBlock(p, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                    for (int y = GarrisonAnchors.BOTTOM_Y + 1; y < GarrisonAnchors.GROUND_Y; y++) {
                        p.set(j.origin.getX() + w[0], j.origin.getY() + y, j.origin.getZ() + w[1]);
                        BlockState old = level.getBlockState(p);
                        if (old.isAir() || !old.getFluidState().isEmpty()) level.setBlock(p, Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
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
        SealSavedData.get(level.getServer()).setGarrison(j.origin, q, true);
        job = null;
    }
}
