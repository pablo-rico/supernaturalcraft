package org.papiricoh.supernaturalcraft.heaven.plot;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.ArenaCell;
import org.papiricoh.supernaturalcraft.layout.LayoutDecor;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The permanent, batched writer of Heaven's builds (v0.18): one or more {@link LayoutPlan}s, each at its own origin, written a
 * budget of blocks at a time (flags {@code UPDATE_CLIENTS | UPDATE_KNOWN_SHAPE}: the plans carry final shapes), then their
 * {@link LayoutDecor}. Nothing is restored afterwards: this is the world itself, not an arena.
 * <p>Order: the parts in order; inside a part its zones in order; inside a zone the cells grouped by chunk (a stable sort, so two
 * cells on one block keep their order). Resumable: a {@link Cursor} (stage, part, index) is all a caller saves, since the plans
 * are pure and rebuilt the same. Chunks are forced as the writer reaches them; the caller keeps the set and frees it when done.
 * Block states are parsed once each ({@link BlockStateParser}); an unparsable one is logged once and written as air.
 */
public final class PlotWriter {

    private static final Logger LOG = LogUtils.getLogger();
    /** Budget a decor item costs (entities and block entities are dearer than blocks). */
    public static final int DECOR_COST = 32;
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final Set<String> WARNED = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /** A plan written at an origin ({@code y = 0} of the plan is the origin's y). */
    public record Part(BlockPos origin, LayoutPlan plan) {
    }

    public enum Stage { BLOCKS, DECOR, DONE }

    /** Where a writer is: its stage, the part, and the cell (or decor) index inside that part. */
    public record Cursor(Stage stage, int part, int index) {
        public static final Cursor START = new Cursor(Stage.BLOCKS, 0, 0);

        public CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putString("Stage", stage.name());
            t.putInt("Part", part);
            t.putInt("Index", index);
            return t;
        }

        public static Cursor load(CompoundTag t) {
            if (!t.contains("Stage")) return START;
            Stage s;
            try {
                s = Stage.valueOf(t.getString("Stage"));
            } catch (IllegalArgumentException e) {
                s = Stage.BLOCKS;
            }
            return new Cursor(s, t.getInt("Part"), t.getInt("Index"));
        }
    }

    /** What a step did. */
    public enum Event { NONE, BLOCKS_DONE, DONE }

    private final List<Part> parts;
    private final List<List<ArenaCell>> cells = new ArrayList<>();
    /** Per part: zone name → its [start, end) in that part's ordered cells. */
    private final List<Map<String, int[]>> zones = new ArrayList<>();
    private final Map<String, BlockState> states = new HashMap<>();
    private final long totalCells, totalDecor;
    private Stage stage;
    private int part, index;

    public PlotWriter(List<Part> parts, Cursor at) {
        this.parts = List.copyOf(parts);
        long cellCount = 0, decorCount = 0;
        for (Part p : parts) {
            List<ArenaCell> ordered = new ArrayList<>(p.plan().size());
            Map<String, int[]> ranges = new LinkedHashMap<>();
            for (LayoutPlan.Zone z : p.plan().zones()) {
                int start = ordered.size();
                List<ArenaCell> zc = new ArrayList<>(z.cells());
                zc.sort(Comparator.comparingLong(c -> ChunkPos.asLong(Math.floorDiv(c.dx(), 16), Math.floorDiv(c.dz(), 16))));
                ordered.addAll(zc);
                ranges.putIfAbsent(z.name(), new int[]{start, ordered.size()});
            }
            cells.add(ordered);
            zones.add(ranges);
            cellCount += ordered.size();
            decorCount += p.plan().decor().size();
        }
        this.totalCells = cellCount;
        this.totalDecor = decorCount;
        this.stage = at.stage();
        this.part = Math.max(0, Math.min(at.part(), parts.size()));
        this.index = Math.max(0, at.index());
    }

    public Cursor cursor() {
        return new Cursor(stage, part, index);
    }

    public boolean done() {
        return stage == Stage.DONE;
    }

    public List<Part> parts() {
        return parts;
    }

    /** How far along it is, 0-100 (blocks are 90 of it, decor the rest). */
    public int percent() {
        if (stage == Stage.DONE) return 100;
        if (stage == Stage.BLOCKS) {
            long before = 0;
            for (int i = 0; i < part && i < cells.size(); i++) before += cells.get(i).size();
            return totalCells == 0 ? 90 : (int) Math.min(90, (before + index) * 90 / totalCells);
        }
        long before = 0;
        for (int i = 0; i < part && i < parts.size(); i++) before += parts.get(i).plan().decor().size();
        return 90 + (totalDecor == 0 ? 9 : (int) Math.min(9, (before + index) * 9 / totalDecor));
    }

    /** The [start, end) of zone {@code name} in part {@code part}'s order, or null if it has none. */
    public int[] zone(int part, String name) {
        return part < zones.size() ? zones.get(part).get(name) : null;
    }

    /** Every cell of part {@code part}, in write order. */
    public List<ArenaCell> cells(int part) {
        return cells.get(part);
    }

    /**
     * Writes zone {@code name} of part {@code part} at once (a gate's plaza, so that whoever crosses has ground to land on) and, if
     * the cursor stands at its start, moves the cursor past it.
     */
    public void writeZoneNow(ServerLevel level, int part, String name, Set<Long> forced) {
        int[] range = zone(part, name);
        if (range == null) return;
        BlockPos origin = parts.get(part).origin();
        for (int i = range[0]; i < range[1]; i++) write(level, origin, cells.get(part).get(i), forced);
        if (stage == Stage.BLOCKS && this.part == part && index == range[0]) index = range[1];
    }

    /**
     * Writes up to {@code budget} blocks (a decor item costs {@link #DECOR_COST}).
     *
     * @param forced the chunks forced so far (added to as the writer reaches new ones)
     */
    public Event step(ServerLevel level, int budget, Set<Long> forced) {
        int left = budget;
        while (left > 0 && stage == Stage.BLOCKS) {
            if (part >= parts.size()) {
                stage = Stage.DECOR;
                part = 0;
                index = 0;
                return Event.BLOCKS_DONE;
            }
            List<ArenaCell> list = cells.get(part);
            BlockPos origin = parts.get(part).origin();
            int end = Math.min(list.size(), index + left);
            for (int i = index; i < end; i++) write(level, origin, list.get(i), forced);
            left -= end - index;
            index = end;
            if (index >= list.size()) {
                part++;
                index = 0;
            }
        }
        while (left > 0 && stage == Stage.DECOR) {
            if (part >= parts.size()) {
                stage = Stage.DONE;
                return Event.DONE;
            }
            List<LayoutDecor> decor = parts.get(part).plan().decor();
            if (index >= decor.size()) {
                part++;
                index = 0;
                continue;
            }
            Part p = parts.get(part);
            LayoutDecor d = decor.get(index++);
            force(level, p.origin().offset((int) Math.floor(d.x()), 0, (int) Math.floor(d.z())), forced);
            DecorWriter.place(level, p.origin(), d);
            left -= DECOR_COST;
        }
        return Event.NONE;
    }

    /** Runs to the end at once (tests, commands on small plans). */
    public void writeAll(ServerLevel level, Set<Long> forced) {
        while (stage != Stage.DONE) step(level, Integer.MAX_VALUE / 2, forced);
    }

    private void write(ServerLevel level, BlockPos origin, ArenaCell c, Set<Long> forced) {
        BlockPos pos = origin.offset(c.dx(), c.dy(), c.dz());
        if (level.isOutsideBuildHeight(pos)) return;
        force(level, pos, forced);
        BlockState state = states.computeIfAbsent(c.block(), s -> {
            BlockState parsed = parse(level, s);
            return parsed == null ? Blocks.AIR.defaultBlockState() : parsed;
        });
        level.setBlock(pos, state, FLAGS);
    }

    /** Forces the chunk of {@code pos} for the rest of the write (once). */
    static void force(ServerLevel level, BlockPos pos, Set<Long> forced) {
        long key = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
        if (forced.add(key)) level.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, true);
    }

    /** Frees every chunk a writer forced. */
    public static void release(ServerLevel level, Set<Long> forced) {
        for (long key : forced) level.setChunkForced(ChunkPos.getX(key), ChunkPos.getZ(key), false);
        forced.clear();
    }

    /** A block state from its string, or null (logged once per string) if it cannot be parsed. */
    public static @Nullable BlockState parse(ServerLevel level, String s) {
        try {
            return BlockStateParser.parseForBlock(level.holderLookup(Registries.BLOCK), s, false).blockState();
        } catch (CommandSyntaxException | RuntimeException e) {
            if (WARNED.add(s)) LOG.warn("Heaven layout: unparsable block state '{}' ({}), written as air", s, e.getMessage());
            return null;
        }
    }

    /** A fresh, empty set for {@code forced}. */
    public static Set<Long> chunkSet() {
        return new HashSet<>();
    }
}
