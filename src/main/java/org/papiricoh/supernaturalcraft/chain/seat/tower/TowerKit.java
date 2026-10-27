package org.papiricoh.supernaturalcraft.chain.seat.tower;

import org.papiricoh.supernaturalcraft.buildkit.Brush;
import org.papiricoh.supernaturalcraft.buildkit.Canvas;
import org.papiricoh.supernaturalcraft.buildkit.Dir;
import org.papiricoh.supernaturalcraft.buildkit.Kinds;
import org.papiricoh.supernaturalcraft.buildkit.Light;
import org.papiricoh.supernaturalcraft.buildkit.St;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Small drawing helpers for the SucroCorp tower (pure): pattern brushes, the logo and lettering, the closing light pass. */
final class TowerKit {

    private TowerKit() {
    }

    static int salt(long seed) {
        return (int) (seed ^ (seed >>> 32));
    }

    /**
     * {@code a} with {@code b} on every third cell of a diagonal lattice: on any plane (a wall either way, a floor) every 5 × 5
     * patch holds at least 7 of {@code b}, so a long wall never reads as one flat block.
     */
    static Brush tri(String a, String b) {
        String sa = St.full(a), sb = St.full(b);
        return (x, y, z) -> Math.floorMod(x + z + 2 * y, 3) == 0 ? sb : sa;
    }

    /** One-cell checker of two blocks on the floor plane. */
    static Brush checker(String a, String b) {
        String sa = St.full(a), sb = St.full(b);
        return (x, y, z) -> ((x + z) & 1) == 0 ? sa : sb;
    }

    // --- the SucroCorp mark ---------------------------------------------------------------------------------------------------

    /** The emblem's side (cells): a ring round a block-letter S. */
    static final int LOGO = 11;
    private static final String[] S = {".XXXX", "X....", "X....", ".XXX.", "....X", "....X", "XXXX."};

    /** The emblem at column {@code i} (left to right as seen), row {@code j} (top to bottom): 0 nothing, 1 the ring, 2 the S. */
    static int logo(int i, int j) {
        double dx = i - 5, dy = j - 5, d = Math.sqrt(dx * dx + dy * dy);
        if (d >= 4.5 && d < 5.6) return 1;
        if (i >= 3 && i <= 7 && j >= 2 && j <= 8 && S[j - 2].charAt(i - 3) == 'X') return 2;
        return 0;
    }

    private static final Map<Character, String[]> FONT = Map.of(
            'S', new String[]{"XXX", "X..", "XXX", "..X", "XXX"},
            'U', new String[]{"X.X", "X.X", "X.X", "X.X", "XXX"},
            'C', new String[]{"XXX", "X..", "X..", "X..", "XXX"},
            'R', new String[]{"XX.", "X.X", "XX.", "X.X", "X.X"},
            'O', new String[]{"XXX", "X.X", "X.X", "X.X", "XXX"},
            'P', new String[]{"XXX", "X.X", "XXX", "X..", "X.."});

    /** The width of a word in the 3 × 5 block font (one empty column between letters). */
    static int textWidth(String word) {
        return word.length() * 4 - 1;
    }

    /**
     * A word in the 3 × 5 block font, its top row at {@code top}, starting at {@code x0} and running toward {@code dir} (+1 east:
     * read from the south; -1 west: read from the north), in the plane {@code z}. Only the letters' cells are written.
     */
    static void text(Canvas c, String word, int x0, int top, int z, int dir, String block) {
        for (int k = 0; k < word.length(); k++) {
            String[] g = FONT.get(word.charAt(k));
            for (int row = 0; row < 5; row++) {
                for (int col = 0; col < 3; col++) {
                    if (g[row].charAt(col) == 'X') c.set(x0 + dir * (k * 4 + col), top - row, z, block);
                }
            }
        }
    }

    // --- light ---------------------------------------------------------------------------------------------------------------

    private static final Set<String> NATURAL = Set.of("minecraft:grass_block", "minecraft:dirt", "minecraft:coarse_dirt", "minecraft:podzol",
            "minecraft:moss_block", "minecraft:rooted_dirt", "minecraft:stone", "minecraft:andesite", "minecraft:calcite",
            "minecraft:diorite", "minecraft:tuff", "minecraft:dripstone_block", "minecraft:sand", "minecraft:gravel", "minecraft:clay",
            "minecraft:mud", "minecraft:dirt_path", "minecraft:farmland", "minecraft:snow_block", "minecraft:packed_mud",
            "minecraft:deepslate", "minecraft:granite");

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
     * The last pass: every standable interior cell darker than {@code min} gets a light — the floor block under it turned into
     * {@code lightBlock} (an inlaid lamp), else a ceiling block close over it, else the invisible light.
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
        if (below != null && Kinds.opaqueCube(below) && Kinds.emission(below) == 0 && !Kinds.soilPlant(here) && !St.path(here).endsWith("_door")
                && !St.id(below).startsWith("supernaturalcraft:")) {
            c.set(x, y - 1, z, lightBlock);
            return;
        }
        for (int yy = y + 2; yy <= y + 5; yy++) {
            String s = c.get(x, yy, z);
            if (s == null || Kinds.air(s)) continue;
            if (Kinds.opaqueCube(s) && Kinds.emission(s) == 0 && !Kinds.log(s) && !St.id(s).startsWith("supernaturalcraft:")) {
                c.set(x, yy, z, lightBlock);
                return;
            }
            break;
        }
        if (Kinds.air(here)) c.set(x, y, z, St.light(15));
        else if (Kinds.air(c.world(x, y + 1, z))) c.set(x, y + 1, z, St.light(15));
    }
}
