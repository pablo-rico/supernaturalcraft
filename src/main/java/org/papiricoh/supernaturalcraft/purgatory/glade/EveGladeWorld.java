package org.papiricoh.supernaturalcraft.purgatory.glade;

import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.chain.seat.SeatPlacement;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.ArenaCell;
import org.papiricoh.supernaturalcraft.layout.LayoutPoint;
import org.papiricoh.supernaturalcraft.purgatory.PurgatoryDimension;
import org.papiricoh.supernaturalcraft.purgatory.PurgatorySavedData;
import org.papiricoh.supernaturalcraft.purgatory.PurgatoryWorld;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Eve's glade in the world (v0.39): its site worked out once from the seed ({@link EveGladeSite}, the generator asked without
 * loading a chunk), and the plan ({@link EveGladePlan}) raised in batches the first time a hunter comes within
 * {@link #WAKE_REACH} of it, like Crowley's asylum: its chunks forced, the land over the clearing cleared, footings under the
 * ground, the plan. A restart drops the work; the next approach begins it again at the same origin.
 */
public final class EveGladeWorld {

    public static final int BATCH = 6000, LOAD_TIMEOUT = 1200;
    /** How near a hunter comes before the glade is raised. */
    public static final double WAKE_REACH = 160;
    public static final int MAX_RELIEF = 7, SAMPLES = 4;

    private static @Nullable Job job;

    private EveGladeWorld() {
    }

    private static final class Job {
        final ServerLevel level;
        final BlockPos origin;
        final List<ArenaCell> cells;
        final Map<String, BlockState> states = new HashMap<>();
        final List<long[]> chunks = new ArrayList<>();
        int phase = -1, index, waited;

        Job(ServerLevel level, BlockPos origin, long seed) {
            this.level = level;
            this.origin = origin.immutable();
            this.cells = EveGladePlan.plan(seed).cells();
            for (ArenaCell c : cells) states.computeIfAbsent(c.block(), SeatPlacement::parse);
        }
    }

    /** The glade's origin in {@code level} (Purgatory, or the tests' stand-in), worked out and remembered the first time. */
    public static BlockPos site(ServerLevel level) {
        PurgatorySavedData data = PurgatorySavedData.get(level.getServer());
        if (data.glade() != null) return data.glade();
        BlockPos o = PurgatoryWorld.origin(level);
        List<int[]> cands = EveGladeSite.candidates(level.getSeed());
        boolean real = PurgatoryWorld.real(level);
        int k = real ? EveGladeSite.choose(i -> wooded(level, o, cands.get(i)), i -> flat(level, o, cands.get(i))) : 0;
        int x = o.getX() + cands.get(k)[0], z = o.getZ() + cands.get(k)[1];
        var source = level.getChunkSource();
        int y = source.getGenerator().getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, source.randomState());
        y = Math.max(y, PurgatoryDimension.SEA_LEVEL + 1);
        y = Math.clamp(y, level.getMinBuildHeight() + EveGladePlan.DEPTH + 1, level.getMaxBuildHeight() - EveGladePlan.HEIGHT - 1);
        BlockPos at = new BlockPos(x, y, z);
        data.setGlade(at, 0, false);
        return at;
    }

    private static boolean wooded(ServerLevel level, BlockPos o, int[] c) {
        var source = level.getChunkSource();
        int x = o.getX() + c[0], z = o.getZ() + c[1];
        int y = source.getGenerator().getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, source.randomState());
        return source.getGenerator().getBiomeSource().getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z),
                source.randomState().sampler()).is(PurgatoryDimension.MOTHERS_WOOD);
    }

    private static boolean flat(ServerLevel level, BlockPos o, int[] c) {
        var source = level.getChunkSource();
        int[] h = new int[SAMPLES * SAMPLES];
        int n = 0, r = EveGladePlan.RADIUS;
        for (int i = 0; i < SAMPLES; i++) for (int j = 0; j < SAMPLES; j++) {
            int x = o.getX() + c[0] - r + i * 2 * r / (SAMPLES - 1), z = o.getZ() + c[1] - r + j * 2 * r / (SAMPLES - 1);
            int top = source.getGenerator().getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, source.randomState());
            if (top <= PurgatoryDimension.SEA_LEVEL) return false;
            h[n++] = top;
        }
        return EveGladeSite.level(h, MAX_RELIEF);
    }

    /** Where a glade point is in the world. */
    public static BlockPos point(ServerLevel level, String anchor) {
        LayoutPoint p = EveGladePlan.at(anchor);
        return site(level).offset(p.x(), p.y(), p.z());
    }

    public static Vec3 floor(ServerLevel level, String anchor) {
        return Vec3.atBottomCenterOf(point(level, anchor));
    }

    public static boolean built(ServerLevel level) {
        return PurgatorySavedData.get(level.getServer()).gladeBuilt();
    }

    public static boolean building() {
        return job != null;
    }

    // --- building -------------------------------------------------------------------------------------------------------------

    /** Once a second in Purgatory: the glade is raised when a hunter first comes near it. */
    public static void tick(ServerLevel level) {
        if (job != null) {
            if (job.level == level) step(BATCH);
            return;
        }
        if (built(level) || !PurgatoryWorld.real(level) || level.getGameTime() % 20 != 3) return;
        BlockPos at = site(level);
        Vec3 c = Vec3.atCenterOf(at);
        for (ServerPlayer p : level.players()) {
            if (!p.isSpectator() && p.position().distanceToSqr(c) < WAKE_REACH * WAKE_REACH) {
                start(level);
                return;
            }
        }
    }

    /** Begins raising the glade (unless it stands, or is being raised). */
    public static void start(ServerLevel level) {
        if (job != null || built(level)) return;
        BlockPos origin = site(level);
        Job j = new Job(level, origin, level.getSeed());
        int r = EveGladePlan.EXTENT;
        for (int cx = (origin.getX() - r) >> 4; cx <= (origin.getX() + r) >> 4; cx++)
            for (int cz = (origin.getZ() - r) >> 4; cz <= (origin.getZ() + r) >> 4; cz++) {
                level.setChunkForced(cx, cz, true);
                j.chunks.add(new long[]{cx, cz});
            }
        job = j;
    }

    /** Raises it now, in one go (commands, tests). */
    public static void buildNow(ServerLevel level) {
        start(level);
        while (job != null) step(Integer.MAX_VALUE);
    }

    private static void step(int budget) {
        Job j = job;
        if (j == null) return;
        ServerLevel level = j.level;
        if (j.phase < 0) {
            boolean loaded = j.chunks.stream().allMatch(c -> level.getChunkSource().hasChunk((int) c[0], (int) c[1]));
            if (loaded || ++j.waited > LOAD_TIMEOUT || budget == Integer.MAX_VALUE) j.phase = 0;
            return;
        }
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int clear = EveGladePlan.RADIUS + 4;
        if (j.phase == 0) {
            // The land over the clearing goes (hills, trees), a slice at a time.
            do {
                int lx = -clear + j.index;
                for (int lz = -clear; lz <= clear; lz++) {
                    if (lx * lx + lz * lz > clear * clear) continue;
                    for (int y = 0; y <= EveGladePlan.HEIGHT; y++) {
                        p.set(j.origin.getX() + lx, j.origin.getY() + y, j.origin.getZ() + lz);
                        if (!level.getBlockState(p).isAir()) {
                            level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                            budget--;
                        }
                    }
                }
                if (++j.index > 2 * clear) {
                    j.phase = 1;
                    j.index = 0;
                    break;
                }
            } while (budget > 0);
            return;
        }
        if (j.phase == 1) {
            // Footings: under the plan's deepest layer, the ground filled down to firm rock.
            int reach = EveGladePlan.REACH;
            for (int lx = -reach; lx <= reach; lx++)
                for (int lz = -reach; lz <= reach; lz++) {
                    if (lx * lx + lz * lz > reach * reach) continue;
                    for (int y = j.origin.getY() - EveGladePlan.DEPTH - 1; y >= level.getMinBuildHeight(); y--) {
                        p.set(j.origin.getX() + lx, y, j.origin.getZ() + lz);
                        BlockState old = level.getBlockState(p);
                        if (old.isSolidRender(level, p) && old.getFluidState().isEmpty()) break;
                        level.setBlock(p, Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
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
        PurgatorySavedData.get(level.getServer()).setGlade(j.origin, 0, true);
        job = null;
    }

    /** Drops the work (commands, tests). */
    public static void cancel() {
        if (job != null) for (long[] c : job.chunks) job.level.setChunkForced((int) c[0], (int) c[1], false);
        job = null;
    }
}
