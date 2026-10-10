package org.papiricoh.supernaturalcraft.layout;

/**
 * A fixed point of a layout (v0.18, pure contract): where a gate lands, a boss stands, a chest sits. Relative to the layout's
 * origin; {@code y = 0} is the first air layer above the layout's main floor ({@code y = -1} is the floor itself).
 */
public record LayoutPoint(int x, int y, int z) {

    public LayoutPoint offset(int dx, int dy, int dz) {
        return new LayoutPoint(x + dx, y + dy, z + dz);
    }
}
