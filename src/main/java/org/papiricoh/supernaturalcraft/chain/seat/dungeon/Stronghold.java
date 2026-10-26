package org.papiricoh.supernaturalcraft.chain.seat.dungeon;

import org.papiricoh.supernaturalcraft.buildkit.Box;
import org.papiricoh.supernaturalcraft.buildkit.Brush;
import org.papiricoh.supernaturalcraft.buildkit.Canvas;
import org.papiricoh.supernaturalcraft.buildkit.Dir;
import org.papiricoh.supernaturalcraft.buildkit.Family;
import org.papiricoh.supernaturalcraft.buildkit.Furniture;
import org.papiricoh.supernaturalcraft.buildkit.Lighting;
import org.papiricoh.supernaturalcraft.buildkit.Noise;
import org.papiricoh.supernaturalcraft.buildkit.Openings;
import org.papiricoh.supernaturalcraft.buildkit.Outside;
import org.papiricoh.supernaturalcraft.buildkit.Palette;
import org.papiricoh.supernaturalcraft.buildkit.Roofs;
import org.papiricoh.supernaturalcraft.buildkit.St;
import org.papiricoh.supernaturalcraft.buildkit.Trees;
import org.papiricoh.supernaturalcraft.buildkit.Walls;
import org.papiricoh.supernaturalcraft.chain.ChainFaction;

import java.util.List;
import java.util.Random;

/**
 * Draws one stronghold (pure; {@link DungeonCatalog} is the public face). Origin on the ground at the site, the gate to the
 * south (+z), the throne to the north. The layout, outline by outline:
 * <ul>
 *   <li>{@link #OUT}/{@link #IN}: the curtain wall, two thick, with a 7 × 7 tower at each corner;</li>
 *   <li>the castle's lava moat three wide round it (the Monastery has a lawn and a hedge instead), the approach to the south;</li>
 *   <li>{@link #GATE}: the gatehouse in the middle of the south wall, its passage {@code x = -1..1};</li>
 *   <li>the court in the middle; four wing slots beside it ({@link #SLOTS}: west and east, north and south);</li>
 *   <li>{@link #HALL}: the throne hall to the north, the dais at its north end; the vault ({@link #VAULT}) under it.</li>
 * </ul>
 */
final class Stronghold {

    static final int R = DungeonAnchors.RADIUS;
    static final Box OUT = new Box(-32, 0, -32, 32, 0, 24), IN = new Box(-31, 0, -31, 31, 0, 23);
    static final Box HALL = new Box(-12, 0, -30, 12, 0, -11);
    static final Box GATE = new Box(-6, 0, 16, 6, 0, 24);
    /** The vault's room (inside its shell). */
    static final Box VAULT = new Box(-10, -10, -33, 10, -6, -26);

    /** Where a wing is stamped: moved by (dx, dz) after {@code q} quarter turns, so its door opens on the court. */
    record Slot(String id, int dx, int dz, int q) {
    }

    static final List<Slot> SLOTS = List.of(new Slot("west_north", -18, -15, 3), new Slot("west_south", -18, 5, 3),
            new Slot("east_north", 18, -15, 1), new Slot("east_south", 18, 5, 1));

    private final ChainFaction faction;
    private final long seed;
    private final int s;
    private final boolean pit;
    private final DungeonStyle st;
    private final Random rnd;
    private final int curtainTop, hallTop;
    private Canvas c;

    Stronghold(ChainFaction faction, long seed) {
        this.faction = faction;
        this.seed = seed;
        this.s = DungeonKit.salt(seed) ^ (faction == ChainFaction.DEMON ? 0x51D : 0xA4E);
        this.pit = faction == ChainFaction.DEMON;
        this.st = DungeonStyle.of(faction, s);
        this.rnd = new Random(seed ^ (pit ? 0x5EEDL : 0xA16E1L));
        this.curtainTop = pit ? 7 : 4;
        this.hallTop = pit ? 12 : 11;
    }

    Canvas draw() {
        c = new Canvas(DungeonCatalog.name(faction));
        // The stronghold stands on the ground: what is not planned under its floor is rock.
        c.outside(Outside.groundUpTo(-1));
        Box all = DungeonCatalog.bounds();
        for (String z : List.of("grounds", "wings", "hall", "vault", "court", "walls", "gatehouse")) c.zone(z, all);
        c.inZone("grounds", () -> {
            grounds();
            if (pit) moat();
            else lawns();
            approach();
        });
        c.inZone("wings", this::wings);
        c.inZone("hall", this::hall);
        c.inZone("vault", this::vault);
        c.inZone("court", pit ? this::courtyard : this::cloister);
        c.inZone("walls", this::curtain);
        c.inZone("gatehouse", this::gatehouse);
        c.anchor(DungeonAnchors.ENTRANCE, 0, 0, 33);
        c.anchor(DungeonAnchors.ARRIVAL, 0, 0, 13);
        DungeonKit.lightUp(c, st.light, 8);
        return c.finish();
    }

    // --- the ground --------------------------------------------------------------------------------------------------------

    private void grounds() {
        Brush below = pit ? Palette.mix(s + 70, "minecraft:blackstone", 2, "minecraft:basalt", 1) : Brush.of("minecraft:dirt");
        for (int x = -R; x <= R; x++) {
            for (int z = -R; z <= R; z++) {
                boolean inside = IN.containsColumn(x, z);
                c.set(x, -3, z, pit ? below.at(x, -3, z) : "minecraft:stone");
                c.set(x, -2, z, below.at(x, -2, z));
                c.set(x, -1, z, st.court.at(x, -1, z));
                int up = inside ? 9 : 5;
                for (int y = 0; y <= up; y++) c.carve(x, y, z);
            }
        }
    }

    /** The lava moat, its rim railing and the scorched approach. */
    private void moat() {
        for (int x = -R; x <= R; x++) {
            for (int z = -R; z <= R; z++) {
                boolean moat = Math.abs(x) <= 35 && z >= -35 && z <= 27 && !OUT.containsColumn(x, z);
                if (moat) {
                    c.set(x, -3, z, st.plinth.at(x, -3, z));
                    c.set(x, -2, z, "minecraft:lava");
                    c.carve(x, -1, z);
                } else if (!OUT.containsColumn(x, z)) {
                    boolean rim = Math.abs(x) == 36 || z == -36 || z == 28 && Math.abs(x) <= 36;
                    if (rim && !(z == 28 && Math.abs(x) <= 2)) {
                        c.set(x, -4, z, st.plinth.at(x, -4, z));
                        c.set(x, 0, z, Noise.chance(x, 0, z, s + 71, 0.15) ? "minecraft:air" : St.wall(st.post));
                    }
                }
            }
        }
    }

    /** The Monastery's outer lawns: a clipped hedge round the rim, flowers along the enclosure. */
    private void lawns() {
        for (int x = -R; x <= R; x++) {
            for (int z = -R; z <= R; z++) {
                if (OUT.containsColumn(x, z)) continue;
                boolean rim = Math.abs(x) == 36 || z == -36;
                if (rim) {
                    c.set(x, -1, z, "minecraft:grass_block");
                    c.set(x, 0, z, St.leaves(Noise.chance(x, 0, z, s + 72, 0.3) ? "minecraft:flowering_azalea_leaves" : "minecraft:azalea_leaves"));
                } else if ((Math.abs(x) == 33 || z == -33) && Noise.chance(x, 1, z, s + 73, 0.35)) {
                    String[] f = {"minecraft:lily_of_the_valley", "minecraft:oxeye_daisy", "minecraft:azure_bluet", "minecraft:cornflower"};
                    c.set(x, -1, z, "minecraft:grass_block");
                    c.set(x, 0, z, f[Noise.pick(x, 0, z, s + 74, f.length)]);
                }
            }
        }
    }

    private void approach() {
        int z0 = pit ? 28 : 25;
        for (int z = z0; z <= R; z++) {
            for (int x = -2; x <= 2; x++) {
                c.set(x, -1, z, st.path.at(x, -1, z));
                c.carve(x, 0, z);
            }
        }
        if (pit) {
            // Spikes and soul lamps line the way to the drawbridge; braziers at the bridge head.
            for (int z = 30; z <= R; z += 3) {
                for (int side : new int[]{-1, 1}) {
                    int x = side * (4 + Noise.pick(side, 0, z, s + 75, 2));
                    c.set(x, 0, z, "minecraft:basalt");
                    c.set(x, 1, z, St.dripstone(Dir.UP, "frustum"));
                    c.set(x, 2, z, St.dripstone(Dir.UP, "tip"));
                }
            }
            for (int z : new int[]{31, 35}) for (int x : new int[]{-3, 3}) Lighting.lampPost(c, x, 0, z, 2, "minecraft:polished_blackstone", st.post, null, true);
            DungeonKit.brazier(c, -3, 0, 28, st.post, false);
            DungeonKit.brazier(c, 3, 0, 28, st.post, false);
            for (int[] b : new int[][]{{-9, 31}, {8, 34}, {-12, 35}, {11, 30}}) c.set(b[0], 0, b[1], "minecraft:bone_block[axis=y]");
        } else {
            for (int z = 27; z <= R; z += 4) {
                for (int x : new int[]{-3, 3}) Lighting.lampPost(c, x, 0, z, 2, "minecraft:chiseled_stone_bricks", "minecraft:diorite_wall", null, false);
            }
            for (int z = 25; z <= R - 1; z++) {
                for (int x : new int[]{-5, -4, 4, 5}) {
                    c.set(x, 0, z, St.leaves("minecraft:azalea_leaves"));
                }
            }
            Trees.birch(c, -11, 0, 31, 0, s + 76);
            Trees.birch(c, 12, 0, 30, 0, s + 77);
        }
    }

    // --- the wings ---------------------------------------------------------------------------------------------------------

    private void wings() {
        List<Wing> layout = DungeonCatalog.layout(faction, seed);
        for (int i = 0; i < SLOTS.size(); i++) {
            Slot slot = SLOTS.get(i);
            Canvas local = layout.get(i).draw(st, s + 101 * (i + 1));
            c.stamp(local, slot.dx(), 0, slot.dz(), slot.q(), Canvas.Mode.SET, "");
        }
    }

    // --- the throne hall ----------------------------------------------------------------------------------------------------

    private void hall() {
        int top = hallTop;
        Box outline = HALL.withY(0, top);
        // Solid ground under the hall, into which the vault and its stair are cut.
        c.fill(new Box(-14, -12, -32, 14, -2, -10), st.plinth);
        c.fill(new Box(-14, -12, -35, 14, -4, -33), st.plinth);
        for (int x = HALL.x0(); x <= HALL.x1(); x++) {
            for (int z = HALL.z0(); z <= HALL.z1(); z++) {
                boolean in = x > HALL.x0() && x < HALL.x1() && z > HALL.z0() && z < HALL.z1();
                c.set(x, -1, z, (in ? st.floor : st.plinth).at(x, -1, z));
            }
        }
        Walls.enclose(c, outline, 0, top, st.wall);
        for (Walls.Run r : Walls.around(outline)) Walls.body(c, r, 0, 0, st.plinth);
        c.carve(new Box(-11, 0, -29, 11, top, -12));
        List<Walls.Run> runs = Walls.around(outline);
        Walls.Run east = runs.get(1), west = runs.get(3), south = runs.get(2), north = runs.get(0);
        // Side walls: buttresses between tall windows.
        for (Walls.Run r : new Walls.Run[]{east, west}) {
            for (int z : new int[]{-13, -17, -21, -25}) DungeonKit.buttress(c, r, idx(r, z), 0, top - 2, st.plinth, st.trim);
            for (int z : new int[]{-15, -19, -23, -27}) {
                Openings.window(c, r, idx(r, z), pit ? 3 : 2, 1, pit ? 6 : 7,
                        new Openings.WindowStyle(st.pane(z / 4), null, st.trim, st.trim, null, null, false, null));
            }
        }
        // The south front: the great door.
        Openings.archway(c, south, 10, 0, 5, 6, st.trim.stairs());
        for (int x = -1; x <= 1 && pit; x++) c.set(x, 5, -11, St.bars());
        c.set(0, 6, -11, st.accent.block());
        for (int x : new int[]{-3, 3}) Lighting.sconce(c, x, 2, -10, Dir.SOUTH, pit ? st.wood.trapdoorId() : "minecraft:birch_trapdoor", pit);
        for (int i : new int[]{2, 6, 18, 22}) DungeonKit.buttress(c, south, i, 0, top - 3, st.plinth, st.trim);
        // Columns down the nave.
        for (int x : new int[]{-6, 6}) {
            for (int z : new int[]{-14, -18, -22}) column(x, z, top);
        }
        // The runner to the dais and the floor lights either side of it.
        for (int z = -23; z <= -12; z++) {
            c.set(0, 0, z, St.carpet(pit ? "red" : "white"));
            c.set(-1, 0, z, St.carpet(pit ? "black" : "yellow"));
            c.set(1, 0, z, St.carpet(pit ? "black" : "yellow"));
            for (int x : new int[]{-3, 3}) {
                if (pit) {
                    if (z <= -13 && z >= -22) {
                        c.set(x, -2, z, "minecraft:lava");
                        c.set(x, -1, z, St.bars());
                    }
                } else if (Math.floorMod(z, 2) == 0) {
                    c.set(x, -1, z, st.light);
                }
            }
        }
        for (int[] b : new int[][]{{-10, -14}, {10, -14}, {-10, -24}, {10, -24}, {-10, -19}}) {
            if (pit) DungeonKit.brazier(c, b[0], 0, b[1], st.post, false);
            else Lighting.lampPost(c, b[0], 0, b[1], 1, "minecraft:chiseled_quartz_block", st.post, null, false);
        }
        dais();
        if (pit) castleRoof(outline);
        else monasteryRoof(outline);
        c.anchor(DungeonAnchors.THRONE_ROOM, 0, 0, -20);
        int[][] guards = {{-9, -14}, {9, -13}, {-9, -23}, {11, -24}, {-4, -16}, {4, -21}};
        for (int i = 0; i < guards.length; i++) DungeonKit.guard(c, "throne_room", i, guards[i][0], 0, guards[i][1]);
    }

    /** Run index of world z on a side run of the hall. */
    private static int idx(Walls.Run r, int z) {
        return r.along() == Dir.SOUTH ? z - r.z0() : r.z0() - z;
    }

    private void column(int x, int z, int top) {
        String shaft = pit ? "minecraft:polished_basalt[axis=y]" : "minecraft:quartz_pillar[axis=y]";
        String cap = pit ? "minecraft:chiseled_polished_blackstone" : "minecraft:chiseled_quartz_block";
        int capY = pit ? top : top - 1;
        c.set(x, 0, z, st.trim.block());
        for (int y = 1; y < capY; y++) c.set(x, y, z, shaft);
        c.set(x, capY, z, cap);
        for (Dir d : Dir.HORIZONTAL) {
            c.setIfAir(x + d.dx, 0, z + d.dz, st.trim.stairs(d.opposite(), false));
            c.setIfAir(x + d.dx, capY, z + d.dz, st.trim.stairs(d.opposite(), true));
        }
        if (pit) Lighting.hanging(c, x, top + 1, z + 2, 4, true);
    }

    /** The dais at the north end, its full-width steps, the throne and the posts at either hand. */
    private void dais() {
        Brush top = pit ? Palette.mix(s + 80, "minecraft:polished_blackstone_bricks", 3, "minecraft:red_nether_bricks", 2,
                "minecraft:gilded_blackstone", 1, "minecraft:deepslate", 2)
                : Palette.mix(s + 80, "minecraft:quartz_bricks", 3, "minecraft:smooth_quartz", 2, "minecraft:calcite", 2,
                "minecraft:polished_diorite", 2);
        c.fill(new Box(-6, 0, -29, 6, 1, -25), top);
        Furniture.flight(c, -6, 0, -24, Dir.NORTH, 2, 13, st.trim.stairs(), false);
        c.fill(new Box(-6, 0, -25, 6, 0, -25), top);
        // The throne and its back in the north wall.
        String seat = pit ? "minecraft:polished_blackstone_brick_stairs" : "minecraft:quartz_stairs";
        Furniture.chair(c, 0, 2, -29, Dir.SOUTH, seat, pit ? st.wood.trapdoorId() : "minecraft:birch_trapdoor");
        for (int y = 2; y <= 9; y++) {
            c.set(0, y, -30, pit ? "minecraft:crying_obsidian" : y == 7 ? "minecraft:gold_block" : "minecraft:chiseled_quartz_block");
            if (y <= 7) for (int x : new int[]{-1, 1}) c.set(x, y, -30, pit ? "minecraft:red_nether_bricks" : "minecraft:quartz_pillar[axis=y]");
            if (y <= 5) for (int x : new int[]{-2, 2}) c.set(x, y, -30, pit ? "minecraft:polished_blackstone_bricks" : "minecraft:smooth_quartz");
        }
        if (pit) {
            for (int x : new int[]{-3, 3}) DungeonKit.brazier(c, x, 2, -29, st.post, false);
            for (int x : new int[]{-2, 2}) for (int y = 6; y <= hallTop; y++) c.set(x, y, -28, St.chain("y"));
            Lighting.hanging(c, -2, 6, -28, 1, true);
            Lighting.hanging(c, 2, 6, -28, 1, true);
        } else {
            for (int x : new int[]{-3, 3}) {
                c.set(x, 2, -29, "minecraft:quartz_pillar[axis=y]");
                c.set(x, 3, -29, St.lantern(false));
            }
            c.set(-1, 7, -29, St.endRod(Dir.WEST));
            c.set(1, 7, -29, St.endRod(Dir.EAST));
            c.set(0, 10, -29, St.endRod(Dir.UP));
            for (int x = -1; x <= 1; x++) for (int y = x == 0 ? 10 : 9; y <= hallTop; y++) c.set(x, y, -30, St.pane(st.pane(1)));
        }
        c.anchor(DungeonAnchors.LORD, 0, 2, -28);
        c.anchor(DungeonAnchors.COMMANDERS.get(0), -4, 2, -27);
        c.anchor(DungeonAnchors.COMMANDERS.get(1), 4, 2, -27);
    }

    /** The keep's roof: a flat deck behind machicolated battlements, corner turrets with red spires, the Pit's donjon over the dais. */
    private void castleRoof(Box outline) {
        int deck = hallTop + 1;
        c.fill(new Box(HALL.x0(), deck, HALL.z0(), HALL.x1(), deck, HALL.z1()), st.ceiling);
        DungeonKit.battlements(c, outline.withY(0, deck), deck + 1, st.wall, st.accent, s + 81, 0.1);
        Roofs.Style red = new Roofs.Style(st.roofAlt, st.roofTrim, st.wall, 1, Roofs.Pitch.STEEP, 0, false, null, false);
        for (int[] k : new int[][]{{HALL.x0(), HALL.z0()}, {HALL.x1(), HALL.z0()}, {HALL.x0(), HALL.z1()}, {HALL.x1(), HALL.z1()}}) {
            Box t = new Box(k[0] - 1, -2, k[1] - 1, k[0] + 1, 18, k[1] + 1);
            c.fill(t, st.wall);
            for (int y = 3; y <= 15; y += 6) c.set(k[0] + (k[0] < 0 ? -1 : 1), y, k[1], St.bars());
            Roofs.hip(c, t.withY(0, 18), 18, red);
        }
        Box donjon = new Box(-5, deck, -29, 5, 22, -23);
        c.fill(donjon, st.wall);
        DungeonKit.battlements(c, donjon.withY(0, 22), 23, st.wall, st.accent, s + 82, 0.12);
        for (int x : new int[]{-3, 0, 3}) for (int y = 16; y <= 19; y++) c.set(x, y, -23, y == 16 || y == 19 ? st.accent.block() : St.bars());
        c.fill(new Box(-2, 23, -28, 2, 25, -24), st.wall);
        Roofs.hip(c, new Box(-2, 0, -28, 2, 0, -24), 25, red);
        int topY = c.topAt(0, -26, DungeonAnchors.HEIGHT, 25);
        c.set(0, topY + 1, -26, St.wall(st.post));
        c.set(0, topY + 2, -26, St.rod("minecraft:lightning_rod", Dir.UP));
    }

    /** The basilica's roof: a steep copper gable with quartz verges, a rose window in the south gable, pinnacles at the corners. */
    private void monasteryRoof(Box outline) {
        Roofs.gable(c, outline, hallTop, false, st.roofStyle(true));
        Palette.swapFamily(c, new Box(-14, hallTop, -32, 14, 30, -9), st.roof, st.roofAlt, 0.25, 3, s + 83);
        // The rose window.
        int cy = hallTop + 5;
        for (int x = -3; x <= 3; x++) {
            for (int y = cy - 3; y <= cy + 3; y++) {
                double r = Math.hypot(x, y - cy);
                if (r <= 1.2) c.set(x, y, -11, St.pane(st.pane(1)));
                else if (r <= 2.3) c.set(x, y, -11, St.pane(st.pane((x + y) % 2 == 0 ? 0 : 2)));
                else if (r <= 3.3) c.set(x, y, -11, "minecraft:chiseled_quartz_block");
            }
        }
        Roofs.Style cap = new Roofs.Style(st.roof, st.roofTrim, st.wall, 0, Roofs.Pitch.STEEP, 0, false, null, false);
        for (int[] k : new int[][]{{HALL.x0(), HALL.z0()}, {HALL.x1(), HALL.z0()}, {HALL.x0(), HALL.z1()}, {HALL.x1(), HALL.z1()}}) {
            Box t = new Box(k[0] - 1, -2, k[1] - 1, k[0] + 1, 14, k[1] + 1);
            c.fill(t, st.wall);
            for (int y = 2; y <= 14; y += 4) for (Walls.Run r : Walls.around(t)) c.set(r.x(1), y, r.z(1), "minecraft:chiseled_quartz_block");
            Roofs.hip(c, t.withY(0, 14), 14, cap);
            int topY = c.topAt(k[0], k[1], DungeonAnchors.HEIGHT, 14);
            c.set(k[0], topY + 1, k[1], St.endRod(Dir.UP));
        }
        int ridge = c.topAt(0, -20, DungeonAnchors.HEIGHT, hallTop);
        c.set(0, ridge + 1, -20, "minecraft:gold_block");
        c.set(0, ridge + 2, -20, St.rod("minecraft:lightning_rod", Dir.UP));
    }

    // --- the vault ---------------------------------------------------------------------------------------------------------

    private void vault() {
        Box shell = VAULT.grow(1);
        c.fill(shell, st.vault);
        for (int x = VAULT.x0(); x <= VAULT.x1(); x++) for (int z = VAULT.z0(); z <= VAULT.z1(); z++) c.set(x, shell.y0(), z, st.floor.at(x, shell.y0(), z));
        c.carve(VAULT);
        // The stair down from the hall (east of the dais), its railing round the opening in the hall's floor.
        Furniture.flight(c, 9, -10, -25, Dir.SOUTH, 10, 2, st.trim.stairs(), true);
        for (int z = -20; z <= -17; z++) {
            c.set(7, 0, z, St.wall(st.post));
            c.set(10, 0, z, St.wall(st.post));
        }
        c.set(8, 0, -20, St.wall(st.post));
        c.set(9, 0, -20, St.wall(st.post));
        int y0 = VAULT.y0();
        // Pillars, and light: lava behind bars (castle), lamps in the vault's ceiling (Monastery).
        for (int x : new int[]{-5, 5}) {
            for (int y = y0; y <= VAULT.y1(); y++) c.set(x, y, -29, pit ? "minecraft:polished_basalt[axis=y]" : "minecraft:quartz_pillar[axis=y]");
        }
        if (pit) {
            for (int z : new int[]{-31, -28}) {
                for (int side : new int[]{-1, 1}) {
                    c.set(side * 11, y0 + 1, z, "minecraft:lava");
                    c.set(side * 10, y0 + 1, z, St.bars());
                }
            }
            for (int[] g : new int[][]{{-9, -33}, {9, -33}, {-10, -32}}) {
                c.set(g[0], y0, g[1], "minecraft:gold_block");
                c.set(g[0], y0 + 1, g[1], "minecraft:raw_gold_block");
            }
            for (int[] ch : new int[][]{{-7, -27}, {7, -30}, {0, -27}}) {
                c.set(ch[0], VAULT.y1(), ch[1], St.chain("y"));
                c.set(ch[0], VAULT.y1() - 1, ch[1], St.soulLantern(true));
            }
        } else {
            for (int x = -8; x <= 8; x += 4) for (int z : new int[]{-31, -28}) c.set(x, VAULT.y1() + 1, z, "minecraft:sea_lantern");
            c.set(0, y0, -30, "minecraft:chiseled_quartz_block");
            c.set(0, y0 + 1, -30, "minecraft:gold_block");
            c.set(0, y0 + 2, -30, St.candle("white", 4, true));
            for (int x : new int[]{-9, 9}) {
                c.set(x, y0, -33, "minecraft:quartz_pillar[axis=y]");
                c.set(x, y0 + 1, -33, St.candle("yellow", 3, true));
            }
        }
        for (int x = -8; x <= 8; x += 4) for (int z : new int[]{-30, -27}) if (Math.abs(x) != 4 || z != -30) c.set(x, y0 - 1, z, st.light);
        int n = 2 + rnd.nextInt(3);
        int[][] spots = n == 2 ? new int[][]{{-2, -32}, {2, -32}} : n == 3 ? new int[][]{{-4, -32}, {0, -32}, {4, -32}}
                : new int[][]{{-7, -32}, {-2, -32}, {2, -32}, {7, -32}};
        for (int i = 0; i < spots.length; i++) c.anchor(DungeonAnchors.vault(i), spots[i][0], y0, spots[i][1]);
        DungeonKit.guard(c, "vault", 0, -3, y0, -27);
        DungeonKit.guard(c, "vault", 1, 3, y0, -27);
    }

    // --- the castle's courtyard ----------------------------------------------------------------------------------------------

    private void courtyard() {
        // Paths: gate to hall round the Pit, across to the south wings, up the alleys to the north wings.
        for (int x = -17; x <= 17; x++) {
            for (int z = -10; z <= 15; z++) {
                double r = Math.hypot(x, z + 3);
                boolean path = Math.abs(x) <= 2 && (z >= 4 || z <= -9) || r >= 5 && r <= 7 && Math.abs(x) <= 7 || z >= 4 && z <= 6;
                if (path) c.set(x, -1, z, st.path.at(x, -1, z));
            }
        }
        for (int side : new int[]{-1, 1}) {
            for (int z = -16; z <= -9; z++) for (int x = 15; x <= 17; x++) c.set(side * x, -1, z, st.path.at(side * x, -1, z));
        }
        // The Pit: a well of lava under a railing of black stone.
        for (int x = -5; x <= 5; x++) {
            for (int z = -8; z <= 2; z++) {
                double r = Math.hypot(x, z + 3);
                if (r > 4.4) continue;
                for (int y = -6; y <= -1; y++) c.set(x, y, z, st.rubble.at(x, y, z));
                if (r <= 3.3) {
                    c.set(x, -5, z, "minecraft:lava");
                    for (int y = -4; y <= -1; y++) c.carve(x, y, z);
                    if (r > 2.3) c.set(x, -1, z, "minecraft:magma_block");
                } else {
                    c.set(x, 0, z, Noise.chance(x, 0, z, s + 90, 0.12) ? St.AIR : St.wall(st.post));
                }
            }
        }
        for (int[] l : new int[][]{{-4, -3}, {4, -3}, {0, 1}}) {
            c.set(l[0], 0, l[1], St.wall(st.post));
            c.set(l[0], 1, l[1], st.lantern(false));
        }
        // Gibbets by the walls, braziers by the paths, spikes and bones.
        gibbet(-13, 12, Dir.EAST);
        gibbet(13, -6, Dir.WEST);
        for (int[] b : new int[][]{{-4, 12}, {4, 12}, {-4, -9}, {4, -9}, {-12, 3}, {12, 7}}) DungeonKit.brazier(c, b[0], 0, b[1], st.post, b[1] < 0);
        for (int i = 0; i < 7; i++) {
            int x = (rnd.nextBoolean() ? 1 : -1) * (9 + rnd.nextInt(6)), z = -8 + rnd.nextInt(22);
            if (z >= 3 && z <= 7) continue;
            if (!c.isAir(x, 0, z) || !c.isAir(x, 1, z)) continue;
            c.set(x, 0, z, "minecraft:basalt");
            c.set(x, 1, z, St.dripstone(Dir.UP, "tip"));
        }
        for (int i = 0; i < 6; i++) {
            int x = -16 + rnd.nextInt(33), z = -9 + rnd.nextInt(24);
            if (Math.abs(x) <= 3 || z >= 3 && z <= 7 || Math.hypot(x, z + 3) < 8) continue;
            if (c.isAir(x, 0, z)) c.set(x, 0, z, rnd.nextBoolean() ? "minecraft:bone_block[axis=y]" : st.rubble.at(x, 0, z));
        }
        int[][] guards = {{-8, 10}, {8, 10}, {-11, -2}, {11, -2}, {-7, -9}, {7, -9}};
        for (int i = 0; i < guards.length; i++) DungeonKit.guard(c, "courtyard", i, guards[i][0], 0, guards[i][1]);
        for (int[] g : guards) {
            c.carve(g[0], 0, g[1]);
            c.carve(g[0], 1, g[1]);
        }
        DungeonKit.guard(c, "gatehouse", 0, -1, 0, 18);
        DungeonKit.guard(c, "gatehouse", 1, 1, 0, 18);
        DungeonKit.guard(c, "gatehouse", 2, 0, 0, 21);
    }

    /** A gibbet: a crimson post with an arm toward {@code arm} and a cage hung from it. */
    private void gibbet(int x, int z, Dir arm) {
        for (int y = 0; y <= 6; y++) c.set(x, y, z, st.wood.log("y"));
        for (int k = 1; k <= 2; k++) c.set(x + arm.dx * k, 6, z + arm.dz * k, st.wood.log(arm.axis()));
        int cx = x + arm.dx * 2, cz = z + arm.dz * 2;
        DungeonKit.cage(c, cx + arm.dx, 4, cz + arm.dz, 2, Family.POLISHED_BLACKSTONE.slab(), 0);
        c.set(cx + arm.dx, 6, cz + arm.dz, st.wood.log(arm.axis()));
        c.set(cx + arm.dx, 5, cz + arm.dz, St.chain("y"));
    }

    // --- the Monastery's cloister ----------------------------------------------------------------------------------------------

    private void cloister() {
        Box outer = new Box(-15, 0, -9, 15, 0, 13), garth = new Box(-11, 0, -5, 11, 0, 9);
        // Lawn and paths outside the cloister: to the hall, the wings and the gate.
        for (int x = -17; x <= 17; x++) {
            for (int z = -10; z <= 15; z++) {
                boolean path = Math.abs(x) <= 1 && (z == -10 || z >= 14) || Math.abs(x) >= 16 && (z >= 4 && z <= 6);
                if (path) c.set(x, -1, z, st.path.at(x, -1, z));
            }
        }
        for (int side : new int[]{-1, 1}) {
            for (int z = -16; z <= -10; z++) for (int x = 15; x <= 17; x++) c.set(side * x, -1, z, st.path.at(side * x, -1, z));
        }
        // The walks: a stone floor, an arcade on both edges, a flat deck with a copper cap.
        for (int x = outer.x0(); x <= outer.x1(); x++) {
            for (int z = outer.z0(); z <= outer.z1(); z++) {
                if (garth.containsColumn(x, z)) continue;
                c.set(x, -1, z, st.floor.at(x, -1, z));
                for (int y = 0; y <= 3; y++) c.carve(x, y, z);
                c.set(x, 4, z, st.ceiling.at(x, 4, z));
                boolean edge = x == outer.x0() || x == outer.x1() || z == outer.z0() || z == outer.z1()
                        || x == garth.x0() - 1 || x == garth.x1() + 1 || z == garth.z0() - 1 || z == garth.z1() + 1;
                if (edge) {
                    Dir in = inwardOfWalk(x, z, outer, garth);
                    c.set(x, 5, z, in == null ? st.roof.slabBottom() : st.roof.stairs(in, false));
                } else {
                    c.set(x, 5, z, st.roof.block());
                    c.set(x, 6, z, st.roof.slabBottom());
                }
            }
        }
        Palette.swapFamily(c, new Box(-16, 5, -10, 16, 6, 14), st.roof, st.roofAlt, 0.3, 2.5, s + 91);
        arcade(new Box(-12, 0, -6, 12, 0, 10), true);
        arcade(outer, false);
        for (int x = outer.x0() + 2; x <= outer.x1() - 2; x += 4) {
            Lighting.hanging(c, x, 4, -7, 1, false);
            Lighting.hanging(c, x, 4, 11, 1, false);
        }
        for (int z = outer.z0() + 4; z <= outer.z1() - 4; z += 4) {
            Lighting.hanging(c, -13, 4, z, 1, false);
            Lighting.hanging(c, 13, 4, z, 1, false);
        }
        // The garth: cross walks, a fountain in the middle, flower beds, azaleas and two birches.
        for (int x = garth.x0(); x <= garth.x1(); x++) {
            for (int z = garth.z0(); z <= garth.z1(); z++) {
                boolean walk = Math.abs(x) <= 1 || z >= 1 && z <= 3;
                c.set(x, -1, z, walk ? st.path.at(x, -1, z) : "minecraft:grass_block");
                if (walk || Math.abs(x) <= 3 && z >= -1 && z <= 5) continue;
                double n = Noise.hash01(x, 0, z, s + 92);
                if (n < 0.07) c.set(x, 0, z, "minecraft:flowering_azalea");
                else if (n < 0.12) c.set(x, 0, z, "minecraft:azalea");
                else if (n < 0.34) {
                    String[] f = {"minecraft:lily_of_the_valley", "minecraft:oxeye_daisy", "minecraft:azure_bluet", "minecraft:cornflower",
                            "minecraft:dandelion", "minecraft:white_tulip"};
                    c.set(x, 0, z, f[Noise.pick(x, 1, z, s + 93, f.length)]);
                }
            }
        }
        fountain(0, 2);
        int tree = rnd.nextInt(2);
        Trees.birch(c, tree == 0 ? -7 : 7, 0, -2, 0, s + 94);
        Trees.birch(c, tree == 0 ? 7 : -7, 0, 7, 0, s + 95);
        int[][] guards = {{-13, -7}, {13, -7}, {-13, 11}, {13, 11}, {-5, 2}, {5, 2}};
        for (int i = 0; i < guards.length; i++) {
            c.carve(guards[i][0], 0, guards[i][1]);
            c.carve(guards[i][0], 1, guards[i][1]);
            DungeonKit.guard(c, "cloister", i, guards[i][0], 0, guards[i][1]);
        }
        DungeonKit.guard(c, "gatehouse", 0, -1, 0, 18);
        DungeonKit.guard(c, "gatehouse", 1, 1, 0, 18);
        DungeonKit.guard(c, "gatehouse", 2, 0, 0, 21);
    }

    /** The direction from a walk-edge cell toward the walk's middle (where the cap's stairs climb), or null at a corner. */
    private static Dir inwardOfWalk(int x, int z, Box outer, Box garth) {
        boolean westWalk = x < garth.x0(), eastWalk = x > garth.x1(), northWalk = z < garth.z0(), southWalk = z > garth.z1();
        if ((westWalk || eastWalk) && (northWalk || southWalk)) return null;
        if (westWalk) return x == outer.x0() ? Dir.EAST : Dir.WEST;
        if (eastWalk) return x == outer.x1() ? Dir.WEST : Dir.EAST;
        if (northWalk) return z == outer.z0() ? Dir.SOUTH : Dir.NORTH;
        return z == outer.z1() ? Dir.NORTH : Dir.SOUTH;
    }

    /** Pillars on an outline every four cells (and its corners), arches between them; a dwarf wall on the garth's side. */
    private void arcade(Box line, boolean dwarf) {
        for (Walls.Run r : Walls.around(line)) {
            for (int i = 0; i < r.length(); i++) {
                int x = r.x(i), z = r.z(i);
                int along = r.out().dx != 0 ? z : x;
                boolean pillar = i == 0 || i == r.length() - 1 || Math.floorMod(along - (r.out().dx != 0 ? 2 : 0), 4) == 2;
                if (pillar) {
                    c.set(x, 0, z, st.trim.block());
                    for (int y = 1; y <= 2; y++) c.set(x, y, z, "minecraft:quartz_pillar[axis=y]");
                    c.set(x, 3, z, "minecraft:chiseled_quartz_block");
                } else {
                    boolean axis = r.out().dx != 0 ? z >= 1 && z <= 3 : Math.abs(x) <= 1;
                    c.set(x, 3, z, Family.SMOOTH_QUARTZ.slabTop());
                    if (dwarf && !axis) c.set(x, 0, z, St.wall(st.post));
                }
            }
        }
    }

    private void fountain(int cx, int cz) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                int x = cx + dx, z = cz + dz;
                boolean rim = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                c.set(x, -2, z, "minecraft:quartz_bricks");
                if (rim) {
                    c.set(x, -1, z, "minecraft:quartz_bricks");
                    c.set(x, 0, z, Family.SMOOTH_QUARTZ.slabBottom());
                } else if (dx == 0 && dz == 0) {
                    c.set(x, -1, z, "minecraft:quartz_pillar[axis=y]");
                    c.set(x, 0, z, "minecraft:quartz_pillar[axis=y]");
                    c.set(x, 1, z, "minecraft:chiseled_quartz_block");
                    c.set(x, 2, z, St.endRod(Dir.UP));
                } else {
                    c.set(x, -1, z, St.water());
                    c.carve(x, 0, z);
                }
            }
        }
    }

    // --- the curtain wall and its towers ---------------------------------------------------------------------------------------

    private void curtain() {
        int top = curtainTop;
        for (Box ring : new Box[]{OUT, IN}) {
            for (Walls.Run r : Walls.around(ring)) {
                for (int i = 0; i < r.length(); i++) {
                    int x = r.x(i), z = r.z(i);
                    for (int y = -4; y <= -1; y++) c.set(x, y, z, st.plinth.at(x, y, z));
                    for (int y = 0; y <= top; y++) c.set(x, y, z, (y == 0 ? st.plinth : st.wall).at(x, y, z));
                }
            }
        }
        Box out = OUT.withY(0, top);
        for (Walls.Run r : Walls.around(out)) {
            int[] at = Walls.pillarPositions(r, 8);
            for (int k = 1; k < at.length - 1; k++) {
                int i = at[k];
                if (Math.abs(r.x(i)) <= 8 && r.out() == Dir.SOUTH) continue;
                DungeonKit.buttress(c, r, i, pit ? -3 : 0, top - 3, st.plinth, st.trim);
            }
        }
        if (pit) {
            DungeonKit.battlements(c, out, top + 1, st.wall, st.accent, s + 100, 0.12);
            for (Walls.Run r : Walls.around(out)) {
                Walls.Run o = r.offset(1);
                for (int i = -1; i <= r.length(); i++) if (c.isAir(o.x(i), top + 2, o.z(i)) && !c.isAir(o.x(i), top + 1, o.z(i))) c.set(o.x(i), top + 2, o.z(i), st.wall.at(o.x(i), top + 2, o.z(i)));
            }
            // A breach: one stretch of the wall-walk thrown down, its stones in a heap at the foot.
            Walls.Run r = Walls.around(out).get(rnd.nextBoolean() ? 1 : 3);
            int from = 10 + rnd.nextInt(r.length() - 24);
            for (int i = from; i < from + 4; i++) {
                for (Walls.Run rr : new Walls.Run[]{r, r.offset(-1), r.offset(1)}) {
                    for (int y = top - 1 - (i == from || i == from + 3 ? 0 : 1); y <= top + 2; y++) c.carve(rr.x(i), y, rr.z(i));
                }
            }
        } else {
            for (Walls.Run r : Walls.around(out)) Walls.belt(c, r, top, st.trim);
            for (Box ring : new Box[]{OUT, IN}) {
                for (Walls.Run r : Walls.around(ring)) for (int i = 0; i < r.length(); i++) c.set(r.x(i), top + 1, r.z(i), st.trim.slabBottom());
            }
        }
        // The corner towers.
        int ruined = rnd.nextInt(4), spire = rnd.nextInt(4);
        int[][] corners = {{-29, -29}, {29, -29}, {-29, 21}, {29, 21}};
        for (int i = 0; i < 4; i++) tower(corners[i][0], corners[i][1], i == ruined && pit, i == spire || !pit);
    }

    private void tower(int cx, int cz, boolean ruined, boolean spire) {
        int top = pit ? 15 : 8;
        Box t = new Box(cx - 3, -4, cz - 3, cx + 3, top, cz + 3);
        c.fill(t, st.wall);
        for (Walls.Run r : Walls.around(t)) {
            Walls.body(c, r, -4, 0, st.plinth);
            Walls.belt(c, r, pit ? 8 : 4, st.trim);
            for (int y = pit ? 3 : 2; y <= top - 3; y += pit ? 6 : 4) {
                c.set(r.x(3), y, r.z(3), pit ? St.bars() : St.pane(st.pane(2)));
                c.set(r.x(3), y + 1, r.z(3), pit ? St.bars() : St.pane(st.pane(2)));
            }
        }
        Box outline = t.withY(0, top);
        if (pit && !spire) {
            DungeonKit.battlements(c, outline, top + 1, st.wall, st.accent, s + 110 + cx, ruined ? 0.45 : 0.1);
            if (ruined) {
                // The crown thrown down on one side: a jagged slope of broken masonry, nothing left hanging.
                for (int x = t.x0() - 1; x <= t.x1() + 1; x++) {
                    for (int z = t.z0() - 1; z <= t.z1() + 1; z++) {
                        double lean = ((x - t.x0()) + (z - t.z0())) / 14.0;
                        int drop = 1 + (int) Math.round(lean * 6 + Noise.value2(x, z, 2, s + 111) * 2);
                        for (int y = top + 1 - drop; y <= top + 3; y++) c.carve(x, y, z);
                    }
                }
            }
        } else {
            Roofs.Style style = pit ? new Roofs.Style(st.roofAlt, st.roofTrim, st.wall, 1, Roofs.Pitch.STEEP, 0, false, null, false)
                    : st.roofStyle(false);
            if (pit) for (Walls.Run r : Walls.around(outline)) Walls.belt(c, r, top - 1, st.trim);
            Roofs.hip(c, outline, top, style);
            int peak = c.topAt(cx, cz, DungeonAnchors.HEIGHT, top);
            c.set(cx, peak + 1, cz, pit ? St.wall(st.post) : St.endRod(Dir.UP));
            if (pit) c.set(cx, peak + 2, cz, St.rod("minecraft:lightning_rod", Dir.UP));
        }
    }

    // --- the gatehouse -----------------------------------------------------------------------------------------------------

    private void gatehouse() {
        int top = pit ? 12 : 7;
        Box body = new Box(GATE.x0(), -4, GATE.z0(), GATE.x1(), top, GATE.z1());
        c.fill(body, st.wall);
        for (Walls.Run r : Walls.around(body)) Walls.body(c, r, -4, 0, st.plinth);
        Box outline = GATE.withY(0, top);
        if (pit) {
            DungeonKit.battlements(c, outline, top + 1, st.wall, st.accent, s + 120, 0.05);
            for (int sx : new int[]{-1, 1}) {
                Box turret = new Box(sx < 0 ? -7 : 3, -4, 20, sx < 0 ? -3 : 7, 16, 25);
                c.fill(turret, st.wall);
                for (Walls.Run r : Walls.around(turret)) Walls.body(c, r, -4, 0, st.plinth);
                DungeonKit.battlements(c, turret.withY(0, 16), 17, st.wall, st.accent, s + 121 + sx, 0.08);
                for (int y = 4; y <= 12; y += 4) {
                    c.set(sx * 5, y, 25, St.bars());
                    c.set(sx * 5, y + 1, 25, St.bars());
                }
            }
        } else {
            Roofs.gable(c, outline, top, true, st.roofStyle(false));
            belfry();
        }
        // The passage, arched at both ends, a raised portcullis (castle) and a stone floor.
        for (int x = -1; x <= 1; x++) {
            for (int z = GATE.z0(); z <= GATE.z1(); z++) {
                c.set(x, -1, z, st.path.at(x, -1, z));
                for (int y = 0; y <= 3; y++) c.carve(x, y, z);
            }
        }
        for (int z : new int[]{GATE.z0(), GATE.z1()}) {
            c.set(-1, 3, z, st.trim.stairs(Dir.WEST, true));
            c.set(1, 3, z, st.trim.stairs(Dir.EAST, true));
        }
        if (pit) {
            for (int x = -1; x <= 1; x++) c.set(x, 3, 22, St.bars());
            // The drawbridge: crimson planks over the moat, chains for rails.
            for (int z = 25; z <= 27; z++) {
                for (int x = -2; x <= 2; x++) c.set(x, -1, z, Math.abs(x) == 2 ? st.wood.planks() : st.wood.slabTop());
                for (int x : new int[]{-2, 2}) c.set(x, 0, z, St.chain("z"));
            }
            c.set(-2, -1, 26, st.wood.log("z"));
            c.set(2, -1, 26, st.wood.log("z"));
            // A gibbet beam out of the gate's face over the bridge, a cage hung from it.
            for (int z = 25; z <= 26; z++) c.set(0, 10, z, st.wood.log("z"));
            DungeonKit.cage(c, 0, 7, 26, 2, Family.POLISHED_BLACKSTONE.slab(), 2);
        }
        for (int x : new int[]{-1, 1}) Lighting.hanging(c, x, 4, GATE.centerZ(), 1, pit);
        c.set(0, 4, GATE.z1(), st.accent.block());
    }

    /** The Monastery's bell tower over the gate: a belfry open on four sides, a copper spire and a gilded finial. */
    private void belfry() {
        int top = 26, belfry = 21;
        Box tower = new Box(-3, 0, 17, 3, top, 23);
        c.fill(tower, st.wall);
        for (Walls.Run r : Walls.around(tower)) {
            Walls.belt(c, r, 8, st.trim);
            Walls.belt(c, r, belfry, st.trim);
            Walls.belt(c, r, top, st.trim);
            for (int y = belfry + 1; y <= top - 1; y++) for (int i = 2; i <= 4; i++) c.carve(r.x(i), y, r.z(i));
            c.set(r.x(2), top - 1, r.z(2), st.trim.stairs(r.along().opposite(), true));
            c.set(r.x(4), top - 1, r.z(4), st.trim.stairs(r.along(), true));
            for (int i = 2; i <= 4; i++) c.set(r.x(i), belfry + 1, r.z(i), St.wall(st.post));
            for (int y = 11; y <= 17; y++) if (r.out() != Dir.NORTH) c.set(r.x(3), y, r.z(3), y == 14 ? "minecraft:chiseled_quartz_block" : St.pane(st.pane(y % 2)));
        }
        c.carve(new Box(-2, belfry + 1, 18, 2, top - 1, 22));
        c.set(0, top - 1, 20, St.chain("y"));
        c.set(0, top - 2, 20, "minecraft:gold_block");
        c.set(0, top - 3, 20, St.rod("minecraft:lightning_rod", Dir.DOWN));
        for (int[] l : new int[][]{{-2, 18}, {2, 22}}) c.set(l[0], belfry + 1, l[1], St.lantern(false));
        Roofs.hip(c, tower.withY(0, top), top, st.roofStyle(false));
        int peak = c.topAt(0, 20, DungeonAnchors.HEIGHT, top);
        c.set(0, peak + 1, 20, "minecraft:gold_block");
        c.set(0, peak + 2, 20, St.rod("minecraft:lightning_rod", Dir.UP));
    }
}
