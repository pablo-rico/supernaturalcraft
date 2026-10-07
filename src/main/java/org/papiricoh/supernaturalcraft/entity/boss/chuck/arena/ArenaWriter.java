package org.papiricoh.supernaturalcraft.entity.boss.chuck.arena;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.Chapter;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Author's pen on one arena: a job queue that unwrites whatever stands there (top-down, from the rim inward,
 * the letters rising) and then writes the chapter's {@link ArenaPlan} (bottom-up, from the centre outward), a few
 * hundred blocks a tick, every block through {@link ArenaController#mutate} so the arena gives it all back. It also
 * holds the arena's timed hazards (the ceiling of pages, the burning floor) and the Blank Page's erosion.
 *
 * <p>Not persistent: after a reload {@link ChuckArenas#tick} finds no job for an arena that has been written on and
 * simply begins the current chapter again (blocks already right are skipped).
 */
final class ArenaWriter {

    /** Blocks changed per tick while writing or unwriting. */
    static final int MUTATIONS_PER_TICK = 300;
    /** Block reads per tick (scanning what stands, skipping what is already right). */
    static final int READS_PER_TICK = 16_000;
    /** How high above the floor the unwriting looks for things standing (trees, the cabin's roof). */
    static final int SCAN_TOP = 40;
    /** One particle per this many blocks changed. */
    private static final int PARTICLE_EVERY = 24;

    enum Stage { IDLE, SCAN, UNWRITE, WRITE, DONE }

    private static final Map<UUID, ArenaWriter> ALL = new HashMap<>();

    /** A hazard block that goes back to what it replaced at {@code at} (game time). */
    private record Timed(BlockPos pos, BlockState hazard, BlockState restore, long at) {
    }

    private Stage stage = Stage.IDLE;
    private @Nullable Chapter chapter;
    private @Nullable ArenaPlan plan;
    private int[] columns = new int[0];
    private int cursor;
    private final List<BlockPos> unwrite = new ArrayList<>();
    private final List<Timed> timed = new ArrayList<>();
    private @Nullable List<BlankPageErosion.Column> erosion;
    private int eroded;
    private double nearestEroded = Double.MAX_VALUE;
    /** Ticks spent in the current job, for the logs and the tests. */
    private int ticks, changed;

    static @Nullable ArenaWriter get(ArenaController arena) {
        return ALL.get(arena.id());
    }

    static ArenaWriter of(ArenaController arena) {
        return ALL.computeIfAbsent(arena.id(), id -> new ArenaWriter());
    }

    static void forget(ArenaController arena) {
        ALL.remove(arena.id());
    }

    Stage stage() {
        return stage;
    }

    @Nullable Chapter chapter() {
        return chapter;
    }

    @Nullable ArenaPlan plan() {
        return plan;
    }

    boolean writing() {
        return stage == Stage.SCAN || stage == Stage.UNWRITE || stage == Stage.WRITE;
    }

    int ticks() {
        return ticks;
    }

    int changed() {
        return changed;
    }

    // --- a chapter -------------------------------------------------------------------------------------------

    void begin(ServerLevel level, ArenaController arena, Chapter chapter) {
        this.chapter = chapter;
        this.plan = ChuckArenas.plan(arena, chapter);
        stage = Stage.SCAN;
        cursor = 0;
        ticks = 0;
        changed = 0;
        unwrite.clear();
        // The new chapter overwrites whatever hazards were pending.
        timed.clear();
        erosion = null;
        eroded = 0;
        nearestEroded = Double.MAX_VALUE;
        arena.setFloorRadius(-1);
        columns = discColumns(arena.radius());
        level.playSound(null, arena.center(), AllSounds.CHUCK_REWRITE.get(), SoundSource.HOSTILE, 4f, 1f);
    }

    void tick(ServerLevel level, ArenaController arena) {
        tickTimed(level, arena);
        if (!writing() || plan == null || !arena.isActive()) return;
        ticks++;
        switch (stage) {
            case SCAN -> scan(level, arena);
            case UNWRITE -> unwrite(level, arena);
            case WRITE -> write(level, arena);
            default -> {
            }
        }
    }

    /** Finds everything standing above the floor that the new chapter does not keep. */
    private void scan(ServerLevel level, ArenaController arena) {
        BlockPos c = arena.center();
        int floorY = ChuckArenas.floorY(arena);
        int reads = 0;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        while (cursor < columns.length && reads < READS_PER_TICK) {
            int dx = columns[cursor] >> 16, dz = (short) columns[cursor];
            cursor++;
            int x = c.getX() + dx, z = c.getZ() + dz;
            int top = Math.min(level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1, floorY + SCAN_TOP);
            for (int y = top; y > floorY; y--) {
                reads++;
                if (level.getBlockState(m.set(x, y, z)).isAir()) continue;
                if (plan.at(dx, y - floorY, dz) == null) unwrite.add(m.immutable());
            }
            reads++;
        }
        if (cursor < columns.length) return;
        // Top-down, then from the rim inward.
        Comparator<BlockPos> topDown = Comparator.comparingInt(BlockPos::getY);
        Comparator<BlockPos> outsideIn = Comparator.comparingDouble(p -> horizontal(p, c));
        unwrite.sort(topDown.reversed().thenComparing(outsideIn.reversed()));
        cursor = 0;
        if (unwrite.isEmpty()) {
            startWriting(level, arena);
        } else {
            stage = Stage.UNWRITE;
            ChuckArenas.wave(level, arena, -1, (unwrite.size() + MUTATIONS_PER_TICK - 1) / MUTATIONS_PER_TICK);
            level.playSound(null, c, AllSounds.CHUCK_ERASE.get(), SoundSource.HOSTILE, 4f, 1f);
        }
    }

    private void unwrite(ServerLevel level, ArenaController arena) {
        BlockState air = Blocks.AIR.defaultBlockState();
        int n = 0;
        while (cursor < unwrite.size() && n < MUTATIONS_PER_TICK) {
            BlockPos p = unwrite.get(cursor++);
            if (level.getBlockState(p).isAir()) continue;
            n++;
            if (arena.mutate(level, p, air, 0)) {
                changed++;
                if (changed % PARTICLE_EVERY == 0) {
                    level.sendParticles(AllParticles.INK_LETTER.get(), p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5,
                            1, 0.3, 0.3, 0.3, 0.02);
                }
            }
        }
        if (cursor >= unwrite.size()) {
            unwrite.clear();
            startWriting(level, arena);
        }
    }

    private void startWriting(ServerLevel level, ArenaController arena) {
        stage = Stage.WRITE;
        cursor = 0;
        ChuckArenas.wave(level, arena, chapter.ordinal(), (plan.cells().size() + MUTATIONS_PER_TICK - 1) / MUTATIONS_PER_TICK);
        level.playSound(null, arena.center(), AllSounds.CHUCK_WRITE.get(), SoundSource.HOSTILE, 4f, 1f);
    }

    private void write(ServerLevel level, ArenaController arena) {
        BlockPos c = arena.center();
        int floorY = ChuckArenas.floorY(arena);
        List<ArenaPlan.Cell> cells = plan.cells();
        int n = 0, reads = 0;
        while (cursor < cells.size() && n < MUTATIONS_PER_TICK && reads < READS_PER_TICK) {
            ArenaPlan.Cell cell = cells.get(cursor++);
            BlockPos p = new BlockPos(c.getX() + cell.dx(), floorY + cell.dy(), c.getZ() + cell.dz());
            BlockState target = ArenaPalette.state(plan, cell);
            reads++;
            if (level.getBlockState(p) == target) continue;
            n++;
            if (arena.mutate(level, p, target, 0)) {
                changed++;
                if (changed % PARTICLE_EVERY == 0) {
                    level.sendParticles(AllParticles.INK_LETTER.get(), p.getX() + 0.5, p.getY() + 1.2, p.getZ() + 0.5,
                            1, 0.3, 0.2, 0.3, 0.02);
                }
            }
        }
        if (cursor >= cells.size()) stage = Stage.DONE;
    }

    // --- the Blank Page eats itself ----------------------------------------------------------------------------

    void erode(ServerLevel level, ArenaController arena, double progress) {
        if (stage != Stage.DONE || chapter != Chapter.BLANK || !arena.isActive()) return;
        if (erosion == null) erosion = BlankPageErosion.order(arena.radius(), ChuckArenas.seed(arena));
        int due = BlankPageErosion.due(erosion, progress);
        if (eroded >= due) return;
        BlockPos c = arena.center();
        int floorY = ChuckArenas.floorY(arena);
        BlockState air = Blocks.AIR.defaultBlockState();
        int n = 0;
        while (eroded < due && n < MUTATIONS_PER_TICK) {
            BlankPageErosion.Column col = erosion.get(eroded++);
            nearestEroded = Math.min(nearestEroded, Math.sqrt(col.dx() * col.dx() + col.dz() * col.dz()));
            int x = c.getX() + col.dx(), z = c.getZ() + col.dz();
            for (int dy = 0; dy > -BlankPageErosion.DEPTH; dy--) {
                BlockPos p = new BlockPos(x, floorY + dy, z);
                BlockState s = level.getBlockState(p);
                if (s.isAir()) continue;
                n++;
                arena.mutate(level, p, air, 0);
                if (dy == 0 && eroded % 6 == 0) {
                    level.sendParticles(AllParticles.PAGE_SCRAP.get(), x + 0.5, floorY + 1.2, z + 0.5, 2, 0.4, 0.3, 0.4, 0.03);
                }
            }
        }
        arena.setFloorRadius(Math.max(BlankPageErosion.SAFE_RADIUS,
                Math.min(arena.radius(), (int) Math.floor(nearestEroded) - 1)));
    }

    int eroded() {
        return eroded;
    }

    // --- timed hazards -------------------------------------------------------------------------------------------

    /** Remembers a hazard block to put back after {@code ticks}. */
    void timed(BlockPos pos, BlockState hazard, BlockState restore, long at) {
        timed.add(new Timed(pos.immutable(), hazard, restore, at));
    }

    private void tickTimed(ServerLevel level, ArenaController arena) {
        if (timed.isEmpty()) return;
        long now = level.getGameTime();
        Iterator<Timed> it = timed.iterator();
        while (it.hasNext()) {
            Timed t = it.next();
            if (t.at() > now) continue;
            it.remove();
            if (level.getBlockState(t.pos()) == t.hazard()) arena.mutate(level, t.pos(), t.restore(), 0);
        }
    }

    // --- geometry ----------------------------------------------------------------------------------------------

    /** Every column of a disc, packed as {@code dx << 16 | dz & 0xFFFF}. */
    static int[] discColumns(int r) {
        List<Integer> out = new ArrayList<>();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz <= r * r) out.add((dx << 16) | (dz & 0xFFFF));
            }
        }
        return out.stream().mapToInt(Integer::intValue).toArray();
    }

    private static double horizontal(BlockPos p, BlockPos c) {
        double dx = p.getX() - c.getX(), dz = p.getZ() - c.getZ();
        return dx * dx + dz * dz;
    }
}
