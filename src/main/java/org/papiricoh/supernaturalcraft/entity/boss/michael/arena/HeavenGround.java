package org.papiricoh.supernaturalcraft.entity.boss.michael.arena;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.ArenaCell;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.HorsemenGround;

import java.util.ArrayList;
import java.util.List;

/**
 * Michael's three Heavens laid over his arena. The {@link HeavenLayouts#union} of their cells is pinned to real positions
 * once (each column's surface found before anything is built on it), with each cell's index standing in for its block;
 * every change of Heaven then rewrites those same positions with that Heaven's blocks, a few hundred a tick, through
 * {@link HorsemenGround} (so all of it goes through {@code ArenaController.mutate} and comes back when the arena closes).
 */
public final class HeavenGround {

    private final HorsemenGround pinned;
    private final List<List<String>> blocks = new ArrayList<>();
    private int current = -1;
    private @Nullable HorsemenGround writing;

    private HeavenGround(HorsemenGround pinned, List<List<String>> blocks) {
        this.pinned = pinned;
        this.blocks.addAll(blocks);
    }

    public static HeavenGround pin(ServerLevel level, ArenaController arena, long seed) {
        int radius = arena.radius();
        List<ArenaCell> union = HeavenLayouts.union(radius, seed);
        List<ArenaCell> indexed = new ArrayList<>(union.size());
        for (int i = 0; i < union.size(); i++) {
            ArenaCell c = union.get(i);
            indexed.add(new ArenaCell(c.dx(), c.dy(), c.dz(), "#" + i));
        }
        List<List<String>> blocks = new ArrayList<>();
        for (int w = 0; w < HeavenLayouts.COUNT; w++) blocks.add(HeavenLayouts.blocksOf(w, union, radius, seed));
        return new HeavenGround(HorsemenGround.pin(level, arena, indexed), blocks);
    }

    /** Starts writing Heaven {@code which} over whatever stands there now. */
    public void begin(int which) {
        if (which == current && writing != null) return;
        current = which;
        List<String> mine = blocks.get(which);
        writing = pinned.mapped(index -> mine.get(Integer.parseInt(index.substring(1))));
    }

    /** Writes the next batch; true once the current Heaven is all down. */
    public boolean tick(ServerLevel level, ArenaController arena) {
        return writing == null || writing.tick(level, arena);
    }

    /** Writes everything left of the current Heaven at once (tests, previews). */
    public void finish(ServerLevel level, ArenaController arena) {
        if (writing != null) writing.finish(level, arena);
    }

    public boolean writing() {
        return writing != null && !writing.done();
    }

    /** The Heaven being written or standing (-1 before the first). */
    public int current() {
        return current;
    }

    public int size() {
        return pinned.size();
    }

    /** Where the current Heaven put {@code block}. */
    public List<BlockPos> positionsOf(String block) {
        return writing != null ? writing.positionsOf(block) : List.of();
    }
}
