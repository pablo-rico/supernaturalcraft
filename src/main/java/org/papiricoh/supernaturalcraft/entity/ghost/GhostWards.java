package org.papiricoh.supernaturalcraft.entity.ghost;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;

/**
 * A ghost passes through walls but never across salt. A column holding a salt line from a little
 * under its feet to its waist is closed to it: any move that would carry its box into such a
 * column (from outside it) loses that horizontal component.
 */
public final class GhostWards {

    /** Salt this far below the feet still bars the way (it floats), and this far above. */
    public static final int BELOW = 3, ABOVE = 1;

    private GhostWards() {
    }

    public static Vec3 clip(Entity e, Vec3 delta) {
        if (delta.x == 0 && delta.z == 0) return delta;
        Level level = e.level();
        AABB box = e.getBoundingBox();
        double dx = delta.x, dz = delta.z;
        if (dx != 0 && entersWard(level, box, box.move(dx, delta.y, 0))) dx = 0;
        AABB afterX = box.move(dx, delta.y, 0);
        if (dz != 0 && entersWard(level, box, afterX.move(0, 0, dz))) dz = 0;
        return dx == delta.x && dz == delta.z ? delta : new Vec3(dx, delta.y, dz);
    }

    /** Whether {@code to} covers a salted column that {@code from} does not. */
    static boolean entersWard(Level level, AABB from, AABB to) {
        int fx0 = Mth.floor(from.minX), fx1 = Mth.floor(from.maxX - 1.0e-7), fz0 = Mth.floor(from.minZ), fz1 = Mth.floor(from.maxZ - 1.0e-7);
        int feet = Mth.floor(to.minY);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = Mth.floor(to.minX); x <= Mth.floor(to.maxX - 1.0e-7); x++) {
            for (int z = Mth.floor(to.minZ); z <= Mth.floor(to.maxZ - 1.0e-7); z++) {
                if (x >= fx0 && x <= fx1 && z >= fz0 && z <= fz1) continue;
                for (int y = feet - BELOW; y <= feet + ABOVE; y++) {
                    if (level.getBlockState(p.set(x, y, z)).is(AllBlocks.SALT_LINE.get())) return true;
                }
            }
        }
        return false;
    }
}
