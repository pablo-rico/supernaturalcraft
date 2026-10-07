package org.papiricoh.supernaturalcraft.entity.boss.chuck.arena;

import static org.papiricoh.supernaturalcraft.entity.boss.chuck.arena.ArenaKind.*;

/**
 * Chapter five, the Blank Page: everything else is unwritten and the floor becomes one vast sheet of paper, ruled
 * with faint lines of ink and a margin. Nothing stands on it but the Author, at its centre; over the chapter it
 * erases itself from the edges inward ({@link BlankPageErosion}). Pure.
 */
public final class BlankPageLayout {

    /** Rows between two ruled lines. */
    public static final int RULE = 4;

    private BlankPageLayout() {
    }

    /** The x of the margin line (left of centre, like a notebook's). */
    public static int margin(int r) {
        return -Math.round(r * 0.58f);
    }

    public static ArenaPlan plan(int r, long seed) {
        ArenaPlan.Builder b = new ArenaPlan.Builder(r);
        int margin = margin(r);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (!b.inside(dx, dz)) continue;
                boolean ruled = Math.floorMod(dz, RULE) == 2 || dx == margin;
                b.set(dx, 0, dz, ruled ? INK : PAGE);
            }
        }
        return b.build(0, 1, 0);
    }
}
