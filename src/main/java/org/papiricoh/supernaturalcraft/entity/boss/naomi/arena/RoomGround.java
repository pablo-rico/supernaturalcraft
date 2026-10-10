package org.papiricoh.supernaturalcraft.entity.boss.naomi.arena;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.ArenaCell;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.HorsemenGround;
import org.papiricoh.supernaturalcraft.layout.LayoutPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Naomi's room as her fight sees it (v0.18): {@link ReprogrammingRoomLayout}'s points pinned to a real origin (the room's floor
 * centre, {@code y = 0} the first air above it). In a hunter's Heaven the room is already there (written with the plot): only the
 * change of phase ({@link ReprogrammingRoomLayout#phaseCells}) is written. Called by egg or command anywhere else, the whole room
 * is written round her first. Every block goes through {@code ArenaController.mutate} ({@link HorsemenGround}, a few hundred a
 * tick), so the arena gives it all back.
 */
public final class RoomGround {

    private final BlockPos origin;
    private @Nullable HorsemenGround writing;
    private boolean begun;

    private RoomGround(BlockPos origin) {
        this.origin = origin.immutable();
    }

    /** The room at {@code origin} ({@code y = 0} of the layout). */
    public static RoomGround at(BlockPos origin) {
        return new RoomGround(origin);
    }

    /** Starts writing the whole room (once): an egg or a command far from any Heaven. */
    public void build() {
        if (begun) return;
        begun = true;
        writing = fixed(ReprogrammingRoomLayout.plan().cells());
    }

    /** The room stands already (a plot's, or written before a reload): nothing to write. */
    public void standing() {
        begun = true;
    }

    /** Starts writing what changes in {@code phase} (after whatever is still being written is down). */
    public void phase(ServerLevel level, ArenaController arena, int phase) {
        if (writing != null && !writing.done()) writing.finish(level, arena);
        List<ArenaCell> cells = ReprogrammingRoomLayout.phaseCells(phase);
        writing = cells.isEmpty() ? null : fixed(cells);
    }

    private HorsemenGround fixed(List<ArenaCell> cells) {
        List<BlockPos> positions = new ArrayList<>(cells.size());
        List<String> blocks = new ArrayList<>(cells.size());
        for (ArenaCell c : cells) {
            positions.add(origin.offset(c.dx(), c.dy(), c.dz()));
            blocks.add(c.block());
        }
        return HorsemenGround.fixed(positions, blocks);
    }

    public boolean begun() {
        return begun;
    }

    /** Writes the next batch; true once the current work is all down. */
    public boolean tick(ServerLevel level, ArenaController arena) {
        if (writing != null && !writing.done()) writing.tick(level, arena);
        return !writing();
    }

    /** Writes everything left at once (tests, previews). */
    public void finish(ServerLevel level, ArenaController arena) {
        if (writing != null) writing.finish(level, arena);
    }

    public boolean writing() {
        return writing != null && !writing.done();
    }

    /** The room's origin ({@code y = 0}, the first air above the floor at its centre). */
    public BlockPos origin() {
        return origin;
    }

    /** Where a point of the layout is in the world. */
    public BlockPos at(LayoutPoint p) {
        return origin.offset(p.x(), p.y(), p.z());
    }

    /** The middle of a point's block at floor level (where something stands). */
    public Vec3 feet(LayoutPoint p) {
        return Vec3.atBottomCenterOf(at(p));
    }

    /** Lifts {@code e} out of any block the room was written into, to the first free space above. */
    public static void unstick(ServerLevel level, Entity e) {
        if (level.noCollision(e, e.getBoundingBox())) return;
        for (int up = 1; up <= ReprogrammingRoomLayout.HEIGHT + 2; up++) {
            if (level.noCollision(e, e.getBoundingBox().move(0, up, 0))) {
                if (e instanceof net.neoforged.neoforge.common.util.FakePlayer) e.moveTo(e.getX(), e.getY() + up, e.getZ());
                else e.teleportTo(e.getX(), e.getY() + up, e.getZ());
                return;
            }
        }
    }
}
