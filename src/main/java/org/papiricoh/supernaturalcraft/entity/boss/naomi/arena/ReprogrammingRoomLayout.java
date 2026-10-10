package org.papiricoh.supernaturalcraft.entity.boss.naomi.arena;

import org.papiricoh.supernaturalcraft.buildkit.Box;
import org.papiricoh.supernaturalcraft.buildkit.Brush;
import org.papiricoh.supernaturalcraft.buildkit.Canvas;
import org.papiricoh.supernaturalcraft.buildkit.Dir;
import org.papiricoh.supernaturalcraft.buildkit.Family;
import org.papiricoh.supernaturalcraft.buildkit.Palette;
import org.papiricoh.supernaturalcraft.buildkit.St;
import org.papiricoh.supernaturalcraft.buildkit.Walls;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.ArenaCell;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;
import org.papiricoh.supernaturalcraft.layout.LayoutPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Naomi's reprogramming room (v0.18, pure): a sterile white clinic in the wing of a hunter's Heaven. Its origin is the room's
 * floor centre ({@code y = 0} first air above the floor), placed at {@code HeavenPlotLayout.WING_ORIGIN}. The permanent room is
 * written with the plot; the fight changes it only through its arena ({@link #phaseCells}), which restores it afterwards.
 * <p><b>Contract</b> fixed by the foundations: the points below; the room must keep {@link #RADIUS} and {@link #HEIGHT}. No block
 * entities anywhere (the room is also written through an arena when she is summoned far from a Heaven).
 * <p>The build (buildkit): a 35 × 35 hall, two-thick walls of white concrete and smooth quartz between quartz pilasters, a
 * light-grey plinth, a band of pale blue observation glass and a quartz cornice; a coffered ceiling whose white diffusers hide sea
 * lanterns (in phase 2 they glow red); a checkered terrazzo floor with a cyan ring round the test floor and an inlaid pad under
 * each chair; the console desk with dark screens before the lift's quartz portal; glass screens round the chair bays and the
 * waiting area (they retract in phase 2); lit niches for the guards.
 */
public final class ReprogrammingRoomLayout {

    /** Where the hunter comes in from the plot (the wing's door side, south). */
    public static final LayoutPoint ENTRY = new LayoutPoint(0, 0, 15);
    /** Where Naomi waits and walks back to. */
    public static final LayoutPoint NAOMI_SPOT = new LayoutPoint(0, 0, -9);
    /** The two reprogramming chairs (the chair entity stands here, facing south). */
    public static final List<LayoutPoint> CHAIRS = List.of(new LayoutPoint(-9, 0, -5), new LayoutPoint(9, 0, -5));
    /** The recalibration console (a block). */
    public static final LayoutPoint CONSOLE = new LayoutPoint(0, 0, -14);
    /** Her guards' alcoves (they step out from here). */
    public static final List<LayoutPoint> GUARD_SPAWNS = List.of(new LayoutPoint(-15, 0, -10), new LayoutPoint(15, 0, -10),
            new LayoutPoint(-15, 0, 6), new LayoutPoint(15, 0, 6));
    /** Where a training test's copies stand, on the flat test floor. */
    public static final List<LayoutPoint> COPY_SPOTS = List.of(new LayoutPoint(-6, 0, 2), new LayoutPoint(-3, 0, 4), new LayoutPoint(0, 0, 5),
            new LayoutPoint(3, 0, 4), new LayoutPoint(6, 0, 2), new LayoutPoint(0, 0, 0));
    /** The lift up to Zachariah's office (sealed until she falls). */
    public static final LayoutPoint ELEVATOR = new LayoutPoint(0, 0, -17);
    /** The room fits in this radius round its origin, and this height above its floor. */
    public static final int RADIUS = 20, HEIGHT = 10;

    /** The hall's inside reaches ±{@code IN}; the ceiling row. */
    static final int IN = 17, CEIL = 8;
    static final String CONSOLE_BLOCK = "supernaturalcraft:reprogramming_console";
    static final String DIFFUSER = "minecraft:white_stained_glass", ALARM = "minecraft:red_stained_glass";

    private ReprogrammingRoomLayout() {
    }

    /** The permanent room. */
    public static LayoutPlan plan() {
        return canvas().toPlan();
    }

    /** The room as a finished kit canvas (tests and the layout dump). */
    public static Canvas canvas() {
        Canvas c = new Canvas("naomi_reprogramming_room");
        c.zone("room", new Box(-RADIUS, -3, -RADIUS, RADIUS, HEIGHT - 1, RADIUS));
        c.inZone("room", () -> build(c));
        c.anchor("entry", ENTRY.x(), ENTRY.y(), ENTRY.z());
        c.anchor("naomi", NAOMI_SPOT.x(), NAOMI_SPOT.y(), NAOMI_SPOT.z());
        c.anchor("console", CONSOLE.x(), CONSOLE.y(), CONSOLE.z());
        c.anchor("elevator", ELEVATOR.x(), ELEVATOR.y(), ELEVATOR.z());
        for (int i = 0; i < CHAIRS.size(); i++) c.anchor("chair_" + i, CHAIRS.get(i).x(), 0, CHAIRS.get(i).z());
        for (int i = 0; i < GUARD_SPAWNS.size(); i++) c.anchor("guard_" + i, GUARD_SPAWNS.get(i).x(), 0, GUARD_SPAWNS.get(i).z());
        for (int i = 0; i < COPY_SPOTS.size(); i++) c.anchor("copy_" + i, COPY_SPOTS.get(i).x(), 0, COPY_SPOTS.get(i).z());
        return c.finish();
    }

    /** Large-format wall tiles: two-by-two checks of white concrete and smooth quartz. */
    static final Brush WALL = (x, y, z) -> ((Math.floorDiv(x + z, 2) + Math.floorDiv(y, 2)) & 1) == 0 ? "minecraft:white_concrete" : "minecraft:smooth_quartz";
    /** The outside: two-by-two checks of smooth quartz and calcite, a few quartz bricks. */
    static final Brush OUTER = ((Brush) (x, y, z) -> ((Math.floorDiv(x + z, 2) + Math.floorDiv(y, 2)) & 1) == 0 ? "minecraft:smooth_quartz" : "minecraft:calcite")
            .sprinkle(Brush.of("minecraft:quartz_bricks"), 0.1, 1502);

    private static void build(Canvas c) {
        int o = IN + 1, w = IN + 2;
        // The slab under the room and its floor: terrazzo in two-block checks, a light-grey border along the walls.
        for (int x = -w; x <= w; x++) {
            for (int z = -w; z <= w; z++) {
                c.set(x, -2, z, ((x + z) & 1) == 0 ? "minecraft:smooth_stone" : "minecraft:polished_andesite");
                boolean inside = Math.abs(x) <= IN && Math.abs(z) <= IN;
                String floor;
                if (!inside) floor = "minecraft:polished_andesite";
                else if (Math.abs(x) == IN || Math.abs(z) == IN) floor = "minecraft:light_gray_concrete";
                else floor = ((Math.floorDiv(x, 2) + Math.floorDiv(z, 2)) & 1) == 0 ? "minecraft:polished_diorite" : "minecraft:white_concrete";
                c.set(x, -1, z, floor);
                for (int y = 0; y < CEIL; y++) if (inside) c.carve(x, y, z);
            }
        }
        // The test floor's cyan ring and its pale centre; a pad under each chair.
        for (int x = -9; x <= 9; x++) {
            for (int z = -6; z <= 10; z++) {
                double d = Math.hypot(x, z - 2);
                if (d >= 7.5 && d < 8.5) c.set(x, -1, z, "minecraft:cyan_terracotta");
                else if (d < 1.5) c.set(x, -1, z, "minecraft:light_blue_concrete");
            }
        }
        for (LayoutPoint ch : CHAIRS) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    boolean edge = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                    c.set(ch.x() + dx, -1, ch.z() + dz, edge ? "minecraft:light_blue_terracotta" : "minecraft:smooth_quartz");
                }
            }
        }
        // Walls: two thick; inside a plinth, white panels between quartz pilasters, a band of glass high up on the long sides.
        Box inner = new Box(-o, 0, -o, o, 0, o), outer = new Box(-w, 0, -w, w, 0, w);
        for (Walls.Run r : Walls.around(outer)) Walls.body(c, r, 0, CEIL, OUTER);
        for (Walls.Run r : Walls.around(inner)) {
            Walls.body(c, r, 0, 0, Brush.of("minecraft:light_gray_concrete"));
            Walls.body(c, r, 1, CEIL - 1, WALL);
            int[] pil = Walls.pillarPositions(r, 6);
            for (int p : pil) for (int y = 0; y < CEIL; y++) c.set(r.x(p), y, r.z(p), St.axis("minecraft:quartz_pillar", "y"));
            if (r.out() == Dir.EAST || r.out() == Dir.WEST) {
                for (int k = 0; k + 1 < pil.length; k++) {
                    for (int i = pil[k] + 2; i <= pil[k + 1] - 2; i++) {
                        for (int y = 4; y <= 5; y++) {
                            c.set(r.x(i), y, r.z(i), St.pane("minecraft:light_blue_stained_glass_pane"));
                            c.set(r.x(i) + r.out().dx, y, r.z(i) + r.out().dz, "minecraft:light_blue_stained_glass");
                        }
                    }
                }
            }
        }
        // The cornice round the top of the hall (upside-down quartz stairs, backs to the wall; corners turn themselves).
        for (int x = -IN; x <= IN; x++) {
            c.set(x, CEIL - 1, -IN, Family.SMOOTH_QUARTZ.stairs(Dir.NORTH, true));
            c.set(x, CEIL - 1, IN, Family.SMOOTH_QUARTZ.stairs(Dir.SOUTH, true));
        }
        for (int z = -IN + 1; z < IN; z++) {
            c.set(-IN, CEIL - 1, z, Family.SMOOTH_QUARTZ.stairs(Dir.WEST, true));
            c.set(IN, CEIL - 1, z, Family.SMOOTH_QUARTZ.stairs(Dir.EAST, true));
        }
        // The coffered ceiling: quartz beams every fourth row, white diffusers over sea lanterns in the coffers, a roof over all.
        for (int x = -w; x <= w; x++) {
            for (int z = -w; z <= w; z++) {
                c.set(x, CEIL + 1, z, Family.SMOOTH_QUARTZ.slabBottom());
                if (Math.abs(x) > IN || Math.abs(z) > IN) {
                    c.set(x, CEIL, z, OUTER.at(x, CEIL, z));
                    continue;
                }
                boolean beam = Math.floorMod(x, 4) == 2 || Math.floorMod(z, 4) == 2;
                boolean light = Math.floorMod(x, 4) == 0 && Math.floorMod(z, 4) == 0;
                if (light) {
                    c.set(x, CEIL, z, DIFFUSER);
                    c.set(x, CEIL + 1, z, "minecraft:sea_lantern");
                } else {
                    c.set(x, CEIL, z, beam ? "minecraft:smooth_quartz" : "minecraft:white_concrete");
                }
            }
        }
        // The lift's portal in the north wall: quartz jambs, a chiselled head, the sealed doorway left as air.
        int ez = ELEVATOR.z();
        for (int y = 0; y <= 3; y++) {
            c.set(-2, y, ez, St.axis("minecraft:quartz_pillar", "y"));
            c.set(2, y, ez, St.axis("minecraft:quartz_pillar", "y"));
        }
        for (int x = -2; x <= 2; x++) c.set(x, 3, ez, x == 0 ? "minecraft:chiseled_quartz_block" : "minecraft:quartz_bricks");
        c.set(-1, 2, ez, Family.SMOOTH_QUARTZ.stairs(Dir.WEST, true));
        c.set(1, 2, ez, Family.SMOOTH_QUARTZ.stairs(Dir.EAST, true));
        for (int y = 0; y <= 1; y++) for (int x = -1; x <= 1; x++) c.carve(x, y, ez);
        // The console desk: the console between two quartz wings, dark screens on a quartz frame behind.
        int cz = CONSOLE.z();
        c.set(CONSOLE.x(), 0, cz, CONSOLE_BLOCK);
        c.set(-1, 0, cz, Family.SMOOTH_QUARTZ.stairs(Dir.NORTH, false));
        c.set(1, 0, cz, Family.SMOOTH_QUARTZ.stairs(Dir.NORTH, false));
        c.set(-2, 0, cz, Family.SMOOTH_QUARTZ.stairs(Dir.EAST, false));
        c.set(2, 0, cz, Family.SMOOTH_QUARTZ.stairs(Dir.WEST, false));
        for (int x = -3; x <= 3; x++) {
            boolean frame = Math.abs(x) == 3;
            for (int y = 0; y <= 2; y++) {
                String s = frame ? St.axis("minecraft:quartz_pillar", "y") : y == 0 ? "minecraft:smooth_quartz"
                        : x == 0 && y == 1 ? "minecraft:tinted_glass" : St.pane("minecraft:black_stained_glass_pane");
                c.set(x, y, cz - 1, s);
            }
            c.set(x, 3, cz - 1, Family.SMOOTH_QUARTZ.slabBottom());
        }
        // The chairs' bays: a surgical lamp over each (a chain, an end rod), instrument stands beside.
        for (LayoutPoint ch : CHAIRS) {
            for (int y = CEIL - 1; y >= 4; y--) c.set(ch.x(), y, ch.z(), St.chain("y"));
            c.set(ch.x(), 3, ch.z(), St.endRod(Dir.DOWN));
            int side = ch.x() < 0 ? -1 : 1;
            c.set(ch.x() + side * 3, 0, ch.z() - 1, "minecraft:cauldron");
            c.set(ch.x() + side * 3, 0, ch.z() + 1, St.rod("minecraft:lightning_rod", Dir.UP));
            c.set(ch.x() + side * 3, 1, ch.z() + 1, St.rod("minecraft:lightning_rod", Dir.UP));
        }
        // The guards' niches: a recess in the side wall framed in quartz, a cold light at its back.
        for (LayoutPoint g : GUARD_SPAWNS) {
            int side = g.x() < 0 ? -1 : 1, wx = side * o;
            for (int dz = -1; dz <= 1; dz++) for (int y = 0; y <= 2; y++) c.carve(wx, y, g.z() + dz);
            for (int y = 0; y <= 3; y++) {
                c.set(wx, y, g.z() - 2, St.axis("minecraft:quartz_pillar", "y"));
                c.set(wx, y, g.z() + 2, St.axis("minecraft:quartz_pillar", "y"));
            }
            for (int dz = -1; dz <= 1; dz++) {
                c.set(wx, 3, g.z() + dz, "minecraft:quartz_bricks");
                c.set(wx + side, 1, g.z() + dz, dz == 0 ? "minecraft:sea_lantern" : "minecraft:smooth_quartz");
            }
        }
        // The entrance: an opening through the south wall framed in quartz.
        for (int x = -1; x <= 1; x++) for (int y = 0; y <= 2; y++) for (int z = o; z <= w; z++) c.carve(x, y, z);
        for (int y = 0; y <= 3; y++) {
            for (int z = o; z <= w; z++) {
                c.set(-2, y, z, St.axis("minecraft:quartz_pillar", "y"));
                c.set(2, y, z, St.axis("minecraft:quartz_pillar", "y"));
            }
        }
        for (int x = -1; x <= 1; x++) for (int z = o; z <= w; z++) c.set(x, 3, z, "minecraft:chiseled_quartz_block");
        // Glass screens (phase 1): round the chair bays and across the waiting area by the door.
        for (int[] s : screens()) screen(c, s[0], s[1], s[2], s[3]);
        // Waiting benches along the south wall, white tulips between; supply cabinets along the side walls by the chairs.
        for (int side : new int[]{-1, 1}) {
            for (int k = 6; k <= 11; k++) {
                int x = side * k;
                if (k == 8 || k == 11) {
                    c.set(x, 0, IN - 1, "minecraft:smooth_quartz");
                    c.set(x, 1, IN - 1, k == 8 ? "minecraft:potted_white_tulip" : "minecraft:potted_azure_bluet");
                } else {
                    c.set(x, 0, IN - 1, Family.SMOOTH_QUARTZ.stairs(Dir.SOUTH, false));
                }
            }
            for (int z = -4; z <= -1; z++) {
                int x = side * IN;
                c.set(x, 0, z, "minecraft:smooth_quartz");
                c.set(x, 1, z, "minecraft:smooth_quartz");
                c.set(x, 2, z, Family.SMOOTH_QUARTZ.slabBottom());
                Dir front = side < 0 ? Dir.EAST : Dir.WEST;
                c.set(x + front.dx, 0, z, St.trapdoorAgainst("minecraft:iron_trapdoor", front.opposite()));
                c.set(x + front.dx, 1, z, St.trapdoorAgainst("minecraft:iron_trapdoor", front.opposite()));
            }
        }
        // Fill light: invisible light blocks hung between the coffers where the air is free.
        for (int x = -IN + 1; x < IN; x++) {
            for (int z = -IN + 1; z < IN; z++) {
                if (Math.floorMod(x, 4) == 2 && Math.floorMod(z, 4) == 2) c.setIfAir(x, 3, z, St.light(15));
            }
        }
        c.setIfAir(0, 3, ELEVATOR.z() + 1, St.light(15));
        c.setIfAir(-15, 3, 15, St.light(15));
        c.setIfAir(15, 3, 15, St.light(15));
        // White tulips each side of the entrance.
        for (int x : new int[]{-5, 5}) {
            c.set(x, 0, IN - 1, "minecraft:smooth_quartz");
            c.set(x, 1, IN - 1, "minecraft:potted_white_tulip");
        }
    }

    /** The screens' runs {x0, z0, x1, z1} (inclusive, straight). */
    static int[][] screens() {
        return new int[][]{{-13, -9, -13, -1}, {13, -9, 13, -1}, {-12, 10, -6, 10}, {6, 10, 12, 10}};
    }

    /** A screen: a quartz kerb, two rows of white glass panes, a slab rail on top. */
    static void screen(Canvas c, int x0, int z0, int x1, int z1) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
            for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
                c.set(x, 0, z, "minecraft:smooth_quartz");
                c.set(x, 1, z, St.pane("minecraft:white_stained_glass_pane"));
                c.set(x, 2, z, St.pane("minecraft:white_stained_glass_pane"));
                c.set(x, 3, z, Family.SMOOTH_QUARTZ.slabBottom());
            }
        }
    }

    /**
     * What changes when the fight reaches {@code phase} (2: the diffusers glow red, the glass screens sink into the floor). Cells
     * relative to the room's origin; no block entities.
     */
    public static List<ArenaCell> phaseCells(int phase) {
        List<ArenaCell> out = new ArrayList<>();
        if (phase < 2) return out;
        for (int x = -IN; x <= IN; x++) {
            for (int z = -IN; z <= IN; z++) {
                if (Math.floorMod(x, 4) == 0 && Math.floorMod(z, 4) == 0) out.add(new ArenaCell(x, CEIL, z, ALARM));
            }
        }
        for (int[] s : screens()) {
            for (int x = Math.min(s[0], s[2]); x <= Math.max(s[0], s[2]); x++) {
                for (int z = Math.min(s[1], s[3]); z <= Math.max(s[1], s[3]); z++) {
                    for (int y = 0; y <= 3; y++) out.add(new ArenaCell(x, y, z, St.AIR));
                }
            }
        }
        return out;
    }
}
