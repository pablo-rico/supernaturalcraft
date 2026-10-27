package org.papiricoh.supernaturalcraft.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.chorus.Melody;
import org.papiricoh.supernaturalcraft.structure.spire.SpireLayoutCatalog;
import org.papiricoh.supernaturalcraft.structure.spire.SpireSites;

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
            List<BoundingBox> boxes = new java.util.ArrayList<>(List.of(SpireLayout.summitBox(plan), SpireLayout.ascentBox(plan), SpireLayout.templeBox(plan)));
            for (SpireSites.Site site : plan.sites()) boxes.add(SpireLayout.siteBox(site));
            for (BoundingBox b : boxes) {
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
            // The stair passes in front of the door, at most a forecourt's depth (and the walkway's half width) away.
            BlockPos door = plan.templeOrigin();
            BlockPos nearest = plan.path().stream().min(java.util.Comparator.comparingDouble(p -> p.distSqr(door.atY(p.getY())))).orElseThrow();
            double best = Math.sqrt(nearest.distSqr(door.atY(nearest.getY())));
            int ahead = (nearest.getX() - door.getX()) * plan.templeFacing().getStepX() + (nearest.getZ() - door.getZ()) * plan.templeFacing().getStepZ();
            assertTrue(best <= 7.5 && ahead > 0, "seed " + seed + ": the stair passes " + best + " blocks from the door");
        }
    }

    @Test
    void everyPartStaysInReachOnARealisticMountain() {
        for (long seed = 0; seed < 25; seed++) {
            SpireLayout.Plan plan = SpireLayoutCatalog.plan(SpireLayoutCatalog.MOUNTAIN, seed);
            BlockPos a = plan.altar();
            List<BoundingBox> boxes = new java.util.ArrayList<>(List.of(SpireLayout.summitBox(plan), SpireLayout.ascentBox(plan), SpireLayout.templeBox(plan)));
            for (SpireSites.Site site : plan.sites()) boxes.add(SpireLayout.siteBox(site));
            for (BoundingBox b : boxes) {
                int reach = Math.max(Math.max(Math.abs(b.minX() - a.getX()), Math.abs(b.maxX() - a.getX())),
                        Math.max(Math.abs(b.minZ() - a.getZ()), Math.abs(b.maxZ() - a.getZ())));
                assertTrue(reach <= 128 - 34 && reach <= SpireLayout.MAX_REACH, "seed " + seed + ": a piece reaches " + reach + " blocks from the peak");
            }
            // The parts never overlap one another and never sit on the stair.
            List<SpireSites.Site> all = new java.util.ArrayList<>(List.of(plan.temple()));
            all.addAll(plan.sites());
            for (int i = 0; i < all.size(); i++) {
                List<SpireSites.Site> others = new java.util.ArrayList<>(all);
                others.remove(i);
                assertTrue(SpireSites.fits(all.get(i), plan.path(), a.getX(), a.getZ(), others), "seed " + seed + ": " + all.get(i));
            }
        }
    }

    @Test
    void aBuildingGoesWhereTheGroundIsLevelALookoutWhereItFalls() {
        // A ring path at one level round a steep peak, with one level shelf beside it (between 240° and 300°).
        List<BlockPos> path = new java.util.ArrayList<>();
        for (int k = 0; k < 110; k++) {
            double ang = k * 2.0 / 35;
            BlockPos p = new BlockPos((int) Math.round(Math.cos(ang) * 35), 100, (int) Math.round(Math.sin(ang) * 35));
            if (path.isEmpty() || !path.getLast().equals(p)) path.add(p);
        }
        HeightSampler shelf = (x, z) -> {
            double r = Math.hypot(x, z), deg = Math.toDegrees(Math.atan2(z, x));
            if (deg < 0) deg += 360;
            if (deg >= 240 && deg <= 300 && r < 75) return 99;
            return (int) Math.round(99 - Math.max(0, r - 35) * 1.5);
        };
        List<SpireSites.Site> sites = SpireSites.choose(path, 0, 0, shelf, 5, -100);
        SpireSites.Site monastery = sites.stream().filter(s -> s.kind() == SpireSites.Kind.MONASTERY).findFirst().orElseThrow();
        BlockPos at = path.get(monastery.node());
        double deg = (Math.toDegrees(Math.atan2(at.getZ(), at.getX())) + 360) % 360;
        assertTrue(deg >= 235 && deg <= 305, "the monastery should take the shelf, not " + deg + "°");
        assertTrue(SpireSites.score(monastery, shelf) < 3, "on the shelf it needs almost no footing");
        for (SpireSites.Site s : sites) {
            if (s.kind() != SpireSites.Kind.LOOKOUT) continue;
            BlockPos p = path.get(s.node());
            double d = (Math.toDegrees(Math.atan2(p.getZ(), p.getX())) + 360) % 360;
            assertTrue(d < 240 || d > 300, "a lookout looks over a drop, not the shelf (" + d + "°)");
        }
        // On a cliff, no building fits at all.
        HeightSampler cliff = (x, z) -> (int) Math.round(99 - Math.max(0, Math.hypot(x, z) - 35) * 4);
        assertTrue(SpireSites.score(SpireSites.at(SpireSites.Kind.MONASTERY, path, 10, 0, 0, 0), cliff) == Double.MAX_VALUE);
    }

    @Test
    void theSameSeedPlansTheSameParts() {
        for (long seed : new long[]{3, 77}) {
            SpireLayout.Plan a = SpireLayoutCatalog.plan(SpireLayoutCatalog.MOUNTAIN, seed), b = SpireLayoutCatalog.plan(SpireLayoutCatalog.MOUNTAIN, seed);
            assertEquals(a.sites(), b.sites());
            assertArrayEquals(a.groundIn(), b.groundIn());
            assertArrayEquals(a.groundOut(), b.groundOut());
            assertEquals(a.templeOrigin(), b.templeOrigin());
        }
    }

    /** A steep peak (the usual one) still gets its monastery and belfry: they stand out from the cliff on taller piers. */
    @Test
    void aSteepPeakStillHasItsMonastery() {
        HeightSampler steep = (x, z) -> (int) (200 - Math.hypot(x, z) * 1.2 + Math.sin(x * 0.21) * Math.cos(z * 0.17) * 3);
        int monasteries = 0, belfries = 0;
        for (long seed = 1; seed <= 10; seed++) {
            for (var s : SpireLayout.plan(steep, 0, 200, 0, seed).sites()) {
                if (s.kind() == org.papiricoh.supernaturalcraft.structure.spire.SpireSites.Kind.MONASTERY) monasteries++;
                if (s.kind() == org.papiricoh.supernaturalcraft.structure.spire.SpireSites.Kind.BELFRY) belfries++;
            }
        }
        assertTrue(monasteries >= 9 && belfries >= 9, monasteries + " monasteries and " + belfries + " belfries in 10 spires");
    }
}
