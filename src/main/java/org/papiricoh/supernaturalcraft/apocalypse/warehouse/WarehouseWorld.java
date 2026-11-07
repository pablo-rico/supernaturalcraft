package org.papiricoh.supernaturalcraft.apocalypse.warehouse;

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
import org.papiricoh.supernaturalcraft.chain.seat.GeneratedChunks;
import org.papiricoh.supernaturalcraft.chain.seat.SeatPlacement;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.ArenaCell;
import org.papiricoh.supernaturalcraft.hunter.DevilsTrapBlock;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Alastair's warehouse in the world (v0.40): one per world, hidden from {@code /locate} (it is no structure), raised the first
 * time Ruby sends a hunter there ({@link #ensure}), in country nobody has explored if there is any ({@link GeneratedChunks}),
 * dry and level. Written in batches like Crowley's lab ({@code PactWorld}): its chunks forced, the land above the pad cleared,
 * the plan, footings under it, and last the devil's trap painted in the middle of the floor. A restart drops the work; the next
 * {@link #ensure} begins it again at the same origin.
 */
public final class WarehouseWorld {

    public static final int BATCH = 6000, LOAD_TIMEOUT = 1200;
    /** The candidate ring round the world's centre (blocks), and how many candidates (nearer than Crowley's lab). */
    public static final int RING_MIN = 900, RING_STEP = 150, CANDIDATES = 16;
    public static final int MAX_RELIEF = 6, SAMPLES = 5;

    private static @Nullable Job job;

    private WarehouseWorld() {
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

    /** The warehouse's origin, worked out (and remembered) the first time; it is built if it is not yet. */
    public static BlockPos ensure(ServerLevel overworld) {
        WarehouseSavedData data = WarehouseSavedData.get(overworld.getServer());
        if (data.origin() == null) {
            int[] site = site(overworld);
            int q = (int) Math.floorMod(overworld.getSeed() >>> 13, 4L);
            var source = overworld.getChunkSource();
            int y = source.getGenerator().getBaseHeight(site[0], site[1], Heightmap.Types.WORLD_SURFACE_WG, overworld, source.randomState());
            y = Math.clamp(y - WarehouseAnchors.GROUND_Y - 1, overworld.getMinBuildHeight() - WarehouseAnchors.BOTTOM_Y + 1,
                    overworld.getMaxBuildHeight() - WarehouseAnchors.TOP_Y - 1);
            data.set(new BlockPos(site[0], y, site[1]), q, false);
        }
        if (!data.built() && job == null) start(overworld, data.origin(), data.q());
        return data.origin();
    }

    public static @Nullable WarehouseSite site(MinecraftServer server) {
        WarehouseSavedData data = WarehouseSavedData.get(server);
        return data.origin() == null ? null : new WarehouseSite(data.origin(), data.q());
    }

    /** The middle of the devil's trap, or null (no warehouse yet). */
    public static @Nullable BlockPos trap(MinecraftServer server) {
        WarehouseSite s = site(server);
        return s == null ? null : s.trap();
    }

    public static boolean built(MinecraftServer server) {
        return WarehouseSavedData.get(server).built();
    }

    public static boolean building() {
        return job != null;
    }

    static int[] site(ServerLevel level) {
        Random r = new Random(level.getSeed() ^ 0x5EA5_0040L);
        List<int[]> candidates = new ArrayList<>();
        for (int i = 0; i < CANDIDATES; i++) {
            double a = r.nextDouble() * Math.PI * 2;
            int d = RING_MIN + i * RING_STEP;
            candidates.add(new int[]{(int) Math.round(Math.cos(a) * d), (int) Math.round(Math.sin(a) * d)});
        }
        GeneratedChunks generated = new GeneratedChunks(level);
        int[] fallback = null;
        int reach = reach() + 16;
        for (int[] c : candidates) {
            boolean untouched = !generated.any(c[0] - reach, c[1] - reach, c[0] + reach, c[1] + reach);
            if (untouched && judge(level, c)) return c;
            if (fallback == null && untouched) fallback = c;
        }
        return fallback != null ? fallback : candidates.getFirst();
    }

    static int reach() {
        return Math.max(Math.max(-WarehouseAnchors.MIN_X, WarehouseAnchors.MAX_X), Math.max(-WarehouseAnchors.MIN_Z, WarehouseAnchors.MAX_Z));
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
        Job j = new Job(origin, q, WarehouseSite.plan(q));
        int[] a = WarehousePlan.rotate(WarehouseAnchors.MIN_X, WarehouseAnchors.MIN_Z, q), b = WarehousePlan.rotate(WarehouseAnchors.MAX_X, WarehouseAnchors.MAX_Z, q);
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

    /** Builds the warehouse at {@code origin} in one go, forgetting any other (commands, tests, previews). */
    public static WarehouseSite placeAt(ServerLevel level, BlockPos origin, int q) {
        job = null;
        WarehouseSavedData.get(level.getServer()).set(origin, q, false);
        start(level, origin, q);
        while (job != null) step(level, Integer.MAX_VALUE);
        return new WarehouseSite(origin, q);
    }

    public static void tick(ServerLevel overworld) {
        if (job != null) step(overworld, BATCH);
    }

    /** Paints the devil's trap (3×3) round {@code centre}: the angels' trap that holds him. */
    public static void paintTrap(ServerLevel level, BlockPos centre) {
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) {
                int part = (dz + 1) * 3 + (dx + 1);
                BlockPos p = centre.offset(dx, 0, dz);
                level.setBlock(p, AllBlocks.DEVILS_TRAP.get().defaultBlockState().setValue(DevilsTrapBlock.PART, part), Block.UPDATE_CLIENTS);
            }
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
            int width = WarehouseAnchors.MAX_X - WarehouseAnchors.MIN_X;
            do {
                int lx = WarehouseAnchors.MIN_X + j.index;
                for (int lz = WarehouseAnchors.MIN_Z; lz <= WarehouseAnchors.MAX_Z; lz++) {
                    int[] w = WarehousePlan.rotate(lx, lz, q);
                    for (int y = WarehouseAnchors.GROUND_Y + 1; y <= WarehouseAnchors.TOP_Y + 8; y++) {
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
            for (int lx = WarehouseAnchors.MIN_X; lx <= WarehouseAnchors.MAX_X; lx++)
                for (int lz = WarehouseAnchors.MIN_Z; lz <= WarehouseAnchors.MAX_Z; lz++) {
                    int[] w = WarehousePlan.rotate(lx, lz, q);
                    p.set(j.origin.getX() + w[0], j.origin.getY() + WarehouseAnchors.BOTTOM_Y, j.origin.getZ() + w[1]);
                    for (int y = p.getY(); y >= level.getMinBuildHeight(); y--) {
                        p.setY(y);
                        BlockState old = level.getBlockState(p);
                        if (old.isSolidRender(level, p) && old.getFluidState().isEmpty()) break;
                        level.setBlock(p, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                    for (int y = WarehouseAnchors.BOTTOM_Y + 1; y < WarehouseAnchors.GROUND_Y; y++) {
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
        paintTrap(level, new WarehouseSite(j.origin, q).trap());
        for (long[] c : j.chunks) level.setChunkForced((int) c[0], (int) c[1], false);
        WarehouseSavedData.get(level.getServer()).set(j.origin, q, true);
        job = null;
    }
}
