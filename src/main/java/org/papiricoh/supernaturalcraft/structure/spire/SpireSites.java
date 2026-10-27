package org.papiricoh.supernaturalcraft.structure.spire;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.papiricoh.supernaturalcraft.buildkit.Box;
import org.papiricoh.supernaturalcraft.buildkit.Canvas;
import org.papiricoh.supernaturalcraft.buildkit.Noise;
import org.papiricoh.supernaturalcraft.structure.HeightSampler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Where the Hymnal Spire's fixed parts stand along its stair (pure, deterministic): each part is a level plan set beside a
 * node of the path, its door toward the stair (the node, then {@link #standoff} blocks straight out from the peak), turned to
 * the nearest compass direction. A node is a good site for a building when the ground under its footprint lies close to the
 * stair's level (little footing to build, little rock to cut), and a good lookout when the ground falls far away under it.
 * Parts never cover the stair (except their forecourt in front of their own node), never overlap, keep off the summit and
 * stay well inside the structure's reach.
 */
public final class SpireSites {

    public enum Kind {TEMPLE, MONASTERY, BELFRY, LOOKOUT}

    /** One part: what it is, the node it opens onto, its origin (door, on the floor's first air layer), its facing (the way the door looks, toward the stair), its variant. */
    public record Site(Kind kind, int node, BlockPos origin, Direction facing, int variant) {

        /** Quarter turns clockwise from the drawn orientation (door facing north). */
        public int turn() {
            return quarter(facing);
        }
    }

    /** The farthest (Chebyshev) any part's footprint may reach from the peak. */
    public static final int REACH = 80;
    /** Parts keep this far from the altar (outside the Chorus arena). */
    public static final int SUMMIT_CLEAR = SummitLayout.RADIUS + 5;
    static final int GRID = 4, MAX_FILL = 34, MAX_MEAN_FILL = 14, LOOKOUTS = 3;
    /** The second pass for the monastery and the belfry on a steep peak: deeper piers allowed (a variant no part uses). */
    static final int CLIFF = -2, CLIFF_FILL = 56, CLIFF_MEAN_FILL = 26;

    private SpireSites() {
    }

    public static int quarter(Direction facing) {
        return switch (facing) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
    }

    public static Box footprint(Kind k) {
        return switch (k) {
            case TEMPLE -> TempleLayout.FOOTPRINT;
            case MONASTERY -> MonasteryLayout.FOOTPRINT;
            case BELFRY -> BelfryLayout.FOOTPRINT;
            case LOOKOUT -> LookoutLayout.FOOTPRINT;
        };
    }

    /** How far either side of the door the forecourt reaches (the stair in front of it must be level with it). */
    static int frontHalfWidth(Kind k) {
        return switch (k) {
            case TEMPLE -> 4;
            case MONASTERY -> 5;
            case BELFRY -> 3;
            case LOOKOUT -> 2;
        };
    }

    public static int standoff(Kind k) {
        return k == Kind.LOOKOUT ? 2 : 3;
    }

    /**
     * How far out a part may stand back from its node where the stair runs aslant of it (its forecourt still reaches the
     * walkway): the temple's and the monastery's are five deep, the belfry's four, a lookout has none.
     */
    static int maxStandoff(Kind k) {
        return switch (k) {
            case TEMPLE, MONASTERY -> 7;
            case BELFRY -> 6;
            case LOOKOUT -> 2;
        };
    }

    /** The nearest compass direction from the peak out to a node. */
    public static Direction outward(BlockPos node, int cx, int cz) {
        double ox = node.getX() - cx, oz = node.getZ() - cz, len = Math.max(1, Math.hypot(ox, oz));
        return Direction.getNearest((float) (ox / len), 0, (float) (oz / len));
    }

    /** The site a part of kind {@code k} would have at node {@code i}. */
    public static Site at(Kind k, List<BlockPos> path, int i, int cx, int cz, int variant) {
        return at(k, path, i, cx, cz, variant, standoff(k));
    }

    static Site at(Kind k, List<BlockPos> path, int i, int cx, int cz, int variant, int standoff) {
        BlockPos node = path.get(i);
        Direction out = outward(node, cx, cz);
        return new Site(k, i, node.relative(out, standoff), out.getOpposite(), variant);
    }

    /** The footprint's world columns: {minX, minZ, maxX, maxZ}. */
    public static int[] rect(Site s) {
        Box f = footprint(s.kind());
        int[] a = Canvas.turn(f.x0(), f.z0(), s.turn()), b = Canvas.turn(f.x1(), f.z1(), s.turn());
        int ox = s.origin().getX(), oz = s.origin().getZ();
        return new int[]{ox + Math.min(a[0], b[0]), oz + Math.min(a[1], b[1]), ox + Math.max(a[0], b[0]), oz + Math.max(a[1], b[1])};
    }

    /** (x, z) in the site's drawn frame (door at the origin facing north). */
    static int[] local(Site s, int x, int z) {
        return Canvas.turn(x - s.origin().getX(), z - s.origin().getZ(), -s.turn());
    }

    // --- choosing -----------------------------------------------------------------------------------------------------------

    /**
     * The temple (always, and first in the list: if no node suits it, the old rule — a third of the way down — stands), the
     * monastery (chosen before the temple: it is the hardest to fit), the belfry and up to {@link #LOOKOUTS} lookouts where they
     * fit. {@code avoid}: nodes no part may open onto (the wayside
     * shrine's), with a margin.
     */
    public static List<Site> choose(List<BlockPos> path, int cx, int cz, HeightSampler h, long seed, int avoid) {
        List<Site> out = new ArrayList<>();
        int n = path.size();
        // The monastery is the hardest to fit (the widest, the deepest): it chooses first.
        Site monastery = best(Kind.MONASTERY, path, cx, cz, h, out, n / 3, n - 6, avoid, 0);
        // A steep peak (the usual one) has no shelf for it: then it stands out from the cliff on taller piers.
        if (monastery == null) monastery = best(Kind.MONASTERY, path, cx, cz, h, out, n / 3, n - 6, avoid, CLIFF);
        if (monastery != null) out.add(monastery);
        Site temple = best(Kind.TEMPLE, path, cx, cz, h, out, Math.max(4, n / 5), n / 2, avoid, 0);
        if (temple == null) temple = best(Kind.TEMPLE, path, cx, cz, h, out, 4, n - 4, avoid, 0);
        if (temple == null) temple = best(Kind.TEMPLE, path, cx, cz, (x, z) -> Math.max(h.top(x, z), path.get(0).getY() - 200), out, 4, n - 4, -100, -1);
        if (temple == null) temple = at(Kind.TEMPLE, path, Math.min(n - 1, Math.max(4, n / 3)), cx, cz, 0);
        out.addFirst(temple);
        Site belfry = best(Kind.BELFRY, path, cx, cz, h, out, n / 6, n - 4, avoid, 0);
        if (belfry == null) belfry = best(Kind.BELFRY, path, cx, cz, h, out, n / 6, n - 4, avoid, CLIFF);
        if (belfry != null) out.add(belfry);
        int first = Math.floorMod((int) (seed ^ seed >>> 29), LookoutLayout.VARIANTS);
        for (int k = 0; k < LOOKOUTS; k++) {
            Site look = best(Kind.LOOKOUT, path, cx, cz, h, out, 6, n - 3, avoid, (first + k) % LookoutLayout.VARIANTS);
            if (look == null) break;
            out.add(look);
        }
        return out;
    }

    private static Site best(Kind k, List<BlockPos> path, int cx, int cz, HeightSampler h, List<Site> taken, int from, int to, int avoid, int variant) {
        Site best = null;
        double bestScore = Double.MAX_VALUE;
        for (int i = Math.max(1, from); i <= Math.min(path.size() - 2, to); i++) {
            if (Math.abs(i - avoid) < 6) continue;
            Site s = null;
            for (int so = standoff(k); so <= maxStandoff(k) && s == null; so++) {
                Site t = at(k, path, i, cx, cz, variant == CLIFF ? 0 : Math.max(0, variant), so);
                if (fits(t, path, cx, cz, taken)) s = t;
            }
            if (s == null) continue;
            // variant -1: any site that fits will do, the least footing first (the temple's last resort).
            double score = variant == CLIFF ? score(s, h, CLIFF_FILL, CLIFF_MEAN_FILL) : variant < 0 ? relaxed(s, h) : score(s, h);
            if (score < Double.MAX_VALUE && score < bestScore) {
                bestScore = score;
                best = s;
            }
        }
        return best;
    }

    /** Whether a site keeps to its bounds: inside the reach, off the summit, clear of the stair and of the other parts. */
    public static boolean fits(Site s, List<BlockPos> path, int cx, int cz, List<Site> taken) {
        int[] r = rect(s);
        for (int[] q : new int[][]{{r[0], r[1]}, {r[2], r[3]}}) if (Math.max(Math.abs(q[0] - cx), Math.abs(q[1] - cz)) > REACH) return false;
        double nx = Math.max(r[0], Math.min(cx, r[2])), nz = Math.max(r[1], Math.min(cz, r[3]));
        if (Math.hypot(nx - cx, nz - cz) < SUMMIT_CLEAR) return false;
        Box f = footprint(s.kind());
        int y = path.get(s.node()).getY(), front = frontHalfWidth(s.kind());
        for (int j = 0; j < path.size(); j++) {
            int[] l = local(s, path.get(j).getX(), path.get(j).getZ());
            // Nothing of the stair may pass over, beside or behind the building...
            if (l[1] >= 0 && l[1] <= f.z1() + 2 && l[0] >= f.x0() - 2 && l[0] <= f.x1() + 2) return false;
            // ...and where it passes in front of the door it must be at the door's level (the forecourt is paved at it).
            if (l[1] < 0 && l[1] >= f.z0() - 2 && Math.abs(l[0]) <= front && Math.abs(path.get(j).getY() - y) > 1) return false;
        }
        for (Site o : taken) {
            int[] q = rect(o);
            if (Math.abs(o.node() - s.node()) < 8) return false;
            if (r[0] - 3 <= q[2] && q[0] - 3 <= r[2] && r[1] - 3 <= q[3] && q[1] - 3 <= r[3]) return false;
        }
        return true;
    }

    /**
     * How well a site suits its part (lower is better): for a building, the mean footing it needs plus the rock it must cut
     * (sampled on a {@link #GRID} grid over the building, the forecourt left out), worse for a deep worst case, impossible past
     * {@link #MAX_FILL}; for a lookout, the deeper the drop under it the better (it needs at least five blocks).
     */
    public static double score(Site s, HeightSampler h) {
        return score(s, h, MAX_FILL, MAX_MEAN_FILL);
    }

    /** {@link #score(Site, HeightSampler)} with other footing limits (the cliff pass: {@link #CLIFF_FILL}). */
    static double score(Site s, HeightSampler h, int maxFill, int maxMeanFill) {
        Box f = footprint(s.kind());
        int floor = s.origin().getY() - 1;
        double fill = 0, cut = 0, worst = 0;
        int count = 0;
        int[] r = rect(s);
        for (int x = Math.floorDiv(r[0], GRID) * GRID; x <= r[2]; x += GRID) {
            for (int z = Math.floorDiv(r[1], GRID) * GRID; z <= r[3]; z += GRID) {
                if (x < r[0] || z < r[1]) continue;
                int[] l = local(s, x, z);
                if (l[1] < 0 || l[0] < f.x0() || l[0] > f.x1() || l[1] > f.z1()) continue;
                int g = h.top(x, z);
                fill += Math.max(0, floor - g);
                cut += Math.max(0, g - floor);
                worst = Math.max(worst, floor - g);
                count++;
            }
        }
        if (count == 0) return Double.MAX_VALUE;
        fill /= count;
        cut /= count;
        if (s.kind() == Kind.LOOKOUT) return fill < 5 ? Double.MAX_VALUE : -fill + cut * 2 + Noise.hash01(s.node(), 0, 0, 77) * 0.5;
        if (worst > maxFill || fill > maxMeanFill) return Double.MAX_VALUE;
        return fill + cut * 0.6 + worst * 0.15;
    }

    /** The mean footing a building needs, no limits. */
    static double relaxed(Site s, HeightSampler h) {
        int[] r = rect(s);
        int floor = s.origin().getY() - 1;
        double fill = 0;
        int count = 0;
        for (int x = r[0]; x <= r[2]; x += GRID) {
            for (int z = r[1]; z <= r[3]; z += GRID) {
                fill += Math.abs(floor - h.top(x, z));
                count++;
            }
        }
        return fill / Math.max(1, count);
    }

    /** Sites sorted by node (for logs and tests). */
    public static List<Site> byNode(List<Site> sites) {
        List<Site> out = new ArrayList<>(sites);
        out.sort(Comparator.comparingInt(Site::node));
        return out;
    }
}
