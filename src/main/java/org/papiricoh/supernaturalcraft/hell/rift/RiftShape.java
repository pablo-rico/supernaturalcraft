package org.papiricoh.supernaturalcraft.hell.rift;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;

/**
 * The shape of a rift: an upright oval three blocks wide and five tall, standing on its anchor
 * (the bottom-centre block) and lying along one horizontal axis.
 *
 * <pre>
 *  .#.
 *  ###
 *  ###
 *  ###
 *  .#.   ← anchor at the bottom centre
 * </pre>
 */
public final class RiftShape {

    public static final int WIDTH = 3, HEIGHT = 5;

    private RiftShape() {
    }

    /** Every block of a rift anchored at {@code anchor} and lying along {@code axis} (X or Z). */
    public static List<BlockPos> blocks(BlockPos anchor, Direction.Axis axis) {
        List<BlockPos> out = new ArrayList<>();
        for (int y = 0; y < HEIGHT; y++) {
            int half = y == 0 || y == HEIGHT - 1 ? 0 : 1;
            for (int s = -half; s <= half; s++) {
                out.add(axis == Direction.Axis.X ? anchor.offset(s, y, 0) : anchor.offset(0, y, s));
            }
        }
        return out;
    }

    /** The axis a rift lies along so its face looks toward {@code facing} (a horizontal direction). */
    public static Direction.Axis axisFacing(Direction facing) {
        return facing.getAxis() == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
    }
}
