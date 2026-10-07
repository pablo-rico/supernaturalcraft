package org.papiricoh.supernaturalcraft.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.chorus.Melody;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpireLayoutTest {

    /** A cone of a mountain: 200 at the peak, falling 1.2 blocks per block outward. */
    private static final HeightSampler CONE = (x, z) -> (int) (200 - Math.sqrt(x * x + z * z) * 1.2);

    @Test
    void theStairNeverClimbsAndStepsDownOneAtATime() {
        for (long seed : new long[]{1, 42, 99999}) {
            SpireLayout.Plan plan = SpireLayout.plan(CONE, 0, 200, 0, seed);
            List<BlockPos> path = plan.path();
            assertTrue(path.size() > 30, "a real stair, not " + path.size() + " steps");
            for (int i = 1; i < path.size(); i++) {
                int dy = path.get(i).getY() - path.get(i - 1).getY();
                assertTrue(dy == 0 || dy == -1, "step " + i + " changes height by " + dy);
                int dx = Math.abs(path.get(i).getX() - path.get(i - 1).getX()), dz = Math.abs(path.get(i).getZ() - path.get(i - 1).getZ());
                assertTrue(dx <= 2 && dz <= 2, "step " + i + " jumps " + dx + "," + dz);
            }
            assertEquals(SpireLayout.floorY(plan), path.getFirst().getY(), "the stair starts at the platform");
            assertTrue(SpireLayout.floorY(plan) - path.getLast().getY() >= 20, "it should carry you a long way down");
        }
    }

    @Test
    void everyPieceStaysNearItsStart() {
        // The start chunk can be up to ~33 blocks from the peak; pieces must be within 8 chunks of it.
        for (long seed = 0; seed < 50; seed++) {
            SpireLayout.Plan plan = SpireLayout.plan(CONE, 0, 200, 0, seed);
            for (BoundingBox b : List.of(SpireLayout.summitBox(plan), SpireLayout.ascentBox(plan), SpireLayout.templeBox(plan))) {
                int reach = Math.max(Math.max(Math.abs(b.minX()), Math.abs(b.maxX())), Math.max(Math.abs(b.minZ()), Math.abs(b.maxZ())));
                assertTrue(reach <= 128 - 34, "seed " + seed + ": a piece reaches " + reach + " blocks from the peak");
            }
        }
    }

    @Test
    void theSameSeedBuildsTheSameSpire() {
        SpireLayout.Plan a = SpireLayout.plan(CONE, 0, 200, 0, 7), b = SpireLayout.plan(CONE, 0, 200, 0, 7);
        assertArrayEquals(a.melody(), b.melody());
        assertEquals(a.path(), b.path());
        assertEquals(a.templeOrigin(), b.templeOrigin());
        assertTrue(Melody.valid(a.melody()));
        long differing = java.util.stream.LongStream.range(0, 40)
                .filter(s -> !Arrays.equals(SpireLayout.plan(CONE, 0, 200, 0, s).melody(), a.melody())).count();
        assertTrue(differing > 30, "hymns should differ from spire to spire");
    }

    @Test
    void theTempleDoorFacesTheStair() {
        for (long seed = 0; seed < 20; seed++) {
            SpireLayout.Plan plan = SpireLayout.plan(CONE, 0, 200, 0, seed);
            BlockPos inFront = plan.templeOrigin().relative(plan.templeFacing(), 3);
            double best = plan.path().stream().mapToDouble(p -> Math.sqrt(p.distSqr(inFront.atY(p.getY())))).min().orElseThrow();
            assertTrue(best <= 1.5, "seed " + seed + ": the stair passes " + best + " blocks from the door");
        }
    }
}
