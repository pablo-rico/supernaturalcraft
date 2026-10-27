package org.papiricoh.supernaturalcraft.chain.seat.tower;

import org.papiricoh.supernaturalcraft.buildkit.Box;
import org.papiricoh.supernaturalcraft.buildkit.Brush;
import org.papiricoh.supernaturalcraft.buildkit.Canvas;
import org.papiricoh.supernaturalcraft.buildkit.Dir;
import org.papiricoh.supernaturalcraft.buildkit.Kinds;
import org.papiricoh.supernaturalcraft.buildkit.Lighting;
import org.papiricoh.supernaturalcraft.buildkit.Noise;
import org.papiricoh.supernaturalcraft.buildkit.Outside;
import org.papiricoh.supernaturalcraft.buildkit.St;
import org.papiricoh.supernaturalcraft.buildkit.Trees;
import org.papiricoh.supernaturalcraft.chain.seat.dungeon.DungeonAnchors;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Draws the SucroCorp tower (pure; {@link SucroTower} is the public face): the plaza, the shell (floor slabs, curtain wall,
 * setbacks, columns), the stair core, the crown, then each floor's fit-out ({@link TowerFloors}) and a closing light pass.
 *
 * <p>Plan, in world cells (x east, z south): the tower's footprint is x {@code -20..20}, z {@link #ZN}..{@link #ZS} (41 × 41),
 * stepping in on the east and west as it rises ({@link #A}, {@link #B}, {@link #C}; notched corners). The stair core stands in
 * the south-east corner ({@link #CX0}..{@link #CX1}, {@link #CZ0}..{@link #CZ1}): two lanes running east-west, a floor-level
 * landing at the west end (doors north from it), a half landing at the east end.
 */
final class TowerDraw {

    static final int R = SucroTower.RADIUS, TZ = SucroTower.TOWER_Z, ZN = TZ - 20, ZS = TZ + 20;
    /** The stair core's outer walls. */
    static final int CX0 = 5, CX1 = 15, CZ0 = ZS - 6, CZ1 = ZS;
    /** The core's lanes (z): the north lane climbs east, the south lane climbs back west; the spine wall between them. */
    static final int LANE_A0 = CZ0 + 1, LANE_B0 = CZ0 + 4, SPINE = CZ0 + 3;
    /** The roof slab over the boardroom. */
    static final int ROOF = SucroTower.BOARDROOM_FLOOR + SucroTower.BOARDROOM_HEADROOM;

    /** A section of the tower: x half-width, and how deep the north and south corners are notched. */
    record Sec(int hx, int kn, int ks) {

        boolean inside(int x, int z) {
            int lz = z - TZ, ax = Math.abs(x);
            if (ax > hx || lz < -20 || lz > 20) return false;
            if (ax > hx - kn && lz < -20 + kn) return false;
            return !(ax > hx - ks && lz > 20 - ks);
        }

        /** On the curtain wall: inside, with something outside among its eight neighbours. */
        boolean edge(int x, int z) {
            if (!inside(x, z)) return false;
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) if (!inside(x + dx, z + dz)) return true;
            return false;
        }

        boolean interior(int x, int z) {
            return inside(x, z) && !edge(x, z);
        }
    }

    static final Sec A = new Sec(20, 4, 4), B = new Sec(19, 5, 3), C = new Sec(17, 3, 2);

    static Sec sec(int y) {
        return y < 59 ? A : y < 104 ? B : C;
    }

    /** The four interior columns (x, z), on every storey under the boardroom. */
    static final int[][] COLUMNS = {{-10, TZ - 10}, {10, TZ - 10}, {-10, TZ + 10}, {10, TZ + 10}};

    static final String SPANDREL = "minecraft:black_concrete", MULLION = "minecraft:gray_concrete", FIN = "minecraft:smooth_stone",
            CORNICE = "minecraft:yellow_terracotta", LETTER = "minecraft:ochre_froglight[axis=y]", LAMP = "minecraft:sea_lantern";
    static final Brush SLAB = TowerKit.checker("minecraft:smooth_stone", "minecraft:polished_andesite");
    static final Brush CORE_WALL = TowerKit.tri("minecraft:light_gray_concrete", "minecraft:polished_andesite");

    final long seed;
    final int s;
    final Random rnd;
    Canvas c;

    TowerDraw(long seed) {
        this.seed = seed;
        this.s = TowerKit.salt(seed) ^ 0x5C0C;
        this.rnd = new Random(seed ^ 0x5C0C0B9L);
    }

    Canvas draw() {
        c = new Canvas(SucroTower.NAME);
        c.outside(Outside.groundUpTo(-1));
        Box all = SucroTower.bounds();
        TowerFloors floors = new TowerFloors(this);
        for (String z : List.of("plaza", "shell", "core", "crown")) c.zone(z, all);
        for (SucroTower.Floor f : SucroTower.FLOORS) if (f.playable()) c.zone(f.room(), all);
        c.zone("lights", all);
        c.inZone("plaza", this::plaza);
        c.inZone("shell", this::shell);
        c.inZone("core", this::core);
        c.inZone("crown", this::crown);
        for (SucroTower.Floor f : SucroTower.FLOORS) if (f.playable()) c.inZone(f.room(), () -> floors.draw(f));
        c.anchor(DungeonAnchors.ENTRANCE, 0, 0, 33);
        c.anchor(DungeonAnchors.ARRIVAL, 0, 0, ZS - 3);
        c.inZone("lights", () -> TowerKit.lightUp(c, LAMP, 8));
        return c.finish();
    }

    // --- helpers -------------------------------------------------------------------------------------------------------------

    static boolean core(int x, int z) {
        return x >= CX0 && x <= CX1 && z >= CZ0 && z <= CZ1;
    }

    static boolean column(int x, int z) {
        for (int[] p : COLUMNS) if (p[0] == x && p[1] == z) return true;
        return false;
    }

    /** The floor grid of recessed lights: one every six cells each way. */
    static boolean lampCell(int x, int z) {
        return Math.floorMod(x, 6) == 3 && Math.floorMod(z - TZ, 6) == 3;
    }

    /** A pane of the curtain wall: dark glass, a few bays blue or grey, chosen per bay and storey. */
    String glass(int bay, int storey, int face) {
        double n = Noise.hash01(bay * 7 + face * 131, storey, face, s + 11);
        if (n < 0.42) return "minecraft:black_stained_glass";
        if (n < 0.80) return "minecraft:blue_stained_glass";
        if (n < 0.90) return "minecraft:gray_stained_glass";
        return "minecraft:light_blue_stained_glass";
    }

    // --- the plaza -----------------------------------------------------------------------------------------------------------

    private void plaza() {
        Brush paving = (x, y, z) -> Math.floorMod(x, 4) == 0 || Math.floorMod(z, 4) == 0 ? "minecraft:polished_andesite"
                : ((Math.floorDiv(x, 4) + Math.floorDiv(z, 4)) & 1) == 0 ? "minecraft:smooth_stone" : "minecraft:light_gray_concrete";
        Brush path = (x, y, z) -> Math.abs(x) == 3 ? "minecraft:yellow_terracotta" : ((x + z) & 1) == 0 ? "minecraft:smooth_quartz"
                : "minecraft:polished_diorite";
        for (int x = -35; x <= 35; x++) {
            for (int z = -35; z <= 35; z++) {
                if (A.inside(x, z)) continue;
                boolean approach = Math.abs(x) <= 3 && z > ZS;
                c.set(x, -1, z, approach ? path.at(x, -1, z) : paving.at(x, -1, z));
            }
        }
        // The SucroCorp mark set in the forecourt, its S lit from below.
        for (int i = 0; i < TowerKit.LOGO; i++) {
            for (int j = 0; j < TowerKit.LOGO; j++) {
                int m = TowerKit.logo(i, j);
                if (m != 0) c.set(i - 5, -1, 24 + j - 5, m == 1 ? "minecraft:yellow_concrete" : LETTER);
            }
        }
        pools();
        planters();
        for (int z = 18; z <= 30; z += 6) {
            for (int x : new int[]{-5, 5}) Lighting.lampPost(c, x, 0, z, 3, "minecraft:polished_andesite", "minecraft:andesite_wall", null, false);
        }
        for (int[] p : new int[][]{{-31, -31}, {31, -31}, {-31, 31}, {31, 31}, {-31, 0}, {31, 0}}) {
            Lighting.lampPost(c, p[0], 0, p[1], 3, "minecraft:polished_andesite", "minecraft:andesite_wall", null, false);
        }
        gateway();
        hedge();
    }

    /** Two reflecting pools either side of the approach, a quartz kerb round them. */
    private void pools() {
        for (int side : new int[]{-1, 1}) {
            int x0 = side < 0 ? -17 : 9, x1 = side < 0 ? -9 : 17;
            for (int x = x0 - 1; x <= x1 + 1; x++) {
                for (int z = 17; z <= 31; z++) {
                    boolean water = x >= x0 && x <= x1 && z >= 18 && z <= 30;
                    if (water) {
                        c.set(x, -2, z, ((x + z) & 1) == 0 ? "minecraft:dark_prismarine" : "minecraft:prismarine_bricks");
                        c.set(x, -1, z, St.water());
                    } else {
                        c.set(x, 0, z, St.slabBottom("minecraft:smooth_quartz_slab"));
                    }
                }
            }
        }
    }

    /** Planters with birches and flowers on the forecourt, a row of them down each side of the tower and behind it. */
    private void planters() {
        int k = 0;
        for (int side : new int[]{-1, 1}) {
            for (int z : new int[]{20, 28}) planter(side * 25, z, k++);
            for (int z = ZN + 3; z <= ZS - 3; z += 9) planter(side * 29, z, k++);
        }
        for (int x = -22; x <= 22; x += 11) planter(x, -31, k++);
    }

    private void planter(int cx, int cz, int k) {
        String[] flowers = {"minecraft:azure_bluet", "minecraft:oxeye_daisy", "minecraft:cornflower", "minecraft:lily_of_the_valley"};
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                int x = cx + dx, z = cz + dz;
                if (Math.abs(dx) == 2 || Math.abs(dz) == 2) {
                    c.set(x, 0, z, (Math.abs(dx) == 2 && Math.abs(dz) == 2) ? "minecraft:polished_andesite" : St.slabTop("minecraft:smooth_stone_slab"));
                } else {
                    c.set(x, 0, z, "minecraft:grass_block");
                    if ((dx != 0 || dz != 0) && Noise.chance(x, 1, z, s + 21, 0.6)) c.set(x, 1, z, flowers[Noise.pick(x, 1, z, s + 22, flowers.length)]);
                }
            }
        }
        if (Noise.chance(cx, 0, cz, s + 23 + k, 0.8)) Trees.birch(c, cx, 1, cz, 0, s + 24 + k);
        else Trees.bush(c, cx, 1, cz, "minecraft:flowering_azalea_leaves", 1.4, s + 25 + k);
    }

    /** The gateway over the approach on the plaza's south edge: two piers and a lettered beam, the name read from both sides. */
    private void gateway() {
        Brush stone = TowerKit.tri("minecraft:polished_deepslate", "minecraft:polished_blackstone");
        for (int z = 33; z <= 34; z++) {
            for (int x = -20; x <= 20; x++) {
                boolean pier = Math.abs(x) >= 19;
                for (int y = pier ? 0 : 5; y <= 11; y++) c.set(x, y, z, stone.at(x, y, z));
            }
        }
        for (int x = -20; x <= 20; x++) {
            c.set(x, 12, 33, St.slabBottom("minecraft:smooth_quartz_slab"));
            c.set(x, 12, 34, St.slabBottom("minecraft:smooth_quartz_slab"));
        }
        for (int x = -18; x <= 18; x++) for (int z = 33; z <= 34; z++) c.set(x, 4, z, x % 6 == 0 ? LAMP : St.slabTop("minecraft:smooth_quartz_slab"));
        int w = TowerKit.textWidth("SUCROCORP");
        TowerKit.text(c, "SUCROCORP", -w / 2, 10, 34, 1, LETTER);
        TowerKit.text(c, "SUCROCORP", w / 2, 10, 33, -1, LETTER);
    }

    /** A clipped hedge round the plaza's edge, open for the approach. */
    private void hedge() {
        for (int x = -35; x <= 35; x++) {
            for (int z = -35; z <= 35; z++) {
                if (Math.abs(x) != 35 && Math.abs(z) != 35) continue;
                if (z == 35 && Math.abs(x) <= 6) continue;
                if (z == 35 && Math.abs(x) >= 19 && Math.abs(x) <= 20) continue;
                c.set(x, 0, z, St.leaves(Noise.chance(x, 0, z, s + 31, 0.25) ? "minecraft:flowering_azalea_leaves" : "minecraft:azalea_leaves"));
            }
        }
    }

    // --- the shell -----------------------------------------------------------------------------------------------------------

    private void shell() {
        for (int i = 0; i < SucroTower.FLOORS.size(); i++) {
            SucroTower.Floor f = SucroTower.FLOORS.get(i);
            Sec sec = sec(f.floorY());
            Sec below = i == 0 ? sec : sec(SucroTower.FLOORS.get(i - 1).floorY());
            slab(f, sec, below);
            for (int y = f.y(); y <= f.topY(); y++) {
                for (int x = -sec.hx(); x <= sec.hx(); x++) {
                    for (int z = ZN; z <= ZS; z++) {
                        if (!sec.interior(x, z) || core(x, z)) continue;
                        if (f.y() < SucroTower.BOARDROOM_FLOOR && column(x, z)) c.set(x, y, z, "minecraft:quartz_pillar[axis=y]");
                        else c.carve(x, y, z);
                    }
                }
            }
            facade(f, i, sec);
        }
        // The roof over the boardroom.
        for (int x = -C.hx(); x <= C.hx(); x++) {
            for (int z = ZN; z <= ZS; z++) {
                if (!C.inside(x, z)) continue;
                c.set(x, ROOF, z, C.edge(x, z) ? SPANDREL : lampCell(x, z) ? LAMP : SLAB.at(x, ROOF, z));
            }
        }
        entrance();
        logos();
    }

    /** A storey's floor slab over its section, and where the tower steps in, a terrace roofing the section below. */
    private void slab(SucroTower.Floor f, Sec sec, Sec below) {
        int y = f.floorY();
        for (int x = -below.hx(); x <= below.hx(); x++) {
            for (int z = ZN; z <= ZS; z++) {
                if (sec.inside(x, z)) {
                    if (!sec.edge(x, z)) c.set(x, y, z, lampCell(x, z) && !core(x, z) ? LAMP : SLAB.at(x, y, z));
                } else if (below.inside(x, z)) {
                    // Terrace: a yellow cornice on the old edge, a glass rail on it.
                    if (below.edge(x, z)) {
                        c.set(x, y, z, CORNICE);
                        c.set(x, y + 1, z, St.pane("minecraft:light_gray_stained_glass_pane"));
                    } else {
                        c.set(x, y, z, SLAB.at(x, y, z));
                    }
                }
            }
        }
    }

    /**
     * The curtain wall of one storey: a spandrel band at the floor, dark glass between mullions every four cells, and a fin
     * standing out from every mullion on the straight faces. Tall storeys (the lobby, the boardroom) get a transom halfway.
     */
    private void facade(SucroTower.Floor f, int storey, Sec sec) {
        int y0 = f.floorY(), y1 = f.topY(), transom = f.height() > 5 ? f.y() + 4 : Integer.MIN_VALUE;
        for (int x = -sec.hx(); x <= sec.hx(); x++) {
            for (int z = ZN; z <= ZS; z++) {
                if (!sec.edge(x, z)) continue;
                Dir out = null;
                int outs = 0;
                for (Dir d : Dir.HORIZONTAL) {
                    if (!sec.inside(x + d.dx, z + d.dz)) {
                        out = d;
                        outs++;
                    }
                }
                int along = out == null || out.dz != 0 ? x : z - TZ;
                boolean mullion = outs != 1 || Math.floorMod(along, 4) == 0;
                boolean fin = outs == 1 && Math.floorMod(along, 8) == 0;
                int face = out == null ? 4 : out.ordinal();
                for (int y = y0; y <= y1; y++) {
                    String st = y == y0 ? SPANDREL : mullion || y == transom ? MULLION : glass(Math.floorDiv(along, 4), storey, face);
                    c.set(x, y, z, st);
                    if (fin && !finBlocked(x + out.dx, y, z + out.dz)) c.set(x + out.dx, y, z + out.dz, FIN);
                }
            }
        }
    }

    /** No fins across the lobby doors or the canopy, nor in front of the facade logos. */
    private static boolean finBlocked(int x, int y, int z) {
        if (z > ZS && Math.abs(x) <= 6 && y <= 5) return true;
        return Math.abs(x) <= 6 && (z > ZS || z < ZN) && y >= LOGO_Y - 1 && y <= LOGO_Y + TowerKit.LOGO;
    }

    /** The bottom row of the logos on the south and north faces. */
    static final int LOGO_Y = 76;

    /** The SucroCorp emblem set in the glass of the south and north faces, its S lit. */
    private void logos() {
        for (int i = 0; i < TowerKit.LOGO; i++) {
            for (int j = 0; j < TowerKit.LOGO; j++) {
                int m = TowerKit.logo(i, j);
                if (m == 0) continue;
                String st = m == 1 ? "minecraft:yellow_concrete" : LETTER;
                int y = LOGO_Y + TowerKit.LOGO - 1 - j;
                c.set(i - 5, y, ZS, st);
                c.set(5 - i, y, ZN, st);
            }
        }
    }

    /** The lobby doors (a five-wide glazed opening in the south face) under a quartz canopy with lights set in it. */
    private void entrance() {
        for (int x = -2; x <= 2; x++) {
            for (int y = 0; y <= 3; y++) c.carve(x, y, ZS);
            c.set(x, -1, ZS, "minecraft:polished_deepslate");
        }
        for (int x = -6; x <= 6; x++) {
            for (int z = ZS + 1; z <= ZS + 4; z++) {
                boolean rim = Math.abs(x) == 6 || z == ZS + 4;
                c.set(x, 5, z, rim ? CORNICE : (x % 3 == 0 && z == ZS + 2) ? LETTER : St.slabTop("minecraft:smooth_quartz_slab"));
            }
        }
        for (int x = -2; x <= 2; x++) c.set(x, -1, ZS + 1, St.full("minecraft:polished_deepslate"));
    }

    // --- the stair core ------------------------------------------------------------------------------------------------------

    /**
     * The stair core, bottom to top: walls on three sides (the south side is the curtain wall), a floor-level landing at the
     * west end of every storey, two flights between storeys (the north lane up to a half landing at the east end, the south lane
     * back). Doors open north from the landing on the playable floors only; the boardroom's is an iron door.
     */
    private void core() {
        Map<Long, String> solid = new HashMap<>();
        Set<Long> steps = new HashSet<>();
        List<SucroTower.Floor> fl = SucroTower.FLOORS;
        for (int i = 0; i < fl.size(); i++) {
            SucroTower.Floor f = fl.get(i);
            int y = f.y();
            for (int x = CX0 + 1; x <= CX0 + 2; x++) for (int z = CZ0 + 1; z < CZ1; z++) solid.put(Canvas.key(x, y - 1, z), landing(x, y - 1, z));
            if (i == fl.size() - 1) break;
            int rise = f.height(), a = rise / 2 + (rise % 2), b = rise - a;
            for (int k = 0; k < a; k++) {
                for (int z = LANE_A0; z <= LANE_A0 + 1; z++) {
                    long key = Canvas.key(CX0 + 3 + k, y + k, z);
                    solid.put(key, St.stairs("minecraft:polished_andesite_stairs", Dir.EAST, false));
                    steps.add(key);
                }
            }
            for (int k = 0; k < b; k++) {
                for (int z = LANE_B0; z <= LANE_B0 + 1; z++) {
                    long key = Canvas.key(CX0 + 2 + b - k, y + a + k, z);
                    solid.put(key, St.stairs("minecraft:polished_andesite_stairs", Dir.WEST, false));
                    steps.add(key);
                }
            }
            int half = y + a - 1;
            for (int x = CX0 + 3 + a; x < CX1; x++) for (int z = LANE_A0; z <= LANE_A0 + 1; z++) solid.put(Canvas.key(x, half, z), landing(x, half, z));
            for (int x = CX0 + 3 + b; x < CX1; x++) for (int z = LANE_B0; z <= LANE_B0 + 1; z++) solid.put(Canvas.key(x, half, z), landing(x, half, z));
            for (int x = CX1 - 2; x < CX1; x++) solid.put(Canvas.key(x, half, SPINE), landing(x, half, SPINE));
        }
        // Headroom: three cells over every tread and landing stay clear; soffits go under the steps where nothing needs the room.
        Set<Long> clear = new HashSet<>();
        for (long k : solid.keySet()) for (int h = 1; h <= 3; h++) clear.add(Canvas.key(Canvas.kx(k), Canvas.ky(k) + h, Canvas.kz(k)));
        for (long k : steps) {
            long under = Canvas.key(Canvas.kx(k), Canvas.ky(k) - 1, Canvas.kz(k));
            if (!solid.containsKey(under) && !clear.contains(under) && Canvas.ky(k) > 0) {
                Dir f = Dir.of(St.get(solid.get(k), "facing"));
                solid.put(under, St.stairs("minecraft:polished_andesite_stairs", f.opposite(), true));
            }
        }
        int top = ROOF - 1;
        for (int y = -1; y <= top; y++) {
            for (int x = CX0; x <= CX1; x++) {
                for (int z = CZ0; z <= CZ1; z++) {
                    boolean wall = x == CX0 || x == CX1 || z == CZ0;
                    if (z == CZ1) continue; // the curtain wall
                    if (wall) {
                        c.set(x, y, z, CORE_WALL.at(x, y, z));
                        continue;
                    }
                    String st = solid.get(Canvas.key(x, y, z));
                    if (st != null) c.set(x, y, z, st);
                    else if (z == SPINE && x >= CX0 + 3 && x <= CX1 - 3 && y >= 0) c.set(x, y, z, CORE_WALL.at(x, y, z));
                    else if (y >= 0) c.carve(x, y, z);
                }
            }
        }
        for (SucroTower.Floor f : fl) {
            if (!f.playable()) continue;
            if (f.room().equals("boardroom")) {
                c.setPair(CX0 + 1, f.y(), CZ0, St.door("minecraft:iron_door", Dir.NORTH, false, false));
                c.anchor(SucroTower.BOARDROOM_DOOR, CX0 + 1, f.y(), CZ0);
            } else {
                for (int x = CX0 + 1; x <= CX0 + 2; x++) {
                    c.carve(x, f.y(), CZ0);
                    c.carve(x, f.y() + 1, CZ0);
                    c.set(x, f.y() - 1, CZ0, "minecraft:polished_deepslate");
                }
            }
        }
        // The lobby's "lifts": two pairs of steel doors in the core's west wall.
        c.setPair(CX0, 0, CZ0 + 2, St.door("minecraft:iron_door", Dir.WEST, false, false));
        c.setPair(CX0, 0, CZ0 + 4, St.door("minecraft:iron_door", Dir.WEST, true, false));
    }

    private static String landing(int x, int y, int z) {
        return ((x + z) & 1) == 0 ? "minecraft:polished_andesite" : "minecraft:smooth_stone";
    }

    // --- the crown -----------------------------------------------------------------------------------------------------------

    /**
     * Over the boardroom: a glass parapet whose fins rise higher toward the middle of each face, the company's name on bands
     * standing out from the south and north faces, a plant box carrying the helipad, and the antenna mast.
     */
    private void crown() {
        int y0 = ROOF + 1;
        for (int x = -C.hx(); x <= C.hx(); x++) {
            for (int z = ZN; z <= ZS; z++) {
                if (!C.edge(x, z)) continue;
                int outs = 0;
                Dir out = null;
                for (Dir d : Dir.HORIZONTAL) {
                    if (!C.inside(x + d.dx, z + d.dz)) {
                        out = d;
                        outs++;
                    }
                }
                int along = out == null || out.dz != 0 ? x : z - TZ;
                boolean mullion = outs != 1 || Math.floorMod(along, 4) == 0;
                int span = out != null && out.dz != 0 ? C.hx() : 20;
                int rise = mullion ? 5 + (span - Math.abs(along)) / 6 : 2;
                for (int y = y0; y < y0 + rise; y++) {
                    c.set(x, y, z, mullion ? MULLION : y == y0 ? SPANDREL : "minecraft:black_stained_glass");
                    if (mullion && outs == 1 && Math.floorMod(along, 8) == 0 && y < y0 + 5) c.set(x + out.dx, y, z + out.dz, FIN);
                }
                if (mullion) c.set(x, y0 + rise, z, St.slabBottom("minecraft:smooth_quartz_slab"));
            }
        }
        // The name bands (south read from the forecourt, north from behind).
        Brush band = TowerKit.tri("minecraft:polished_deepslate", "minecraft:polished_blackstone");
        int w = TowerKit.textWidth("SUCROCORP");
        for (int x = -C.hx(); x <= C.hx(); x++) {
            for (int y = y0 + 1; y <= y0 + 7; y++) {
                c.set(x, y, ZS + 1, band.at(x, y, ZS + 1));
                c.set(x, y, ZN - 1, band.at(x, y, ZN - 1));
            }
            c.set(x, y0, ZS + 1, St.stairs("minecraft:polished_deepslate_stairs", Dir.NORTH, true));
            c.set(x, y0, ZN - 1, St.stairs("minecraft:polished_deepslate_stairs", Dir.SOUTH, true));
            c.set(x, y0 + 8, ZS + 1, St.slabBottom("minecraft:smooth_quartz_slab"));
            c.set(x, y0 + 8, ZN - 1, St.slabBottom("minecraft:smooth_quartz_slab"));
        }
        TowerKit.text(c, "SUCROCORP", -w / 2, y0 + 6, ZS + 1, 1, LETTER);
        TowerKit.text(c, "SUCROCORP", w / 2, y0 + 6, ZN - 1, -1, LETTER);
        helipad(y0);
        mast(y0);
    }

    /** A plant box on the roof, the helipad deck on it: a yellow H in a ring, lights round the rim. */
    private void helipad(int y0) {
        int cz = SucroTower.BOARDROOM_Z, deck = y0 + 9;
        Brush box = TowerKit.tri("minecraft:light_gray_concrete", "minecraft:smooth_stone");
        for (int x = -8; x <= 8; x++) {
            for (int z = cz - 8; z <= cz + 8; z++) {
                for (int y = y0; y < deck; y++) {
                    boolean grille = (Math.abs(x) == 8 || Math.abs(z - cz) == 8) && y == y0 + 3 && Math.floorMod(x + z, 2) == 0;
                    c.set(x, y, z, grille ? "minecraft:waxed_copper_grate" : box.at(x, y, z));
                }
            }
        }
        for (int x = -10; x <= 10; x++) {
            for (int z = cz - 10; z <= cz + 10; z++) {
                int dx = Math.abs(x), dz = Math.abs(z - cz);
                double d = Math.sqrt(dx * dx + dz * dz);
                boolean rim = dx == 10 || dz == 10;
                boolean h = dx <= 3 && dz <= 4 && (dx == 3 || dz == 0);
                String st;
                if (rim) st = (x + z) % 3 == 0 ? LETTER : "minecraft:yellow_concrete";
                else if (h || d >= 7.5 && d < 8.6) st = "minecraft:yellow_concrete";
                else st = ((x + z) & 1) == 0 ? "minecraft:gray_concrete" : "minecraft:black_concrete";
                c.set(x, deck, z, st);
            }
        }
    }

    /** The antenna: a plinth, a mast with two red collars, a whip of bars and a lightning rod at the top. */
    private void mast(int y0) {
        int mx = 13, mz = ZN + 5;
        for (int x = mx - 1; x <= mx + 1; x++) for (int z = mz - 1; z <= mz + 1; z++) for (int y = y0; y <= y0 + 2; y++) c.set(x, y, z, "minecraft:polished_andesite");
        int tip = SucroTower.HEIGHT - 1;
        for (int y = y0 + 3; y < tip; y++) {
            String st;
            if (y > tip - 7) st = St.bars();
            else if (y == y0 + 12 || y == y0 + 20) st = "minecraft:red_concrete";
            else st = St.wall("minecraft:andesite_wall");
            c.set(mx, y, mz, st);
        }
        c.set(mx, tip, mz, St.of("minecraft:lightning_rod", "facing", "up", "powered", "false", "waterlogged", "false"));
        for (Dir d : Dir.HORIZONTAL) c.set(mx + d.dx, y0 + 12, mz + d.dz, St.of("minecraft:end_rod", "facing", d.id()));
    }

    /** Whether the cell is open floor in a storey's interior (carved air, not the core, a column, or the curtain wall). */
    boolean open(int x, int y, int z) {
        return Kinds.air(c.world(x, y, z)) && Kinds.floor(c.world(x, y - 1, z));
    }
}
