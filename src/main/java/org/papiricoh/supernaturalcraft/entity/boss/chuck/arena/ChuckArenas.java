package org.papiricoh.supernaturalcraft.entity.boss.chuck.arena;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.Chapter;
import org.papiricoh.supernaturalcraft.network.AuthorFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything the Author's fight asks of its arena (the facade between the combat and the arena work). The arena is
 * rewritten each chapter: the old one unwrites into letters and the new one rises block by block, always through
 * {@link ArenaController#mutate}, so the cabin and the forest come back when the arena closes.
 *
 * <p>The chapters' arenas are pure plans ({@link ArenaLayouts}: Eden, Hell and the Cage, the Chorus's storm, the
 * Scribe's library, the Blank Page) written by an {@link ArenaWriter}, 300 blocks a tick. The floor of every chapter
 * is the layer just below the arena's centre ({@link #floorY}); the centre is the cabin's door, at feet level.
 */
public final class ChuckArenas {

    /** Height of the ceiling of pages above the floor (the gravity rule drops hunters onto its underside). */
    public static final int CEILING_DY = 12;

    private ChuckArenas() {
    }

    /** Starts writing {@code chapter}'s arena over whatever stands (chapter 1 erases the cabin). */
    public static void begin(ServerLevel level, ArenaController arena, Chapter chapter) {
        ArenaWriter.of(arena).begin(level, arena, chapter);
    }

    /** One server tick of the arena: writing, the blank page eating itself, timed hazards. {@code chapterTicks} counts up. */
    public static void tick(ServerLevel level, ArenaController arena, Chapter chapter, int chapterTicks) {
        ArenaWriter w = ArenaWriter.get(arena);
        if (w == null) {
            // Nothing held for an arena already written on: the server was reloaded. Write the chapter again.
            if (!arena.isActive() || arena.placedBlocks().isEmpty()) return;
            w = ArenaWriter.of(arena);
            w.begin(level, arena, chapter);
        }
        w.tick(level, arena);
        if (chapter == Chapter.BLANK) w.erode(level, arena, BlankPageErosion.progress(chapterTicks));
    }

    /** Whether the arena is still being written (the fight waits, the Author narrates). */
    public static boolean writing(ArenaController arena) {
        ArenaWriter w = ArenaWriter.get(arena);
        return w != null && w.writing();
    }

    /** Where the Author stands to fight in {@code chapter} (feet). */
    public static Vec3 spawnPoint(ArenaController arena, Chapter chapter) {
        ArenaPlan.Cell s = plan(arena, chapter).spawn();
        BlockPos c = arena.center();
        return new Vec3(c.getX() + s.dx() + 0.5, floorY(arena) + s.dy(), c.getZ() + s.dz() + 0.5);
    }

    /** A ceiling of pages over the arena for {@code ticks}, for the gravity to drop hunters onto. */
    public static void tempCeiling(ServerLevel level, ArenaController arena, int ticks) {
        ArenaWriter w = ArenaWriter.of(arena);
        BlockState page = AllBlocks.PAGE_BLOCK.get().defaultBlockState(), air = Blocks.AIR.defaultBlockState();
        BlockPos c = arena.center();
        int y = ceilingY(arena);
        long until = level.getGameTime() + ticks;
        for (int col : ArenaWriter.discColumns(arena.radius())) {
            BlockPos p = new BlockPos(c.getX() + (col >> 16), y, c.getZ() + (short) col);
            if (level.getBlockState(p).isAir() && arena.mutate(level, p, page, 0)) w.timed(p, page, air, until);
        }
    }

    /** "The floor is lava" around {@code center} for {@code ticks}, sparing islands of safe ground. */
    public static void lavaZone(ServerLevel level, ArenaController arena, Vec3 center, int radius, int ticks) {
        ArenaWriter w = ArenaWriter.of(arena);
        BlockState ink = AllBlocks.BURNING_INK.get().defaultBlockState();
        List<LavaIslands.Island> islands = LavaIslands.islands(radius, level.getGameTime() ^ seed(arena));
        int y = floorY(arena);
        int cx = (int) Math.floor(center.x), cz = (int) Math.floor(center.z);
        long until = level.getGameTime() + ticks;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius || LavaIslands.spared(islands, dx, dz)) continue;
                BlockPos p = new BlockPos(cx + dx, y, cz + dz);
                BlockState s = level.getBlockState(p);
                if (s.isAir() || s == ink || !s.getFluidState().isEmpty() || !s.isSolidRender(level, p)) continue;
                if (arena.mutate(level, p, ink, 0)) w.timed(p, ink, s, until);
            }
        }
    }

    /** Light sources in the current chapter (for "light hurts"): the planned lights still standing. */
    public static List<BlockPos> lightSources(ServerLevel level, ArenaController arena) {
        ArenaWriter w = ArenaWriter.get(arena);
        List<BlockPos> out = new ArrayList<>();
        if (w == null || w.plan() == null) return out;
        ArenaPlan plan = w.plan();
        BlockPos c = arena.center();
        int y = floorY(arena);
        for (ArenaPlan.Cell cell : plan.lights()) {
            BlockPos p = new BlockPos(c.getX() + cell.dx(), y + cell.dy(), c.getZ() + cell.dz());
            if (level.getBlockState(p).getLightEmission(level, p) > 0) out.add(p);
        }
        return out;
    }

    /** Whether {@code pos} is water of the arena (for "water burns"). */
    public static boolean isWater(ServerLevel level, ArenaController arena, BlockPos pos) {
        return arena.contains(pos) && level.getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER);
    }

    /** Forgets everything held for this arena (it closed). */
    public static void forget(ArenaController arena) {
        ArenaWriter.forget(arena);
    }

    // --- additions beyond the wave-0 contract --------------------------------------------------------------------

    /** The plan of a chapter's arena for this arena (its radius and its seed). */
    public static ArenaPlan plan(ArenaController arena, Chapter chapter) {
        return ArenaLayouts.plan(chapter, arena.radius(), seed(arena));
    }

    /** The chapter whose arena is being (or was last) written here, if any. */
    public static @Nullable Chapter written(ArenaController arena) {
        ArenaWriter w = ArenaWriter.get(arena);
        return w == null ? null : w.chapter();
    }

    /** The y of every chapter's floor: the layer just under the arena's centre (the cabin door, at feet level). */
    public static int floorY(ArenaController arena) {
        return arena.center().getY() - 1;
    }

    /** The y of the ceiling of pages ({@link #tempCeiling}). */
    public static int ceilingY(ArenaController arena) {
        return floorY(arena) + CEILING_DY;
    }

    /** The arena's seed: each cabin's arenas are their own, and the same every fight. */
    public static long seed(ArenaController arena) {
        return arena.center().asLong() * 0x2545F4914F6CDD1DL;
    }

    /** How many columns of the Blank Page have been erased so far (0 in other chapters). */
    public static int erodedColumns(ArenaController arena) {
        ArenaWriter w = ArenaWriter.get(arena);
        return w == null ? 0 : w.eroded();
    }

    /**
     * The cabin comes back: one {@code ARENA_WAVE} with {@code arg} -1, timed to the arena's restoration (call it
     * when the arena begins to restore, after a victory or a defeat).
     */
    public static void cabinReturns(ServerLevel level, ArenaController arena) {
        int ticks = Math.max(20, (arena.snapshotSize() + ArenaController.RESTORE_BATCH - 1) / ArenaController.RESTORE_BATCH);
        wave(level, arena, -1, ticks);
    }

    /** Sends one wave of letters to everyone near the arena: from its centre out to its rim over {@code duration}. */
    static void wave(ServerLevel level, ArenaController arena, int arg, int duration) {
        Entity boss = arena.bossId() == null ? null : level.getEntity(arena.bossId());
        AuthorFxPayload payload = new AuthorFxPayload(boss == null ? -1 : boss.getId(), AuthorFxPayload.ARENA_WAVE, arg,
                arena.centerVec(), arena.radius(), Math.max(1, duration), "");
        for (ServerPlayer p : level.players()) {
            if (arena.horizontalDistance(p.position()) <= arena.radius() + 48) PacketDistributor.sendToPlayer(p, payload);
        }
    }
}
