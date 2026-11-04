package org.papiricoh.supernaturalcraft.asylum;

import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.asylum.layout.AsylumAnchors;
import org.papiricoh.supernaturalcraft.asylum.layout.AsylumPlan;
import org.papiricoh.supernaturalcraft.chain.seat.GeneratedChunks;
import org.papiricoh.supernaturalcraft.chain.seat.SeatPlacement;
import org.papiricoh.supernaturalcraft.entity.boss.crowley.CrowleySavedData;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.ArenaCell;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Crowley's asylum in the world (v0.35): one per world, hidden from {@code /locate} (it is no structure), raised the first time
 * a hunter is given its case ({@code AsylumCase}) — in country nobody has explored if there is any, judged from the region
 * files and the generator alone ({@link GeneratedChunks}), dry and level, in a forest if one comes up. Written in batches like
 * the chain's strongholds: its chunks forced, the land above its pad cleared, the plan, footings under it. A restart drops the
 * work; the next {@link #ensure} begins it again at the same origin. Its first visitor finds it garrisoned (patients in the
 * cells, suits in the hall), once.
 */
public final class AsylumWorld {

    public static final int BATCH = 6000, LOAD_TIMEOUT = 1200;
    /** The candidate ring round the world's centre (blocks), and how many candidates. */
    public static final int RING_MIN = 1600, RING_STEP = 220, CANDIDATES = 16;
    public static final int MAX_RELIEF = 8, SAMPLES = 5;
    /** How close a hunter comes before the garrison rises. */
    public static final double GARRISON_REACH = 48;

    private static @Nullable Job job;

    private AsylumWorld() {
    }

    private static final class Job {
        final BlockPos origin;
        final List<ArenaCell> cells;
        final Map<String, BlockState> states = new HashMap<>();
        final List<long[]> chunks = new ArrayList<>();
        int phase = -1, index, waited;

        Job(BlockPos origin, LayoutPlan plan) {
            this.origin = origin.immutable();
            this.cells = plan.cells();
            for (ArenaCell c : cells) states.computeIfAbsent(c.block(), SeatPlacement::parse);
        }
    }

    // --- where ----------------------------------------------------------------------------------------------------------

    /** The asylum's origin and turn, worked out (and remembered) the first time; it is built if it is not yet. */
    public static BlockPos ensure(ServerLevel overworld) {
        CrowleySavedData data = CrowleySavedData.get(overworld.getServer());
        if (data.asylum() == null) {
            int[] site = site(overworld);
            int q = (int) Math.floorMod(overworld.getSeed() >>> 7, 4L);
            var source = overworld.getChunkSource();
            int y = source.getGenerator().getBaseHeight(site[0], site[1], Heightmap.Types.WORLD_SURFACE_WG, overworld, source.randomState());
            y = Math.clamp(y - AsylumAnchors.GROUND_Y, overworld.getMinBuildHeight() - AsylumAnchors.BOTTOM_Y + 1,
                    overworld.getMaxBuildHeight() - AsylumAnchors.TOP_Y - 1);
            data.setAsylum(new BlockPos(site[0], y, site[1]), q, false);
        }
        if (!data.asylumBuilt() && job == null) start(overworld, data.asylum(), data.asylumQ());
        return data.asylum();
    }

    public static @Nullable AsylumFight fight(MinecraftServer server) {
        CrowleySavedData data = CrowleySavedData.get(server);
        return data.asylum() == null ? null : new AsylumFight(data.asylum(), data.asylumQ());
    }

    public static boolean building() {
        return job != null;
    }

    /** The first candidate untouched, dry and level (a forest preferred); else the first dry one; else the first. */
    static int[] site(ServerLevel level) {
        Random r = new Random(level.getSeed() ^ 0xC0A51E7L);
        List<int[]> candidates = new ArrayList<>();
        for (int i = 0; i < CANDIDATES; i++) {
            double a = r.nextDouble() * Math.PI * 2;
            int d = RING_MIN + i * RING_STEP;
            candidates.add(new int[]{(int) Math.round(Math.cos(a) * d), (int) Math.round(Math.sin(a) * d)});
        }
        GeneratedChunks generated = new GeneratedChunks(level);
        int[] dry = null;
        for (int[] c : candidates) {
            boolean untouched = !generated.any(c[0] + AsylumAnchors.MIN_X - 16, c[1] + AsylumAnchors.MIN_Z - 16,
                    c[0] + AsylumAnchors.MAX_X + 16, c[1] + AsylumAnchors.MAX_Z + 16);
            int verdict = judge(level, c);
            if (untouched && verdict == 2) return c;
            if (verdict >= 1 && dry == null && untouched) dry = c;
        }
        return dry != null ? dry : candidates.getFirst();
    }

    /** 0 wet or rough, 1 dry and level, 2 that and wooded. */
    static int judge(ServerLevel level, int[] c) {
        var source = level.getChunkSource();
        var generator = source.getGenerator();
        int sea = generator.getSeaLevel(), low = Integer.MAX_VALUE, high = Integer.MIN_VALUE;
        boolean wooded = false;
        for (int i = 0; i < SAMPLES; i++) for (int j = 0; j < SAMPLES; j++) {
            int x = c[0] + AsylumAnchors.MIN_X + i * (AsylumAnchors.MAX_X - AsylumAnchors.MIN_X) / (SAMPLES - 1);
            int z = c[1] + AsylumAnchors.MIN_Z + j * (AsylumAnchors.MAX_Z - AsylumAnchors.MIN_Z) / (SAMPLES - 1);
            int surface = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, source.randomState());
            int floor = generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, source.randomState());
            var biome = generator.getBiomeSource().getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(surface), QuartPos.fromBlock(z),
                    source.randomState().sampler());
            if (biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_RIVER) || biome.is(BiomeTags.IS_BEACH) || surface > floor || surface <= sea) return 0;
            if (biome.is(Biomes.DARK_FOREST) || biome.is(BiomeTags.IS_FOREST) || biome.is(BiomeTags.IS_TAIGA)) wooded = true;
            low = Math.min(low, floor);
            high = Math.max(high, floor);
            if (high - low > MAX_RELIEF) return 0;
        }
        return wooded ? 2 : 1;
    }

    // --- building -------------------------------------------------------------------------------------------------------

    static void start(ServerLevel level, BlockPos origin, int q) {
        Job j = new Job(origin, AsylumPlan.plan(q));
        int[] a = AsylumPlan.rotate(AsylumAnchors.MIN_X, AsylumAnchors.MIN_Z, q), b = AsylumPlan.rotate(AsylumAnchors.MAX_X, AsylumAnchors.MAX_Z, q);
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

    public static void tick(ServerLevel overworld) {
        if (job != null) step(overworld, BATCH);
    }

    private static void step(ServerLevel level, int budget) {
        Job j = job;
        if (j == null) return;
        CrowleySavedData data = CrowleySavedData.get(level.getServer());
        int q = data.asylumQ();
        if (j.phase < 0) {
            boolean loaded = j.chunks.stream().allMatch(c -> level.getChunkSource().hasChunk((int) c[0], (int) c[1]));
            if (loaded || ++j.waited > LOAD_TIMEOUT || budget == Integer.MAX_VALUE) j.phase = 0;
            return;
        }
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        if (j.phase == 0) {
            // The land above the pad goes (hills, trees), one slice of the footprint at a time.
            int width = AsylumAnchors.MAX_X - AsylumAnchors.MIN_X;
            do {
                int lx = AsylumAnchors.MIN_X + j.index;
                for (int lz = AsylumAnchors.MIN_Z; lz <= AsylumAnchors.MAX_Z; lz++) {
                    int[] w = AsylumPlan.rotate(lx, lz, q);
                    for (int y = AsylumAnchors.GROUND_Y + 1; y <= AsylumAnchors.TOP_Y + 8; y++) {
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
            // Footings: the pad is firm ground (grass on top), and the ground under it filled to rock.
            for (int lx = AsylumAnchors.MIN_X; lx <= AsylumAnchors.MAX_X; lx++)
                for (int lz = AsylumAnchors.MIN_Z; lz <= AsylumAnchors.MAX_Z; lz++) {
                    int[] w = AsylumPlan.rotate(lx, lz, q);
                    p.set(j.origin.getX() + w[0], j.origin.getY() + AsylumAnchors.GROUND_Y, j.origin.getZ() + w[1]);
                    level.setBlock(p, Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
                    for (int y = p.getY() - 1; y >= level.getMinBuildHeight(); y--) {
                        p.setY(y);
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
        data.setAsylum(j.origin, q, true);
        job = null;
    }

    // --- the garrison ---------------------------------------------------------------------------------------------------

    /** Once a second: the garrison rises for the first hunter near a built asylum. */
    public static void tickGarrison(ServerLevel level) {
        CrowleySavedData data = CrowleySavedData.get(level.getServer());
        if (!data.asylumBuilt() || data.garrisoned() || data.decided()) return;
        AsylumFight f = new AsylumFight(data.asylum(), data.asylumQ());
        Vec3 c = Vec3.atCenterOf(data.asylum());
        boolean near = false;
        for (ServerPlayer p : level.players()) if (!p.isSpectator() && p.position().distanceToSqr(c) < GARRISON_REACH * GARRISON_REACH) near = true;
        if (!near || !level.isLoaded(data.asylum())) return;
        data.setGarrisoned(true);
        for (Vec3 s : f.spawns(1)) rise(level, AllEntities.CROWLEY_SUIT.get().create(level), s);
        for (Vec3 s : f.spawns(2)) rise(level, AllEntities.POSSESSED_PATIENT.get().create(level), s);
    }

    private static void rise(ServerLevel level, @Nullable Mob m, Vec3 at) {
        if (m == null) return;
        m.moveTo(at.x, at.y, at.z, level.random.nextFloat() * 360, 0);
        m.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(at)), MobSpawnType.STRUCTURE, null);
        m.setPersistenceRequired();
        level.addFreshEntity(m);
    }
}
