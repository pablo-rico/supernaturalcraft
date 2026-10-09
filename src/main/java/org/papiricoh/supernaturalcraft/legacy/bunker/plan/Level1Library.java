package org.papiricoh.supernaturalcraft.legacy.bunker.plan;

import static org.papiricoh.supernaturalcraft.legacy.bunker.plan.Kit.*;
import static org.papiricoh.supernaturalcraft.legacy.bunker.plan.Level1.*;

/**
 * The library (pure), after the series' set. A hall 26 × 33 and 13 high, of red-brown brick over a polished granite skirting, with
 * a cream frieze and cornice. The ceiling is coffered: cream beams run from pillar to pillar, with spruce panels between. Six square
 * pillars stand in two rows (cream bases and capitals, fluted quartz shafts, tube sconces). They divide the hall into a nave 9 wide and
 * two aisles 10 wide. Down the nave runs one long reading table with captain's chairs and an amber lamp, under two rows of
 * globe pendants. The aisles hold low bookcases on every wall, the four research desks and small reading tables. At the far (west)
 * end a cream-framed arch, hung with red curtains, holds a brass telescope, with a lamp table before it. Over the east end is a
 * mezzanine, reached from the war room's west gallery door. It has a wooden balustrade, and a flight down into each aisle, against
 * the wall.
 *
 * <p>Axes: the nave runs along X from the war room's door (east) to the arch (west), on the rotunda's axis z = -30.
 */
final class Level1Library {

    private static final int F = Zones.LEVEL1, M = Zones.GALLERY, CZ = Zones.ROTUNDA_Z;
    /** The hall's inside; the cornice row, the coffers' ceiling. */
    static final int X0 = -44, X1 = -19, Z0 = -46, Z1 = -14, H = 13, TOP = F + H, CEIL = TOP + 1;
    /** The mezzanine's front edge: it spans x MX..X1 at the gallery's floor. */
    static final int MX = -23;
    /** The pillars, 2 × 2 at (px, px+1) × (pz, pz+1); the bays between them are 5 wide, centred on {@link #BAY}. */
    static final int[] PX = {-39, -32, -25}, PZ = {-36, -25}, BAY = {-42, -35, -28};
    /** The flights: from the mezzanine's edge down west, 8 steps, one against each long wall. */
    static final int FLIGHT_X = -24, STEPS = 8;

    static final String BRICK = "minecraft:bricks", GRANITE = "minecraft:polished_granite", FLUTE = St.axis("minecraft:quartz_pillar", "y"),
            BORDER = "minecraft:stripped_dark_oak_wood", INLAY = "minecraft:mangrove_planks", COFFER = "minecraft:spruce_planks",
            TREAD = "minecraft:spruce_stairs", SEAT = "minecraft:warped_stairs", BRASS = "minecraft:waxed_copper_trapdoor",
            LEATHER = "minecraft:red_nether_brick_stairs", ARMREST = "minecraft:mangrove_trapdoor",
            ROD = St.of("end_rod", "facing", "up");
    /** Red-brown brick, a burnt brick or a tan one here and there. */
    static final Pattern BRICKS = mix(31, BRICK, 26, "minecraft:terracotta", 1, "minecraft:mud_bricks", 1);
    static final Pattern PARQUET = parquet(OAK, "minecraft:spruce_planks", true);

    private Level1Library() {
    }

    static boolean pillarX(int x) {
        for (int px : PX) if (x == px || x == px + 1) return true;
        return false;
    }

    static boolean pillarZ(int z) {
        for (int pz : PZ) if (z == pz || z == pz + 1) return true;
        return false;
    }

    /** The walls by height: a granite skirting, brick, a cut frieze course, then cream to the cornice. */
    static Pattern wall() {
        return (x, y, z) -> {
            int off = y - F;
            if (off <= 1) return GRANITE;
            if (off <= 10) return BRICKS.at(x, y, z);
            return off == 11 ? CREAM_COURSE : CREAM;
        };
    }

    /** Dark parquet with a stripped-oak border along the walls and a mangrove inlay just inside it. */
    static Pattern floor() {
        return (x, y, z) -> {
            int d = Math.min(Math.min(x - X0, X1 - x), Math.min(z - Z0, Z1 - z));
            return d <= 1 ? BORDER : d == 2 ? INLAY : PARQUET.at(x, y, z);
        };
    }

    static void build(Plan p) {
        room(p, new Room(X0, X1, F, H, Z0, Z1), new Style(floor(), wall(), solid(COFFER), CREAM_STAIRS, null, 0, null, 0));
        beams(p);
        mezzanine(p);
        for (int px : PX) for (int pz : PZ) pillar(p, px, pz);
        pilasters(p);
        flights(p);
        arch(p);
        doorways(p);
        shelves(p);
        longTable(p);
        aisles(p);
        lights(p);
        furnish(p);
        p.view("library", -21, F + 1, CZ, -45, -15, CZ);
        p.view("reading_tables", -27, F + 1, -33, -37, -17, -30);
        p.view("library_gallery", -21, M + 1, -28, -44, -14, -35);
        p.view("library_stairs", -38, F + 1, -39, -24, -12, -44);
    }

    // --- structure ------------------------------------------------------------------------------------------------------------

    /** The coffers' cream beams, a block deep under the ceiling: over the pillar lines both ways and down the nave's axis. */
    private static void beams(Plan p) {
        for (int x = X0; x <= X1; x++) {
            for (int z = Z0; z <= Z1; z++) {
                if (pillarX(x) || pillarZ(z) || z == CZ && x < MX) p.set(x, TOP, z, pillarX(x) && pillarZ(z) ? CREAM_COURSE : CREAM);
            }
        }
    }

    /** A cream shaft one block square: a smooth base, a cut band, fluted quartz, a cut band, a smooth top. */
    private static void shaft(Plan p, int x, int z, int y0, int y1) {
        for (int y = y0; y <= y1; y++) p.set(x, y, z, y == y0 || y == y1 ? CREAM : y == y0 + 1 || y == y1 - 1 ? CREAM_COURSE : FLUTE);
    }

    /** A free-standing pillar, 2 × 2: four shafts, a plinth step of slabs round its foot, a flared capital under the beams. */
    private static void pillar(Plan p, int px, int pz) {
        for (int x = px; x <= px + 1; x++) for (int z = pz; z <= pz + 1; z++) shaft(p, x, z, F + 1, TOP);
        for (int x = px - 1; x <= px + 2; x++) {
            for (int z = pz - 1; z <= pz + 2; z++) {
                if (x >= px && x <= px + 1 && z >= pz && z <= pz + 1) continue;
                String toward = z < pz ? "south" : z > pz + 1 ? "north" : x < px ? "east" : "west";
                if (p.isAir(x, F + 1, z)) p.set(x, F + 1, z, St.slab(CREAM_SLAB, false));
                if (p.isAir(x, TOP - 1, z)) p.set(x, TOP - 1, z, St.stairs(CREAM_STAIRS, toward, true));
            }
        }
    }

    /** Pilasters on the walls in line with the pillars (on the east wall, through the mezzanine). */
    private static void pilasters(Plan p) {
        for (int px : PX) {
            for (int x = px; x <= px + 1; x++) {
                shaft(p, x, Z0, F + 1, TOP);
                shaft(p, x, Z1, F + 1, TOP);
            }
        }
        for (int pz : PZ) {
            for (int z = pz; z <= pz + 1; z++) {
                shaft(p, X0, z, F + 1, TOP);
                shaft(p, X1, z, F + 1, TOP);
            }
        }
    }

    /**
     * The mezzanine over the east end: its parquet, a cream fascia with a moulding under the edge, a dark oak balustrade (lanterns
     * on its end posts) and lanterns hung beneath it.
     */
    private static void mezzanine(Plan p) {
        Pattern fl = floor();
        for (int x = MX; x <= X1; x++) for (int z = Z0; z <= Z1; z++) p.set(x, M, z, fl.at(x, M, z));
        for (int z = Z0; z <= Z1; z++) {
            p.set(MX, M, z, CREAM);
            p.set(MX, M - 1, z, CREAM_COURSE);
            if (p.isAir(MX - 1, M - 1, z)) p.set(MX - 1, M - 1, z, St.stairs(CREAM_STAIRS, "east", true));
        }
        for (int z = Z0 + 4; z <= Z1 - 4; z++) p.set(MX, M + 1, z, St.fence(OAK_FENCE));
        p.set(MX, M + 2, Z0 + 4, St.lantern(false));
        p.set(MX, M + 2, Z1 - 4, St.lantern(false));
        for (int z : new int[]{-43, -36, -24, -17}) p.set(-21, M - 1, z, St.lantern(true));
    }

    /**
     * A flight down each aisle against its long wall: three lanes, the open one carrying chain balusters, a dark oak soffit, a newel
     * with a lantern at the foot. Beside it the wall is brought forward flush with the bookcases, a handrail let into it.
     */
    private static void flights(Plan p) {
        for (int side : new int[]{-1, 1}) {
            int wallZ = side < 0 ? Z0 : Z1, wallLane = wallZ - side, openLane = wallZ - 3 * side;
            flight(p, FLIGHT_X, wallLane, M, "west", side < 0 ? "south" : "north", 3, STEPS, TREAD, OAK_STAIRS);
            balusters(p, FLIGHT_X, openLane, M, "west", STEPS);
            Pattern w = wall();
            for (int x = FLIGHT_X - STEPS + 1; x <= FLIGHT_X; x++) {
                if (pillarX(x)) continue;
                for (int y = F + 1; y <= TOP; y++) p.set(x, y, wallZ, w.at(x, y, wallZ));
                p.set(x, M - (FLIGHT_X - x) + 2, wallZ, St.axis(OAK_LOG, "x"));
            }
            p.set(FLIGHT_X - STEPS, F + 1, openLane, St.axis(OAK_LOG, "y"));
            p.set(FLIGHT_X - STEPS, F + 2, openLane, St.lantern(false));
        }
    }

    /**
     * The far end: a cream-framed arch, 5 wide and 7 high, set in the west wall. Red curtains are drawn to its sides under a red
     * valance, with a red backdrop behind. A brass telescope stands in it on a tripod, aimed up over the hall.
     */
    private static void arch(Plan p) {
        int ax = X0 - 1, back = X0 - 2;
        for (int z = CZ - 3; z <= CZ + 3; z++) for (int y = F; y <= F + 8; y++) p.set(back, y, z, "minecraft:red_terracotta");
        for (int z = CZ - 2; z <= CZ + 2; z++) {
            p.set(ax, F, z, GRANITE);
            for (int y = F + 1; y <= F + 6; y++) p.set(ax, y, z, St.AIR);
        }
        for (int z = CZ - 1; z <= CZ + 1; z++) p.set(ax, F + 7, z, St.AIR);
        // The frame in front of the wall: two piers and a head, the arch's shoulders rounded on both faces.
        for (int y = F + 1; y <= F + 8; y++) {
            p.set(X0, y, CZ - 3, CREAM_COURSE);
            p.set(X0, y, CZ + 3, CREAM_COURSE);
        }
        for (int z = CZ - 2; z <= CZ + 2; z++) p.set(X0, F + 8, z, CREAM);
        for (int x : new int[]{ax, X0}) {
            p.set(x, F + 7, CZ - 2, St.stairs(CREAM_STAIRS, "north", true));
            p.set(x, F + 7, CZ + 2, St.stairs(CREAM_STAIRS, "south", true));
        }
        // Curtains drawn to the sides, a valance across the top, a strip of red carpet.
        for (int y = F + 1; y <= F + 6; y++) {
            p.set(ax, y, CZ - 2, "minecraft:red_wool");
            p.set(ax, y, CZ + 2, "minecraft:red_wool");
        }
        for (int z = CZ - 1; z <= CZ + 1; z++) {
            p.set(ax, F + 7, z, St.of("red_wall_banner", "facing", "east"));
            p.set(ax, F + 1, z, St.carpet("red"));
        }
        // The telescope: a brass tripod post, the tube laid over it pointing out into the hall, an eyepiece behind.
        p.set(ax, F + 1, CZ, St.of("lightning_rod", "facing", "up", "powered", "false", "waterlogged", "false"));
        p.set(ax, F + 2, CZ, St.of("lightning_rod", "facing", "up", "powered", "false", "waterlogged", "false"));
        p.set(ax, F + 3, CZ, St.of("lightning_rod", "facing", "east", "powered", "false", "waterlogged", "false"));
        p.set(X0, F + 4, CZ, St.of("lightning_rod", "facing", "east", "powered", "false", "waterlogged", "false"));
        p.set(ax, F + 1, CZ - 1, St.chain("y"));
        p.set(ax, F + 1, CZ + 1, St.chain("y"));
        // The lamp table before it, the order's banner over the arch.
        table(p, X0 + 1, X0 + 1, F + 1, CZ - 1, CZ + 1, OAK_SLAB);
        amberLamp(p, X0 + 1, F + 2, CZ - 1);
        amberLamp(p, X0 + 1, F + 2, CZ + 1);
        p.set(X0 + 1, F + 2, CZ, St.candle("brown", 2, true));
        banner(p, X0, F + 10, CZ, "east", "black", ORDER_BANNER);
    }

    /** The doorways from the war room: on the floor under the mezzanine, and from the west gallery onto the mezzanine. */
    private static void doorways(Plan p) {
        doorway(p, X1 + 2, F, CZ, "west", 2, 3, 4, CREAM_COURSE, CREAM_STAIRS, null);
        doorway(p, X1 + 2, M, CZ, "west", 2, 3, 3, CREAM_COURSE, CREAM_STAIRS, null);
        for (int x = X1 + 1; x <= X1 + 2; x++) {
            for (int z = CZ - 1; z <= CZ + 1; z++) {
                p.set(x, F, z, GRANITE);
                p.set(x, M, z, GRANITE);
            }
        }
    }

    // --- furniture ------------------------------------------------------------------------------------------------------------

    /** Low bookcases on every wall between the pilasters, on the floor and on the mezzanine. */
    private static void shelves(Plan p) {
        for (int[] run : new int[][]{{X0, -40}, {-37, -33}}) {
            bookcases(p, run[0], Z0, run[1], Z0, F + 1, 3, "south", OAK_STAIRS);
            bookcases(p, run[0], Z1, run[1], Z1, F + 1, 3, "north", OAK_STAIRS);
        }
        bookcases(p, MX, Z0, X1, Z0, F + 1, 3, "south", OAK_STAIRS);
        bookcases(p, MX, Z1, X1, Z1, F + 1, 3, "north", OAK_STAIRS);
        bookcases(p, MX, Z0, X1, Z0, M + 1, 2, "south", OAK_STAIRS);
        bookcases(p, MX, Z1, X1, Z1, M + 1, 2, "north", OAK_STAIRS);
        for (int[] run : new int[][]{{Z0 + 1, -37}, {-34, -34}, {-26, -26}, {-23, Z1 - 1}}) {
            bookcases(p, X0, run[0], X0, run[1], F + 1, 3, "east", OAK_STAIRS);
        }
        for (int[] run : new int[][]{{Z0 + 1, -37}, {-34, -32}, {-28, -26}, {-23, Z1 - 1}}) {
            bookcases(p, X1, run[0], X1, run[1], F + 1, 3, "west", OAK_STAIRS);
            bookcases(p, X1, run[0], X1, run[1], M + 1, 2, "west", OAK_STAIRS);
        }
        // The rolling ladders.
        for (int y = F + 1; y <= F + 3; y++) {
            p.set(-37, y, Z0 + 1, St.of("ladder", "facing", "south", "waterlogged", "false"));
            p.set(X0 + 1, y, -37, St.of("ladder", "facing", "east", "waterlogged", "false"));
        }
    }

    /**
     * The nave's long reading table down the axis: 3 wide, 11 long, five captain's chairs a side (green leather seats, dark oak
     * arms) and one at each end, an amber lamp in the middle, candles and clutter along it.
     */
    private static void longTable(Plan p) {
        int xa = -38, xb = -28, y = F + 1;
        table(p, xa, xb, y, CZ - 1, CZ + 1, OAK_SLAB);
        for (int x = xa + 1; x < xb; x += 2) {
            chair(p, x, y, CZ - 2, "south", SEAT, OAK_TRAPDOOR);
            chair(p, x, y, CZ + 2, "north", SEAT, OAK_TRAPDOOR);
        }
        chair(p, xa - 1, y, CZ, "east", SEAT, null);
        chair(p, xb + 1, y, CZ, "west", SEAT, null);
        amberLamp(p, -33, y + 1, CZ);
        p.set(-37, y + 1, CZ - 1, St.candle("orange", 3, true));
        p.set(-29, y + 1, CZ + 1, St.candle("orange", 2, true));
        p.set(-36, y + 1, CZ + 1, pot("south"));
        p.set(-30, y + 1, CZ - 1, "minecraft:potted_fern");
        p.set(-31, y + 1, CZ, St.candle("white", 1, false));
    }

    /**
     * The aisles: a research desk in the first two bays against each wall (a barrel or a lectern beside it, a chair before it), and
     * a small reading table with four chairs in the middle of each bay.
     */
    private static void aisles(Plan p) {
        for (int i = 0; i < DESKS.length; i++) {
            int[] d = DESKS[i];
            boolean north = d[2] < CZ;
            String out = north ? "south" : "north";
            p.set(d[0], d[1], d[2], DESK);
            chair(p, d[0], d[1], d[2] + (north ? 1 : -1), north ? "north" : "south", SEAT, OAK_TRAPDOOR);
            if (d[0] == BAY[0]) loot(p, d[0] - 1, d[1], d[2], barrel(out), "supernaturalcraft:chests/bunker_library");
            else p.set(d[0] - 1, d[1], d[2], St.of("lectern", "facing", out, "has_book", "true", "powered", "false"));
        }
        for (int zt : new int[]{-41, -19}) {
            for (int xc : BAY) {
                table(p, xc - 1, xc + 1, F + 1, zt, zt, OAK_SLAB);
                chair(p, xc - 1, F + 1, zt - 1, "south", SEAT, null);
                chair(p, xc + 1, F + 1, zt - 1, "south", SEAT, null);
                chair(p, xc - 1, F + 1, zt + 1, "north", SEAT, null);
                chair(p, xc + 1, F + 1, zt + 1, "north", SEAT, null);
                p.set(xc, F + 2, zt, (xc + zt & 1) == 0 ? St.candle("orange", 2, true) : St.candle("white", 3, true));
            }
        }
    }

    /**
     * The light: two rows of globe pendants (pearlescent froglights on chains) over the nave, pairs of glowing tube sconces on every pillar's
     * nave and aisle faces, brass louvred lamps on the walls over the bookcases, lanterns under and on the mezzanine.
     */
    private static void lights(Plan p) {
        for (int xc : BAY) {
            for (int z : new int[]{CZ - 3, CZ + 3}) {
                p.set(xc, TOP, z, St.chain("y"));
                p.set(xc, TOP - 1, z, St.chain("y"));
                p.set(xc, TOP - 2, z, St.axis("minecraft:pearlescent_froglight", "y"));
            }
        }
        for (int px : PX) {
            for (int pz : PZ) {
                for (int x = px; x <= px + 1; x++) {
                    p.set(x, F + 5, pz - 1, ROD);
                    p.set(x, F + 5, pz + 2, ROD);
                }
            }
        }
        for (int x : new int[]{BAY[0], BAY[1]}) {
            louvre(p, x, F + 6, Z0 - 1, "south");
            louvre(p, x, F + 6, Z1 + 1, "north");
        }
        for (int z : new int[]{-41, -34, -26, -19}) louvre(p, X0 - 1, F + 6, z, "east");
        for (int z : new int[]{-44, -38, -22, -16}) p.set(X1, M + 4, z, St.lantern(false));
    }

    /** A brass wall lamp: a warm light set in the stone behind an open copper louvre (the set's glowing wall sconces). */
    private static void louvre(Plan p, int x, int y, int z, String out) {
        p.set(x, y, z, HIDDEN_LIGHT);
        p.set(x + St.dx(out), y, z + St.dz(out), St.trapdoor(BRASS, out, false, true));
    }

    /** The rest: the reading nooks under the mezzanine, armchairs and show cases on it, paintings, pots on the bookcases. */
    private static void furnish(Plan p) {
        // Under the mezzanine: a leather sofa with a low table at each end; the atlas on its lectern and the map table by the door.
        for (int side : new int[]{-1, 1}) {
            int zc = side < 0 ? -42 : -18;
            for (int z = zc - 1; z <= zc + 1; z++) p.set(X1 - 1, F + 1, z, St.stairs(LEATHER, "east", false));
            p.set(X1 - 1, F + 1, zc - 2, St.trapdoor(ARMREST, "north", false, true));
            p.set(X1 - 1, F + 1, zc + 2, St.trapdoor(ARMREST, "south", false, true));
            p.set(X1 - 3, F + 1, zc, St.slab(OAK_SLAB, true));
            p.set(X1 - 3, F + 2, zc, St.candle("orange", 3, true));
        }
        p.set(-21, F + 1, -34, St.of("lectern", "facing", "south", "has_book", "true", "powered", "false"));
        p.set(-21, F + 1, -26, "minecraft:cartography_table");
        // On the mezzanine: two armchairs looking out over the hall, two show cases (a granite plinth, a book laid in a frame).
        chair(p, -20, M + 1, -36, "west", LEATHER, ARMREST);
        chair(p, -20, M + 1, -24, "west", LEATHER, ARMREST);
        for (int z : new int[]{-41, -19}) {
            p.set(-20, M + 1, z, GRANITE);
            p.decor(Decor.frame(-20, M + 2, z, "up", z < CZ ? "minecraft:writable_book" : "minecraft:enchanted_book"));
        }
        // Paintings over the aisles' end walls; pots and candles on the bookcases' tops.
        p.decor(Decor.painting(X0, F + 5, -44, "east", "wanderer"));
        p.decor(Decor.painting(X0, F + 5, -16, "east", "prairie_ride"));
        p.set(X0, F + 5, -34, pot("east"));
        p.set(X0, F + 5, -26, "minecraft:potted_fern");
        p.set(-40, F + 5, Z0, St.candle("white", 2, false));
        p.set(-33, F + 5, Z1, pot("north"));
        p.set(-44, F + 5, Z1, "minecraft:potted_azalea_bush");
        p.set(X1, F + 5, -44, pot("west"));
    }
}
