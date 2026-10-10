package org.papiricoh.supernaturalcraft.buildkit;

/**
 * A direction on the canvas (pure). X grows east, Y up, Z south, so north is -Z. {@link #id()} is the lower-case name vanilla
 * uses in block states.
 */
public enum Dir {
    NORTH(0, 0, -1), EAST(1, 0, 0), SOUTH(0, 0, 1), WEST(-1, 0, 0), UP(0, 1, 0), DOWN(0, -1, 0);

    /** The four horizontal directions, clockwise from north (as vanilla's {@code Direction.Plane.HORIZONTAL} order differs, use this). */
    public static final Dir[] HORIZONTAL = {NORTH, EAST, SOUTH, WEST};

    public final int dx, dy, dz;

    Dir(int dx, int dy, int dz) {
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
    }

    public String id() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public Dir opposite() {
        return switch (this) {
            case NORTH -> SOUTH;
            case SOUTH -> NORTH;
            case EAST -> WEST;
            case WEST -> EAST;
            case UP -> DOWN;
            case DOWN -> UP;
        };
    }

    /** Clockwise seen from above: north → east → south → west. */
    public Dir cw() {
        return switch (this) {
            case NORTH -> EAST;
            case EAST -> SOUTH;
            case SOUTH -> WEST;
            case WEST -> NORTH;
            default -> throw new IllegalStateException("no horizontal turn for " + this);
        };
    }

    public Dir ccw() {
        return cw().opposite();
    }

    /** {@code x}, {@code y} or {@code z}. */
    public String axis() {
        return dx != 0 ? "x" : dy != 0 ? "y" : "z";
    }

    public boolean horizontal() {
        return dy == 0;
    }

    public static Dir of(String id) {
        return valueOf(id.toUpperCase(java.util.Locale.ROOT));
    }

    /** The horizontal direction of a step (dx, dz); the larger component wins, ties go to X. */
    public static Dir toward(int dx, int dz) {
        if (dx == 0 && dz == 0) throw new IllegalArgumentException("no direction");
        if (Math.abs(dx) >= Math.abs(dz)) return dx > 0 ? EAST : WEST;
        return dz > 0 ? SOUTH : NORTH;
    }
}
