package org.papiricoh.supernaturalcraft.structure.enochian;

import org.papiricoh.supernaturalcraft.buildkit.Box;
import org.papiricoh.supernaturalcraft.buildkit.Brush;
import org.papiricoh.supernaturalcraft.buildkit.Canvas;
import org.papiricoh.supernaturalcraft.buildkit.Decor;
import org.papiricoh.supernaturalcraft.buildkit.Dir;
import org.papiricoh.supernaturalcraft.buildkit.Family;
import org.papiricoh.supernaturalcraft.buildkit.Furniture;
import org.papiricoh.supernaturalcraft.buildkit.Kinds;
import org.papiricoh.supernaturalcraft.buildkit.Lighting;
import org.papiricoh.supernaturalcraft.buildkit.Noise;
import org.papiricoh.supernaturalcraft.buildkit.Outside;
import org.papiricoh.supernaturalcraft.buildkit.Palette;
import org.papiricoh.supernaturalcraft.buildkit.St;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;
import org.papiricoh.supernaturalcraft.layout.LayoutPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * The buried Enochian crypt (v0.36 "The Word and the Ward"), drawn with the buildkit (pure, deterministic per seed; the seed
 * only moves small things: the masonry's mix, the shrine's ruin, which side of each door its plate is on, the murals' order,
 * a gap in an oil line, cobwebs and candles). Plan space and the contract: {@link EnochianCryptAnchors}.
 *
 * <p>The route, from the surface down:
 * <ol>
 *   <li>a ruined shrine of pale Enochian limestone on the surface (a gate of two pillars and a lintel, broken columns and low
 *       walls), its stairwell going down south, 3 wide, twelve steps to the crypt floor;</li>
 *   <li>the antechamber (four pillars, a glyph mosaic, candle niches), then the "ancient traps" corridor east: three devil's
 *       traps painted on its ceiling over the walkway, unlit holy-oil lines across its floor, soul lanterns in wall niches;</li>
 *   <li>the door gallery north: three chambers, each closed by a 3 × 3 sealed door with its sigil plate beside it
 *       (banishing, then ward, then alarm);</li>
 *   <li>the library west of the last chamber: ten Word-of-God mural blocks in bays of the north and west walls, bookshelves,
 *       reading tables, two chandeliers;</li>
 *   <li>the vault south of the library: the tablet pedestal on a glyph dais in the centre, a chest on each side.</li>
 * </ol>
 * Walls, floors and ceilings are explicit (the crypt does not rely on the rock round it); the crypt's air is explicit too.
 */
public final class EnochianCryptPlan {

    public static final String STONE = "supernaturalcraft:enochian_stone", BRICKS = "supernaturalcraft:enochian_stone_bricks",
            CHISELED = "supernaturalcraft:chiseled_enochian_stone", PILLAR_ID = "supernaturalcraft:enochian_pillar",
            TRAP_ID = "supernaturalcraft:devils_trap", OIL = "supernaturalcraft:holy_oil_trail", PLATE_ID = "supernaturalcraft:sigil_plate",
            DOOR = "supernaturalcraft:sealed_crypt_door", MURAL_ID = "supernaturalcraft:word_mural",
            PEDESTAL_ID = "supernaturalcraft:tablet_pedestal";
    public static final String PILLAR = PILLAR_ID + "[axis=y]", PEDESTAL = PEDESTAL_ID + "[full=true]";
    /** Every mod block id the plan uses (for {@code BlockIds.allow}). */
    public static final List<String> MOD_IDS = List.of(STONE, BRICKS, CHISELED, PILLAR_ID, TRAP_ID, OIL, PLATE_ID, DOOR, MURAL_ID, PEDESTAL_ID);
    /** Loot table the vault's chests point at (decor). */
    public static final String LOOT_VAULT = "supernaturalcraft:chests/enochian_crypt";

    static {
        // Decals: a painted trap under the ceiling and an oil line on the floor are thin, see-through and walked through.
        Kinds.declareLike(TRAP_ID, "minecraft:glow_lichen");
        Kinds.declareLight(TRAP_ID, 0);
        Kinds.declareLike(OIL, "minecraft:glow_lichen");
        Kinds.declareLight(OIL, 0);
        // The pedestal is a pedestal, not a cube: light goes round it, a tablet lies on it.
        Kinds.declarePartial(PEDESTAL_ID, 2);
        // The sealed door is a solid slab of stone, not a two-high door, whatever its id ends with.
        Kinds.declareLike(DOOR, "minecraft:stone");
    }

    // --- rooms (interiors, feet level 0 unless said) -----------------------------------------------------------------------------

    static final int G = EnochianCryptAnchors.GROUND_Y;
    static final Box ANTE = new Box(1, 0, 21, 11, 4, 31);
    static final Box CORRIDOR = new Box(13, 0, 25, 29, 3, 27);
    static final Box[] GALLERY = {new Box(31, 0, 22, 37, 4, 31), new Box(31, 0, 15, 37, 4, 20), new Box(31, 0, 8, 37, 4, 13),
            new Box(31, 0, 1, 37, 4, 6)};
    static final Box LIBRARY = new Box(15, 0, 1, 29, 6, 13);
    static final Box VAULT = new Box(17, 0, 16, 27, 5, 22);
    /** The cross wall (z) holding door i, and the door's x span. */
    static final int[] DOOR_Z = {21, 14, 7};
    static final int DOOR_X0 = 33, DOOR_X1 = 35;
    /** The stair: 3 wide (x 5..7), step k at z = STAIR_Z0 - k and y = k. */
    static final int STAIR_X0 = 5, STAIR_X1 = 7, STAIR_Z0 = 20, STEPS = G;
    static final int[] TRAP_X = {16, 21, 26};

    private final long seed;
    private final int s;
    private final Canvas canvas;
    private final EnochianCryptAnchors anchors;

    private final List<LayoutPoint> chests = new ArrayList<>(), guardians = new ArrayList<>(), traps = new ArrayList<>(),
            doorMin = new ArrayList<>(), doorMax = new ArrayList<>();
    private final List<EnochianCryptAnchors.Plate> plates = new ArrayList<>();
    private final List<EnochianCryptAnchors.Mural> murals = new ArrayList<>();
    private Brush wall, floor, ceiling;

    private EnochianCryptPlan(long seed) {
        this.seed = seed;
        this.s = (int) (seed ^ (seed >>> 32)) * 0x9E3779B1 + 0x36;
        this.canvas = new Canvas("enochian_crypt");
        // The crypt is buried: rock up to the terrain's top block, air above it.
        canvas.outside(Outside.groundUpTo(G - 1));
        draw();
        canvas.finish();
        LayoutPoint entrance = new LayoutPoint(6, G, 5);
        LayoutPoint pedestal = new LayoutPoint(VAULT.centerX(), 0, 19);
        anchors = new EnochianCryptAnchors(entrance, pedestal, chests, guardians, traps, plates, doorMin, doorMax, murals);
    }

    /** The crypt for {@code seed} (drawn and finished). */
    public static EnochianCryptPlan of(long seed) {
        return new EnochianCryptPlan(seed);
    }

    public long seed() {
        return seed;
    }

    /** The finished canvas (do not write to it). */
    public Canvas canvas() {
        return canvas;
    }

    /** The layout contract: zones in write order (shrine, stair, rooms…), named points. */
    public LayoutPlan plan() {
        return canvas.toPlan();
    }

    public EnochianCryptAnchors anchors() {
        return anchors;
    }

    // --- drawing ---------------------------------------------------------------------------------------------------------------

    private void draw() {
        wall = Palette.patches(s, 1.6, BRICKS, 3, STONE, 3, "minecraft:calcite", 1, "minecraft:polished_diorite", 1)
                .sprinkle(Brush.of(CHISELED), 0.05, s + 3);
        Brush a = Palette.mix(s + 4, BRICKS, 3, "minecraft:polished_diorite", 1);
        Brush b = Palette.mix(s + 5, STONE, 3, "minecraft:calcite", 1, "minecraft:diorite", 1);
        floor = Palette.checker(1, a, b);
        ceiling = Palette.patches(s + 6, 2.0, STONE, 3, BRICKS, 2, "minecraft:calcite", 1, "minecraft:polished_tuff", 1);

        // Shells first, then every interior carved, then openings and features.
        List<Box> rooms = new ArrayList<>(List.of(ANTE, CORRIDOR, LIBRARY, VAULT));
        rooms.addAll(List.of(GALLERY));
        for (Box r : rooms) shell(r);
        for (Box r : rooms) canvas.carve(r);
        openings();
        shrine();
        stair();
        antechamber();
        corridor();
        gallery();
        library();
        vault();
    }

    private void shell(Box in) {
        Box out = in.grow(1);
        for (int x = out.x0(); x <= out.x1(); x++)
            for (int z = out.z0(); z <= out.z1(); z++)
                for (int y = out.y0(); y <= out.y1(); y++) {
                    if (in.contains(x, y, z)) continue;
                    canvas.set(x, y, z, y == out.y0() ? floor.at(x, y, z) : y == out.y1() ? ceiling.at(x, y, z) : wall.at(x, y, z));
                }
    }

    private void openings() {
        // Antechamber → corridor, corridor → first gallery chamber: 3 wide, 3 high, a chiseled lintel over each.
        for (int z = 25; z <= 27; z++) {
            for (int y = 0; y <= 2; y++) {
                canvas.carve(12, y, z);
                canvas.carve(30, y, z);
            }
            canvas.set(12, 3, z, CHISELED);
            canvas.set(30, 3, z, CHISELED);
        }
        // Last gallery chamber → library (x = 30, z 2..4).
        for (int z = 2; z <= 4; z++) for (int y = 0; y <= 2; y++) canvas.carve(30, y, z);
        // Library → vault, through both walls (z 14 and 15), x 21..23.
        for (int x = 21; x <= 23; x++)
            for (int z = 14; z <= 15; z++) for (int y = 0; y <= 2; y++) canvas.carve(x, y, z);
        for (int x = 21; x <= 23; x++) canvas.set(x, 3, 14, CHISELED);
    }

    // --- 1. the shrine on the surface ---------------------------------------------------------------------------------------------

    private void shrine() {
        Box pad = new Box(1, G - 1, 1, 11, G - 1, 15);
        Brush paving = Palette.checker(1, Palette.mix(s + 10, BRICKS, 3, "minecraft:calcite", 1, "minecraft:mossy_stone_bricks", 1),
                Palette.mix(s + 11, STONE, 3, "minecraft:polished_diorite", 1, "minecraft:cracked_stone_bricks", 1));
        for (int x = pad.x0(); x <= pad.x1(); x++)
            for (int z = pad.z0(); z <= pad.z1(); z++) {
                // Corners crumbled away into the grass.
                boolean corner = (x == pad.x0() || x == pad.x1()) && (z == pad.z0() || z == pad.z1());
                if (corner && Noise.chance(x, 0, z, s + 12, 0.6)) continue;
                canvas.set(x, G - 1, z, paving.at(x, G - 1, z));
                canvas.set(x, G - 2, z, Noise.chance(x, 1, z, s + 13, 0.5) ? STONE : "minecraft:cobblestone");
                // Clear what grows on the pad.
                for (int y = G; y <= G + 2; y++) canvas.carve(x, y, z);
            }
        // The gate at the north: two pillars and a lintel with a glyph keystone, a lantern on it.
        int gz = 4;
        for (int x : new int[]{3, 9}) {
            canvas.set(x, G, gz, CHISELED);
            for (int y = G + 1; y <= G + 3; y++) canvas.set(x, y, gz, PILLAR);
            canvas.set(x, G + 4, gz, BRICKS);
        }
        for (int x = 4; x <= 8; x++) canvas.set(x, G + 4, gz, x == 6 ? CHISELED : Family.POLISHED_DIORITE.stairs(x < 6 ? Dir.WEST : Dir.EAST, true));
        canvas.set(6, G + 5, gz, St.soulLantern(false));
        canvas.set(6, G + 3, gz, St.lantern(true));
        // Broken columns down both sides, of uneven height; low ruined walls between them.
        for (int x : new int[]{1, 11}) {
            for (int z : new int[]{7, 11, 15}) {
                int h = 1 + Noise.pick(x, 0, z, s + 14, 4);
                canvas.set(x, G, z, CHISELED);
                for (int y = G + 1; y < G + h; y++) canvas.set(x, y, z, PILLAR);
                canvas.set(x, G + h, z, h >= 3 ? Family.POLISHED_DIORITE.slabBottom() : "minecraft:moss_carpet");
            }
            for (int z = 5; z <= 14; z++) {
                if (z == 7 || z == 11) continue;
                if (Noise.chance(x, 1, z, s + 15, 0.3)) {
                    if (Noise.chance(x, 2, z, s + 16, 0.4)) canvas.set(x, G, z, St.mossCarpet());
                    continue;
                }
                canvas.set(x, G, z, wall.at(x, G, z));
                if (Noise.chance(x, 3, z, s + 17, 0.35)) canvas.set(x, G + 1, z, Family.POLISHED_DIORITE.wallBlock());
            }
        }
        // Rubble and moss on the paving (never on the walkway to the stair).
        for (int x = 2; x <= 10; x++)
            for (int z = 2; z <= 15; z++) {
                if (x >= 4 && x <= 8 && z >= 4) continue;
                if (!canvas.isAir(x, G, z)) continue;
                double n = Noise.hash01(x, 5, z, s + 18);
                if (n < 0.08) canvas.set(x, G, z, St.mossCarpet());
                else if (n < 0.12) canvas.set(x, G, z, Family.POLISHED_DIORITE.slabBottom());
            }
        // The stairwell's parapet: low walls on both sides and across its south end, glyph stones at its corners.
        for (int z = 9; z <= 13; z++) {
            canvas.set(4, G, z, z == 9 || z == 13 ? CHISELED : Family.POLISHED_DIORITE.wallBlock());
            canvas.set(8, G, z, z == 9 || z == 13 ? CHISELED : Family.POLISHED_DIORITE.wallBlock());
        }
        for (int x = 5; x <= 7; x++) canvas.set(x, G, 13, Family.POLISHED_DIORITE.wallBlock());
        canvas.set(4, G + 1, 9, St.candle("white", 2, true));
        canvas.set(8, G + 1, 13, St.candle("white", 1 + Noise.pick(8, 0, 13, s + 19, 3), true));
        canvas.anchor("entrance", 6, G, 5);
    }

    // --- the stair down ----------------------------------------------------------------------------------------------------------

    private void stair() {
        for (int k = 0; k < STEPS; k++) {
            int z = STAIR_Z0 - k;
            // Side walls from the step up to the ceiling (or the surface).
            if (k > 0) {
                int top = k <= 7 ? k + 4 : G - 1;
                for (int y = k; y <= top; y++) {
                    canvas.set(4, y, z, wall.at(4, y, z));
                    canvas.set(8, y, z, wall.at(8, y, z));
                }
                if (k <= 7) for (int x = STAIR_X0; x <= STAIR_X1; x++) canvas.set(x, k + 4, z, k == 7 ? CHISELED : ceiling.at(x, k + 4, z));
            } else {
                for (int x = STAIR_X0; x <= STAIR_X1; x++) canvas.set(x, 4, z, CHISELED);
            }
        }
        Furniture.flight(canvas, STAIR_X0, 0, STAIR_Z0, Dir.NORTH, STEPS, 3, "minecraft:polished_diorite_stairs", false);
        // Air over the open stairwell up to the surface.
        for (int z = 9; z <= 12; z++) for (int x = STAIR_X0; x <= STAIR_X1; x++) for (int y = G; y <= G + 2; y++) canvas.carve(x, y, z);
        // Soul lanterns in niches of the stair's walls.
        niche(4, 5, 17, Dir.EAST, St.soulLantern(false));
        niche(8, 8, 14, Dir.WEST, St.soulLantern(false));
    }

    /** A niche carved into a wall cell, {@code what} standing in it on the wall block below. */
    private void niche(int x, int y, int z, Dir open, String what) {
        canvas.set(x, y, z, what);
        canvas.set(x, y - 1, z, CHISELED);
        canvas.set(x, y + 1, z, Family.POLISHED_DIORITE.slabTop());
    }

    // --- 2a. the antechamber ----------------------------------------------------------------------------------------------------

    private void antechamber() {
        for (int[] p : new int[][]{{3, 23}, {9, 23}, {3, 29}, {9, 29}}) {
            canvas.set(p[0], 0, p[1], CHISELED);
            for (int y = 1; y <= 3; y++) canvas.set(p[0], y, p[1], PILLAR);
            canvas.set(p[0], 4, p[1], BRICKS);
            for (Dir d : Dir.HORIZONTAL) canvas.setIfAir(p[0] + d.dx, 4, p[1] + d.dz, Family.POLISHED_DIORITE.stairs(d.opposite(), true));
        }
        // A glyph mosaic in the floor: a ring of glyph stones round a gold-flecked centre.
        int cx = 6, cz = 26;
        for (int dx = -2; dx <= 2; dx++)
            for (int dz = -2; dz <= 2; dz++) {
                int r = Math.max(Math.abs(dx), Math.abs(dz));
                if (r == 2) canvas.set(cx + dx, -1, cz + dz, (dx + dz) % 2 == 0 ? CHISELED : "minecraft:polished_diorite");
                else if (r == 1) canvas.set(cx + dx, -1, cz + dz, "minecraft:calcite");
                else canvas.set(cx + dx, -1, cz + dz, "minecraft:gold_block");
            }
        // Candle niches in the west and south walls.
        for (int z = 22; z <= 30; z += 4) candleNiche(0, z, Dir.EAST);
        for (int x = 3; x <= 9; x += 3) candleNiche(x, 32, Dir.NORTH);
        Lighting.hanging(canvas, 6, 5, 26, 1, false);
        Lighting.hanging(canvas, 6, 5, 22, 1, false);
        Lighting.hanging(canvas, 6, 5, 30, 1, false);
        cobwebs(ANTE, s + 30);
        guardian(4, 26);
        guardian(8, 27);
        guardian(6, 30);
    }

    private void candleNiche(int x, int z, Dir open) {
        canvas.set(x, 0, z, CHISELED);
        canvas.set(x, 1, z, St.candle(Noise.chance(x, 0, z, s + 31, 0.5) ? "white" : "", 3 + Noise.pick(x, 1, z, s + 32, 2), true));
        canvas.set(x, 2, z, Family.POLISHED_DIORITE.slabTop());
    }

    private void cobwebs(Box room, int seed) {
        for (int x : new int[]{room.x0(), room.x1()})
            for (int z : new int[]{room.z0(), room.z1()})
                if (Noise.chance(x, 0, z, seed, 0.6) && canvas.isAir(x, room.y1(), z)) canvas.set(x, room.y1(), z, "minecraft:cobweb");
    }

    private void guardian(int x, int z) {
        guardians.add(new LayoutPoint(x, 0, z));
        canvas.anchor("guardian_" + (guardians.size() - 1), x, 0, z);
    }

    // --- 2b. the trap corridor -----------------------------------------------------------------------------------------------------

    private void corridor() {
        int y = CORRIDOR.y1();
        for (int i = 0; i < TRAP_X.length; i++) {
            int cx = TRAP_X[i], cz = 26;
            for (int dx = -1; dx <= 1; dx++)
                for (int dz = -1; dz <= 1; dz++)
                    canvas.set(cx + dx, y, cz + dz, St.of(TRAP_ID, "face", "ceiling", "part", String.valueOf((dz + 1) * 3 + (dx + 1))));
            traps.add(new LayoutPoint(cx, y, cz));
            canvas.anchor("trap_" + i, cx, y, cz);
        }
        // Unlit holy oil poured in lines across the floor between the traps; one line is broken.
        int broken = Noise.pick(0, 0, 0, s + 40, 4);
        int[] lines = {13, 18, 24, 29};
        for (int i = 0; i < lines.length; i++)
            for (int z = 25; z <= 27; z++) {
                if (i == broken && z == 25 + Noise.pick(i, 0, 0, s + 41, 3)) continue;
                canvas.set(lines[i], 0, z, OIL);
            }
        // Soul lanterns in niches along both walls; glyph stones between them.
        for (int x = 14; x <= 26; x += 4) niche(x, 1, 24, Dir.SOUTH, St.soulLantern(false));
        for (int x = 16; x <= 28; x += 4) niche(x, 1, 28, Dir.NORTH, St.soulLantern(false));
        for (int x = 13; x <= 29; x++) {
            if (canvas.get(x, 1, 24) != null && !St.path(canvas.get(x, 1, 24)).contains("lantern")) canvas.set(x, 3, 24, x % 2 == 0 ? CHISELED : BRICKS);
            if (canvas.get(x, 1, 28) != null && !St.path(canvas.get(x, 1, 28)).contains("lantern")) canvas.set(x, 3, 28, x % 2 == 1 ? CHISELED : BRICKS);
        }
        guardian(16, 26);
        guardian(21, 25);
        guardian(26, 27);
    }

    // --- 3. the door gallery ---------------------------------------------------------------------------------------------------------

    private void gallery() {
        for (int i = 0; i < EnochianCryptAnchors.DOORS; i++) {
            int z = DOOR_Z[i];
            for (int x = DOOR_X0; x <= DOOR_X1; x++) {
                for (int y = 0; y <= 2; y++) canvas.set(x, y, z, DOOR);
                canvas.set(x, 3, z, x == 34 ? CHISELED : BRICKS);
            }
            // Pillars flank the door on the near side; the plate sits in the wall beside it.
            for (int x : new int[]{32, 36}) {
                canvas.set(x, 0, z + 1, CHISELED);
                for (int y = 1; y <= 3; y++) canvas.set(x, y, z + 1, PILLAR);
            }
            boolean east = Noise.chance(i, 0, 0, s + 50, 0.5);
            int px = east ? 37 : 31;
            String sigil = EnochianCryptAnchors.DOOR_SIGILS.get(i);
            canvas.set(px, 1, z, St.of(PLATE_ID, "facing", "south", "sigil", sigil, "solved", "false"));
            canvas.set(px, 2, z, CHISELED);
            canvas.set(px, 0, z, CHISELED);
            plates.add(new EnochianCryptAnchors.Plate(new LayoutPoint(px, 1, z), "south", sigil));
            canvas.anchor("plate_" + i, px, 1, z);
            doorMin.add(new LayoutPoint(DOOR_X0, 0, z));
            doorMax.add(new LayoutPoint(DOOR_X1, 2, z));
            canvas.anchor("door_" + i + "_min", DOOR_X0, 0, z);
            canvas.anchor("door_" + i + "_max", DOOR_X1, 2, z);
            // A candle on the floor under the plate.
            canvas.set(px, 0, z + 1, St.candle("", 1 + Noise.pick(px, 0, z, s + 51, 3), true));
        }
        for (Box g : GALLERY) {
            int midZ = (g.z0() + g.z1()) / 2;
            if (g.sizeZ() > 7) {
                Lighting.hanging(canvas, 34, 5, g.z0() + 2, 1, false);
                Lighting.hanging(canvas, 34, 5, g.z1() - 2, 1, false);
            } else {
                Lighting.hanging(canvas, 34, 5, midZ, 1, false);
            }
            cobwebs(g, s + 52 + g.z0());
        }
        // Wall candles in the first chamber's east wall, a glyph frieze on the back wall of the last.
        for (int z = 24; z <= 30; z += 3) candleNiche(38, z, Dir.WEST);
        for (int x = 31; x <= 37; x++) canvas.set(x, 3, 0, x % 2 == 0 ? CHISELED : BRICKS);
    }

    // --- 4. the library ------------------------------------------------------------------------------------------------------------

    private void library() {
        Box L = LIBRARY;
        // Pilasters: four on the north wall, three on the west; mural bays between them.
        for (int x : new int[]{16, 20, 24, 28}) pilaster(x, L.z0());
        for (int z : new int[]{3, 7, 11}) pilaster(L.x0(), z);
        // Mural variants: each bay a pair, every variant at least twice, the order by seed.
        int[] order = {0, 1, 2, 3, 0, 2, 1, 3, 2, 0};
        int shift = Noise.pick(0, 1, 0, s + 60, 4);
        int m = 0;
        for (int x : new int[]{18, 22, 26}) {
            for (int y = 1; y <= 2; y++) mural(x, y, L.z0() - 1, Dir.SOUTH, (order[m++] + shift) % 4);
            bay(x, L.z0() - 1, Dir.SOUTH);
        }
        for (int z : new int[]{5, 9}) {
            for (int y = 1; y <= 2; y++) mural(L.x0() - 1, y, z, Dir.EAST, (order[m++] + shift) % 4);
            bay(L.x0() - 1, z, Dir.EAST);
        }
        // Bookshelves: along the east wall (north of the vault door's line) and the south wall either side of the vault door.
        for (int z = 6; z <= 12; z++) for (int y = 0; y <= 2; y++) canvas.set(L.x1(), y, z, y == 1 && z % 3 == 0 ? "minecraft:chiseled_bookshelf[facing=west,slot_0_occupied=true,slot_1_occupied=false,slot_2_occupied=true,slot_3_occupied=true,slot_4_occupied=false,slot_5_occupied=true]" : "minecraft:bookshelf");
        for (int x = L.x0(); x <= L.x1(); x++) {
            if (x >= 20 && x <= 24) continue;
            for (int y = 0; y <= 2; y++) canvas.set(x, y, L.z1(), y == 2 && x % 4 == 1 ? CHISELED : "minecraft:bookshelf");
        }
        // Reading tables with candles, a lectern between them.
        for (int x0 : new int[]{18, 25}) {
            for (int x = x0; x <= x0 + 1; x++) for (int z = 6; z <= 8; z++) canvas.set(x, 0, z, Family.POLISHED_DIORITE.slabTop());
            canvas.set(x0, 1, 7, St.candle("white", 1 + Noise.pick(x0, 0, 7, s + 61, 3), true));
            canvas.set(x0 + 1, 1, 6, St.candle("", 1, true));
        }
        canvas.set(22, 0, 9, St.lectern(Dir.SOUTH));
        // Chandeliers, and a cornice of upside-down stairs round the walls under the ceiling.
        Lighting.chandelier(canvas, 19, L.y1() + 1, 7, 2, "minecraft:dark_oak_fence", "white");
        Lighting.chandelier(canvas, 26, L.y1() + 1, 7, 2, "minecraft:dark_oak_fence", "white");
        for (int[] l : new int[][]{{22, 12}, {27, 2}, {17, 2}, {17, 11}}) Lighting.hanging(canvas, l[0], L.y1() + 1, l[1], 2, false);
        for (int x = L.x0(); x <= L.x1(); x++) {
            canvas.setIfAir(x, L.y1(), L.z0(), Family.POLISHED_DIORITE.stairs(Dir.NORTH, true));
            canvas.setIfAir(x, L.y1(), L.z1(), Family.POLISHED_DIORITE.stairs(Dir.SOUTH, true));
        }
        for (int z = L.z0() + 1; z < L.z1(); z++) {
            canvas.setIfAir(L.x0(), L.y1(), z, Family.POLISHED_DIORITE.stairs(Dir.WEST, true));
            canvas.setIfAir(L.x1(), L.y1(), z, Family.POLISHED_DIORITE.stairs(Dir.EAST, true));
        }
        // A worn carpet runner from the east door to the vault door.
        for (int x = 23; x <= 29; x++) canvas.setIfAir(x, 0, 3, St.carpet("red"));
        for (int z = 4; z <= 12; z++) if (canvas.isAir(22, 0, z)) canvas.set(22, 0, z, St.carpet("red"));
    }

    private void pilaster(int x, int z) {
        canvas.set(x, 0, z, CHISELED);
        for (int y = 1; y <= LIBRARY.y1() - 1; y++) canvas.set(x, y, z, PILLAR);
        canvas.set(x, LIBRARY.y1(), z, BRICKS);
    }

    /** The frame round a mural pair: a glyph sill under it, a lintel and a gold boss over it. */
    private void bay(int x, int z, Dir face) {
        canvas.set(x, 0, z, CHISELED);
        canvas.set(x, 3, z, "minecraft:gold_block");
        canvas.set(x, 4, z, CHISELED);
        int fx = x + face.dx, fz = z + face.dz;
        canvas.set(fx, 0, fz, St.candle("white", 3 + Noise.pick(x, 0, z, s + 62, 2), true));
    }

    private void mural(int x, int y, int z, Dir face, int variant) {
        canvas.set(x, y, z, St.of(MURAL_ID, "facing", face.id(), "variant", String.valueOf(variant)));
        murals.add(new EnochianCryptAnchors.Mural(new LayoutPoint(x, y, z), face.id(), variant));
        canvas.anchor("mural_" + (murals.size() - 1), x, y, z);
    }

    // --- 5. the vault ----------------------------------------------------------------------------------------------------------------

    private void vault() {
        Box V = VAULT;
        int cx = V.centerX(), cz = 19;
        // The dais: a gold-ringed glyph floor under the pedestal, candles on its corners.
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) canvas.set(cx + dx, -1, cz + dz, dx == 0 && dz == 0 ? "minecraft:gold_block" : CHISELED);
        for (int dx = -2; dx <= 2; dx++)
            for (int dz = -2; dz <= 2; dz++)
                if (Math.max(Math.abs(dx), Math.abs(dz)) == 2) canvas.set(cx + dx, -1, cz + dz, "minecraft:polished_diorite");
        canvas.set(cx, 0, cz, PEDESTAL);
        canvas.anchor("pedestal", cx, 0, cz);
        for (int dx : new int[]{-1, 1}) for (int dz : new int[]{-1, 1}) canvas.set(cx + dx, 0, cz + dz, St.candle("white", 1 + Noise.pick(dx, 0, dz, s + 70, 2), true));
        for (int[] p : new int[][]{{19, 17}, {25, 17}, {19, 21}, {25, 21}}) {
            canvas.set(p[0], 0, p[1], CHISELED);
            for (int y = 1; y <= V.y1() - 1; y++) canvas.set(p[0], y, p[1], PILLAR);
            canvas.set(p[0], V.y1(), p[1], BRICKS);
        }
        canvas.set(V.x0(), 0, cz, St.of("chest", "facing", "east", "type", "single", "waterlogged", "false"));
        canvas.set(V.x1(), 0, cz, St.of("chest", "facing", "west", "type", "single", "waterlogged", "false"));
        chests.add(new LayoutPoint(V.x0(), 0, cz));
        chests.add(new LayoutPoint(V.x1(), 0, cz));
        for (int i = 0; i < chests.size(); i++) {
            LayoutPoint c = chests.get(i);
            canvas.decor(Decor.loot(c.x(), c.y(), c.z(), LOOT_VAULT));
            canvas.anchor("chest_" + i, c.x(), c.y(), c.z());
        }
        // A glyph frieze round the walls at head height, lanterns hanging over the dais.
        for (int x = V.x0(); x <= V.x1(); x++) {
            canvas.set(x, 3, V.z1() + 1, x % 2 == 0 ? CHISELED : "minecraft:gold_block");
        }
        for (int z = V.z0(); z <= V.z1(); z++) {
            canvas.set(V.x0() - 1, 3, z, z % 2 == 0 ? CHISELED : BRICKS);
            canvas.set(V.x1() + 1, 3, z, z % 2 == 0 ? CHISELED : BRICKS);
        }
        Lighting.hanging(canvas, cx - 3, V.y1() + 1, cz, 2, false);
        Lighting.hanging(canvas, cx + 3, V.y1() + 1, cz, 2, false);
        Lighting.hanging(canvas, cx, V.y1() + 1, V.z0() + 1, 1, true);
        cobwebs(V, s + 71);
    }
}
