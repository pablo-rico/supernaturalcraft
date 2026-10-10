package org.papiricoh.supernaturalcraft.buildkit;

/** An inclusive axis-aligned box of cells (pure). Always normalised: {@code x0 <= x1}, {@code y0 <= y1}, {@code z0 <= z1}. */
public record Box(int x0, int y0, int z0, int x1, int y1, int z1) {

    public Box {
        if (x0 > x1 || y0 > y1 || z0 > z1) throw new IllegalArgumentException("box not normalised, use Box.of: " + x0 + "," + y0 + "," + z0 + " → " + x1 + "," + y1 + "," + z1);
    }

    /** A box from any two opposite corners. */
    public static Box of(int ax, int ay, int az, int bx, int by, int bz) {
        return new Box(Math.min(ax, bx), Math.min(ay, by), Math.min(az, bz), Math.max(ax, bx), Math.max(ay, by), Math.max(az, bz));
    }

    public boolean contains(int x, int y, int z) {
        return x >= x0 && x <= x1 && y >= y0 && y <= y1 && z >= z0 && z <= z1;
    }

    public boolean containsColumn(int x, int z) {
        return x >= x0 && x <= x1 && z >= z0 && z <= z1;
    }

    public Box grow(int by) {
        return new Box(x0 - by, y0 - by, z0 - by, x1 + by, y1 + by, z1 + by);
    }

    /** Grown sideways only (X and Z). */
    public Box growXZ(int by) {
        return new Box(x0 - by, y0, z0 - by, x1 + by, y1, z1 + by);
    }

    public Box union(Box o) {
        return new Box(Math.min(x0, o.x0), Math.min(y0, o.y0), Math.min(z0, o.z0), Math.max(x1, o.x1), Math.max(y1, o.y1), Math.max(z1, o.z1));
    }

    public Box shift(int dx, int dy, int dz) {
        return new Box(x0 + dx, y0 + dy, z0 + dz, x1 + dx, y1 + dy, z1 + dz);
    }

    public Box withY(int ya, int yb) {
        return new Box(x0, Math.min(ya, yb), z0, x1, Math.max(ya, yb), z1);
    }

    public int sizeX() {
        return x1 - x0 + 1;
    }

    public int sizeY() {
        return y1 - y0 + 1;
    }

    public int sizeZ() {
        return z1 - z0 + 1;
    }

    public long volume() {
        return (long) sizeX() * sizeY() * sizeZ();
    }

    public int centerX() {
        return Math.floorDiv(x0 + x1, 2);
    }

    public int centerZ() {
        return Math.floorDiv(z0 + z1, 2);
    }
}
