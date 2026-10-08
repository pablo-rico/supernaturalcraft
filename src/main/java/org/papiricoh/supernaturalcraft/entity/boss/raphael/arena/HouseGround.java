package org.papiricoh.supernaturalcraft.entity.boss.raphael.arena;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.ArenaCell;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.HorsemenGround;

import java.util.ArrayList;
import java.util.List;

/**
 * Raphael's house laid over his arena (v0.16): {@link HouseLayout}'s plan pinned once to real positions on one level (the
 * floor under the altar), then written a few hundred blocks a tick through {@link HorsemenGround} (every block by
 * {@code ArenaController.mutate}, so the arena gives it all back). The last phase tears the roof off the same way: the roof
 * group's positions, written again as air ({@link HorsemenGround#mapped}).
 */
public final class HouseGround {

    private final BlockPos origin;
    private final HorsemenGround house;
    private final HorsemenGround roof;
    private @Nullable HorsemenGround writing;
    private boolean roofOn = true, begun, pendingTear;

    private HouseGround(BlockPos origin, HorsemenGround house, HorsemenGround roof) {
        this.origin = origin;
        this.house = house;
        this.roof = roof;
    }

    /** Pins the house to {@code arena}: its floor is the block under the arena's centre (the altar's, or the boss's feet). */
    public static HouseGround pin(ArenaController arena) {
        BlockPos origin = arena.center().below();
        return new HouseGround(origin, fixed(origin, HouseLayout.plan()), fixed(origin, HouseLayout.roof()));
    }

    private static HorsemenGround fixed(BlockPos origin, List<ArenaCell> cells) {
        List<BlockPos> positions = new ArrayList<>(cells.size());
        List<String> blocks = new ArrayList<>(cells.size());
        for (ArenaCell c : cells) {
            positions.add(origin.offset(c.dx(), c.dy(), c.dz()));
            blocks.add(c.block());
        }
        return HorsemenGround.fixed(positions, blocks);
    }

    /** Starts writing the whole house (once). */
    public void build() {
        if (begun) return;
        begun = true;
        roofOn = true;
        writing = house;
    }

    /** Starts tearing the roof off (its group written as air), once the house is all down. */
    public void tearOffRoof() {
        if (!roofOn) return;
        if (!begun) build();
        roofOn = false;
        if (writing()) pendingTear = true;
        else writing = roof.mapped(b -> HouseLayout.AIR);
    }

    public boolean roofOn() {
        return roofOn;
    }

    public boolean begun() {
        return begun;
    }

    /** Writes the next batch; true once the current work is all down. */
    public boolean tick(ServerLevel level, ArenaController arena) {
        if (writing != null && !writing.done()) writing.tick(level, arena);
        if ((writing == null || writing.done()) && pendingTear) {
            pendingTear = false;
            writing = roof.mapped(b -> HouseLayout.AIR);
            return false;
        }
        return !writing();
    }

    /** Writes everything left at once (tests, previews). */
    public void finish(ServerLevel level, ArenaController arena) {
        for (int i = 0; i < 3 && (writing() || pendingTear); i++) {
            if (writing != null) writing.finish(level, arena);
            tick(level, arena);
        }
        if (writing != null) writing.finish(level, arena);
    }

    public boolean writing() {
        return (writing != null && !writing.done()) || pendingTear;
    }

    /** The floor block under the altar (cell 0, 0, 0). */
    public BlockPos origin() {
        return origin;
    }

    /** Where a cell of the plan is in the world. */
    public BlockPos at(int dx, int dy, int dz) {
        return origin.offset(dx, dy, dz);
    }

    /** The block above the floor at a column (where a ring's oil lies, where one stands). */
    public BlockPos onFloor(HouseLayout.Spot spot) {
        return origin.offset(spot.dx(), 1, spot.dz());
    }

    public int size() {
        return house.size();
    }

    /** The roof group's positions (the tests check them gone, and back). */
    public List<BlockPos> roofPositions() {
        return roof.positions();
    }

    /** Lifts {@code e} out of any block the house was written into, to the first free space above. */
    public static void unstick(ServerLevel level, Entity e) {
        if (level.noCollision(e, e.getBoundingBox())) return;
        for (int up = 1; up <= HouseLayout.TOP + 2; up++) {
            if (level.noCollision(e, e.getBoundingBox().move(0, up, 0))) {
                if (e instanceof net.neoforged.neoforge.common.util.FakePlayer) e.moveTo(e.getX(), e.getY() + up, e.getZ());
                else e.teleportTo(e.getX(), e.getY() + up, e.getZ());
                return;
            }
        }
    }
}
