package org.papiricoh.supernaturalcraft.chain.seat;

import org.papiricoh.supernaturalcraft.buildkit.*;
import org.papiricoh.supernaturalcraft.chain.ChainFaction;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** Pure faction seats. All anchors are unobstructed feet cells; no block entities are used. */
public final class SeatLayoutCatalog implements LayoutCatalog {
    public static final int RADIUS = 12, HEIGHT = 12, DEPTH = 4;
    public static final String LORD = "lord", ENTRANCE = "entrance";
    public static final List<String> LIEUTENANTS = List.of("lieutenant_1", "lieutenant_2", "lieutenant_3");

    @Override
    public Map<String, Supplier<LayoutPlan>> layouts() {
        Map<String, Supplier<LayoutPlan>> out = new LinkedHashMap<>();
        out.put("chain_pit_court", () -> plan(ChainFaction.DEMON));
        out.put("chain_garrison", () -> plan(ChainFaction.ANGEL));
        return out;
    }

    public static LayoutPlan plan(ChainFaction faction) {
        return canvas(faction).toPlan();
    }

    public static Canvas canvas(ChainFaction faction) {
        boolean pit = faction == ChainFaction.DEMON;
        Canvas c = new Canvas(pit ? "chain_pit_court" : "chain_garrison");
        Family trim = pit ? Family.POLISHED_BLACKSTONE_BRICKS : Family.SMOOTH_QUARTZ;
        Brush wall = pit ? Palette.mix(71, "blackstone", 3, "polished_blackstone_bricks", 3,
                "cracked_polished_blackstone_bricks", 2, "basalt", 1)
                : Palette.checker(2, Brush.of("calcite"), Brush.of("white_concrete"));
        Brush floor = Palette.checker(2, Brush.of(pit ? "polished_blackstone" : "smooth_stone"),
                Brush.of(pit ? "polished_basalt" : "quartz_bricks"));
        c.fill(new Box(-12, -4, -12, 12, -1, 12), wall);
        c.fill(new Box(-12, -1, -12, 12, -1, 12), floor);
        c.carve(new Box(-12, 0, -12, 12, HEIGHT, 12));
        Box perimeter = new Box(-11, 0, -11, 11, 5, 11);
        Walls.enclose(c, perimeter, 0, 4, wall);
        for (Walls.Run r : Walls.around(perimeter)) {
            Walls.belt(c, r, 4, trim);
            // Buttresses give the enclosure a load-bearing rhythm and a deep silhouette.
            for (int i = 2; i < r.length() - 1; i += 5) {
                int x = r.x(i) + r.out().dx, z = r.z(i) + r.out().dz;
                c.fill(new Box(x, 0, z, x, 5, z), pit ? "polished_basalt" : "quartz_pillar");
                c.set(x, 6, z, trim.slabBottom());
            }
        }
        // The northern hall holds the throne; three arcaded bays face the open court.
        hall(c, new Box(-10, 0, -10, 10, 5, -5), wall, trim, pit, true);
        hall(c, new Box(-10, 0, -3, -6, 4, 3), wall, trim, pit, false);
        hall(c, new Box(6, 0, -3, 10, 4, 3), wall, trim, pit, false);
        hall(c, new Box(6, 0, 5, 10, 4, 9), wall, trim, pit, false);
        c.carve(new Box(-6, 0, -1, -6, 2, 1));
        c.carve(new Box(6, 0, -1, 6, 2, 1));
        c.carve(new Box(6, 0, 6, 6, 2, 8));
        c.carve(new Box(-2, 0, -5, 2, 3, -5));
        c.fill(new Box(-3, 0, -9, 3, 0, -6), floor);
        for (int x = -2; x <= 2; x++) c.set(x, 0, -5, trim.stairs(Dir.NORTH, false));
        Furniture.chair(c, 0, 1, -9, Dir.SOUTH, trim.stairs(), null);
        c.fill(new Box(-1, 1, -10, 1, 4, -10), pit ? "crying_obsidian" : "chiseled_quartz_block");
        for (int x : new int[]{-2, 2}) {
            c.fill(new Box(x, 1, -9, x, 2, -9), trim.block());
            c.set(x, 3, -9, pit ? "shroomlight" : "sea_lantern");
        }
        c.anchor(LORD, 0, 1, -7);
        c.anchor(LIEUTENANTS.get(0), -8, 0, 0);
        c.anchor(LIEUTENANTS.get(1), 8, 0, 0);
        c.anchor(LIEUTENANTS.get(2), 8, 0, 7);
        // A broad, unroofed entrance descends into the demonic court.
        c.carve(new Box(-2, 0, 10, 2, 8, 12));
        if (pit) {
            for (int z = 10; z <= 12; z++) {
                c.fill(new Box(-2, 0, z, 2, z - 10, z), wall);
                for (int x = -2; x <= 2; x++) c.set(x, z - 10, z, trim.stairs(Dir.SOUTH, false));
            }
            c.anchor(ENTRANCE, 0, 3, 12);
            trap(c);
            c.carve(new Box(-8, 6, -9, -6, 9, -7));
            c.carve(new Box(-7, 5, -10, -6, 8, -9));
            // Broken wall crowns and isolated rubble keep the court visibly ruined.
            for (int z : new int[]{-3, 3, 7}) c.carve(new Box(-11, 3, z, -11, 4, z + 1));
            for (int x : new int[]{-4, 4}) for (int z : new int[]{-3, 5}) {
                c.set(x, 0, z, "soul_soil");
                c.set(x, 1, z, "soul_fire");
            }
            for (int x : new int[]{-4, 4}) {
                c.fill(new Box(x, 2, -6, x, 5, -6), "chain[axis=y]");
                c.set(x, 1, -6, "soul_lantern[hanging=true]");
            }
        } else {
            c.anchor(ENTRANCE, 0, 0, 12);
            // A quiet cruciform paving axis, with low stone benches around the open sky.
            c.fill(new Box(-1, -1, -4, 1, -1, 10), "polished_diorite");
            c.fill(new Box(-5, -1, 1, 5, -1, 2), "polished_diorite");
            c.fill(new Box(0, -1, 1, 1, -1, 2), "chiseled_quartz_block");
            for (int z : new int[]{0, 5, 8}) Furniture.chair(c, -4, 0, z, Dir.EAST, trim.stairs(), null);
        }
        for (int x : new int[]{-4, 4}) for (int z : new int[]{-2, 3, 8})
            c.set(x, -1, z, pit ? "shroomlight" : "sea_lantern");
        for (int x : new int[]{-10, 10}) for (int z : new int[]{-4, 4, 10})
            c.set(x, -1, z, pit ? "shroomlight" : "sea_lantern");
        // Recessed perimeter lights illuminate the buttress niches below the cornice.
        for (int i = -12; i <= 12; i += 3) {
            for (int edge : new int[]{-12, 12}) {
                c.set(i, -1, edge, pit ? "shroomlight" : "sea_lantern");
                c.set(edge, -1, i, pit ? "shroomlight" : "sea_lantern");
            }
        }
        return c.finish();
    }

    private static void hall(Canvas c, Box b, Brush wall, Family trim, boolean pit, boolean throne) {
        int top = throne ? 5 : 4;
        Walls.enclose(c, b, 0, top, wall);
        for (Walls.Run r : Walls.around(b)) Walls.belt(c, r, top, trim);
        Roofs.gable(c, b, top, throne, new Roofs.Style(trim, trim, wall, 0,
                Roofs.Pitch.HALF, 0, true, null, false));
        // Recessed windows and luminous transoms are stone and glass, never block entities.
        for (int x = b.x0() + 2; x < b.x1(); x += 3) {
            c.set(x, 2, b.z0(), pit ? "crying_obsidian" : "glass");
            c.set(x, 3, b.z0(), pit ? "shroomlight" : "sea_lantern");
        }
        for (int x = b.x0() + 1; x < b.x1(); x += 3)
            for (int z = b.z0() + 1; z < b.z1(); z += 3)
                c.set(x, top, z, pit ? "shroomlight" : "sea_lantern");
        if (!throne) Furniture.chair(c, b.x1() - 1, 0, b.z0() + 1, Dir.SOUTH, trim.stairs(), null);
    }

    private static void trap(Canvas c) {
        int[][] vertices = new int[5][2];
        for (int i = 0; i < 5; i++) {
            double a = -Math.PI / 2 + i * 2 * Math.PI / 5;
            vertices[i] = new int[]{(int) Math.round(Math.cos(a) * 4), 2 + (int) Math.round(Math.sin(a) * 4)};
        }
        for (int x = -5; x <= 5; x++) for (int z = -3; z <= 7; z++) {
            double r = Math.hypot(x, z - 2);
            if (r >= 4.5 && r <= 5.3) c.set(x, -1, z, "crying_obsidian");
        }
        for (int i = 0; i < 5; i++) {
            int[] a = vertices[i], b = vertices[(i + 2) % 5];
            int steps = Math.max(Math.abs(a[0] - b[0]), Math.abs(a[1] - b[1]));
            for (int j = 0; j <= steps; j++) c.set(Math.round(a[0] + (b[0] - a[0]) * j / (float) steps), -1,
                    Math.round(a[1] + (b[1] - a[1]) * j / (float) steps), "red_nether_bricks");
        }
    }
}
