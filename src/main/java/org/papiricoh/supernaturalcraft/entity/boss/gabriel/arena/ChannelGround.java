package org.papiricoh.supernaturalcraft.entity.boss.gabriel.arena;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaTerrain;
import org.papiricoh.supernaturalcraft.entity.boss.gabriel.Channel;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.ArenaCell;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.HorsemenGround;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TV Land's four sets laid over Gabriel's arena (the pattern of Michael's {@code HeavenGround}). The {@link ChannelLayouts#union}
 * is pinned to real positions once (each column's surface found before anything is built on it), each cell's index standing
 * in for its block; every change of channel rewrites those same positions with that set's blocks, a few hundred a tick,
 * through {@link HorsemenGround} (so all of it goes through {@code ArenaController.mutate} and comes back when the arena
 * closes). It also answers where a set's named spots are in the world ({@link #at}).
 */
public final class ChannelGround {

    private final HorsemenGround pinned;
    private final List<List<String>> blocks = new ArrayList<>();
    /** Every pinned cell's world position, by its {@link ChannelLayouts#key}. */
    private final Map<Long, BlockPos> byCell;
    private @Nullable Channel current;
    private @Nullable HorsemenGround writing;

    private ChannelGround(HorsemenGround pinned, List<List<String>> blocks, Map<Long, BlockPos> byCell) {
        this.pinned = pinned;
        this.blocks.addAll(blocks);
        this.byCell = byCell;
    }

    public static ChannelGround pin(ServerLevel level, ArenaController arena) {
        int radius = arena.radius();
        List<ArenaCell> union = ChannelLayouts.union(radius);
        List<ArenaCell> indexed = new ArrayList<>(union.size());
        Map<Long, BlockPos> byCell = new HashMap<>();
        Map<Long, BlockPos> surfaces = new HashMap<>();
        BlockPos c = arena.center();
        for (int i = 0; i < union.size(); i++) {
            ArenaCell cell = union.get(i);
            indexed.add(new ArenaCell(cell.dx(), cell.dy(), cell.dz(), "#" + i));
            int x = c.getX() + cell.dx(), z = c.getZ() + cell.dz();
            // The same surface HorsemenGround.pin finds for this column (nothing has been written yet).
            BlockPos floor = surfaces.computeIfAbsent(BlockPos.asLong(x, 0, z), k -> {
                BlockPos s = ArenaTerrain.surface(level, arena, x, z);
                return s != null ? s : BlockPos.ZERO;
            });
            if (floor != BlockPos.ZERO) byCell.put(ChannelLayouts.key(cell.dx(), cell.dy(), cell.dz()), floor.above(cell.dy()));
        }
        List<List<String>> blocks = new ArrayList<>();
        for (Channel ch : Channel.values()) blocks.add(ChannelLayouts.blocksOf(ch, union, radius));
        return new ChannelGround(HorsemenGround.pin(level, arena, indexed), blocks, byCell);
    }

    /** Starts writing {@code channel}'s set over whatever stands there now. */
    public void begin(Channel channel) {
        if (channel == current && writing != null) return;
        current = channel;
        List<String> mine = blocks.get(channel.ordinal());
        writing = pinned.mapped(index -> mine.get(Integer.parseInt(index.substring(1))));
    }

    /** Writes the next batch; true once the current set is all down. */
    public boolean tick(ServerLevel level, ArenaController arena) {
        return writing == null || writing.tick(level, arena);
    }

    /** Writes everything left of the current set at once (tests, previews). */
    public void finish(ServerLevel level, ArenaController arena) {
        if (writing != null) writing.finish(level, arena);
    }

    public boolean writing() {
        return writing != null && !writing.done();
    }

    /** The set being written or standing (null before the first). */
    public @Nullable Channel current() {
        return current;
    }

    public int size() {
        return pinned.size();
    }

    /** Where a cell of the sets is in the world, or null if that column had no floor (or no set uses it). */
    public @Nullable BlockPos at(int dx, int dy, int dz) {
        return byCell.get(ChannelLayouts.key(dx, dy, dz));
    }

    public @Nullable BlockPos at(ChannelLayouts.Spot spot) {
        return at(spot.dx(), spot.dy(), spot.dz());
    }

    /**
     * Where a spot is even off the sets' cells: its column's floor (pinned if the column is, else found now) raised by
     * {@code dy}. Null if the column has no floor at all.
     */
    public @Nullable BlockPos anywhere(ServerLevel level, ArenaController arena, ChannelLayouts.Spot spot) {
        BlockPos floor = at(spot.dx(), 0, spot.dz());
        if (floor == null) {
            BlockPos c = arena.center();
            floor = ArenaTerrain.surface(level, arena, c.getX() + spot.dx(), c.getZ() + spot.dz());
        }
        return floor != null ? floor.above(spot.dy()) : null;
    }
}
