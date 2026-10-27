package org.papiricoh.supernaturalcraft.structure.spire;

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
import org.papiricoh.supernaturalcraft.buildkit.Openings;
import org.papiricoh.supernaturalcraft.buildkit.Roofs;
import org.papiricoh.supernaturalcraft.buildkit.St;
import org.papiricoh.supernaturalcraft.buildkit.Walls;

import java.util.List;

/**
 * The choir's monastery (pure), on a ledge part way down the Hymnal Spire's stair. Drawn with its gate at the origin facing
 * north (toward the stair) and the quadrangle running south; {@link #canvas} turns it.
 *
 * <p>Four white ranges round a cloister on a podium of old stone (footings to the mountain, tall buttresses down its open
 * faces): the north range holds the gate passage between a scriptorium and a storeroom; the west range the refectory (one
 * long table, a fireplace, the reader's lectern); the east range three monks' cells; the south range the chapel, from which a
 * stair goes down into the choristers' crypt ({@link CryptLayout}). The cloister walk runs behind an arcade under a copper
 * lean-to; a well stands in the garth. Verdigris roofs, one fallen in over a cell.
 * <p><b>Contract</b>: {@link #CHESTS} hold {@link SpireKit#LOOT_MONASTERY}; the crypt's chest {@link CryptLayout#CHEST}.
 */
public final class MonasteryLayout {

    public static final int TOP = 6, PODIUM = -10;
    static final Box NORTH = new Box(-10, 0, 0, 10, 0, 4), SOUTH = new Box(-10, 0, 16, 10, 0, 20), WEST = new Box(-10, 0, 4, -6, 0, 16),
            EAST = new Box(6, 0, 4, 10, 0, 16), COURT = new Box(-5, 0, 5, 5, 0, 15);
    /** The monastery's chests: the storeroom's and the scriptorium's. */
    public static final List<int[]> CHESTS = List.of(new int[]{9, 0, 1}, new int[]{-9, 0, 1});
    /** Footprint for siting (x and z, forecourt and buttresses included). */
    public static final Box FOOTPRINT = new Box(-12, 0, -6, 12, 0, 22);

    private MonasteryLayout() {
    }

    public static Canvas canvas(long seed, int dir) {
        Canvas south = drawn(seed);
        Canvas c = new Canvas("spire_monastery");
        c.zone("monastery", new Box(-40, -64, -40, 40, 40, 40));
        c.inZone("monastery", () -> c.stamp(south, 0, 0, 0, Math.floorMod(dir, 4), Canvas.Mode.SET, ""));
        return c.finish();
    }

    static Canvas drawn(long seed) {
        SpireKit.init();
        int s = (int) (seed ^ seed >>> 32) + 202;
        Canvas c = new Canvas("monastery_drawn");
        podium(c, s);
        ranges(c, s);
        cloister(c, s);
        roofs(c, s);
        CryptLayout.draw(c, s + 50);
        rooms(c, s);
        weather(c, s);
        c.anchor("entry", 0, 0, -3);
        c.anchor("garth", 0, 0, 8);
        c.anchor("crypt", 2, CryptLayout.FLOOR, 13);
        return c;
    }

    // --- podium and walls ---------------------------------------------------------------------------------------------------

    private static void podium(Canvas c, int s) {
        Brush foot = SpireKit.footing(s);
        Brush floor = SpireKit.floor(s + 1);
        for (int x = -10; x <= 10; x++) {
            for (int z = 0; z <= 20; z++) {
                c.set(x, -1, z, floor.at(x, -1, z));
                SpireKit.pier(c, x, z, -2, PODIUM, foot);
                for (int y = 0; y <= TOP; y++) c.carve(x, y, z);
            }
        }
        // Buttresses down the open faces, from the podium's foot to the eaves.
        Brush stone = SpireKit.wall(s + 2, -1);
        for (Walls.Run r : Walls.around(new Box(-10, 0, 0, 10, 0, 20))) {
            if (r.out() == Dir.NORTH) continue;
            for (int i : Walls.pillarPositions(r, 5)) {
                int x = r.x(i) + r.out().dx, z = r.z(i) + r.out().dz;
                for (int y = PODIUM; y <= 3; y++) c.set(x, y, z, y < 0 ? foot.at(x, y, z) : y == 0 ? SpireKit.QUARTZ : stone.at(x, y, z));
                c.set(x, 4, z, SpireKit.QUARTZ_TRIM.stairs(r.out().opposite(), false));
                c.set(x, -1, z, SpireKit.CHISELED);
            }
        }
        // The forecourt toward the stair.
        for (int x = -4; x <= 4; x++) {
            for (int z = -5; z <= -1; z++) {
                c.set(x, -1, z, Math.abs(x) == 4 ? SpireKit.DIORITE : floor.at(x, -1, z));
                SpireKit.pier(c, x, z, -2, -2, foot);
                for (int y = 0; y <= 4; y++) c.carve(x, y, z);
            }
        }
        Lighting.lampPost(c, -4, 0, -2, 2, SpireKit.CHISELED, SpireKit.BALUSTER, null, false);
        Lighting.lampPost(c, 4, 0, -2, 2, SpireKit.CHISELED, SpireKit.BALUSTER, null, false);
    }

    private static void ranges(Canvas c, int s) {
        Brush stone = SpireKit.wall(s + 2, -1);
        for (Box b : new Box[]{NORTH, SOUTH, WEST, EAST}) {
            for (Walls.Run r : Walls.around(b)) {
                Walls.body(c, r, 0, TOP, stone);
            }
        }
        for (Walls.Run r : Walls.around(new Box(-10, 0, 0, 10, 0, 20))) {
            Walls.plinth(c, r, 0, 1, Family.MOSSY_STONE_BRICKS, r.out() != Dir.NORTH);
            Walls.belt(c, r, TOP - 2, SpireKit.TRIM);
        }
        // Partitions: the gate passage in the north range, the cells in the east range.
        for (int x : new int[]{-2, 2}) for (int z = 1; z <= 3; z++) for (int y = 0; y <= TOP; y++) c.set(x, y, z, stone.at(x, y, z));
        for (int z : new int[]{8, 12}) for (int x = 7; x <= 9; x++) for (int y = 0; y <= TOP; y++) c.set(x, y, z, stone.at(x, y, z));
        // The gate: arches through both walls of the north range, a gilded keystone and quartz jambs outside.
        Walls.Run outer = new Walls.Run(-10, 0, 10, 0, Dir.NORTH), inner = new Walls.Run(-10, 4, 10, 4, Dir.SOUTH);
        Openings.archway(c, outer, 9, 0, 3, 4, "minecraft:quartz_stairs");
        Openings.archway(c, inner, 9, 0, 3, 4, "minecraft:quartz_stairs");
        for (int x : new int[]{-2, 2}) {
            for (int y = 0; y <= 4; y++) c.set(x, y, -1, y == 4 ? SpireKit.CHISELED : SpireKit.pillar("y"));
            c.set(x, 5, -1, SpireKit.QUARTZ_TRIM.stairs(Dir.SOUTH, false));
        }
        for (int x = -1; x <= 1; x++) c.set(x, 4, 0, x == 0 ? SpireKit.GOLD : SpireKit.CHISELED);
        Openings.DoorStyle door = new Openings.DoorStyle("minecraft:birch_door", null, null, null, null, null);
        Openings.door(c, new Walls.Run(-2, 1, -2, 3, Dir.WEST), 1, 0, 1, door);
        Openings.door(c, new Walls.Run(2, 1, 2, 3, Dir.EAST), 1, 0, 1, door);
        // Off the cloister walk: two doors into the refectory, one into each cell, the chapel through an arch.
        Walls.Run westIn = new Walls.Run(-6, 4, -6, 16, Dir.WEST), eastIn = new Walls.Run(6, 4, 6, 16, Dir.EAST);
        for (int z : new int[]{8, 12}) Openings.door(c, westIn, z - 4, 0, 1, door);
        for (int z : new int[]{6, 10, 14}) Openings.door(c, eastIn, z - 4, 0, 1, door);
        Openings.archway(c, new Walls.Run(-10, 16, 10, 16, Dir.NORTH), 9, 0, 3, 4, "minecraft:quartz_stairs");
        // Windows.
        Openings.WindowStyle lancet = new Openings.WindowStyle("minecraft:white_stained_glass_pane", null, SpireKit.TRIM, null, null, null, false,
                null);
        for (int x : new int[]{-7, -4, 4, 7}) Openings.window(c, outer, x + 10, 1, 1, 3, lancet);
        Walls.Run west = new Walls.Run(-10, 20, -10, 0, Dir.WEST), east = new Walls.Run(10, 0, 10, 20, Dir.EAST),
                south = new Walls.Run(10, 20, -10, 20, Dir.SOUTH);
        for (int z : new int[]{2, 6, 14, 18}) Openings.window(c, west, 20 - z, 1, 1, 3, lancet);
        for (int z : new int[]{2, 6, 10, 14}) Openings.window(c, east, z, 1, 1, 2, lancet);
        for (int x : new int[]{-6, -2, 2, 7}) Openings.window(c, south, 10 - x, 1, 1, 3, lancet);
        Openings.window(c, east, 18, 1, 1, 3, new Openings.WindowStyle("minecraft:yellow_stained_glass_pane", null, SpireKit.TRIM, null, null,
                null, false, null));
        // The refectory's fireplace in the west wall, its chimney through the roof.
        Furniture.fireplace(c, -10, 0, 10, Dir.EAST, Family.STONE_BRICKS, true);
        Roofs.chimney(c, -11, 10, 0, SpireKit.footing(s + 3), Family.STONE_BRICKS, true);
    }

    // --- the cloister ----------------------------------------------------------------------------------------------------

    private static void cloister(Canvas c, int s) {
        // The arcade: columns every third cell round the garth, pointed openings between them, a lintel over all.
        Brush stone = SpireKit.wall(s + 2, -1);
        for (int x = -3; x <= 3; x++) {
            for (int z = 7; z <= 13; z++) {
                boolean line = Math.abs(x) == 3 || z == 7 || z == 13;
                if (!line) continue;
                boolean column = (x % 3 == 0) && (z - 7) % 3 == 0;
                if (column) {
                    c.set(x, 0, z, SpireKit.QUARTZ);
                    for (int y = 1; y <= 2; y++) c.set(x, y, z, SpireKit.pillar("y"));
                    c.set(x, 3, z, SpireKit.CHISELED);
                } else {
                    Dir toCol;
                    if (Math.abs(x) == 3) toCol = Math.floorMod(z - 7, 3) == 1 ? Dir.NORTH : Dir.SOUTH;
                    else toCol = Math.floorMod(x + 3, 3) == 1 ? Dir.WEST : Dir.EAST;
                    c.set(x, 3, z, SpireKit.QUARTZ_TRIM.stairs(toCol, true));
                }
                c.set(x, 4, z, stone.at(x, 4, z));
            }
        }
        // The walk's lean-to: copper over the walk, stepping up from the arcade to the ranges' walls.
        for (int x = -5; x <= 5; x++) {
            for (int z = 5; z <= 15; z++) {
                int dx = Math.abs(x), dz = Math.abs(z - 10);
                int d = Math.max(dx, dz);
                if (d < 3) continue;
                Dir toWall = dx >= dz ? (x < 0 ? Dir.WEST : Dir.EAST) : (z < 10 ? Dir.NORTH : Dir.SOUTH);
                if (d == 3) c.set(x, 5, z, SpireKit.ROOF.stairs(toWall, false));
                else if (d == 4) {
                    c.set(x, 5, z, SpireKit.ROOF.block());
                    c.set(x, 6, z, SpireKit.ROOF.stairs(toWall, false));
                } else {
                    c.set(x, 5, z, SpireKit.ROOF.block());
                    c.set(x, 6, z, SpireKit.ROOF.block());
                }
            }
        }
        // Lanterns in the walk's corners and midway along.
        for (int[] l : new int[][]{{-5, 5}, {5, 5}, {-5, 15}, {5, 15}, {0, 5}, {0, 15}, {-5, 10}, {5, 10}}) Lighting.hanging(c, l[0], 5, l[1], 1, false);
        // The garth: grass and flowers round a well.
        Brush grass = org.papiricoh.supernaturalcraft.buildkit.Palette.patches2(s + 5, 2, "minecraft:grass_block", 6, "minecraft:moss_block", 2,
                "minecraft:podzol", 1);
        for (int x = -2; x <= 2; x++) for (int z = 8; z <= 12; z++) c.set(x, -1, z, grass.at(x, -1, z));
        for (int x = -1; x <= 1; x++) {
            for (int z = 9; z <= 11; z++) {
                c.set(x, -1, z, SpireKit.CALCITE);
                c.set(x, 0, z, x == 0 && z == 10 ? St.water() : x != 0 && z != 10 ? SpireKit.CHISELED : SpireKit.TRIM.slabBottom());
            }
        }
        c.set(0, 0, 9, SpireKit.SMOOTH);
        c.set(0, 0, 11, SpireKit.SMOOTH);
        c.set(-1, 0, 10, SpireKit.SMOOTH);
        c.set(1, 0, 10, SpireKit.SMOOTH);
        for (int x : new int[]{-1, 1}) {
            c.set(x, 1, 10, St.wall(SpireKit.BALUSTER));
            c.set(x, 2, 10, St.wall(SpireKit.BALUSTER));
            c.set(x, 3, 10, SpireKit.TRIM.slabBottom());
        }
        c.set(0, 3, 10, SpireKit.SMOOTH);
        c.set(0, 2, 10, St.chain("y"));
        String[] flowers = {"minecraft:lily_of_the_valley", "minecraft:oxeye_daisy", "minecraft:azure_bluet", "minecraft:white_tulip"};
        for (int x = -2; x <= 2; x++) {
            for (int z = 8; z <= 12; z++) {
                if (!c.isAir(x, 0, z) || !Kinds.soil(c.world(x, -1, z))) continue;
                if (Noise.chance(x, 0, z, s + 6, 0.55)) c.set(x, 0, z, flowers[Noise.pick(x, 1, z, s + 7, flowers.length)]);
            }
        }
    }

    private static void roofs(Canvas c, int s) {
        Roofs.Style copper = SpireKit.roof(s + 8, SpireKit.wall(s + 9, -1));
        new Roofs.Plan().gable(NORTH, TOP, true, copper).gable(SOUTH, TOP, true, copper).gable(WEST, TOP, false, copper)
                .gable(EAST, TOP, false, copper).draw(c);
        SpireKit.weatherRoof(c, new Box(-12, TOP, -2, 12, 20, 22), s + 10);
        // The roof fallen in over the last cell.
        for (int x = 6; x <= 11; x++) {
            for (int z = 13; z <= 16; z++) {
                if (Noise.hash01(x, 0, z, s + 11) < 0.9 - Math.abs(z - 14.5) * 0.2 - Math.abs(x - 8) * 0.1) {
                    for (int y = TOP + 1; y <= 14; y++) c.carve(x, y, z);
                }
            }
        }
        // A bell-cote over the gate.
        int top = c.topAt(0, 2, 30, TOP);
        c.set(0, top + 1, 2, SpireKit.CHISELED);
        for (int z : new int[]{1, 3}) {
            c.set(0, top + 1, z, SpireKit.TRIM.slabBottom());
            c.set(0, top + 2, z, St.wall(SpireKit.BALUSTER));
            c.set(0, top + 3, z, SpireKit.TRIM.slabBottom());
        }
        c.set(0, top + 3, 2, SpireKit.SMOOTH);
        c.set(0, top + 2, 2, St.of("bell", "attachment", "ceiling", "facing", "north", "powered", "false"));
    }

    // --- the rooms -----------------------------------------------------------------------------------------------------------

    private static void rooms(Canvas c, int s) {
        // Scriptorium: shelves along the cloister wall, two desks under the windows, the chest in the corner.
        Furniture.bookshelves(c, -9, 3, Dir.EAST, 6, 0, 2, Dir.NORTH, true, SpireKit.pillar("y"), 3);
        Furniture.desk(c, -7, 0, 1, Dir.NORTH, "minecraft:birch_slab", "minecraft:birch_stairs", false);
        Furniture.desk(c, -4, 0, 1, Dir.NORTH, "minecraft:birch_slab", "minecraft:birch_stairs", false);
        chest(c, CHESTS.get(1), Dir.EAST);
        // Storeroom: barrels and sacks, the chest.
        for (int[] b : new int[][]{{3, 3}, {4, 3}, {8, 3}, {9, 3}, {9, 2}}) c.set(b[0], 0, b[1], St.barrel(Dir.UP, false));
        c.set(3, 1, 3, St.barrel(Dir.UP, false));
        c.set(9, 1, 3, "minecraft:hay_block");
        c.set(5, 0, 3, St.decoratedPot(Dir.NORTH));
        chest(c, CHESTS.get(0), Dir.WEST);
        Lighting.sconce(c, 6, 2, 3, Dir.NORTH, "minecraft:birch_trapdoor", false);
        Lighting.sconce(c, -6, 2, 1, Dir.SOUTH, "minecraft:birch_trapdoor", false);
        // Refectory: one long table with benches, candles down it, the reader's lectern at its foot.
        Furniture.table(c, -8, 6, -8, 14, 0, "minecraft:birch_slab");
        for (int z = 6; z <= 14; z++) {
            if (z < 9 || z > 11) c.set(-9, 0, z, St.stairs("minecraft:birch_stairs", Dir.WEST, false));
            if (z != 8 && z != 12) c.set(-7, 0, z, St.stairs("minecraft:birch_stairs", Dir.EAST, false));
            if (z % 3 == 0) c.set(-8, 1, z, St.candle("white", 2 + Noise.pick(-8, 1, z, s + 20, 3), true));
        }
        c.set(-8, 0, 15, St.lectern(Dir.NORTH));
        c.set(-8, 0, 5, St.decoratedPot(Dir.SOUTH));
        Lighting.sconce(c, -9, 2, 13, Dir.EAST, "minecraft:birch_trapdoor", false);
        // The cells: a bed, a candle on a shelf, a barrel.
        for (int z0 : new int[]{5, 9, 13}) {
            c.setBed(9, 0, z0, St.bed("white", Dir.SOUTH));
            c.set(8, 0, z0 + 2, SpireKit.TRIM.slabTop());
            c.set(8, 1, z0 + 2, St.candle("white", 1 + Noise.pick(8, 1, z0, s + 21, 3), true));
            c.set(9, 0, z0 + 2, St.barrel(Dir.UP, false));
            if (z0 == 13) {
                c.set(7, 0, 13, St.mossCarpet());
                c.set(8, 0, 14, SpireKit.ROOF.slabBottom());
            }
        }
        // The chapel: the altar at its east end, pews facing it, a parapet round the stairwell to the crypt.
        c.set(8, 0, 18, SpireKit.CHISELED);
        c.set(8, 1, 18, St.candle("white", 4, true));
        c.set(9, 0, 17, SpireKit.GOLD);
        c.set(9, 1, 17, St.candle("yellow", 2, true));
        c.set(9, 0, 19, SpireKit.GOLD);
        c.set(9, 1, 19, St.candle("yellow", 3, true));
        for (int x : new int[]{4, 6}) {
            for (int z : new int[]{17, 19}) c.set(x, 0, z, St.stairs("minecraft:birch_stairs", Dir.WEST, false));
        }
        // The stairwell (open where the flight cuts the floor) behind a parapet; the passage beside it stays free.
        for (int x = CryptLayout.STAIR_HEAD[0] - 4; x < CryptLayout.STAIR_HEAD[0]; x++) c.set(x, 0, 18, St.wall(SpireKit.BALUSTER));
        c.set(CryptLayout.STAIR_HEAD[0] - 5, 0, 19, St.wall(SpireKit.BALUSTER));
        c.set(-8, 0, 19, St.candle("white", 3, true));
        c.set(-9, 0, 19, SpireKit.CHISELED);
        c.set(-9, 1, 19, St.of("skeleton_skull", "rotation", "4", "powered", "false"));
        for (int[] l : new int[][]{{0, 18}, {6, 18}, {-6, 18}, {-8, 7}, {-8, 13}, {6, 2}, {-6, 2}, {0, 2}}) hang(c, l[0], l[1]);
    }

    /** A lantern on a chain from whatever roofs the cell (the first block above head height), hanging to about y = 4. */
    static void hang(Canvas c, int x, int z) {
        for (int y = 4; y <= 20; y++) {
            if (c.isAir(x, y, z)) continue;
            Lighting.hanging(c, x, y, z, Math.max(1, y - 4), false);
            return;
        }
    }

    private static void chest(Canvas c, int[] at, Dir facing) {
        c.set(at[0], at[1], at[2], St.of("chest", "facing", facing.id(), "type", "single", "waterlogged", "false"));
        c.decor(Decor.loot(at[0], at[1], at[2], SpireKit.LOOT_MONASTERY));
    }

    private static void weather(Canvas c, int s) {
        for (int x = -9; x <= 9; x++) {
            for (int z = 1; z <= 19; z++) {
                if (c.isAir(x, 0, z) && Kinds.fullCube(c.world(x, -1, z)) && !Kinds.soil(c.world(x, -1, z))
                        && Noise.chance(x, 0, z, s + 30, Noise.patch2(x, z, 4, s + 31) > 0.6 ? 0.35 : 0.03)) {
                    c.set(x, 0, z, St.mossCarpet());
                }
            }
        }
        for (int[] w : new int[][]{{-9, 5, 1}, {9, 5, 1}, {-9, 4, 15}, {9, 4, 7}, {-9, 4, 19}, {5, 4, 15}}) {
            if (c.isAir(w[0], w[1], w[2])) c.set(w[0], w[1], w[2], "minecraft:cobweb");
        }
        SpireKit.ivy(c, new Box(-12, PODIUM, -1, 12, TOP, 22), new Box(-10, 0, 0, 10, 0, 20), 0.12, s + 32);
    }
}
