package org.papiricoh.supernaturalcraft.asylum.layout;

import org.papiricoh.supernaturalcraft.buildkit.Box;
import org.papiricoh.supernaturalcraft.buildkit.Brush;
import org.papiricoh.supernaturalcraft.buildkit.Canvas;
import org.papiricoh.supernaturalcraft.buildkit.Dir;
import org.papiricoh.supernaturalcraft.buildkit.Family;
import org.papiricoh.supernaturalcraft.buildkit.Furniture;
import org.papiricoh.supernaturalcraft.buildkit.Kinds;
import org.papiricoh.supernaturalcraft.buildkit.Lighting;
import org.papiricoh.supernaturalcraft.buildkit.Noise;
import org.papiricoh.supernaturalcraft.buildkit.Palette;
import org.papiricoh.supernaturalcraft.buildkit.St;
import org.papiricoh.supernaturalcraft.buildkit.Trees;
import org.papiricoh.supernaturalcraft.buildkit.Walls;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;
import org.papiricoh.supernaturalcraft.layout.LayoutPoint;

/**
 * Crowley's asylum (v0.35, pure buildkit plan): a ruined state asylum, the "Needham Asylum". Origin = the ground floor's centre
 * on its floor ({@code y = 0} the first air above it), the façade to the south (+Z), turned by {@code q} clockwise quarter
 * turns ({@link #rotate}, points with {@link #at}). One design, not seeded. The contract is {@link AsylumAnchors}.
 * <p>The build: three storeys of weathered red brick over a rusticated stone plinth, pilasters between the window bays, belt
 * courses at the floors, a stone entablature with a carved name band, a cornice and a balustraded parapet. A portico of four
 * tall columns carries a balcony with balusters over the front door; a steep, wide stone stair with cheek walls and rails
 * climbs to it from the land ({@link AsylumAnchors#GROUND_Y}). Windows barred, boarded or broken; ivy and moss over the walls,
 * overgrown trees and brush close round it.
 * <p>Ground floor: the double-height entrance HALL in the middle (a worn checker floor, chandeliers, a broken reception desk);
 * the cell wing to the west (a wide corridor between two rows of barred cells); the treatment room and the records offices
 * (filing cabinets) to the east; the ruined chapel behind the hall, the basement stair and a storeroom beside it, a ward in the
 * west wing's back. Upstairs: a dormitory and the records room on the first floor, an attic ward on the second, holes through
 * the floors. Below: the THRONE room, a dark brick cellar under groin vaults on brick piers, two tall barred arched windows
 * glowing behind the dais, a three-step dais with Crowley's gothic throne, tables of files, a worn rug over a painted devil's
 * trap. No block entities anywhere (the whole building is the fight's arena: it is restored from a snapshot).
 */
public final class AsylumPlan {

    private AsylumPlan() {
    }

    // --- the grid (plan space, q = 0) ----------------------------------------------------------------------------------------

    /** Outer wall lines. */
    static final int WX0 = -25, WX1 = 25, WZ0 = -20, WZ1 = 19;
    /** Floors: the block under each storey's feet, and the roof deck. */
    static final int F0 = -1, F1 = 5, F2 = 11, ROOF = 17;
    /** The land (the pad's surface block) and the basement's floor block. */
    static final int LAND = AsylumAnchors.GROUND_Y, CELLAR = -10;
    /** The hall's and the wings' partition lines. */
    static final int HALL_X = 12, MID_Z = 1;
    /** The throne room's inside (x ±11, z -21..-1), its back wall. */
    static final int TR_X = 11, TR_Z0 = -21, TR_Z1 = -1, TR_BACK = -22;
    /** The basement stair: x -11..-9, its steps at z = -1..-8. */
    static final int ST_X0 = -11, ST_X1 = -9, STEPS = 8;
    static final int SEED = 3501;

    static final LayoutPoint ENTRY = new LayoutPoint(0, LAND + 1, 31);
    static final LayoutPoint HALL_THRESHOLD = new LayoutPoint(0, 0, 17);
    static final LayoutPoint HALL = new LayoutPoint(0, 0, 10);
    static final LayoutPoint CELLS = new LayoutPoint(-18, 0, 7);
    static final LayoutPoint THRONE = new LayoutPoint(0, CELLAR + 1, -9);
    static final LayoutPoint THRONE_SEAT = new LayoutPoint(0, CELLAR + 4, -21);
    static final LayoutPoint RITE = new LayoutPoint(0, CELLAR + 1, -15);

    /** Local X/Z turned by {@code q} clockwise quarter turns (as {@code RowenaHouseLayout.rotate} and {@link Canvas#turn}). */
    public static int[] rotate(int x, int z, int q) {
        return switch (Math.floorMod(q, 4)) {
            case 1 -> new int[]{-z, x};
            case 2 -> new int[]{-x, -z};
            case 3 -> new int[]{z, -x};
            default -> new int[]{x, z};
        };
    }

    public static LayoutPoint at(LayoutPoint p, int q) {
        int[] r = rotate(p.x(), p.z(), q);
        return new LayoutPoint(r[0], p.y(), r[1]);
    }

    public static LayoutPlan plan(int q) {
        return canvas(q).toPlan();
    }

    /** The asylum as a finished kit canvas (tests and the layout dump). */
    public static Canvas canvas(int q) {
        Canvas south = drawn();
        Canvas c = new Canvas("asylum");
        int r = Math.max(Math.max(-AsylumAnchors.MIN_X, AsylumAnchors.MAX_X), Math.max(-AsylumAnchors.MIN_Z, AsylumAnchors.MAX_Z)) + 8;
        c.zone("asylum", new Box(-r, AsylumAnchors.BOTTOM_Y, -r, r, AsylumAnchors.TOP_Y, r));
        c.inZone("asylum", () -> c.stamp(south, 0, 0, 0, Math.floorMod(q, 4), Canvas.Mode.SET, ""));
        return c.finish();
    }

    // --- brushes -------------------------------------------------------------------------------------------------------------

    static final Brush BRICK = Palette.mix(SEED + 1, "minecraft:bricks", 10, "minecraft:granite", 2, "minecraft:red_terracotta", 1,
            "minecraft:mud_bricks", 1, "minecraft:terracotta", 1, "minecraft:moss_block", 1, "minecraft:packed_mud", 1);
    static final Brush CELLAR_BRICK = Palette.mix(SEED + 2, "minecraft:bricks", 7, "minecraft:brown_terracotta", 2, "minecraft:mud_bricks", 2,
            "minecraft:deepslate_bricks", 2, "minecraft:granite", 1, "minecraft:red_terracotta", 1);
    static final Brush STONE = Palette.mix(SEED + 3, "minecraft:stone_bricks", 5, "minecraft:cracked_stone_bricks", 2,
            "minecraft:mossy_stone_bricks", 3, "minecraft:andesite", 1);
    static final Brush PLASTER = Palette.mix(SEED + 4, "minecraft:calcite", 5, "minecraft:white_concrete_powder", 2, "minecraft:diorite", 2,
            "minecraft:light_gray_terracotta", 1, "minecraft:smooth_stone", 2, "minecraft:mossy_cobblestone", 1);
    static final Brush TILE = Palette.mix(SEED + 5, "minecraft:polished_andesite", 4, "minecraft:polished_diorite", 4,
            "minecraft:cracked_stone_bricks", 2, "minecraft:andesite", 1, "minecraft:mossy_cobblestone", 1);
    static final Brush BOARDS = Palette.mix(SEED + 6, "minecraft:spruce_planks", 5, "minecraft:dark_oak_planks", 3,
            "minecraft:stripped_spruce_wood[axis=y]", 2, "minecraft:oak_planks", 1, "minecraft:mud", 1);
    static final Brush SLABSTONE = Palette.mix(SEED + 7, "minecraft:smooth_stone", 4, "minecraft:stone", 3, "minecraft:andesite", 3,
            "minecraft:cobblestone", 2, "minecraft:tuff", 1);
    static final Brush FOOTING = Palette.mix(SEED + 8, "minecraft:cobblestone", 3, "minecraft:cobbled_deepslate", 2, "minecraft:stone", 2);
    static final Brush ROOFING = Palette.mix(SEED + 9, "minecraft:cobblestone", 3, "minecraft:mossy_cobblestone", 3, "minecraft:tuff", 2,
            "minecraft:moss_block", 2, "minecraft:stone", 2);
    static final Brush CHECKER = (x, y, z) -> Noise.chance(x, y, z, SEED + 10, 0.22) ? TILE.at(x, y, z)
            : Math.floorMod(x + z, 2) == 0 ? "minecraft:polished_diorite" : "minecraft:polished_deepslate";
    static final String GLOW = "minecraft:pearlescent_froglight";

    // --- drawing -------------------------------------------------------------------------------------------------------------

    static Canvas drawn() {
        Canvas c = new Canvas("asylum_south");
        grounds(c);
        trees(c);
        shell(c);
        portico(c);
        groundFloor(c);
        upperFloors(c);
        cellar(c);
        overgrowth(c);
        points(c);
        return c;
    }

    private static void points(Canvas c) {
        put(c, AsylumAnchors.ENTRY, ENTRY);
        put(c, AsylumAnchors.HALL_THRESHOLD, HALL_THRESHOLD);
        put(c, AsylumAnchors.HALL, HALL);
        put(c, AsylumAnchors.CELLS, CELLS);
        put(c, AsylumAnchors.THRONE, THRONE);
        put(c, AsylumAnchors.THRONE_SEAT, THRONE_SEAT);
        put(c, AsylumAnchors.RITE, RITE);
        int[][] hall = {{-8, 0, 5}, {8, 0, 5}, {-8, 0, 15}, {8, 0, 15}};
        int[][] cells = {{-23, 0, 4}, {-23, 0, 10}, {-14, 0, 4}, {-14, 0, 10}};
        int[][] throne = {{-5, CELLAR + 1, -3}, {5, CELLAR + 1, -3}, {-5, CELLAR + 1, -13}, {5, CELLAR + 1, -13}};
        for (int n = 0; n < AsylumAnchors.MIN_SPAWNS; n++) {
            c.anchor(AsylumAnchors.spawn("hall", n), hall[n][0], hall[n][1], hall[n][2]);
            c.anchor(AsylumAnchors.spawn("cells", n), cells[n][0], cells[n][1], cells[n][2]);
            c.anchor(AsylumAnchors.spawn("throne", n), throne[n][0], throne[n][1], throne[n][2]);
        }
        gate(c, AsylumAnchors.GATE_FRONT, -1, 0, WZ1, 1, 3, WZ1);
        gate(c, AsylumAnchors.GATE_CELLS, -HALL_X, 0, 6, -HALL_X, 3, 8);
        gate(c, AsylumAnchors.GATE_THRONE, ST_X0, 0, MID_Z, ST_X1, 3, MID_Z);
    }

    private static void put(Canvas c, String name, LayoutPoint p) {
        c.anchor(name, p.x(), p.y(), p.z());
    }

    /** A gate: its box carved (air: the fight fills it with bars), its corners named. */
    private static void gate(Canvas c, String g, int x0, int y0, int z0, int x1, int y1, int z1) {
        c.carve(new Box(x0, y0, z0, x1, y1, z1));
        c.anchor(AsylumAnchors.gateMin(g), x0, y0, z0);
        c.anchor(AsylumAnchors.gateMax(g), x1, y1, z1);
    }

    // --- the grounds ---------------------------------------------------------------------------------------------------------

    /** The land round it: grass gone to seed, moss, a gravel drive up to the stair. */
    private static void grounds(Canvas c) {
        Brush lawn = Palette.patches2(SEED + 20, 3, "minecraft:grass_block", 10, "minecraft:moss_block", 2, "minecraft:podzol", 2,
                "minecraft:coarse_dirt", 2, "minecraft:rooted_dirt", 1);
        Brush drive = Palette.mix(SEED + 21, "minecraft:gravel", 4, "minecraft:coarse_dirt", 2, "minecraft:cobblestone", 1,
                "minecraft:mossy_cobblestone", 1, "minecraft:dirt_path", 2);
        for (int x = AsylumAnchors.MIN_X; x <= AsylumAnchors.MAX_X; x++) {
            for (int z = AsylumAnchors.MIN_Z; z <= AsylumAnchors.MAX_Z; z++) {
                if (x >= WX0 && x <= WX1 && z >= WZ0 && z <= WZ1) continue;
                boolean path = Math.abs(x) <= 2 + (z > 30 ? 1 : 0) && z >= 27;
                c.set(x, LAND, z, path ? drive.at(x, LAND, z) : lawn.at(x, LAND, z));
            }
        }
    }

    // --- the shell -----------------------------------------------------------------------------------------------------------

    static final Box FOOT = new Box(WX0, 0, WZ0, WX1, 0, WZ1);

    /** Footings, floors, the outer walls with their pilasters, mouldings, windows, the entablature, the roof and parapet. */
    private static void shell(Canvas c) {
        // Footings and the ground floor's floor; the storeys' floors and the roof.
        for (int x = WX0; x <= WX1; x++) {
            for (int z = WZ0; z <= WZ1; z++) {
                for (int y = LAND; y <= F0 - 1; y++) c.set(x, y, z, FOOTING.at(x, y, z));
                boolean edge = x == WX0 || x == WX1 || z == WZ0 || z == WZ1;
                c.set(x, F0, z, edge ? STONE.at(x, F0, z) : SLABSTONE.at(x, F0, z));
                if (edge) continue;
                for (int y = 0; y < ROOF; y++) c.carve(x, y, z);
                c.set(x, F1, z, BOARDS.at(x, F1, z));
                c.set(x, F2, z, BOARDS.at(x, F2, z));
                // The roof deck, fallen through in two places.
                boolean hole = inEllipse(x, z, -18, -12, 3.2, 2.4) || inEllipse(x, z, 17, 10, 2.6, 3.4);
                if (!hole) c.set(x, ROOF, z, ROOFING.at(x, ROOF, z));
            }
        }
        for (Walls.Run r : Walls.around(FOOT)) {
            // The rusticated plinth from the land to the ground floor, the brick body, the stone storey courses.
            Walls.body(c, r, LAND + 1, F0, STONE);
            Walls.body(c, r, 0, ROOF - 1, BRICK);
            Walls.plinth(c, r, LAND + 1, 1, Family.STONE_BRICKS, true);
            Walls.belt(c, r, F0, Family.STONE_BRICKS);
            Walls.belt(c, r, F1, Family.STONE_BRICKS);
            Walls.body(c, r, F2, F2, STONE);                       // the second floor's course, flush (a ledge there is a dark nook)
            pilasters(c, r);
            windows(c, r);
            // The entablature: architrave, the frieze (the name band on the front), the cornice, the parapet's balusters.
            Walls.body(c, r, ROOF, ROOF, STONE);
            Walls.body(c, r, ROOF + 1, ROOF + 1, r.out() == Dir.SOUTH ? (x, y, z) -> Math.floorMod(x, 3) == 0
                    ? "minecraft:chiseled_stone_bricks" : Noise.chance(x, y, z, SEED + 30, 0.3) ? "minecraft:cracked_stone_bricks"
                    : "minecraft:polished_andesite" : STONE);
            Walls.cornice(c, r, ROOF + 2, Family.STONE_BRICKS, null);
            for (int i = 0; i < r.length(); i++) {
                int x = r.x(i), z = r.z(i);
                boolean post = Math.floorMod(i, 5) == 0 || i == r.length() - 1;
                if (Noise.chance(x, ROOF + 3, z, SEED + 31, 0.1) && !post) continue;   // a baluster fallen
                c.set(x, ROOF + 3, z, post ? "minecraft:stone_bricks" : St.wall("minecraft:stone_brick_wall"));
                if (post) c.set(x, ROOF + 4, z, St.slabBottom("minecraft:stone_brick_slab"));
            }
        }
        // The roof's walk inside the parapet; two chimney stacks.
        for (int x = WX0 + 1; x < WX1; x++) {
            for (int z = WZ0 + 1; z < WZ1; z++) {
                if (c.has(x, ROOF, z)) {
                    c.set(x, ROOF + 1, z, ROOFING.at(x, ROOF + 1, z));
                    c.set(x, ROOF + 2, z, ROOFING.at(x, ROOF + 2, z));
                }
            }
        }
        for (int[] ch : new int[][]{{-14, -14}, {14, -14}}) {
            for (int dx = 0; dx <= 1; dx++) {
                for (int dz = 0; dz <= 1; dz++) {
                    for (int y = ROOF + 3; y <= ROOF + 4; y++) c.set(ch[0] + dx, y, ch[1] + dz, BRICK.at(ch[0] + dx, y, ch[1] + dz));
                    c.set(ch[0] + dx, ROOF + 5, ch[1] + dz, St.slabBottom("minecraft:brick_slab"));
                }
            }
        }
    }

    private static boolean inEllipse(int x, int z, int cx, int cz, double rx, double rz) {
        double dx = (x - cx) / rx, dz = (z - cz) / rz;
        return dx * dx + dz * dz <= 1;
    }

    /** Pilasters standing out of the wall between the bays, stone-capped, a stone base on the plinth. */
    private static void pilasters(Canvas c, Walls.Run r) {
        Walls.Run o = r.offset(1);
        for (int i = 0; i < r.length(); i++) {
            if (Math.floorMod(i, 5) != 0 && i != r.length() - 1) continue;
            int x = o.x(i), z = o.z(i);
            if (r.out() == Dir.SOUTH && Math.abs(x) <= 9) continue;   // the portico's own columns stand there
            c.set(x, 0, z, "minecraft:stone_bricks");
            for (int y = 1; y < ROOF; y++) {
                if (y == F1 || y == F2) c.set(x, y, z, "minecraft:chiseled_stone_bricks");
                else c.set(x, y, z, BRICK.at(x, y, z));
            }
            c.set(x, ROOF, z, "minecraft:stone_bricks");
        }
    }

    /** Window bays between the pilasters: two wide, three tall on each storey; barred, boarded or broken, stone sill and lintel. */
    private static void windows(Canvas c, Walls.Run r) {
        int[] sills = {1, F1 + 2, F2 + 2};
        for (int i = 0; i < r.length(); i++) {
            int m = Math.floorMod(i, 5);
            if (m != 2 || i + 1 >= r.length() - 1) continue;
            for (int s = 0; s < sills.length; s++) {
                int y0 = sills[s];
                int x0 = r.x(i), z0 = r.z(i);
                if (r.out() == Dir.SOUTH && Math.abs(x0) <= 3 && s == 0) continue;   // the front door
                if (r.out() == Dir.SOUTH && Math.abs(x0) <= 3 && s == 2) continue;   // the balcony door
                int kind = Noise.pick(x0, y0, z0, SEED + 40, 7);
                if (s == 2 && (kind == 2 || kind == 3)) kind = 0;   // the attic's boards long gone
                for (int k = 0; k < 2; k++) {
                    int x = r.x(i + k), z = r.z(i + k);
                    for (int y = y0; y < y0 + 3; y++) {
                        String pane = switch (kind) {
                            case 0, 1 -> St.bars();                                                       // barred
                            case 2 -> y == y0 + 1 ? "minecraft:spruce_planks" : St.bars();                 // a board across bars
                            case 3 -> "minecraft:dark_oak_planks";                                         // boarded up
                            case 4 -> Noise.chance(x, y, z, SEED + 41, 0.5) ? St.pane("minecraft:glass_pane") : St.bars();
                            default -> Noise.chance(x, y, z, SEED + 42, 0.6) ? St.bars() : St.AIR;      // broken
                        };
                        c.set(x, y, z, pane);
                    }
                    c.set(x, y0 + 3, z, "minecraft:chiseled_stone_bricks");
                    Walls.Run o = r.offset(1);
                    c.setIfAir(o.x(i + k), y0 - 1, o.z(i + k), St.stairs("minecraft:stone_brick_stairs", r.out().opposite(), true));
                }
            }
        }
    }

    // --- the portico ---------------------------------------------------------------------------------------------------------

    /** Four tall columns carrying the balcony, the deck under them, the steep front stair with its cheek walls and rails. */
    private static void portico(Canvas c) {
        int px = 8, z0 = WZ1 + 1, z1 = WZ1 + 5;
        for (int x = -px; x <= px; x++) {
            for (int z = z0; z <= z1; z++) {
                for (int y = LAND; y < F0; y++) c.set(x, y, z, FOOTING.at(x, y, z));
                c.set(x, F0, z, Math.abs(x) == px || z == z1 ? STONE.at(x, F0, z) : TILE.at(x, F0, z));
                for (int y = 0; y < F2; y++) c.carve(x, y, z);
                // The balcony's deck over the columns.
                c.set(x, F2, z, x == -px || x == px || z == z1 ? "minecraft:stone_bricks" : SLABSTONE.at(x, F2, z));
            }
        }
        // The columns: a stone base, pale weathered shafts, a carved capital under the deck.
        Brush shaft = Palette.mix(SEED + 50, "minecraft:quartz_pillar[axis=y]", 4, "minecraft:calcite", 2, "minecraft:diorite", 2,
                "minecraft:mossy_stone_bricks", 1);
        for (int x : new int[]{-6, -2, 2, 6}) {
            int z = z1 - 1;
            c.set(x, 0, z, "minecraft:stone_bricks");
            for (Dir d : Dir.HORIZONTAL) c.set(x + d.dx, 0, z + d.dz, St.stairs("minecraft:stone_brick_stairs", d.opposite(), false));
            for (int y = 1; y < F2 - 1; y++) c.set(x, y, z, shaft.at(x, y, z));
            c.set(x, F2 - 1, z, "minecraft:chiseled_quartz_block");
            for (Dir d : Dir.HORIZONTAL) c.set(x + d.dx, F2 - 1, z + d.dz, St.stairs("minecraft:quartz_stairs", d.opposite(), true));
        }
        // Responds on the façade behind them.
        for (int x : new int[]{-6, -2, 2, 6}) for (int y = 0; y < F2; y++) c.set(x, y, WZ1 + 1, y == 0 || y == F2 - 1 ? "minecraft:stone_bricks" : STONE.at(x, y, WZ1 + 1));
        // The balcony's balustrade.
        for (int x = -px; x <= px; x++) {
            for (int z = z0; z <= z1; z++) {
                boolean rim = x == -px || x == px || z == z1;
                if (!rim) continue;
                boolean post = Math.floorMod(x, 4) == 0 && z == z1 || z == z1 && Math.abs(x) == px || Math.abs(x) == px && z == z0;
                c.set(x, F2 + 1, z, post ? "minecraft:stone_bricks" : St.wall("minecraft:andesite_wall"));
                if (post) c.set(x, F2 + 2, z, St.slabBottom("minecraft:stone_brick_slab"));
            }
        }
        // The front door: a stone surround with a keystone over the opening; the balcony door above.
        for (int y = 0; y <= 4; y++) {
            c.set(-2, y, WZ1, "minecraft:stone_bricks");
            c.set(2, y, WZ1, "minecraft:stone_bricks");
        }
        for (int x = -1; x <= 1; x++) c.set(x, 4, WZ1, x == 0 ? "minecraft:chiseled_stone_bricks" : "minecraft:stone_bricks");
        for (int y = F2 + 1; y <= F2 + 3; y++) c.carve(0, y, WZ1);
        c.set(0, F2 + 4, WZ1, "minecraft:chiseled_stone_bricks");
        // The stair: three steep courses down to the land, cheek walls with a rail, lanterns on their newels.
        int sx = 6;
        for (int k = 1; k <= 3; k++) {
            int z = z1 + k, y = F0 - k;
            for (int x = -sx; x <= sx; x++) {
                c.set(x, y, z, St.stairs("minecraft:stone_brick_stairs", Dir.NORTH, false));
                for (int yy = LAND; yy < y; yy++) c.set(x, yy, z, FOOTING.at(x, yy, z));
            }
            for (int x : new int[]{-sx - 1, sx + 1}) {
                for (int yy = LAND; yy <= y; yy++) c.set(x, yy, z, STONE.at(x, yy, z));
                c.set(x, y + 1, z, St.wall("minecraft:stone_brick_wall"));
            }
        }
        for (int x : new int[]{-sx - 1, sx + 1}) {
            c.set(x, F0 - 2, z1 + 4, "minecraft:stone_bricks");
            c.set(x, F0 - 1, z1 + 4, St.lantern(false));
        }
    }

    // --- the ground floor ----------------------------------------------------------------------------------------------------

    private static void groundFloor(Canvas c) {
        partitions(c);
        hall(c);
        cellWing(c);
        westWard(c);
        treatment(c);
        offices(c);
        chapel(c);
        stairHall(c);
    }

    private static void wallX(Canvas c, int x, int z0, int z1, int y0, int y1, Brush b) {
        for (int z = z0; z <= z1; z++) for (int y = y0; y <= y1; y++) c.set(x, y, z, b.at(x, y, z));
    }

    private static void wallZ(Canvas c, int z, int x0, int x1, int y0, int y1, Brush b) {
        for (int x = x0; x <= x1; x++) for (int y = y0; y <= y1; y++) c.set(x, y, z, b.at(x, y, z));
    }

    private static void opening(Canvas c, int x0, int z0, int x1, int z1, int y0, int y1) {
        c.carve(new Box(Math.min(x0, x1), y0, Math.min(z0, z1), Math.max(x0, x1), y1, Math.max(z0, z1)));
    }

    /** The partitions, floor to roof: the hall's sides, its back, the wings' cross walls. */
    private static void partitions(Canvas c) {
        wallX(c, -HALL_X, WZ0 + 1, WZ1 - 1, 0, ROOF - 1, PLASTER);
        wallX(c, HALL_X, WZ0 + 1, WZ1 - 1, 0, ROOF - 1, PLASTER);
        wallZ(c, MID_Z, -HALL_X + 1, HALL_X - 1, 0, F2 - 1, PLASTER);
        wallZ(c, MID_Z, HALL_X + 1, WX1 - 1, 0, F1 - 1, PLASTER);
        wallZ(c, MID_Z, HALL_X + 1, WX1 - 1, F1 + 1, F2 - 1, PLASTER);
        wallZ(c, -4, WX0 + 1, -HALL_X - 1, 0, F1 - 1, PLASTER);
        wallX(c, -8, WZ0 + 1, 0, 0, F2 - 1, PLASTER);
        wallZ(c, -10, ST_X0, ST_X1, 0, F1 - 1, PLASTER);
        // The hall and chapel are two storeys high: no first floor over them.
        for (int x = -HALL_X + 1; x <= HALL_X - 1; x++) {
            for (int z = WZ0 + 1; z <= WZ1 - 1; z++) {
                if (x <= -8 && z <= 0 || z == MID_Z) continue;
                c.carve(x, F1, z);
            }
        }
        // Doorways.
        opening(c, 4, MID_Z, 6, MID_Z, 0, 3);              // hall → chapel
        opening(c, HALL_X, 9, HALL_X, 10, 0, 2);           // hall → treatment
        opening(c, 18, MID_Z, 19, MID_Z, 0, 2);            // treatment → offices
        opening(c, -8, -15, -8, -15, 0, 2);                // storeroom → chapel
        opening(c, -HALL_X, -15, -HALL_X, -15, 0, 2);      // storeroom → ward
        opening(c, -14, -4, -14, -4, 0, 2);                // cell corridor's end → ward
        opening(c, 20, MID_Z, 20, MID_Z, F1 + 1, F1 + 2);  // records, front ↔ back
        opening(c, -HALL_X, 0, -HALL_X, 0, F2 + 1, F2 + 2);
        opening(c, HALL_X, 0, HALL_X, 0, F2 + 1, F2 + 2);
        opening(c, -HALL_X, 12, -HALL_X, 12, F2 + 1, F2 + 2);
        opening(c, HALL_X, 12, HALL_X, 12, F2 + 1, F2 + 2);
        // Wainscot along the rooms' walls: a dark band of boards at the foot of the plaster.
        for (int x = WX0 + 1; x < WX1; x++) {
            for (int z = WZ0 + 1; z < WZ1; z++) {
                if (!c.isAir(x, 0, z)) continue;
                for (Dir d : Dir.HORIZONTAL) {
                    String n = c.get(x + d.dx, 0, z + d.dz);
                    if (n != null && (St.path(n).equals("calcite") || St.path(n).equals("diorite")) && Noise.chance(x, 0, z, SEED + 60, 0.5)) {
                        c.set(x + d.dx, 0, z + d.dz, "minecraft:stripped_dark_oak_wood[axis=y]");
                    }
                }
            }
        }
    }

    /** The entrance hall: double height, a worn checker floor, chandeliers, the reception's broken desk, benches. */
    private static void hall(Canvas c) {
        for (int x = -HALL_X + 1; x <= HALL_X - 1; x++) for (int z = MID_Z + 1; z <= WZ1 - 1; z++) c.set(x, F0, z, CHECKER.at(x, F0, z));
        // Pilasters round the walls (relief in the double height).
        for (int z = 4; z <= 16; z += 6) {
            for (int y = 0; y < F2; y++) {
                c.set(-HALL_X + 1, y, z, y == 0 || y == F2 - 1 ? "minecraft:stone_bricks" : "minecraft:smooth_quartz");
                c.set(HALL_X - 1, y, z, y == 0 || y == F2 - 1 ? "minecraft:stone_bricks" : "minecraft:smooth_quartz");
            }
        }
        // The reception desk, broken, against the back wall, a fallen ledger's papers.
        for (int x = -4; x <= 4; x++) {
            if (x == 1) continue;
            c.set(x, 0, 3, x == -4 || x == 4 ? "minecraft:dark_oak_planks" : St.stairs("minecraft:dark_oak_stairs", Dir.NORTH, true));
        }
        c.set(-2, 1, 3, St.carpet("white"));
        c.set(3, 1, 3, St.candle("white", 2, true));
        c.set(1, 0, 2, St.carpet("white"));
        // Benches along the side walls.
        for (int z : new int[]{6, 7, 12, 13}) {
            c.set(-HALL_X + 2, 0, z, St.stairs("minecraft:spruce_stairs", Dir.WEST, false));
            c.set(HALL_X - 2, 0, z, St.stairs("minecraft:spruce_stairs", Dir.EAST, false));
        }
        // Debris and cobwebs high up; a fallen chandelier in a corner.
        for (int[] d : new int[][]{{-10, 2}, {10, 17}, {-9, 17}, {7, 2}}) c.set(d[0], 0, d[1], St.carpet(d[0] < 0 ? "light_gray" : "gray"));
        for (int[] w : new int[][]{{-11, 10, 2}, {11, 10, 18}, {-11, 10, 18}, {0, 10, 18}}) c.set(w[0], w[1], w[2], "minecraft:cobweb");
        Lighting.chandelier(c, -5, F2, 10, 3, "minecraft:dark_oak_fence", "white");
        Lighting.chandelier(c, 5, F2, 10, 3, "minecraft:dark_oak_fence", "white");
        Lighting.hanging(c, 0, F2, 16, 2, false);
        Lighting.hanging(c, 0, F2, 4, 2, false);
        c.set(9, 0, 17, St.fence("minecraft:dark_oak_fence"));
        c.set(10, 0, 16, St.lantern(false));
        // Sconces low on the side walls, between the pilasters.
        for (int z : new int[]{2, 7, 13, 18}) {
            Lighting.sconce(c, -HALL_X + 1, 2, z, Dir.EAST, "minecraft:dark_oak_trapdoor", false);
            Lighting.sconce(c, HALL_X - 1, 2, z, Dir.WEST, "minecraft:dark_oak_trapdoor", false);
        }
    }

    /** The cell wing: a wide corridor between two rows of barred cells. */
    private static void cellWing(Canvas c) {
        int x0 = WX0 + 1, x1 = -HALL_X - 1;
        Brush floor = Palette.mix(SEED + 70, "minecraft:smooth_stone", 3, "minecraft:stone", 2, "minecraft:cracked_stone_bricks", 2,
                "minecraft:andesite", 2, "minecraft:mossy_cobblestone", 1);
        for (int x = x0; x <= x1; x++) for (int z = -3; z <= 18; z++) c.set(x, F0, z, floor.at(x, F0, z));
        // Bars in front of the cells, a door gap in each; partitions of tile between them.
        for (int x = x0; x <= x1; x++) {
            for (int y = 0; y < F1; y++) {
                c.set(x, y, 2, y >= 3 ? PLASTER.at(x, y, 2) : St.bars());
                c.set(x, y, 12, y >= 3 ? PLASTER.at(x, y, 12) : St.bars());
            }
        }
        for (int x = x0 + 3; x <= x1; x += 4) {
            wallX(c, x, -3, 1, 0, F1 - 1, PLASTER);
            wallX(c, x, 13, 18, 0, F1 - 1, PLASTER);
        }
        for (int x = x0 + 1; x <= x1; x += 4) {
            for (int y = 0; y <= 1; y++) {
                if (Noise.chance(x, y, 2, SEED + 71, 0.5)) c.carve(x, y, 2);       // a cell door left open
                if (Noise.chance(x, y, 12, SEED + 72, 0.5)) c.carve(x, y, 12);
            }
        }
        // In the cells: a slab cot, a bucket of a pot, scratches on the walls (cracked plaster).
        for (int x = x0; x + 2 <= x1; x += 4) {
            for (int[] row : new int[][]{{-3, -2}, {17, 18}}) {
                c.set(x, 0, row[0], St.slabBottom("minecraft:spruce_slab"));
                c.set(x, 0, row[1], St.slabBottom("minecraft:spruce_slab"));
                c.set(x, 1, row[0], St.carpet("white"));
                c.set(x + 2, 0, row[1], St.potted("dead_bush"));
                if (Noise.chance(x, 1, row[0], SEED + 73, 0.5)) c.set(x + 1, 0, row[0], "minecraft:cobweb");
            }
        }
        for (int x = x0 + 2; x <= x1; x += 4) {
            Lighting.hanging(c, x, F1, 7, 1, false);
            Lighting.hanging(c, x, F1, -1, 1, false);
            Lighting.hanging(c, x, F1, 15, 1, false);
        }
        for (int[] w : new int[][]{{-24, 4, 3}, {-13, 4, 11}, {-20, 4, -3}}) c.set(w[0], w[1], w[2], "minecraft:cobweb");
    }

    /** The ward behind the cells: slab beds in rows, white sheets, a nurse's table. */
    private static void westWard(Canvas c) {
        for (int x = WX0 + 1; x <= -HALL_X - 1; x++) for (int z = WZ0 + 1; z <= -5; z++) c.set(x, F0, z, BOARDS.at(x, F0, z));
        for (int x = WX0 + 2; x <= -HALL_X - 2; x += 3) {
            for (int z : new int[]{-18, -17, -8, -7}) {
                c.set(x, 0, z, St.slabBottom("minecraft:smooth_quartz_slab"));
                c.set(x, 1, z, St.carpet(Noise.chance(x, 1, z, SEED + 80, 0.3) ? "light_gray" : "white"));
            }
        }
        Furniture.table(c, -16, -13, -14, -12, 0, "minecraft:spruce_slab");
        c.set(-15, 1, -13, St.candle("white", 3, true));
        Lighting.hanging(c, -18, F1, -12, 1, false);
        Lighting.hanging(c, -22, F1, -12, 1, false);
        Lighting.hanging(c, -14, F1, -8, 1, false);
        Lighting.hanging(c, -14, F1, -18, 1, false);
    }

    /** The treatment room: reclining chairs, an operating table, cabinets, a bath. */
    private static void treatment(Canvas c) {
        Brush floor = Palette.checker(1, Palette.mix(SEED + 89, "minecraft:calcite", 3, "minecraft:white_concrete_powder", 1),
                Palette.mix(SEED + 90, "minecraft:light_gray_concrete", 3, "minecraft:cyan_terracotta", 1, "minecraft:mossy_cobblestone", 1));
        for (int x = HALL_X + 1; x <= WX1 - 1; x++) for (int z = MID_Z + 1; z <= WZ1 - 1; z++) c.set(x, F0, z, floor.at(x, F0, z));
        Furniture.medicalChair(c, 16, 0, 6, Dir.SOUTH);
        Furniture.medicalChair(c, 21, 0, 6, Dir.SOUTH);
        Furniture.medicalChair(c, 16, 0, 14, Dir.NORTH);
        Furniture.table(c, 20, 13, 22, 14, 0, "minecraft:smooth_quartz_slab");
        c.set(21, 1, 13, St.carpet("white"));
        c.set(20, 1, 14, St.candle("white", 1, true));
        for (int z = 3; z <= 17; z += 2) Furniture.cabinet(c, WX1 - 1, 0, z, Dir.WEST, "minecraft:smooth_quartz", "minecraft:iron_trapdoor", false);
        c.set(14, 0, 17, St.of("water_cauldron", "level", "1"));
        c.set(15, 0, 17, "minecraft:cauldron");
        Lighting.hanging(c, 16, F1, 10, 1, false);
        Lighting.hanging(c, 21, F1, 10, 1, false);
        Lighting.hanging(c, 18, F1, 4, 1, false);
        Lighting.hanging(c, 18, F1, 16, 1, false);
    }

    /** The records offices: desks, filing cabinets in rows, papers everywhere. */
    private static void offices(Canvas c) {
        for (int x = HALL_X + 1; x <= WX1 - 1; x++) for (int z = WZ0 + 1; z <= MID_Z - 1; z++) c.set(x, F0, z, BOARDS.at(x, F0, z));
        for (int z = -18; z <= -2; z += 2) {
            Furniture.filing(c, WX1 - 1, 0, z, Dir.WEST, "minecraft:gray_terracotta");
            if (z <= -6) Furniture.filing(c, 17, 0, z, Dir.EAST, "minecraft:gray_terracotta");
        }
        for (int[] d : new int[][]{{20, -16}, {20, -10}, {14, -5}}) {
            Furniture.desk(c, d[0], 0, d[1], Dir.SOUTH, "minecraft:spruce_slab", "minecraft:spruce_stairs", false);
            c.setIfAir(d[0], 1, d[1], St.carpet("white"));
        }
        for (int[] p : new int[][]{{15, -12}, {22, -3}, {19, -7}, {14, -17}, {21, -13}}) c.setIfAir(p[0], 0, p[1], St.carpet("white"));
        Lighting.hanging(c, 15, F1, -10, 1, false);
        Lighting.hanging(c, 21, F1, -12, 1, false);
        Lighting.hanging(c, 21, F1, -4, 1, false);
        Lighting.hanging(c, 15, F1, -17, 1, false);
        Lighting.hanging(c, 15, F1, -3, 1, false);
    }

    /** The chapel, double height: pews broken and overturned, an altar, its glass gone, moss on the boards. */
    private static void chapel(Canvas c) {
        Brush floor = Palette.mix(SEED + 100, "minecraft:dark_oak_planks", 4, "minecraft:spruce_planks", 3, "minecraft:moss_block", 1,
                "minecraft:stripped_dark_oak_wood[axis=y]", 2, "minecraft:coarse_dirt", 1);
        for (int x = -7; x <= HALL_X - 1; x++) for (int z = WZ0 + 1; z <= MID_Z - 1; z++) c.set(x, F0, z, floor.at(x, F0, z));
        for (int z = -12; z <= -2; z += 2) {
            for (int x = -5; x <= 9; x++) {
                if (x == 2 || x == 3) continue;            // the aisle
                if (Noise.chance(x, 0, z, SEED + 101, 0.22)) continue;
                c.set(x, 0, z, St.stairs("minecraft:dark_oak_stairs", Dir.SOUTH, false));
            }
        }
        // The altar on a step, candles, a broken window of red glass behind it.
        for (int x = -2; x <= 7; x++) c.set(x, 0, -18, St.slabBottom("minecraft:stone_brick_slab"));
        for (int x = 1; x <= 4; x++) c.set(x, 0, -17, "minecraft:chiseled_stone_bricks");
        for (int x = 1; x <= 4; x++) c.set(x, 1, -17, x == 1 || x == 4 ? St.candle("white", 3, true) : St.carpet("red"));
        for (int y = 2; y <= 8; y++) {
            for (int x = 2; x <= 3; x++) {
                String g = Noise.chance(x, y, WZ0, SEED + 102, 0.45) ? St.pane("minecraft:red_stained_glass_pane") : St.bars();
                c.set(x, y, WZ0, y == 8 ? "minecraft:chiseled_stone_bricks" : g);
            }
        }
        Lighting.hanging(c, 2, F2, -8, 3, false);
        Lighting.hanging(c, 8, F2, -14, 3, false);
        Lighting.hanging(c, -4, F2, -14, 3, false);
        Lighting.hanging(c, 8, F2, -3, 3, false);
        for (int[] w : new int[][]{{-7, 10, -19}, {11, 10, -19}, {11, 10, 0}}) c.set(w[0], w[1], w[2], "minecraft:cobweb");
    }

    /** The basement stair (behind the throne gate) and the storeroom beside it. */
    private static void stairHall(Canvas c) {
        for (int x = ST_X0; x <= ST_X1; x++) for (int z = WZ0 + 1; z <= 0; z++) c.set(x, F0, z, SLABSTONE.at(x, F0, z));
        for (int k = 1; k <= STEPS; k++) {
            int z = -k, y = F0 - k;
            for (int x = ST_X0; x <= ST_X1; x++) {
                c.set(x, y, z, St.stairs("minecraft:stone_brick_stairs", Dir.SOUTH, false));
                for (int yy = CELLAR; yy < y; yy++) c.set(x, yy, z, FOOTING.at(x, yy, z));
                for (int yy = y + 1; yy < F1; yy++) c.carve(x, yy, z);
            }
        }
        Lighting.hanging(c, -10, F1, -4, 1, false);
        Lighting.hanging(c, -10, F1, -14, 1, false);
        // The storeroom: crates (barrels would be block entities: hay and boards), shelves.
        c.set(-11, 0, -18, "minecraft:hay_block");
        c.set(-11, 0, -17, "minecraft:spruce_planks");
        c.set(-11, 1, -18, "minecraft:spruce_planks");
        c.set(-9, 0, -19, "minecraft:bookshelf");
        c.set(-10, 0, -19, "minecraft:bookshelf");
    }

    // --- upstairs ------------------------------------------------------------------------------------------------------------

    private static void upperFloors(Canvas c) {
        // First floor: the dormitory (west), the records room (east), a closet over the stair hall.
        for (int x = WX0 + 2; x <= -HALL_X - 2; x += 3) {
            for (int z = -17; z <= 16; z += 5) {
                c.setIfAir(x, F1 + 1, z, St.slabBottom("minecraft:spruce_slab"));
                c.setIfAir(x, F1 + 2, z, St.carpet(Noise.chance(x, 0, z, SEED + 110, 0.4) ? "gray" : "white"));
            }
        }
        for (int z = -18; z <= 18; z += 3) {
            if (z == 0 || z == 3 || Math.abs(z - MID_Z) <= 1) continue;
            Furniture.filing(c, WX1 - 1, F1 + 1, z, Dir.WEST, "minecraft:gray_terracotta");
            Furniture.filing(c, HALL_X + 1, F1 + 1, z, Dir.EAST, "minecraft:gray_terracotta");
        }
        Furniture.bookshelves(c, 18, -18, Dir.EAST, 3, F1 + 1, F1 + 3, Dir.SOUTH, false, null, 0);
        Furniture.bookshelves(c, 18, 17, Dir.EAST, 3, F1 + 1, F1 + 3, Dir.NORTH, false, null, 0);
        for (int x : new int[]{-22, -18, -14}) for (int z = -15; z <= 15; z += 6) Lighting.hanging(c, x, F2, z, 1, false);
        for (int x : new int[]{16, 21}) for (int z = -15; z <= 15; z += 6) Lighting.hanging(c, x, F2, z, 1, false);
        Lighting.hanging(c, -10, F2, -10, 1, false);
        // Holes through the first floor in the wings.
        for (int[] h : new int[][]{{-20, 10}, {19, -8}}) {
            for (int x = h[0] - 1; x <= h[0] + 1; x++) for (int z = h[1] - 1; z <= h[1] + 1; z++) if (!inEllipse(x, z, h[0], h[1], 1.2, 1.2)) continue; else c.carve(x, F1, z);
        }
        // Second floor: one attic ward under the roof, cots and a few pillars of brick, lanterns.
        for (int x = WX0 + 3; x <= WX1 - 3; x += 4) {
            for (int z : new int[]{-17, -9, 9, 17}) {
                if (!c.has(x, F2, z) || !c.isAir(x, F2 + 1, z)) continue;
                c.set(x, F2 + 1, z, St.slabBottom("minecraft:spruce_slab"));
                c.set(x, F2 + 2, z, St.carpet(Noise.chance(x, 1, z, SEED + 111, 0.3) ? "light_gray" : "white"));
            }
        }
        for (int x = -20; x <= 20; x += 8) {
            for (int z : new int[]{-6, 6}) {
                if (Math.abs(x) == HALL_X) continue;
                for (int y = F2 + 1; y < ROOF; y++) c.set(x, y, z, BRICK.at(x, y, z));
            }
        }
        for (int x = -23; x <= 23; x += 5) {
            for (int z : new int[]{-17, -9, 0, 9, 17}) {
                if (!c.isAir(x, F2 + 1, z) || !c.has(x, ROOF, z) || Math.abs(x) == HALL_X) continue;
                Lighting.hanging(c, x, ROOF, z, 1, false);
            }
        }
        // A few broken boards in the second floor.
        for (int[] h : new int[][]{{-6, -14}, {9, 8}}) c.carve(h[0], F2, h[1]);
    }

    // --- the throne room -----------------------------------------------------------------------------------------------------

    /** How high the vault's air reaches over (x, z): groin vaults over bays between the piers ({@link #bay}). */
    static int vaultTop(int x, int z) {
        double[] bx = bay(x, new int[]{-12, -8, 8, 12}), bz = bay(-z, new int[]{0, 7, 14, 22});
        double hx = -6 + rise(bx[1]) * (1 - Math.pow(2 * bx[0] - 1, 2));
        double hz = -6 + rise(bz[1]) * (1 - Math.pow(2 * bz[0] - 1, 2));
        return (int) Math.round(Math.max(hx, hz));
    }

    private static double rise(double span) {
        return span >= 12 ? 3 : span >= 6 ? 2.6 : 2;
    }

    /** Where v falls between the rib lines: {fraction 0..1, span}. */
    private static double[] bay(int v, int[] lines) {
        for (int i = 0; i + 1 < lines.length; i++) {
            if (v >= lines[i] && v <= lines[i + 1]) return new double[]{(v - lines[i]) / (double) (lines[i + 1] - lines[i]), lines[i + 1] - lines[i]};
        }
        return new double[]{0, 1};
    }

    private static boolean pier(int x, int z) {
        return Math.abs(x) == 8 && (z == -7 || z == -14);
    }

    /** Crowley's throne room: a brick cellar under groin vaults, the dais and throne, the barred windows glowing, files, the rug. */
    private static void cellar(Canvas c) {
        int y0 = CELLAR + 1;
        Brush floor = Palette.mix(SEED + 120, "minecraft:bricks", 3, "minecraft:mud_bricks", 2, "minecraft:packed_mud", 2,
                "minecraft:red_terracotta", 1, "minecraft:polished_granite", 1);
        for (int x = -TR_X - 1; x <= TR_X + 1; x++) {
            for (int z = TR_BACK; z <= 0; z++) {
                c.set(x, CELLAR - 1, z, FOOTING.at(x, CELLAR - 1, z));
                boolean wall = Math.abs(x) == TR_X + 1 || z == TR_BACK || z == 0;
                if (wall) {
                    for (int y = CELLAR; y <= F0 - 1; y++) c.set(x, y, z, CELLAR_BRICK.at(x, y, z));
                    continue;
                }
                c.set(x, CELLAR, z, floor.at(x, CELLAR, z));
                boolean stair = x >= ST_X0 && x <= ST_X1 && z >= -STEPS;
                if (stair) continue;
                if (x == -8 && z >= -STEPS) {
                    for (int y = y0; y <= F0 - 1; y++) c.set(x, y, z, CELLAR_BRICK.at(x, y, z));
                    continue;
                }
                if (pier(x, z)) {
                    for (int y = y0; y <= F0 - 1; y++) c.set(x, y, z, y == y0 ? "minecraft:polished_blackstone_bricks" : CELLAR_BRICK.at(x, y, z));
                    continue;
                }
                int top = vaultTop(x, z);
                for (int y = y0; y <= top; y++) c.carve(x, y, z);
                for (int y = top + 1; y <= F0 - 1; y++) c.set(x, y, z, CELLAR_BRICK.at(x, y, z));
            }
        }
        // The stair passage's own walls stay (x = -8 west of the nave); the stair lands at z = -9.
        dais(c);
        windowsBehind(c);
        rug(c);
        files(c);
        // Light: lanterns hung from the vaults' crowns, sconces on the piers, candles.
        for (int[] l : new int[][]{{0, -4}, {-4, -11}, {4, -11}, {0, -18}, {10, -4}, {10, -11}, {10, -18}, {-10, -11}, {-10, -18}}) {
            int top = vaultTop(l[0], l[1]);
            Lighting.hanging(c, l[0], top + 1, l[1], 1, l[0] == 0 && l[1] == -18);
        }
        for (int[] s : new int[][]{{8, -6, 1}, {-8, -15, -1}, {8, -15, -1}}) {
            c.set(s[0], y0 + 1, s[1], St.trapdoor("minecraft:dark_oak_trapdoor", s[2] > 0 ? Dir.NORTH : Dir.SOUTH, true, false));
            c.set(s[0], y0 + 2, s[1], St.lantern(false));
        }
        for (int x : new int[]{-TR_X, TR_X}) {
            for (int z = -3; z >= -19; z -= 8) {
                if (x < 0 && z >= -STEPS) continue;            // the stair passage
                Dir out = x < 0 ? Dir.EAST : Dir.WEST;
                Lighting.sconce(c, x, y0 + 2, z, out, "minecraft:dark_oak_trapdoor", false);
            }
        }
    }

    /** The dais: three wide steps, the platform, Crowley's gothic throne of blackstone against the back wall. */
    private static void dais(Canvas c) {
        int y0 = CELLAR + 1;
        String face = "minecraft:polished_blackstone_bricks";
        for (int t = 0; t < 3; t++) {
            int w = 6 - t, zf = -17 - t;
            for (int x = -w; x <= w; x++) {
                for (int z = zf; z >= TR_Z0; z--) {
                    for (int y = y0; y <= y0 + t; y++) c.set(x, y, z, y == y0 + t && Math.abs(x) <= 4 && z == zf
                            ? St.stairs("minecraft:polished_blackstone_brick_stairs", Dir.NORTH, false) : face);
                }
            }
        }
        // Red carpet up the steps' middle and over the platform.
        for (int z = -20; z >= -21; z--) for (int x = -2; x <= 2; x++) c.set(x, y0 + 3, z, St.carpet(Math.abs(x) <= 1 ? "red" : "black"));
        // The throne: a slab seat (he sits on it), arms with spires, a tall back with a gilded crest.
        int sy = THRONE_SEAT.y(), sz = THRONE_SEAT.z();
        c.set(0, sy, sz, St.slabBottom("minecraft:polished_blackstone_slab"));
        for (int x : new int[]{-1, 1}) {
            c.set(x, sy, sz, "minecraft:polished_blackstone_bricks");
            c.set(x, sy + 1, sz, St.wall("minecraft:polished_blackstone_brick_wall"));
            c.set(x, sy + 2, sz, St.rod("minecraft:lightning_rod", Dir.UP));
        }
        for (int x = -1; x <= 1; x++) {
            for (int y = sy; y <= F0 - 1; y++) c.set(x, y, TR_BACK, x == 0 && y == sy + 3 ? "minecraft:gilded_blackstone"
                    : x == 0 && y == sy + 2 ? "minecraft:chiseled_polished_blackstone" : "minecraft:polished_blackstone");
        }
        for (int x : new int[]{-3, 3}) {
            c.set(x, y0 + 3, -20, St.soulLantern(false));
        }
    }

    /** Two tall barred windows with pointed tops in the back wall, a glow beyond them (a light well). */
    private static void windowsBehind(Canvas c) {
        for (int s : new int[]{-1, 1}) {
            for (int dx = 4; dx <= 6; dx++) {
                int x = s * dx;
                int top = dx == 5 ? -4 : -5;
                for (int y = -8; y <= top; y++) {
                    c.set(x, y, TR_BACK, St.bars());
                    c.set(x, y, TR_BACK - 1, Noise.chance(x, y, 0, SEED + 130, 0.5) ? GLOW : "minecraft:ochre_froglight");
                }
                for (int y = top + 1; y <= F0 - 1; y++) c.set(x, y, TR_BACK - 1, FOOTING.at(x, y, TR_BACK - 1));
                c.set(x, -9, TR_BACK - 1, FOOTING.at(x, -9, TR_BACK - 1));
                c.set(x, -9, TR_BACK, "minecraft:polished_blackstone_bricks");
            }
            c.set(s * 4, -4, TR_BACK, St.stairs("minecraft:polished_blackstone_brick_stairs", s > 0 ? Dir.WEST : Dir.EAST, true));
            c.set(s * 6, -4, TR_BACK, St.stairs("minecraft:polished_blackstone_brick_stairs", s > 0 ? Dir.EAST : Dir.WEST, true));
        }
    }

    /** A worn rug in the middle of the room over a devil's trap painted on the bricks. */
    private static void rug(Canvas c) {
        int y = CELLAR, cx = THRONE.x(), cz = THRONE.z();
        // The trap: a ring and a five-pointed star in red on the floor.
        for (int x = cx - 4; x <= cx + 4; x++) {
            for (int z = cz - 4; z <= cz + 4; z++) {
                double d = Math.hypot(x - cx, z - cz);
                if (d >= 3.5 && d <= 4.3) c.set(x, y, z, "minecraft:red_concrete");
            }
        }
        double[][] pts = new double[5][];
        for (int i = 0; i < 5; i++) {
            double a = -Math.PI / 2 + i * 2 * Math.PI / 5;
            pts[i] = new double[]{cx + 3.6 * Math.cos(a), cz + 3.6 * Math.sin(a)};
        }
        for (int i = 0; i < 5; i++) {
            double[] p = pts[i], q = pts[(i + 2) % 5];
            for (int k = 0; k <= 16; k++) {
                double t = k / 16.0;
                c.set((int) Math.round(p[0] + (q[0] - p[0]) * t), y, (int) Math.round(p[1] + (q[1] - p[1]) * t), "minecraft:red_concrete");
            }
        }
        // The rug, worn through in patches (the trap shows there).
        for (int x = cx - 3; x <= cx + 3; x++) {
            for (int z = cz - 3; z <= cz + 3; z++) {
                if (Noise.chance(x, 0, z, SEED + 140, 0.18)) continue;
                boolean border = Math.abs(x - cx) == 3 || Math.abs(z - cz) == 3;
                c.set(x, y + 1, z, St.carpet(border ? "black" : Noise.chance(x, 1, z, SEED + 141, 0.3) ? "brown" : "red"));
            }
        }
    }

    /** Tables with stacks of files in the aisles, filing cabinets and shelves on the walls, papers on the floor. */
    private static void files(Canvas c) {
        int y0 = CELLAR + 1;
        Furniture.table(c, 10, -11, 10, -9, y0, "minecraft:dark_oak_slab");
        Furniture.table(c, 10, -4, 10, -3, y0, "minecraft:dark_oak_slab");
        Furniture.table(c, -10, -18, -10, -16, y0, "minecraft:dark_oak_slab");
        for (int[] p : new int[][]{{10, -11}, {10, -9}, {10, -3}, {-10, -18}, {-10, -16}}) c.set(p[0], y0 + 1, p[1], St.carpet("white"));
        c.set(10, y0 + 1, -10, St.candle("red", 3, true));
        c.set(-10, y0 + 1, -17, St.candle("red", 2, true));
        c.set(10, y0 + 1, -4, St.candle("black", 1, true));
        for (int z = -19; z <= -17; z++) Furniture.filing(c, TR_X, y0, z, Dir.WEST, "minecraft:gray_terracotta");
        Furniture.bookshelves(c, -TR_X, -13, Dir.SOUTH, 2, y0, y0 + 1, Dir.EAST, false, null, 0);
        for (int[] p : new int[][]{{7, -2}, {-6, -16}, {9, -15}, {-3, -2}, {6, -12}}) c.setIfAir(p[0], y0, p[1], St.carpet("white"));
    }

    // --- outside -------------------------------------------------------------------------------------------------------------

    /** Ivy and moss up the walls, brush at their feet, trees crowding the front corners and the sides. */
    private static void trees(Canvas c) {
        // Drawn before the building: it wins over their crowns.
        int[][] trees = {{-19, 25, 0}, {20, 26, 0}, {-13, 29, 2}, {14, 29, 2}, {23, 22, 1}};
        for (int[] t : trees) {
            int y = LAND + 1;
            switch (t[2]) {
                case 0 -> Trees.oak(c, t[0], y, t[1], 0, SEED + 150 + t[0]);
                case 1 -> Trees.deadOak(c, t[0], y, t[1], 0, false, SEED + 151 + t[0]);
                default -> Trees.spruce(c, t[0], y, t[1], 0, SEED + 152 + t[0]);
            }
        }
    }

    private static void overgrowth(Canvas c) {
        // Lamp posts along the drive (iron and stone, one gone dark).
        for (int[] l : new int[][]{{-4, 30}, {4, 30}, {-4, 34}, {4, 34}}) {
            Lighting.lampPost(c, l[0], LAND + 1, l[1], 2, "minecraft:stone_bricks", "minecraft:stone_brick_wall", null, false);
        }
        // Ivy: vines down the outer walls in patches, from the cornice.
        for (Walls.Run r : Walls.around(FOOT)) {
            Walls.Run o = r.offset(1);
            Dir back = r.out().opposite();
            for (int i = 0; i < r.length(); i++) {
                int ox = o.x(i), oz = o.z(i);
                if (Noise.patch2(ox, oz, 6, SEED + 160) < 0.55) continue;
                int len = 4 + Noise.pick(ox, 0, oz, SEED + 161, 12);
                for (int y = ROOF - 1; y >= ROOF - len && y >= 0; y--) {
                    String wall = c.get(r.x(i), y, r.z(i));
                    if (!c.isAir(ox, y, oz) && c.has(ox, y, oz) || wall == null || !Kinds.fullCube(wall)) break;
                    c.set(ox, y, oz, St.vine(back));
                }
            }
        }
        // Brush and bushes at the walls' feet and over the grounds.
        for (int x = AsylumAnchors.MIN_X; x <= AsylumAnchors.MAX_X; x++) {
            for (int z = AsylumAnchors.MIN_Z; z <= AsylumAnchors.MAX_Z; z++) {
                String g = c.get(x, LAND, z);
                if (g == null || !St.id(g).equals("minecraft:grass_block") || c.has(x, LAND + 1, z)) continue;
                boolean byWall = (Math.abs(x) == WX1 + 1 && z >= WZ0 && z <= WZ1) || ((z == WZ0 - 1 || z == WZ1 + 1) && Math.abs(x) <= WX1);
                if (byWall && Noise.chance(x, 0, z, SEED + 170, 0.45)) {
                    c.set(x, LAND + 1, z, St.leaves(Noise.chance(x, 1, z, SEED + 171, 0.3) ? "minecraft:flowering_azalea_leaves" : "minecraft:azalea_leaves"));
                    continue;
                }
                if (Noise.chance(x, 0, z, SEED + 172, 0.35)) {
                    c.set(x, LAND + 1, z, switch (Noise.pick(x, 0, z, SEED + 173, 6)) {
                        case 0, 1, 2 -> "minecraft:short_grass";
                        case 3 -> "minecraft:fern";
                        case 4 -> "minecraft:sweet_berry_bush[age=1]";
                        default -> "minecraft:dead_bush";
                    });
                }
            }
        }
    }
}
