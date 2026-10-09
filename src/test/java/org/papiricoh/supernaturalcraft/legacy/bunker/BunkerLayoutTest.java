package org.papiricoh.supernaturalcraft.legacy.bunker;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.legacy.bunker.plan.Decor;
import org.papiricoh.supernaturalcraft.legacy.bunker.plan.Kinds;
import org.papiricoh.supernaturalcraft.legacy.bunker.plan.Kit;
import org.papiricoh.supernaturalcraft.legacy.bunker.plan.Plan;
import org.papiricoh.supernaturalcraft.legacy.bunker.plan.PlanLight;
import org.papiricoh.supernaturalcraft.legacy.bunker.plan.PlanShapes;
import org.papiricoh.supernaturalcraft.legacy.bunker.plan.St;
import org.papiricoh.supernaturalcraft.legacy.bunker.plan.Zones;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The bunker's plan: inside its box, the order's furniture in place, every stair, bar and wall shaped as vanilla would shape it,
 * every flight with headroom, nothing hung on air, no dark corner a monster could spawn in, every room reachable on foot from
 * the door, the decoration hung on walls, and the palette as wide as a builder's.
 */
class BunkerLayoutTest {

    private static final Map<Long, String> PLAN = new HashMap<>();
    /** Distinct vanilla blocks the finished bunker must use (raised as the parts fill in: 120 when all are done). */
    private static final int MIN_VANILLA_BLOCKS = 60;

    static {
        for (BunkerLayout.Cell c : BunkerLayout.cells()) PLAN.put(Plan.key(c.x(), c.y(), c.z()), c.state());
    }

    private static String at(int x, int y, int z) {
        return PLAN.get(Plan.key(x, y, z));
    }

    private static String at(int[] p) {
        String s = at(p[0], p[1], p[2]);
        return s == null ? "(none)" : s;
    }

    /** As the world sees it: off the plan, rock below the ground and air above. */
    private static String world(int x, int y, int z) {
        String s = at(x, y, z);
        return s != null ? s : y > Zones.GROUND ? St.AIR : "minecraft:stone";
    }

    @Test
    void everyBlockFitsTheBoxOnce() {
        Set<Long> seen = new HashSet<>();
        int minZ = 0, minX = 0, maxX = 0;
        for (BunkerLayout.Cell c : BunkerLayout.cells()) {
            assertTrue(c.x() >= BunkerLayout.MIN_X && c.x() <= BunkerLayout.MAX_X, c + " outside in X");
            assertTrue(c.z() >= BunkerLayout.MIN_Z && c.z() <= BunkerLayout.MAX_Z, c + " outside in Z");
            assertTrue(c.y() >= BunkerLayout.MIN_Y && c.y() <= BunkerLayout.MAX_Y, c + " outside in Y");
            assertTrue(seen.add(Plan.key(c.x(), c.y(), c.z())), "two blocks at " + c);
            minZ = Math.min(minZ, c.z());
            minX = Math.min(minX, c.x());
            maxX = Math.max(maxX, c.x());
        }
        assertTrue(Math.abs(BunkerLayout.MIN_X) <= 100 && BunkerLayout.MAX_Z <= 100 && Math.abs(BunkerLayout.MIN_Z) <= 100,
                "within 100 blocks of the start (structure references reach 8 chunks)");
        assertTrue(maxX - minX + 1 >= 60, "a big bunker: " + (maxX - minX + 1) + " wide");
    }

    @Test
    void theOrdersFurnitureIsInPlace() {
        assertTrue(at(BunkerLayout.DOOR).startsWith("supernaturalcraft:bunker_door[") && at(BunkerLayout.DOOR).contains("half=lower"), "the door");
        int[] up = {BunkerLayout.DOOR[0], BunkerLayout.DOOR[1] + 1, BunkerLayout.DOOR[2]};
        assertTrue(at(up).contains("half=upper"), "the door's upper half");
        for (int[] t : BunkerLayout.MAP_TABLES) assertEquals("supernaturalcraft:map_table", at(t));
        assertEquals("supernaturalcraft:map_table", at(BunkerLayout.MAP_TABLE));
        assertEquals(4, BunkerLayout.DESKS.length);
        for (int[] d : BunkerLayout.DESKS) assertEquals("supernaturalcraft:research_desk", at(d));
        assertTrue(count("supernaturalcraft:archive_shelf") >= 60, "the archive's shelves");
        assertEquals("supernaturalcraft:men_of_letters_emblem", at(BunkerLayout.EMBLEM_AT), "the emblem in the war room floor");
        assertEquals("minecraft:air", at(BunkerLayout.HENRY), "room for Henry");
        assertEquals("minecraft:air", at(new int[]{BunkerLayout.HENRY[0], BunkerLayout.HENRY[1] + 1, BunkerLayout.HENRY[2]}), "headroom for Henry");
        assertTrue(Kinds.sturdy(at(BunkerLayout.HENRY[0], BunkerLayout.HENRY[1] - 1, BunkerLayout.HENRY[2]), "up"), "floor under Henry");
        assertEquals("minecraft:air", at(BunkerLayout.TRAP), "the dungeon's trap is painted on bare floor");
        assertTrue(Kinds.sturdy(at(BunkerLayout.TRAP[0], BunkerLayout.TRAP[1] - 1, BunkerLayout.TRAP[2]), "up"), "floor under the trap");
    }

    @Test
    void onlyTheOrdersBlocksAndVanillaOnes() {
        Set<String> mod = Set.of(BunkerLayout.DOOR_ID, BunkerLayout.DESK, BunkerLayout.TABLE, BunkerLayout.SHELF, BunkerLayout.EMBLEM);
        for (String s : BunkerLayout.palette()) {
            String id = BunkerLayout.blockId(s);
            assertTrue(id.startsWith("minecraft:") || mod.contains(id), "unexpected block " + id);
        }
    }

    @Test
    void aBuildersPalette() {
        Set<String> ids = new TreeSet<>();
        for (String s : BunkerLayout.palette()) if (s.startsWith("minecraft:") && !Kinds.air(s)) ids.add(BunkerLayout.blockId(s));
        assertTrue(ids.size() >= MIN_VANILLA_BLOCKS, "only " + ids.size() + " vanilla blocks: " + ids);
    }

    // --- shapes --------------------------------------------------------------------------------------------------------------

    @Test
    void stairsTakeTheShapeVanillaGivesThem() {
        List<String> wrong = new ArrayList<>();
        for (Map.Entry<Long, String> e : PLAN.entrySet()) {
            String s = e.getValue();
            if (!Kinds.stairs(s)) continue;
            long k = e.getKey();
            assertTrue(St.get(s, "facing") != null && St.get(s, "half") != null, "stairs without facing or half: " + s);
            String shape = PlanShapes.stairShape(PLAN, s, Plan.kx(k), Plan.ky(k), Plan.kz(k));
            if (!shape.equals(St.get(s, "shape"))) wrong.add(Plan.kx(k) + "," + Plan.ky(k) + "," + Plan.kz(k) + " " + s + " → " + shape);
        }
        assertTrue(wrong.isEmpty(), wrong.size() + " stairs mis-shaped: " + wrong.subList(0, Math.min(10, wrong.size())));
    }

    @Test
    void theVanillaStairRule() {
        // A stair facing north with one facing east in front of it turns an outer corner; one behind, an inner corner.
        Map<Long, String> m = new HashMap<>();
        m.put(Plan.key(0, 0, 0), St.stairs("minecraft:oak_stairs", "north", false));
        m.put(Plan.key(0, 0, -1), St.stairs("minecraft:oak_stairs", "east", false));
        assertEquals("outer_right", PlanShapes.stairShape(m, m.get(Plan.key(0, 0, 0)), 0, 0, 0));
        m.put(Plan.key(0, 0, -1), St.stairs("minecraft:oak_stairs", "west", false));
        assertEquals("outer_left", PlanShapes.stairShape(m, m.get(Plan.key(0, 0, 0)), 0, 0, 0));
        m.remove(Plan.key(0, 0, -1));
        m.put(Plan.key(0, 0, 1), St.stairs("minecraft:oak_stairs", "west", false));
        assertEquals("inner_left", PlanShapes.stairShape(m, m.get(Plan.key(0, 0, 0)), 0, 0, 0));
        m.put(Plan.key(0, 0, 1), St.stairs("minecraft:oak_stairs", "west", true));
        assertEquals("straight", PlanShapes.stairShape(m, m.get(Plan.key(0, 0, 0)), 0, 0, 0), "a different half never joins");
        // A cornice run round a room turns every corner inward.
        Plan p = new Plan();
        Kit.Room r = new Kit.Room(0, 4, 0, 3, 0, 3);
        Kit.room(p, r, new Kit.Style(Kit.solid("minecraft:stone"), Kit.solid("minecraft:stone"), Kit.solid("minecraft:stone"),
                "minecraft:oak_stairs", null, 0, null, 0));
        PlanShapes.apply(p.cells());
        for (int[] c : new int[][]{{0, 0}, {4, 0}, {0, 3}, {4, 3}}) {
            String s = p.get(c[0], r.top(), c[1]);
            assertTrue(St.get(s, "shape").startsWith("inner"), "cornice corner " + c[0] + "," + c[1] + ": " + s);
        }
    }

    @Test
    void barsAndRailingsJoinSomething() {
        List<String> alone = new ArrayList<>();
        for (Map.Entry<Long, String> e : PLAN.entrySet()) {
            String s = e.getValue();
            if (!Kinds.barsLike(s) && !Kinds.fence(s)) continue;
            boolean any = false;
            for (String d : St.HORIZONTAL) any |= "true".equals(St.get(s, d));
            long k = e.getKey();
            if (!any) alone.add(Plan.kx(k) + "," + Plan.ky(k) + "," + Plan.kz(k) + " " + St.path(s));
        }
        assertTrue(alone.size() <= 4, "bars standing alone (a post, not a railing): " + alone);
    }

    // --- walking ---------------------------------------------------------------------------------------------------------------

    @Test
    void everyFlightHasHeadroom() {
        List<String> low = new ArrayList<>();
        for (Map.Entry<Long, String> e : PLAN.entrySet()) {
            String s = e.getValue();
            if (!Kinds.stairs(s) || !"bottom".equals(St.get(s, "half"))) continue;
            long k = e.getKey();
            int x = Plan.kx(k), y = Plan.ky(k), z = Plan.kz(k);
            String down = St.opposite(St.get(s, "facing"));
            String next = at(x + St.dx(down), y - 1, z + St.dz(down));
            if (next == null || !Kinds.stairs(next) || !St.get(next, "facing").equals(St.get(s, "facing"))) continue;
            if (St.path(world(x, y + 1, z)).equals("chain")) continue; // a balustrade lane: its balusters stand on it
            for (int h = 1; h <= 3; h++) {
                if (!passable(world(x, y + h, z))) {
                    low.add(x + "," + y + "," + z + " +" + h + " " + world(x, y + h, z));
                    break;
                }
            }
        }
        assertTrue(low.isEmpty(), "steps without 3 blocks of headroom: " + low.subList(0, Math.min(10, low.size())));
    }

    @Test
    void everyRoomCanBeWalkedToFromTheDoor() {
        Set<Long> reach = walk(BunkerLayout.OUTSIDE);
        assertTrue(reach.contains(key(BunkerLayout.HENRY)), "Henry's place by the map table");
        for (int[] d : BunkerLayout.DESKS) {
            boolean ok = false;
            for (String dir : St.HORIZONTAL) ok |= reach.contains(Plan.key(d[0] + St.dx(dir), d[1], d[2] + St.dz(dir)));
            assertTrue(ok, "desk at " + d[0] + "," + d[2]);
        }
        boolean trap = false;
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) trap |= reach.contains(Plan.key(BunkerLayout.TRAP[0] + dx, BunkerLayout.TRAP[1], BunkerLayout.TRAP[2] + dz));
        assertTrue(trap, "the dungeon's trap");
        assertTrue(BunkerLayout.views().size() >= 8, "a spot in every room: " + BunkerLayout.views().keySet());
        for (Map.Entry<String, Zones.View> v : BunkerLayout.views().entrySet()) {
            Zones.View s = v.getValue();
            assertTrue(reach.contains(Plan.key(s.x(), s.y(), s.z())), "the " + v.getKey() + " (" + s.x() + "," + s.y() + "," + s.z() + ")");
        }
    }

    @Test
    void noDarkCornerForAMonster() {
        PlanLight light = PlanLight.of(PLAN);
        List<String> dark = new ArrayList<>();
        for (Map.Entry<Long, String> e : PLAN.entrySet()) {
            long k = e.getKey();
            int x = Plan.kx(k), y = Plan.ky(k), z = Plan.kz(k);
            if (!Kinds.air(e.getValue()) || !covered(x, y, z)) continue;
            if (!Kinds.air(world(x, y + 1, z)) || !Kinds.sturdy(world(x, y - 1, z), "up")) continue;
            if (light.at(x, y, z) < 1) dark.add(x + "," + y + "," + z);
        }
        assertTrue(dark.isEmpty(), dark.size() + " dark floor cells, e.g. " + dark.subList(0, Math.min(12, dark.size())));
    }

    private static boolean covered(int x, int y, int z) {
        for (int yy = y + 1; yy <= BunkerLayout.MAX_Y; yy++) {
            String s = at(x, yy, z);
            if (s != null && Kinds.opaqueCube(s)) return true;
        }
        return false;
    }

    // --- nothing hangs on air -------------------------------------------------------------------------------------------------

    @Test
    void everythingThatNeedsHoldingIsHeld() {
        List<String> loose = new ArrayList<>();
        for (Map.Entry<Long, String> e : PLAN.entrySet()) {
            String s = e.getValue(), path = St.path(s);
            long k = e.getKey();
            int x = Plan.kx(k), y = Plan.ky(k), z = Plan.kz(k);
            String why = null;
            if (path.endsWith("lantern") && !path.equals("sea_lantern") && !path.equals("jack_o_lantern")) {
                boolean hanging = "true".equals(St.get(s, "hanging"));
                if (hanging ? !Kinds.holdsBelow(world(x, y + 1, z)) : !Kinds.holdsAbove(world(x, y - 1, z))) why = "lantern";
            } else if (path.endsWith("wall_torch") || path.endsWith("_wall_sign") || path.endsWith("_wall_banner")) {
                String f = St.get(s, "facing");
                String behind = world(x - St.dx(f), y, z - St.dz(f));
                if (Kinds.air(behind) || !Kinds.fullCube(behind) && !Kinds.sturdy(behind, f)) why = "nothing behind";
            } else if (path.endsWith("_carpet") || path.endsWith("candle") || path.endsWith("_pressure_plate") || path.equals("flower_pot")
                    || path.startsWith("potted_")) {
                if (!Kinds.holdsAbove(world(x, y - 1, z)) && !Kinds.stairs(world(x, y - 1, z))) why = "nothing under";
            } else if (path.endsWith("_door")) {
                boolean lower = "lower".equals(St.get(s, "half"));
                String other = world(x, lower ? y + 1 : y - 1, z);
                if (!St.id(other).equals(St.id(s)) || !St.get(other, "half").equals(lower ? "upper" : "lower")) why = "half a door";
                if (lower && !Kinds.sturdy(world(x, y - 1, z), "up")) why = "door on air";
            } else if (path.endsWith("_bed")) {
                String f = St.get(s, "facing");
                boolean foot = "foot".equals(St.get(s, "part"));
                int sgn = foot ? 1 : -1;
                String other = world(x + sgn * St.dx(f), y, z + sgn * St.dz(f));
                if (!St.id(other).equals(St.id(s))) why = "half a bed";
            } else if (path.equals("vine")) {
                boolean any = false;
                for (String d : St.HORIZONTAL) if ("true".equals(St.get(s, d))) any |= Kinds.fullCube(world(x + St.dx(d), y, z + St.dz(d)));
                if (!any) why = "vine on nothing";
            }
            if (why != null) loose.add(x + "," + y + "," + z + " " + path + ": " + why);
        }
        assertTrue(loose.isEmpty(), loose.size() + " loose: " + loose.subList(0, Math.min(12, loose.size())));
    }

    @Test
    void theDecorationHangsOnWalls() {
        Set<Long> taken = new HashSet<>();
        List<String> bad = new ArrayList<>();
        for (Decor d : BunkerLayout.decor()) {
            String here = world(d.x(), d.y(), d.z());
            switch (d.kind()) {
                case ITEM_FRAME -> {
                    String back = world(d.x() - St.dx(d.facing()), d.y() - St.dy(d.facing()), d.z() - St.dz(d.facing()));
                    if (!Kinds.air(here) || !Kinds.fullCube(back)) bad.add("frame " + d);
                    if (!taken.add(Plan.key(d.x(), d.y(), d.z()) * 8 + "nesw".indexOf(d.facing().charAt(0)))) bad.add("two frames " + d);
                }
                case PAINTING -> {
                    for (int[] c : d.paintingCells()) {
                        String cell = world(c[0], c[1], c[2]);
                        String back = world(c[0] - St.dx(d.facing()), c[1], c[2] - St.dz(d.facing()));
                        if (!Kinds.air(cell) || !Kinds.fullCube(back)) bad.add("painting " + d + " at " + c[0] + "," + c[1] + "," + c[2]);
                    }
                }
                case ARMOR_STAND -> {
                    if (!Kinds.air(here) || !Kinds.air(world(d.x(), d.y() + 1, d.z()))) bad.add("stand " + d);
                }
                case BANNER -> {
                    if (!St.path(here).endsWith("banner")) bad.add("banner on " + here);
                }
                case SIGN -> {
                    if (!St.path(here).endsWith("sign")) bad.add("sign text on " + here);
                }
                case LOOT -> {
                    String p = St.path(here);
                    if (!(p.equals("barrel") || p.endsWith("chest") || p.endsWith("shulker_box") || p.equals("dispenser") || p.equals("dropper")
                            || p.equals("hopper") || p.equals("decorated_pot"))) {
                        bad.add("loot in " + here);
                    }
                }
                case VAULT -> {
                    if (!St.path(here).equals("vault")) bad.add("vault on " + here);
                }
            }
        }
        assertTrue(bad.isEmpty(), bad.size() + " misplaced: " + bad.subList(0, Math.min(10, bad.size())));
    }

    @Test
    void everyDoorwayBetweenPartsIsOpen() {
        for (Zones.Portal p : Zones.PORTALS) {
            int open = 0;
            Zones.Box b = p.box();
            for (int x = b.x0(); x <= b.x1(); x++) for (int y = b.y0(); y <= b.y1(); y++) for (int z = b.z0(); z <= b.z1(); z++) if (passable(world(x, y, z))) open++;
            assertTrue(open > 0, "the " + p.name() + " doorway is walled up");
        }
    }

    // --- a walker over the plan (off it: open air above the ground, rock below) --------------------------------------------------

    private static long key(int[] p) {
        return Plan.key(p[0], p[1], p[2]);
    }

    private static Set<Long> walk(int[] from) {
        Set<Long> seen = new HashSet<>();
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        queue.add(from);
        seen.add(key(from));
        while (!queue.isEmpty()) {
            int[] p = queue.poll();
            for (String d : St.HORIZONTAL) {
                for (int dy = -1; dy <= 1; dy++) {
                    int[] q = {p[0] + St.dx(d), p[1] + dy, p[2] + St.dz(d)};
                    if (q[0] < BunkerLayout.MIN_X || q[0] > BunkerLayout.MAX_X || q[2] < BunkerLayout.MIN_Z || q[2] > BunkerLayout.MAX_Z
                            || q[1] < BunkerLayout.MIN_Y || q[1] > BunkerLayout.MAX_Y) continue;
                    if (!stand(q)) continue;
                    if (dy == 1 && !passable(world(p[0], p[1] + 2, p[2]))) continue;
                    if (dy == -1 && !passable(world(q[0], q[1] + 2, q[2]))) continue;
                    if (seen.add(key(q))) queue.add(q);
                }
            }
        }
        return seen;
    }

    private static boolean stand(int[] p) {
        return passable(world(p[0], p[1], p[2])) && passable(world(p[0], p[1] + 1, p[2])) && !passable(world(p[0], p[1] - 1, p[2]));
    }

    /** Nothing in the way of a walker: air, doors (the bunker's opens with its key), carpets, torches, signs, banners, plants. */
    private static boolean passable(String s) {
        if (Kinds.air(s)) return true;
        String p = St.path(s);
        return p.endsWith("_door") || p.endsWith("_carpet") || p.endsWith("torch") || p.endsWith("_sign") || p.endsWith("_banner")
                || p.equals("vine") || p.equals("short_grass") || p.endsWith("_button") || p.endsWith("_pressure_plate");
    }

    private static long count(String prefix) {
        return BunkerLayout.cells().stream().filter(c -> c.state().startsWith(prefix)).count();
    }
}
