package org.papiricoh.supernaturalcraft.chain.seat.dungeon;

import org.papiricoh.supernaturalcraft.buildkit.Box;
import org.papiricoh.supernaturalcraft.buildkit.Brush;
import org.papiricoh.supernaturalcraft.buildkit.Canvas;
import org.papiricoh.supernaturalcraft.buildkit.Dir;
import org.papiricoh.supernaturalcraft.buildkit.Family;
import org.papiricoh.supernaturalcraft.buildkit.Kinds;
import org.papiricoh.supernaturalcraft.buildkit.Light;
import org.papiricoh.supernaturalcraft.buildkit.Noise;
import org.papiricoh.supernaturalcraft.buildkit.St;
import org.papiricoh.supernaturalcraft.buildkit.Walls;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Small drawing helpers shared by the two strongholds and their wings (pure). */
final class DungeonKit {

    private DungeonKit() {
    }

    /** The ground and stones that the construction checks read as nature (and that the light check does not count as walls). */
    private static final Set<String> NATURAL = Set.of("minecraft:grass_block", "minecraft:dirt", "minecraft:coarse_dirt", "minecraft:podzol",
            "minecraft:moss_block", "minecraft:rooted_dirt", "minecraft:stone", "minecraft:andesite", "minecraft:calcite",
            "minecraft:diorite", "minecraft:tuff", "minecraft:dripstone_block", "minecraft:sand", "minecraft:gravel", "minecraft:clay",
            "minecraft:mud", "minecraft:dirt_path", "minecraft:farmland", "minecraft:snow_block", "minecraft:packed_mud",
            "minecraft:deepslate", "minecraft:granite");

    static int salt(long seed) {
        return (int) (seed ^ (seed >>> 32));
    }

    static void guard(Canvas c, String room, int n, int x, int y, int z) {
        c.anchor(DungeonAnchors.guard(room, n), x, y, z);
    }

    /** A buttress one cell proud of run cell {@code i}, from {@code y0} to {@code y1}, capped by a slab of {@code cap}. */
    static void buttress(Canvas c, Walls.Run r, int i, int y0, int y1, Brush body, Family cap) {
        int x = r.x(i) + r.out().dx, z = r.z(i) + r.out().dz;
        for (int y = y0; y <= y1; y++) c.set(x, y, z, body.at(x, y, z));
        c.set(x, y1 + 1, z, cap.stairs(r.out().opposite(), false));
        c.setIfAir(x + r.out().dx, y0, z + r.out().dz, cap.stairs(r.out().opposite(), false));
    }

    /**
     * Battlements on a closed outline at row {@code y}: merlons on every other cell (corners always), the gaps carved, and
     * machicolation corbels (upside-down stairs) one cell out a row lower.
     */
    static void battlements(Canvas c, Box outline, int y, Brush merlon, Family corbel, int seed, double broken) {
        for (Walls.Run r : Walls.around(outline)) {
            Walls.belt(c, r, y - 1, corbel);
            Walls.Run o = r.offset(1);
            for (int i = -1; i <= r.length(); i++) {
                int x = o.x(i), z = o.z(i);
                if (Math.floorMod(i, 2) == 0 || i == -1 || i == r.length()) c.set(x, y, z, merlon.at(x, y, z));
                else c.carve(x, y, z);
                if (Noise.chance(x, y, z, seed, broken)) c.carve(x, y, z);
            }
        }
    }

    /** Merlons standing on the outline itself at row {@code y} (a parapet with no corbels). */
    static void merlons(Canvas c, Box outline, int y, Brush merlon, int seed, double broken) {
        for (Walls.Run r : Walls.around(outline)) {
            for (int i = 0; i < r.length(); i++) {
                int x = r.x(i), z = r.z(i);
                if (Math.floorMod(i, 2) == 0 && !Noise.chance(x, y, z, seed, broken)) c.set(x, y, z, merlon.at(x, y, z));
                else c.carve(x, y, z);
            }
        }
    }

    /** A hanging cage of bars (3 × 3 round an empty middle, {@code tall} rows) under a slab lid hung from a chain. */
    static void cage(Canvas c, int x, int top, int z, int tall, String lid, int chain) {
        for (int k = 1; k <= chain; k++) c.set(x, top + k, z, St.chain("y"));
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                c.set(x + dx, top, z + dz, St.slabTop(lid));
                c.set(x + dx, top - tall - 1, z + dz, St.slabTop(lid));
                for (int y = top - tall; y < top; y++) {
                    if (dx == 0 && dz == 0) c.carve(x, y, z);
                    else c.set(x + dx, y, z + dz, St.bars());
                }
            }
        }
    }

    /** A brazier: a masonry post with a block of netherrack (or soul soil) and an everlasting fire on it. */
    static void brazier(Canvas c, int x, int y, int z, String post, boolean soul) {
        c.set(x, y, z, St.wall(post));
        c.set(x, y + 1, z, soul ? "minecraft:soul_soil" : "minecraft:netherrack");
        c.set(x, y + 2, z, soul ? "minecraft:soul_fire" : "minecraft:fire");
    }

    // --- light -------------------------------------------------------------------------------------------------------------

    /** Whether a walker can stand in (x, y, z) (as the construction checks read it). */
    static boolean standable(Canvas c, int x, int y, int z) {
        return Kinds.passable(c.world(x, y, z)) && Kinds.passable(c.world(x, y + 1, z)) && Kinds.floor(c.world(x, y - 1, z))
                && !St.path(c.world(x, y, z)).equals("water");
    }

    /** Roofed and walled on three sides (the construction checks' "interior"). */
    static boolean interior(Canvas c, int x, int y, int z) {
        if (!Light.covered(c, x, y, z, 12)) return false;
        int walled = 0;
        for (Dir d : Dir.HORIZONTAL) {
            for (int k = 1; k <= 6; k++) {
                if (blocks(c.world(x + d.dx * k, y, z + d.dz * k)) || blocks(c.world(x + d.dx * k, y + 1, z + d.dz * k))) {
                    walled++;
                    break;
                }
            }
        }
        return walled >= 3;
    }

    private static boolean blocks(String s) {
        if (Kinds.air(s)) return false;
        String p = St.path(s);
        if (NATURAL.contains(St.id(s)) || p.endsWith("cobblestone")) return false;
        return Kinds.opaqueCube(s) && !Kinds.log(s) || Kinds.fullCube(s) && !Kinds.leaves(s) || p.endsWith("_door") || Kinds.barsLike(s);
    }

    /**
     * The light pass a builder makes last: every standable interior cell darker than {@code min} gets a light — the floor block
     * under it turned into {@code lightBlock} (an inlaid lamp), else a ceiling block close over it, else the invisible light.
     */
    static void lightUp(Canvas c, String lightBlock, int min) {
        for (int pass = 0; pass < 12; pass++) {
            Light light = Light.of(c);
            List<Long> dark = new ArrayList<>();
            for (Map.Entry<Long, String> e : c.map().entrySet()) {
                int x = Canvas.kx(e.getKey()), y = Canvas.ky(e.getKey()), z = Canvas.kz(e.getKey());
                if (!Kinds.passable(e.getValue()) || light.at(x, y, z) >= min) continue;
                if (!standable(c, x, y, z) || !interior(c, x, y, z)) continue;
                dark.add(e.getKey());
            }
            if (dark.isEmpty()) return;
            Collections.sort(dark);
            List<int[]> placed = new ArrayList<>();
            for (long k : dark) {
                int x = Canvas.kx(k), y = Canvas.ky(k), z = Canvas.kz(k);
                boolean near = false;
                for (int[] p : placed) near |= Math.abs(p[0] - x) + Math.abs(p[1] - y) + Math.abs(p[2] - z) <= 4;
                if (near) continue;
                place(c, lightBlock, x, y, z);
                placed.add(new int[]{x, y, z});
            }
        }
    }

    private static void place(Canvas c, String lightBlock, int x, int y, int z) {
        String here = c.world(x, y, z), below = c.get(x, y - 1, z);
        if (below != null && Kinds.opaqueCube(below) && Kinds.emission(below) == 0 && !Kinds.soilPlant(here) && !St.path(here).endsWith("_door")) {
            c.set(x, y - 1, z, lightBlock);
            return;
        }
        for (int yy = y + 2; yy <= y + 5; yy++) {
            String s = c.get(x, yy, z);
            if (s == null || Kinds.air(s)) continue;
            if (Kinds.opaqueCube(s) && Kinds.emission(s) == 0 && !Kinds.log(s)) {
                c.set(x, yy, z, lightBlock);
                return;
            }
            break;
        }
        if (Kinds.air(here)) c.set(x, y, z, St.light(15));
        else if (Kinds.air(c.world(x, y + 1, z))) c.set(x, y + 1, z, St.light(15));
    }
}
