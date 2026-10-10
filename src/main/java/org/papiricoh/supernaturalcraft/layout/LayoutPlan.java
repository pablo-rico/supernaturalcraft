package org.papiricoh.supernaturalcraft.layout;

import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.ArenaCell;

import java.util.List;
import java.util.Map;

/**
 * A built layout as its consumers see it (v0.18, pure contract between the architects and the code that writes or fights in
 * a layout): blocks grouped in zones in the order they should be written, decor written after them, and named points.
 * <p>Cells: {@link ArenaCell} with {@code dx, dy, dz} relative to the layout's origin, {@code dy = 0} the first air layer above
 * its main floor; {@code block} a full block state string ({@code minecraft:oak_stairs[facing=east,half=bottom,shape=straight]}).
 * Air cells carve (they are written too). Builders must never emit an unparsable state: the world writer would place air.
 *
 * @param zones  named groups of cells, in write order (the first is what a visitor lands in)
 * @param decor  block entities' contents and decorative entities
 * @param points named points (the layout class also exposes the ones code relies on as constants)
 */
public record LayoutPlan(List<Zone> zones, List<LayoutDecor> decor, Map<String, LayoutPoint> points) {

    /** A named group of cells. */
    public record Zone(String name, List<ArenaCell> cells) {
    }

    /** Every cell, zones in order. */
    public List<ArenaCell> cells() {
        return zones.stream().flatMap(z -> z.cells().stream()).toList();
    }

    public int size() {
        return zones.stream().mapToInt(z -> z.cells().size()).sum();
    }
}
