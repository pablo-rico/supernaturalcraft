package org.papiricoh.supernaturalcraft.apocworld.fortress;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.apocworld.ApocWorldRegistry;
import org.papiricoh.supernaturalcraft.apocworld.ApocalypseWorld;
import org.papiricoh.supernaturalcraft.chain.seat.SeatPlacement;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.ArenaCell;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Michael's fortress in the world (v0.41): one per world, in the Apocalypse World (or its stand-in), hidden from {@code /locate}
 * (it is no structure). Raised the first time a hunter reaches the burned world ({@link #ensure}), on the highest fairly level
 * ground among a ring of candidates some way out from the landing (a dark citadel on a ridge, seen from far off). Written in
 * batches like Alastair's warehouse: its chunks forced, the land over the plinth cleared, footings under it, the plan. A restart
 * drops the work; the next {@link #ensure} begins it again at the same origin.
 */
public final class FortressWorld {

    public static final int BATCH = 8000, LOAD_TIMEOUT = 1200;
    /** The candidate ring round the burned world's heart (blocks), and how many candidates. */
    public static final int RING_MIN = 420, RING_STEP = 40, CANDIDATES = 12;
    public static final int MAX_RELIEF = 18, SAMPLES = 5;
    /** Where it stands in the stand-in, from the stand-in's heart. */
    public static final int STAND_IN_OFFSET = 320;

    private static @Nullable Job job;

    private FortressWorld() {
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

    /** The fortress's origin, worked out (and remembered) the first time; it is built if it is not yet. */
    public static BlockPos ensure(ServerLevel au) {
        FortressSavedData data = FortressSavedData.get(au.getServer());
        if (data.origin() == null) {
            int[] site = site(au);
            int q = (int) Math.floorMod(au.getSeed() >>> 17, 4L);
            var source = au.getChunkSource();
            int y = source.getGenerator().getBaseHeight(site[0], site[1], Heightmap.Types.WORLD_SURFACE_WG, au, source.randomState());
            y = Math.clamp(y + 1 - FortressAnchors.GROUND_Y - 1, au.getMinBuildHeight() - FortressAnchors.BOTTOM_Y + 1,
                    au.getMaxBuildHeight() - FortressAnchors.TOP_Y - 1);
            data.set(new BlockPos(site[0], y, site[1]), q, false);
        }
        // The GameTest server only works the site out: its tests build the fortress where they want it (placeAt).
        if (!data.built() && job == null && !net.neoforged.neoforge.gametest.GameTestHooks.isGametestServer()) start(au, data.origin(), data.q());
        return data.origin();
    }

    public static @Nullable FortressSite site(MinecraftServer server) {
        FortressSavedData data = FortressSavedData.get(server);
        return data.origin() == null ? null : new FortressSite(data.origin(), data.q());
    }

    public static boolean built(MinecraftServer server) {
        return FortressSavedData.get(server).built();
    }

    public static boolean building() {
        return job != null;
    }

    static int reach() {
        return Math.max(Math.max(-FortressAnchors.MIN_X, FortressAnchors.MAX_X), Math.max(-FortressAnchors.MIN_Z, FortressAnchors.MAX_Z));
    }

    /** The site: in the stand-in a fixed spot; in the real world the highest fairly level candidate on the ring. */
    static int[] site(ServerLevel level) {
        BlockPos o = ApocalypseWorld.origin(level);
        if (!ApocalypseWorld.real(level)) return new int[]{o.getX() + STAND_IN_OFFSET, o.getZ()};
        Random r = new Random(level.getSeed() ^ 0xA0_F047L);
        int[] best = null, fallback = null;
        double bestHeight = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < CANDIDATES; i++) {
            double a = r.nextDouble() * Math.PI * 2;
            int d = RING_MIN + i * RING_STEP;
            int[] c = {o.getX() + (int) Math.round(Math.cos(a) * d), o.getZ() + (int) Math.round(Math.sin(a) * d)};
            if (fallback == null) fallback = c;
            double h = judge(level, c);
            if (h > bestHeight) {
                bestHeight = h;
                best = c;
            }
        }
        return best != null ? best : fallback;
    }

    /** The candidate's mean ground height, or -infinity if it is too broken to build on. */
    static double judge(ServerLevel level, int[] c) {
        var source = level.getChunkSource();
        var generator = source.getGenerator();
        int low = Integer.MAX_VALUE, high = Integer.MIN_VALUE;
        double sum = 0;
        int reach = reach();
        for (int i = 0; i < SAMPLES; i++) for (int j = 0; j < SAMPLES; j++) {
            int x = c[0] - reach + i * 2 * reach / (SAMPLES - 1);
            int z = c[1] - reach + j * 2 * reach / (SAMPLES - 1);
            int surface = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, source.randomState());
            low = Math.min(low, surface);
            high = Math.max(high, surface);
            sum += surface;
        }
        return high - low > MAX_RELIEF ? Double.NEGATIVE_INFINITY : sum / (SAMPLES * SAMPLES);
    }

    static void start(ServerLevel level, BlockPos origin, int q) {
        Job j = new Job(origin, q, FortressSite.plan(q));
        int[] a = FortressPlan.rotate(FortressAnchors.MIN_X, FortressAnchors.MIN_Z, q), b = FortressPlan.rotate(FortressAnchors.MAX_X, FortressAnchors.MAX_Z, q);
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

    /** Builds the fortress at {@code origin} in one go, forgetting any other (commands, tests, previews). */
    public static FortressSite placeAt(ServerLevel level, BlockPos origin, int q) {
        job = null;
        FortressSavedData.get(level.getServer()).set(origin, q, false);
        start(level, origin, q);
        while (job != null) step(level, Integer.MAX_VALUE);
        return new FortressSite(origin, q);
    }

    /** Forgets the fortress (commands, tests): the next {@link #ensure} picks its site again. */
    public static void forget(MinecraftServer server) {
        job = null;
        FortressSavedData.get(server).set(null, 0, false);
    }

    public static void tick(MinecraftServer server) {
        if (job != null) step(ApocalypseWorld.level(server), BATCH);
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
            int width = FortressAnchors.MAX_X - FortressAnchors.MIN_X;
            do {
                int lx = FortressAnchors.MIN_X + j.index;
                for (int lz = FortressAnchors.MIN_Z; lz <= FortressAnchors.MAX_Z; lz++) {
                    int[] w = FortressPlan.rotate(lx, lz, q);
                    for (int y = FortressAnchors.GROUND_Y + 1; y <= FortressAnchors.TOP_Y + 8; y++) {
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
            BlockState footing = ApocWorldRegistry.SCORCHED_STONE.get().defaultBlockState();
            for (int lx = FortressAnchors.MIN_X; lx <= FortressAnchors.MAX_X; lx++)
                for (int lz = FortressAnchors.MIN_Z; lz <= FortressAnchors.MAX_Z; lz++) {
                    int[] w = FortressPlan.rotate(lx, lz, q);
                    p.set(j.origin.getX() + w[0], j.origin.getY() + FortressAnchors.BOTTOM_Y, j.origin.getZ() + w[1]);
                    for (int y = p.getY(); y >= level.getMinBuildHeight(); y--) {
                        p.setY(y);
                        BlockState old = level.getBlockState(p);
                        if (old.isSolidRender(level, p) && old.getFluidState().isEmpty()) break;
                        level.setBlock(p, footing, Block.UPDATE_CLIENTS);
                    }
                    for (int y = FortressAnchors.BOTTOM_Y + 1; y < FortressAnchors.GROUND_Y; y++) {
                        p.set(j.origin.getX() + w[0], j.origin.getY() + y, j.origin.getZ() + w[1]);
                        BlockState old = level.getBlockState(p);
                        if (old.isAir() || !old.getFluidState().isEmpty()) level.setBlock(p, footing, Block.UPDATE_CLIENTS);
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
        FortressSavedData.get(level.getServer()).set(j.origin, q, true);
        job = null;
    }
}
