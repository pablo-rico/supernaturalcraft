package org.papiricoh.supernaturalcraft.hell;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.hell.rift.HellRifts;
import org.papiricoh.supernaturalcraft.hell.rift.RiftShape;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HellRiftsTest {

    private static final double BORDER = 29_999_984;

    @Test
    void theOverworldIsScaledByAnEighth() {
        assertArrayEquals(new int[]{1000, -2000}, HellRifts.scaledTarget(8000.5, -15999.5, 1 / 8.0, BORDER));
    }

    @Test
    void theNetherCrossesOneToOne() {
        assertArrayEquals(new int[]{1500, 300}, HellRifts.scaledTarget(1500.5, 300.5, 1.0, BORDER));
    }

    @Test
    void neverIntoThePit() {
        for (double[] at : new double[][]{{0, 0}, {40, 10}, {-100, 50}, {3, -800}}) {
            int[] xz = HellRifts.scaledTarget(at[0], at[1], 1 / 8.0, BORDER);
            double d = Math.sqrt((double) xz[0] * xz[0] + (double) xz[1] * xz[1]);
            assertTrue(d >= HellDimension.RIFT_KEEP_OUT - 1.5, "landed " + d + " from the Pit's centre");
        }
    }

    @Test
    void keptInsideTheBorder() {
        int[] xz = HellRifts.scaledTarget(1e9, -1e9, 1.0, 1000);
        assertTrue(Math.abs(xz[0]) <= 1000 && Math.abs(xz[1]) <= 1000);
    }

    @Test
    void riftsAreAnUprightOval() {
        List<BlockPos> blocks = RiftShape.blocks(BlockPos.ZERO, Direction.Axis.X);
        assertEquals(11, blocks.size());
        assertEquals(11, new HashSet<>(blocks).size());
        assertTrue(blocks.contains(BlockPos.ZERO) && blocks.contains(new BlockPos(0, 4, 0)));
        assertTrue(blocks.contains(new BlockPos(1, 2, 0)) && !blocks.contains(new BlockPos(0, 2, 1)));
        assertTrue(RiftShape.blocks(BlockPos.ZERO, Direction.Axis.Z).contains(new BlockPos(0, 2, 1)));
        assertEquals(Direction.Axis.X, RiftShape.axisFacing(Direction.NORTH));
        assertEquals(Direction.Axis.Z, RiftShape.axisFacing(Direction.EAST));
    }
}
