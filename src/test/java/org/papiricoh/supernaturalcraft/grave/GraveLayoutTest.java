package org.papiricoh.supernaturalcraft.grave;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraveLayoutTest {

    private static final BlockPos GROUND = new BlockPos(100, 70, -40);

    @Test
    void theSameSeedDigsTheSameGraves() {
        for (long seed = 0; seed < 20; seed++) {
            GraveLayout.Plan a = GraveLayout.plan(GROUND, seed), b = GraveLayout.plan(GROUND, seed);
            assertEquals(a, b);
            assertEquals(a.fenceCells(), b.fenceCells());
        }
    }

    @Test
    void oneToThreeGravesAndExactlyOneRestless() {
        Set<Integer> counts = new HashSet<>();
        Set<Integer> fences = new HashSet<>();
        for (long seed = 0; seed < 200; seed++) {
            GraveLayout.Plan p = GraveLayout.plan(GROUND, seed);
            assertTrue(p.count() >= 1 && p.count() <= 3, "seed " + seed + ": " + p.count() + " graves");
            int restless = 0;
            for (int i = 0; i < p.count(); i++) if (p.isRestless(i)) restless++;
            assertEquals(1, restless, "seed " + seed);
            assertTrue(p.chestGrave() >= 0 && p.chestGrave() < p.count());
            counts.add(p.count());
            fences.add(p.fenceKind());
        }
        assertEquals(Set.of(1, 2, 3), counts, "every size of graveyard should turn up");
        assertEquals(Set.of(GraveLayout.FENCE_NONE, GraveLayout.FENCE_POSTS, GraveLayout.FENCE_RING), fences);
    }

    @Test
    void theBonesLieUnderTheMoundBesideTheirHeadstone() {
        for (long seed = 0; seed < 200; seed++) {
            GraveLayout.Plan p = GraveLayout.plan(GROUND, seed);
            BlockPos bones = p.bones();
            BlockPos head = p.headstone(p.restless());
            BlockPos firstMound = p.mound(p.restless()).get(0);
            assertEquals(firstMound.below(GraveLayout.DEPTH), bones, "bones under the head of their mound");
            assertEquals(GROUND.getY() - GraveLayout.DEPTH, bones.getY());
            assertEquals(GROUND.getY() + 1, head.getY(), "the headstone stands on the ground");
            int dx = Math.abs(firstMound.getX() - head.getX()), dz = Math.abs(firstMound.getZ() - head.getZ());
            assertEquals(1, dx + dz, "the mound starts right in front of its headstone");
            assertEquals(firstMound, head.below().relative(p.headstoneFacing()), "the headstone faces its grave");
            assertNotEquals(bones, p.chest(), "the chest and the bones never share a block");
        }
    }

    @Test
    void gravesNeverOverlap() {
        for (long seed = 0; seed < 200; seed++) {
            GraveLayout.Plan p = GraveLayout.plan(GROUND, seed);
            Set<BlockPos> used = new HashSet<>();
            for (int i = 0; i < p.count(); i++) {
                assertTrue(used.add(p.headstone(i).below()), "seed " + seed);
                for (BlockPos m : p.mound(i)) assertTrue(used.add(m), "seed " + seed);
            }
            for (BlockPos f : p.fenceCells()) assertFalse(used.contains(f.below()), "seed " + seed + ": fence on a grave at " + f);
        }
    }

    @Test
    void everythingStaysInsideItsBox() {
        for (long seed = 0; seed < 200; seed++) {
            GraveLayout.Plan p = GraveLayout.plan(GROUND, seed);
            BoundingBox full = p.fullBox(), piece = p.pieceBox();
            List<BlockPos> all = new ArrayList<>(p.fenceCells());
            all.add(p.bones());
            all.add(p.chest());
            for (int i = 0; i < p.count(); i++) {
                all.add(p.headstone(i));
                all.addAll(p.mound(i));
            }
            for (BlockPos b : all) {
                assertTrue(full.isInside(b), "seed " + seed + ": " + b + " outside " + full);
                assertTrue(b.getX() >= piece.minX() && b.getX() <= piece.maxX() && b.getZ() >= piece.minZ() && b.getZ() <= piece.maxZ(),
                        "seed " + seed + ": " + b + " outside the piece's footprint");
            }
            assertTrue(full.minY() <= GROUND.getY() - GraveLayout.DEPTH - 1);
            assertEquals(GROUND.getY() + 1, piece.minY(), "the beard levels the ground under the first layer above it");
            assertTrue(full.getXSpan() <= 11 && full.getZSpan() <= 11, "a small graveyard: " + full);
        }
    }

    @Test
    void onlyFlatDryGroundWillDo() {
        assertTrue(GraveLayout.suitable(new int[]{70, 71, 69, 70, 70}, new int[]{70, 71, 69, 70, 70}, 63));
        assertFalse(GraveLayout.suitable(new int[]{70, 74, 69, 70, 70}, new int[]{70, 74, 69, 70, 70}, 63), "too steep");
        assertFalse(GraveLayout.suitable(new int[]{70, 70, 70, 70, 70}, new int[]{70, 66, 70, 70, 70}, 63), "a pond");
        assertFalse(GraveLayout.suitable(new int[]{60, 60, 60, 60, 60}, new int[]{60, 60, 60, 60, 60}, 63), "below the sea");
    }
}
