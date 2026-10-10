package org.papiricoh.supernaturalcraft.layout;

import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.ArenaCell;

import java.util.ArrayList;
import java.util.List;

/**
 * Placeholder geometry for the v0.18 layouts until the architects' builds land (a flat disc to stand on). Pure. Nothing but the
 * foundations' stubs should use it; once every layout is real it can be deleted.
 */
public final class LayoutStubs {

    private LayoutStubs() {
    }

    /** A flat disc of {@code block} at {@code dy = -1} round (cx, cz), with air above it up to {@code clear} blocks. */
    public static LayoutPlan.Zone disc(String name, int cx, int cz, int radius, String block, int clear) {
        List<ArenaCell> cells = new ArrayList<>();
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z > radius * radius) continue;
                cells.add(new ArenaCell(cx + x, -1, cz + z, block));
                for (int y = 0; y < clear; y++) cells.add(new ArenaCell(cx + x, y, cz + z, "minecraft:air"));
            }
        }
        return new LayoutPlan.Zone(name, List.copyOf(cells));
    }
}
