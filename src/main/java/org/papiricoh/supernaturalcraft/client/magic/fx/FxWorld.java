package org.papiricoh.supernaturalcraft.client.magic.fx;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** World lookups the effects need (v0.21): where the floor is under a point. */
public final class FxWorld {

    private FxWorld() {
    }

    /** The top of the floor under {@code at} (searching {@code depth} blocks down), or {@code at.y} if there is none. */
    public static double floor(Level level, Vec3 at, int depth) {
        double y = floorOrNaN(level, at, depth);
        return Double.isNaN(y) ? at.y : y;
    }

    /** As {@link #floor}, NaN if there is none. */
    public static double floorOrNaN(Level level, Vec3 at, int depth) {
        BlockPos.MutableBlockPos p = BlockPos.containing(at.x, at.y + 0.3, at.z).mutable();
        for (int i = 0; i < depth; i++, p.move(0, -1, 0)) {
            var shape = level.getBlockState(p).getCollisionShape(level, p);
            if (!shape.isEmpty()) return p.getY() + shape.max(Direction.Axis.Y);
        }
        return Double.NaN;
    }
}
