package org.papiricoh.supernaturalcraft.buildkit;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom trees (pure), grown like a builder sculpts them rather than as vanilla's lollipops: flared trunks with exposed roots,
 * log branches whose {@code axis} follows their direction, clouds of leaves shaped by noise, every leaf persistent and within
 * reach of wood. {@code (x, y, z)} is the trunk's first cell: the air cell right above the ground block it grows from. Leaves
 * and branches only fill air or unplanned cells, so a tree never eats a wall; roots replace the soil round the trunk.
 *
 * <p>{@code size} 0–3 scales a species; {@code seed} varies it. Each method returns the tree's leaf cells' bounding box size
 * (its crown radius) so callers can space trees.
 */
public final class Trees {

    private Trees() {
    }

    // --- species -----------------------------------------------------------------------------------------------------------------

    /** A storybook oak: a flared trunk with roots, 3–5 rising branches, a broad lumpy crown. */
    public static int oak(Canvas c, int x, int y, int z, int size, int seed) {
        Wood w = Wood.OAK;
        int h = 5 + size + Noise.pick(x, y, z, seed, 2);
        trunk(c, x, y, z, h, w, size >= 2, seed);
        roots(c, x, y, z, w, 2 + size / 2, seed);
        int branches = 3 + Math.min(2, size);
        double turn = Noise.hash01(x, 0, z, seed + 1) * Math.PI * 2;
        double r = 2.6 + size * 0.55;
        for (int b = 0; b < branches; b++) {
            double a = turn + b * Math.PI * 2 / branches + (Noise.hash01(b, 1, 0, seed) - 0.5) * 0.8;
            int from = y + h / 2 + Noise.pick(b, 2, 0, seed, Math.max(1, h / 2 - 1));
            double len = 2.5 + size * 0.8 + Noise.hash01(b, 3, 0, seed) * 1.5;
            double ex = x + Math.cos(a) * len, ez = z + Math.sin(a) * len, ey = from + 1.5 + Noise.hash01(b, 4, 0, seed) * 2;
            line(c, x, from, z, ex, ey, ez, w);
            crown(c, ex, ey + 0.8, ez, r * 0.85, 0.7, w.leavesState(), seed + b * 7);
        }
        crown(c, x, y + h + 0.5, z, r, 0.75, w.leavesState(), seed + 99);
        drip(c, x, y + h, z, (int) (r + 2), w.leavesState(), seed);
        return (int) Math.ceil(r + len(size));
    }

    private static double len(int size) {
        return 2.5 + size * 0.8 + 1.5;
    }

    /** A cherry: a short trunk splitting into 2–3 leaning arms under wide, flat, drooping pink canopies; petals beneath. */
    public static int cherry(Canvas c, int x, int y, int z, int size, int seed) {
        Wood w = Wood.CHERRY;
        int h = 3 + size / 2 + Noise.pick(x, y, z, seed, 2);
        trunk(c, x, y, z, h, w, false, seed);
        roots(c, x, y, z, w, 1 + size / 2, seed);
        int arms = 2 + (size >= 2 ? 1 : 0);
        double turn = Noise.hash01(x, 0, z, seed + 1) * Math.PI * 2;
        double r = 2.6 + size * 0.45;
        for (int b = 0; b < arms; b++) {
            double a = turn + b * Math.PI * 2 / arms + (Noise.hash01(b, 1, 0, seed) - 0.5) * 0.6;
            double len = 3 + size * 0.7;
            double ex = x + Math.cos(a) * len, ez = z + Math.sin(a) * len, ey = y + h + 2 + Noise.hash01(b, 4, 0, seed) * 1.5;
            line(c, x, y + h - 1, z, ex, ey, ez, w);
            crown(c, ex, ey + 1, ez, r, 0.4, w.leavesState(), seed + b * 7);
            hangingStrands(c, ex, ey + 1, ez, r, w.leavesState(), null, seed + b, 2);
        }
        petalsBelow(c, x, y, z, (int) (r + 3), seed);
        return (int) Math.ceil(r + 3 + size * 0.7);
    }

    /** A birch: a tall straight white trunk, a narrow tall crown. */
    public static int birch(Canvas c, int x, int y, int z, int size, int seed) {
        Wood w = Wood.BIRCH;
        int h = 6 + size + Noise.pick(x, y, z, seed, 3);
        trunk(c, x, y, z, h, w, false, false, seed);
        double r = 1.7 + size * 0.3;
        blob(c, x, y + h - 2, z, r, 2.6 + size * 0.4, r, w.leavesState(), seed, 0.3);
        blob(c, x, y + h + 0.5, z, r * 0.7, 1.2, r * 0.7, w.leavesState(), seed + 1, 0.2);
        for (int k = 0; k < 2; k++) {
            double a = Noise.hash01(k, 7, x, seed) * Math.PI * 2;
            int from = y + h - 3 - k;
            line(c, x, from, z, x + Math.cos(a) * 1.6, from + 1, z + Math.sin(a) * 1.6, w);
        }
        return (int) Math.ceil(r + 1);
    }

    /** A spruce: a tall trunk in tiers of leaves that shrink toward a spire, alternating wide and narrow. */
    public static int spruce(Canvas c, int x, int y, int z, int size, int seed) {
        Wood w = Wood.SPRUCE;
        int h = 9 + size * 2 + Noise.pick(x, y, z, seed, 3);
        trunk(c, x, y, z, h - 1, w, size >= 3, false, seed);
        roots(c, x, y, z, w, 1 + size / 2, seed);
        double maxR = 2.8 + size * 0.7;
        String leaves = w.leavesState();
        for (int yy = y + 2; yy <= y + h + 1; yy++) {
            double t = (yy - (y + 2)) / (double) (h - 1);
            double r = maxR * (1 - t) + ((yy - y) % 2 == 0 ? 0.6 : -0.3);
            if (yy >= y + h) r = 0.6;
            disc(c, x, yy, z, Math.max(0.6, r), leaves, seed + yy);
        }
        c.setIfAir(x, y + h + 2, z, leaves);
        return (int) Math.ceil(maxR + 0.6);
    }

    /**
     * A dead oak: a gnarled bare trunk of dark wood with twisting branches and twigs; {@code struck} splits and chars its top
     * (lightning).
     */
    public static int deadOak(Canvas c, int x, int y, int z, int size, boolean struck, int seed) {
        Wood w = Wood.DARK_OAK;
        int h = 5 + size + Noise.pick(x, y, z, seed, 2);
        trunk(c, x, y, z, h, w, size >= 2, seed);
        roots(c, x, y, z, w, 2 + size / 2, seed);
        int branches = 3 + Math.min(2, size);
        double turn = Noise.hash01(x, 0, z, seed + 1) * Math.PI * 2;
        for (int b = 0; b < branches; b++) {
            double a = turn + b * Math.PI * 2 / branches + (Noise.hash01(b, 1, 0, seed) - 0.5);
            int from = y + h / 2 + Noise.pick(b, 2, 0, seed, Math.max(1, h / 2));
            double len = 2 + size * 0.7 + Noise.hash01(b, 3, 0, seed) * 2;
            double ex = x + Math.cos(a) * len, ez = z + Math.sin(a) * len, ey = from + 2 + Noise.hash01(b, 4, 0, seed) * 3;
            line(c, x, from, z, ex, ey, ez, w);
            double a2 = a + (Noise.hash01(b, 5, 0, seed) - 0.5) * 1.6;
            line(c, ex, ey, ez, ex + Math.cos(a2) * 1.2, ey + 2, ez + Math.sin(a2) * 1.2, w);
        }
        if (struck) {
            for (int k = 0; k < 3; k++) c.set(x, y + h - k, z, k == 0 ? "minecraft:air" : St.axis("minecraft:basalt", "y"));
            c.set(x + 1, y + h, z, St.axis("minecraft:basalt", "y"));
            c.set(x - 1, y + h + 1, z, St.axis("minecraft:polished_basalt", "y"));
            c.set(x - 1, y + h, z, St.axis("minecraft:basalt", "y"));
        }
        return (int) Math.ceil(4 + size * 0.7);
    }

    /** A willow: a leaning trunk under a wide dome whose rim hangs in long strands of leaves ending in vines. */
    public static int willow(Canvas c, int x, int y, int z, int size, int seed) {
        Wood w = Wood.OAK;
        int h = 5 + size + Noise.pick(x, y, z, seed, 2);
        trunk(c, x, y, z, h, w, size >= 2, seed);
        roots(c, x, y, z, w, 2, seed);
        double r = 3.4 + size * 0.7;
        String leaves = St.leaves("minecraft:oak_leaves");
        for (int b = 0; b < 4; b++) {
            double a = b * Math.PI / 2 + Noise.hash01(b, 1, x, seed);
            line(c, x, y + h - 1, z, x + Math.cos(a) * r * 0.6, y + h + 0.5, z + Math.sin(a) * r * 0.6, w);
        }
        crown(c, x, y + h + 1, z, r, 0.5, leaves, seed);
        hangingStrands(c, x, y + h + 1, z, r, leaves, "vine", seed, 4);
        return (int) Math.ceil(r + 1);
    }

    /** A bush: a stub of wood in a rounded mound of leaves ({@code leaves}: a leaves id, e.g. azalea or flowering azalea). */
    public static void bush(Canvas c, int x, int y, int z, String leaves, double radius, int seed) {
        c.setIfAir(x, y, z, St.log("minecraft:oak_log"));
        blob(c, x, y + radius * 0.4, z, radius + 0.3, radius * 0.8, radius + 0.3, St.leaves(leaves), seed, 0.35);
    }

    /** A clipped hedge over cells {x, z} standing on {@code y}, {@code height} tall, the top rounded by noise. */
    public static void hedge(Canvas c, List<int[]> cells, int y, int height, String leaves, int seed) {
        String s = St.leaves(leaves);
        for (int[] p : cells) {
            int hh = height - (Noise.chance(p[0], 0, p[1], seed, 0.2) ? 1 : 0);
            for (int k = 0; k < hh; k++) c.setIfAir(p[0], y + k, p[1], s);
        }
    }

    // --- parts ---------------------------------------------------------------------------------------------------------------

    /**
     * A trunk {@code h} tall from (x, y, z): upright logs, leaning a cell at most once on the way up, with a flare of logs round
     * its foot (two rows, or three on a thick one).
     */
    static void trunk(Canvas c, int x, int y, int z, int h, Wood w, boolean thick, int seed) {
        trunk(c, x, y, z, h, w, thick, true, seed);
    }

    static void trunk(Canvas c, int x, int y, int z, int h, Wood w, boolean thick, boolean mayLean, int seed) {
        int lean = mayLean ? Noise.pick(x, y, z, seed + 5, 6) : 9;
        Dir leanDir = lean < 4 ? Dir.HORIZONTAL[lean] : null;
        int leanAt = h / 2 + 1;
        int cx = x, cz = z;
        for (int k = 0; k <= h; k++) {
            if (leanDir != null && k == leanAt) {
                // A side step: the log under the step and the one beside it, so the trunk stays face-connected.
                c.set(cx + leanDir.dx, y + k - 1, cz + leanDir.dz, w.log(leanDir.axis()));
                cx += leanDir.dx;
                cz += leanDir.dz;
            }
            c.set(cx, y + k, cz, w.log("y"));
        }
        int flare = thick ? 3 : 1;
        for (Dir d : Dir.HORIZONTAL) {
            int fh = Noise.pick(x + d.dx, y, z + d.dz, seed + 11, flare + 1);
            for (int k = 0; k < fh; k++) c.setIfAir(x + d.dx, y + k, z + d.dz, w.wood("y"));
        }
        if (thick) {
            for (int[] o : new int[][]{{1, 1}, {1, -1}, {-1, 1}, {-1, -1}}) {
                if (Noise.chance(x + o[0], y, z + o[1], seed + 12, 0.6)) c.setIfAir(x + o[0], y, z + o[1], w.wood("y"));
            }
        }
    }

    /** Roots: logs lying along the ground away from the trunk's foot, dipping into the soil at their ends; rooted dirt round them. */
    static void roots(Canvas c, int x, int y, int z, Wood w, int reach, int seed) {
        for (Dir d : Dir.HORIZONTAL) {
            if (!Noise.chance(x, d.ordinal(), z, seed + 21, 0.75)) continue;
            int len = 1 + Noise.pick(x, d.ordinal(), z, seed + 22, reach);
            for (int k = 2; k <= len + 1; k++) {
                int rx = x + d.dx * k, rz = z + d.dz * k;
                int ry = k == len + 1 ? y - 1 : y;
                if (ry == y && !c.isAir(rx, y, rz)) break;
                c.set(rx, ry, rz, w.log(d.axis()));
            }
        }
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                String below = c.get(x + dx, y - 1, z + dz);
                if (below != null && St.path(below).equals("grass_block") && Noise.chance(x + dx, y, z + dz, seed + 23, 0.45)) {
                    c.set(x + dx, y - 1, z + dz, "minecraft:rooted_dirt");
                }
            }
        }
    }

    /** Logs from one point to another, face-connected, each log's axis along the step that placed it. */
    public static void line(Canvas c, double x0, double y0, double z0, double x1, double y1, double z1, Wood w) {
        int px = (int) Math.round(x0), py = (int) Math.round(y0), pz = (int) Math.round(z0);
        double dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
        int steps = (int) Math.ceil(Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz))) * 2) + 1;
        for (int i = 1; i <= steps; i++) {
            double t = i / (double) steps;
            int qx = (int) Math.round(x0 + dx * t), qy = (int) Math.round(y0 + dy * t), qz = (int) Math.round(z0 + dz * t);
            // Move one axis at a time so every log touches the last by a face.
            while (px != qx || py != qy || pz != qz) {
                String axis;
                if (px != qx) {
                    px += Integer.signum(qx - px);
                    axis = "x";
                } else if (pz != qz) {
                    pz += Integer.signum(qz - pz);
                    axis = "z";
                } else {
                    py += Integer.signum(qy - py);
                    axis = "y";
                }
                // Bark on every side (wood, not log): a branch's end grain never shows where it turns.
                if (c.isAir(px, py, pz) || Kinds.leaves(c.get(px, py, pz))) c.set(px, py, pz, w.wood(axis));
            }
        }
    }

    /**
     * A cumulus crown: a core blob and 5–8 smaller blobs pushed out over its upper half and sides, so the outline is lumpy at
     * every scale ({@code flat} squashes it vertically: 1 round, 0.45 a flat cherry canopy). Fills only air.
     */
    public static void crown(Canvas c, double cx, double cy, double cz, double r, double flat, String leaves, int seed) {
        blob(c, cx, cy, cz, r * 0.8, r * 0.8 * flat, r * 0.8, leaves, seed, 0.3);
        int n = 5 + Noise.pick((int) cx, (int) cy, (int) cz, seed, 4);
        double turn = Noise.hash01((int) cx, 3, (int) cz, seed) * Math.PI * 2;
        for (int i = 0; i < n; i++) {
            double a = turn + i * Math.PI * 2 / n + (Noise.hash01(i, 1, 0, seed) - 0.5) * 0.9;
            double up = (Noise.hash01(i, 2, 0, seed) - 0.3) * 0.9;
            double dist = r * (0.45 + 0.25 * Noise.hash01(i, 3, 0, seed));
            double sr = r * (0.45 + 0.2 * Noise.hash01(i, 4, 0, seed));
            blob(c, cx + Math.cos(a) * dist, cy + up * r * flat, cz + Math.sin(a) * dist, sr, sr * Math.max(0.4, flat + 0.1), sr, leaves, seed + 17 * i, 0.45);
        }
    }

    /** An ellipsoid of leaves with a noisy surface ({@code rough} 0–1), flattened underneath; fills only air. */
    public static void blob(Canvas c, double cx, double cy, double cz, double rx, double ry, double rz, String leaves, int seed, double rough) {
        int x0 = (int) Math.floor(cx - rx - 1), x1 = (int) Math.ceil(cx + rx + 1);
        int y0 = (int) Math.floor(cy - ry - 1), y1 = (int) Math.ceil(cy + ry + 1);
        int z0 = (int) Math.floor(cz - rz - 1), z1 = (int) Math.ceil(cz + rz + 1);
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    double ux = (x - cx) / rx, uy = (y - cy) / (y < cy ? ry * 0.75 : ry), uz = (z - cz) / rz;
                    double d = ux * ux + uy * uy + uz * uz;
                    double n = (Noise.value3(x, y, z, 2.2, seed) - 0.5) * 2 * rough + (Noise.hash01(x, y, z, seed + 1) - 0.5) * rough * 0.5;
                    if (d < 1 + n) c.setIfAir(x, y, z, leaves);
                }
            }
        }
    }

    /** A flat disc of leaves (a spruce tier) with a ragged rim. */
    static void disc(Canvas c, int x, int y, int z, double r, String leaves, int seed) {
        int ir = (int) Math.ceil(r) + 1;
        for (int dx = -ir; dx <= ir; dx++) {
            for (int dz = -ir; dz <= ir; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz) + (Noise.hash01(x + dx, y, z + dz, seed) - 0.5) * 0.9;
                if (d <= r) c.setIfAir(x + dx, y, z + dz, leaves);
            }
        }
    }

    /** Single leaves hanging under a crown's rim: they break its flat underside. */
    static void drip(Canvas c, int x, int y, int z, int r, String leaves, int seed) {
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int yy = y + 6; yy >= y - 2; yy--) {
                    String s = c.get(x + dx, yy, z + dz);
                    if (s != null && Kinds.leaves(s)) {
                        if (c.isAir(x + dx, yy - 1, z + dz) && Noise.chance(x + dx, yy, z + dz, seed + 31, 0.18)) c.set(x + dx, yy - 1, z + dz, leaves);
                        break;
                    }
                }
            }
        }
    }

    /** Strands of leaves hanging from the underside of a crown's outer ring, up to {@code maxLen} long, ending in a vine. */
    static void hangingStrands(Canvas c, double cx, double cy, double cz, double r, String leaves, String vineTip, int seed, int maxLen) {
        double share = vineTip == null ? 0.25 : 0.45;
        int ir = (int) Math.ceil(r) + 1;
        int x0 = (int) Math.round(cx), z0 = (int) Math.round(cz), ytop = (int) Math.round(cy) + 2;
        for (int dx = -ir; dx <= ir; dx++) {
            for (int dz = -ir; dz <= ir; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d < r * 0.55 || !Noise.chance(x0 + dx, 0, z0 + dz, seed + 41, share)) continue;
                int x = x0 + dx, z = z0 + dz;
                int yy = ytop;
                while (yy > ytop - 8 && !(c.get(x, yy, z) != null && Kinds.leaves(c.get(x, yy, z)))) yy--;
                if (yy <= ytop - 8) continue;
                while (Kinds.leaves(c.get(x, yy - 1, z))) yy--;
                int len = 1 + Noise.pick(x, 0, z, seed + 42, maxLen);
                int last = yy;
                for (int k = 1; k <= len; k++) {
                    if (!c.isAir(x, yy - k, z)) break;
                    c.set(x, yy - k, z, leaves);
                    last = yy - k;
                }
                if (vineTip != null && last < yy) {
                    // A vine clings to the side of the strand's last leaf.
                    Dir side = Dir.HORIZONTAL[Noise.pick(x, 1, z, seed + 43, 4)];
                    int vx = x + side.dx, vz = z + side.dz;
                    if (c.isAir(vx, last, vz)) c.set(vx, last, vz, St.vine(side.opposite()));
                }
            }
        }
    }

    /** Pink petals on soil round a tree, where the ground is open. */
    static void petalsBelow(Canvas c, int x, int y, int z, int r, int seed) {
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > r * r || !Noise.chance(x + dx, 0, z + dz, seed + 51, 0.35)) continue;
                for (int yy = y + 2; yy >= y - 4; yy--) {
                    String s = c.get(x + dx, yy, z + dz);
                    if (s == null || Kinds.air(s)) continue;
                    if (Kinds.soil(s) && c.isAir(x + dx, yy + 1, z + dz) && !St.path(s).equals("gravel") && !St.path(s).startsWith("sand")) {
                        c.set(x + dx, yy + 1, z + dz, St.petals(1 + Noise.pick(x + dx, 1, z + dz, seed, 4), Dir.HORIZONTAL[Noise.pick(x + dx, 2, z + dz, seed, 4)]));
                    }
                    break;
                }
            }
        }
    }

    /** The cells of a tree's leaves within {@code r} of (x, y, z) (for tests). */
    public static List<int[]> leavesNear(Canvas c, int x, int y, int z, int r) {
        List<int[]> out = new ArrayList<>();
        for (int dx = -r; dx <= r; dx++) for (int dy = -r; dy <= 2 * r; dy++) for (int dz = -r; dz <= r; dz++) {
            if (Kinds.leaves(c.get(x + dx, y + dy, z + dz))) out.add(new int[]{x + dx, y + dy, z + dz});
        }
        return out;
    }
}
