package org.papiricoh.supernaturalcraft.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.papiricoh.supernaturalcraft.buildkit.Box;
import org.papiricoh.supernaturalcraft.buildkit.Canvas;
import org.papiricoh.supernaturalcraft.chorus.Melody;
import org.papiricoh.supernaturalcraft.structure.spire.AscentLayout;
import org.papiricoh.supernaturalcraft.structure.spire.SpireKit;
import org.papiricoh.supernaturalcraft.structure.spire.SpireSites;
import org.papiricoh.supernaturalcraft.structure.spire.SummitLayout;
import org.papiricoh.supernaturalcraft.structure.spire.TempleLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Where a Hymnal Spire's parts go, worked out before a single block is placed (pure: testable, and saved with the pieces so a
 * reloaded piece builds exactly the same):
 *
 * <ul>
 *   <li>the <b>summit</b>: a ruined platform of {@link #PLATFORM_RADIUS} hovering just over the peak, the Choir Altar at its
 *   heart and seven bells around it ({@link SummitLayout});</li>
 *   <li>the <b>ascent</b>: a stair that spirals down from the platform's edge around the mountain, never climbing and never
 *   dropping more than a block per step ({@link AscentLayout}); the ground beside every node is sampled here, so the stair
 *   knows where to bridge, where to wall and where to rail;</li>
 *   <li>the <b>temple</b> whose window gives the hymn away, the <b>monastery</b> with the choristers' crypt under it, the
 *   <b>fallen belfry</b> and two or three <b>lookouts</b>, each beside a node chosen for its ground ({@link SpireSites}).</li>
 * </ul>
 */
public final class SpireLayout {

    public static final int PLATFORM_RADIUS = SummitLayout.RADIUS, LIFT = 4;
    /** Pieces must stay this close to the start chunk, or the chunk generator never places them. */
    public static final int MAX_REACH = 112;
    public static final int TEMPLE_WIDTH = 2 * TempleLayout.HALF + 1, TEMPLE_DEPTH = TempleLayout.BACK + 1, TEMPLE_HEIGHT = 9;
    private static final double STEP = 2.0, OUTWARD = 0.32;
    private static final int MAX_NODES = 150, SIDE = 3;
    /** How far above a part's floor its piece box reaches (roofs, bell-cotes). */
    static final int PART_HEIGHT = 30;
    /** How far a battered footing can step out past what it carries. */
    static final int BATTER_REACH = SpireKit.FOOTING_LIMIT / SpireKit.BATTER;

    /**
     * Altar position (one above the platform), the hymn, the ascent's nodes (feet cells), the temple's door and facing, the
     * seed; the ground's top beside every node (toward the peak, under it, away from it); the other parts.
     */
    public record Plan(BlockPos altar, byte[] melody, List<BlockPos> path, BlockPos templeOrigin, Direction templeFacing, long seed,
                       int[] groundIn, int[] groundMid, int[] groundOut, List<SpireSites.Site> sites) {

        /** A plan with no sampled ground (taken as flush with the stair) and no parts but the summit, stair and temple. */
        public Plan(BlockPos altar, byte[] melody, List<BlockPos> path, BlockPos templeOrigin, Direction templeFacing, long seed) {
            this(altar, melody, path, templeOrigin, templeFacing, seed, flush(path), flush(path), flush(path), List.of());
        }

        /** The temple as a site. */
        public SpireSites.Site temple() {
            // The node the door opens onto: straight out in front of it, a few blocks away.
            for (int so = SpireSites.standoff(SpireSites.Kind.TEMPLE); so <= 7; so++) {
                BlockPos f = templeOrigin.relative(templeFacing, so);
                for (int i = 0; i < path.size(); i++) {
                    if (path.get(i).getX() == f.getX() && path.get(i).getZ() == f.getZ()) {
                        return new SpireSites.Site(SpireSites.Kind.TEMPLE, i, templeOrigin, templeFacing, 0);
                    }
                }
            }
            int node = 0;
            double best = Double.MAX_VALUE;
            BlockPos front = templeOrigin.relative(templeFacing, 3);
            for (int i = 0; i < path.size(); i++) {
                double d = path.get(i).distSqr(front);
                if (d < best) {
                    best = d;
                    node = i;
                }
            }
            return new SpireSites.Site(SpireSites.Kind.TEMPLE, node, templeOrigin, templeFacing, 0);
        }
    }

    private static int[] flush(List<BlockPos> path) {
        int[] g = new int[path.size()];
        for (int i = 0; i < g.length; i++) g[i] = path.get(i).getY() - 1;
        return g;
    }

    private SpireLayout() {
    }

    /** A sampler that remembers what it has read (the site search asks for the same columns again and again). */
    static HeightSampler cached(HeightSampler h) {
        Map<Long, Integer> seen = new HashMap<>();
        return (x, z) -> seen.computeIfAbsent(((long) x << 32) ^ (z & 0xFFFFFFFFL), k -> h.top(x, z));
    }

    /** Plans a spire on the peak at (x, peakY, z). */
    public static Plan plan(HeightSampler sampler, int x, int peakY, int z, long seed) {
        HeightSampler h = cached(sampler);
        RandomSource random = RandomSource.create(seed);
        byte[] melody = Melody.generate(random);
        int floor = peakY + LIFT;
        BlockPos altar = new BlockPos(x, floor + 1, z);

        List<BlockPos> path = new ArrayList<>();
        double angle = random.nextDouble() * Math.PI * 2;
        double r = PLATFORM_RADIUS + 1.5;
        int y = floor;
        int lastX = Integer.MIN_VALUE, lastZ = Integer.MIN_VALUE;
        for (int i = 0; i < MAX_NODES; i++) {
            int px = x + (int) Math.round(Math.cos(angle) * r), pz = z + (int) Math.round(Math.sin(angle) * r);
            if (px != lastX || pz != lastZ) {
                // Far out the turn's rounding can skip a block: put a node half way so no step spans more than two.
                if (!path.isEmpty() && Math.max(Math.abs(px - lastX), Math.abs(pz - lastZ)) > 2) {
                    int mx = Math.floorDiv(px + lastX, 2), mz = Math.floorDiv(pz + lastZ, 2);
                    y = Mth.clamp(h.top(mx, mz) + 1, y - 1, y);
                    path.add(new BlockPos(mx, y, mz));
                }
                // Down with the ground, a block a step at most; never up.
                int ground = h.top(px, pz) + 1;
                if (!path.isEmpty()) y = Mth.clamp(ground, y - 1, y);
                path.add(new BlockPos(px, y, pz));
                lastX = px;
                lastZ = pz;
            }
            angle += STEP / r;
            r += OUTWARD;
            if (r > MAX_REACH - 52) break;
            if (floor - y >= 40 && Math.abs(h.top(px, pz) + 1 - y) <= 1 && i > 90) break;
        }
        List<BlockPos> nodes = List.copyOf(path);

        // The ground beside each node, for the stair's walls, rails and bridges.
        int n = nodes.size();
        int[] in = new int[n], mid = new int[n], out = new int[n];
        List<int[]> rel = relative(nodes, altar);
        for (int i = 0; i < n; i++) {
            double[] f = frame(rel, i);
            BlockPos p = nodes.get(i);
            in[i] = h.top((int) Math.round(p.getX() - f[0] * SIDE), (int) Math.round(p.getZ() - f[1] * SIDE));
            mid[i] = h.top(p.getX(), p.getZ());
            out[i] = h.top((int) Math.round(p.getX() + f[0] * SIDE), (int) Math.round(p.getZ() + f[1] * SIDE));
        }

        List<SpireSites.Site> sites = SpireSites.choose(nodes, x, z, h, seed, AscentLayout.shrineNode(rel));
        SpireSites.Site temple = sites.getFirst();
        return new Plan(altar, melody, nodes, temple.origin(), temple.facing(), seed, in, mid, out, List.copyOf(sites.subList(1, sites.size())));
    }

    /** The outward unit vector (away from the peak, across the stair) at node i, from the stair's drawing frame. */
    private static double[] frame(List<int[]> rel, int i) {
        int[] a = rel.get(Math.max(0, i - 1)), b = rel.get(Math.min(rel.size() - 1, i + 1));
        double tx = b[0] - a[0], tz = b[2] - a[2], len = Math.max(1e-6, Math.hypot(tx, tz));
        double ox = -tz / len, oz = tx / len;
        int[] p = rel.get(i);
        if (p[0] * ox + p[2] * oz < 0) {
            ox = -ox;
            oz = -oz;
        }
        return new double[]{ox, oz};
    }

    /** The path's nodes relative to the altar. */
    public static List<int[]> relative(List<BlockPos> path, BlockPos altar) {
        List<int[]> out = new ArrayList<>(path.size());
        for (BlockPos p : path) out.add(new int[]{p.getX() - altar.getX(), p.getY() - altar.getY(), p.getZ() - altar.getZ()});
        return out;
    }

    /** The ascent's input (altar coordinates) for a plan. */
    public static AscentLayout.Input ascentInput(Plan plan) {
        int ay = plan.altar().getY();
        int n = plan.path().size();
        int[] in = new int[n], mid = new int[n], out = new int[n];
        for (int i = 0; i < n; i++) {
            in[i] = plan.groundIn()[i] - ay;
            mid[i] = plan.groundMid()[i] - ay;
            out[i] = plan.groundOut()[i] - ay;
        }
        return new AscentLayout.Input(relative(plan.path(), plan.altar()), in, mid, out, plan.seed());
    }

    // --- piece boxes ----------------------------------------------------------------------------------------------------------

    public static BoundingBox summitBox(Plan plan) {
        BlockPos a = plan.altar();
        int r = PLATFORM_RADIUS + 1 + BATTER_REACH;
        return new BoundingBox(a.getX() - r, a.getY() - 6 - SpireKit.FOOTING_LIMIT, a.getZ() - r, a.getX() + r, a.getY() + 17, a.getZ() + r);
    }

    public static BoundingBox ascentBox(Plan plan) {
        Box b = AscentLayout.reach(relative(plan.path(), plan.altar()));
        BlockPos a = plan.altar();
        return new BoundingBox(a.getX() + b.x0() - BATTER_REACH, a.getY() + b.y0() - SpireKit.FOOTING_LIMIT, a.getZ() + b.z0() - BATTER_REACH,
                a.getX() + b.x1() + BATTER_REACH, a.getY() + b.y1(), a.getZ() + b.z1() + BATTER_REACH);
    }

    public static BoundingBox templeBox(Plan plan) {
        return siteBox(plan.temple());
    }

    public static BoundingBox siteBox(SpireSites.Site s) {
        int[] r = SpireSites.rect(s);
        int y = s.origin().getY();
        int below = s.kind() == SpireSites.Kind.MONASTERY ? -org.papiricoh.supernaturalcraft.structure.spire.MonasteryLayout.PODIUM : 4;
        int m = 1 + BATTER_REACH;
        return new BoundingBox(r[0] - m, y - below - SpireKit.FOOTING_LIMIT, r[1] - m, r[2] + m, y + PART_HEIGHT, r[3] + m);
    }

    /** The floor of the summit platform (its top layer). */
    public static int floorY(Plan plan) {
        return plan.altar().getY() - 1;
    }

    /** Where the temple window's pane for note {@code i} of the hymn is (read left to right from inside), row 3. */
    public static BlockPos windowPane(Plan plan, int i) {
        return part(plan.temple(), 2 - i * 2, 3, TempleLayout.BACK);
    }

    /** Where the temple's lectern (with the hymn book) stands. */
    public static BlockPos lectern(Plan plan) {
        return part(plan.temple(), 0, 0, TempleLayout.LECTERN);
    }

    /** A cell of a part given in its drawn frame (door at the origin facing north), in the world. */
    public static BlockPos part(SpireSites.Site s, int x, int y, int z) {
        int[] t = Canvas.turn(x, z, s.turn());
        return s.origin().offset(t[0], y, t[1]);
    }
}
