package org.papiricoh.supernaturalcraft.heaven.plot;

/**
 * Where each plot of Heaven lies (v0.18, pure): plot {@code i} sits on a square spiral of grid cells {@code spacing} blocks
 * apart, walked outwards from the origin. Plot 0 (the cell at the origin) is Ash's Roadhouse; hunters get 1, 2, 3... in the
 * order they first come. Ring {@code k} ({@code k >= 1}) holds the {@code 8k} cells whose Chebyshev distance to the origin is
 * {@code k}, starting just above its south-east corner and turning anticlockwise (seen from above with +X east, +Z south).
 */
public final class PlotGrid {

    /** The Roadhouse's index. */
    public static final int HUB = 0;

    private PlotGrid() {
    }

    /** The grid cell {@code {gx, gz}} of plot {@code index} (>= 0). */
    public static int[] cell(int index) {
        if (index < 0) throw new IllegalArgumentException("negative plot index " + index);
        if (index == 0) return new int[]{0, 0};
        int k = ring(index);
        int t = index - (2 * k - 1) * (2 * k - 1);
        int side = t / (2 * k), pos = t % (2 * k);
        return switch (side) {
            case 0 -> new int[]{k, -k + 1 + pos};
            case 1 -> new int[]{k - 1 - pos, k};
            case 2 -> new int[]{-k, k - 1 - pos};
            default -> new int[]{-k + 1 + pos, -k};
        };
    }

    /** The plot index of grid cell ({@code gx}, {@code gz}); the inverse of {@link #cell}. */
    public static int index(int gx, int gz) {
        int k = Math.max(Math.abs(gx), Math.abs(gz));
        if (k == 0) return 0;
        int base = (2 * k - 1) * (2 * k - 1);
        int t;
        if (gx == k && gz > -k) t = gz + k - 1;
        else if (gz == k) t = 2 * k + (k - 1 - gx);
        else if (gx == -k) t = 4 * k + (k - 1 - gz);
        else t = 6 * k + (gx + k - 1);
        return base + t;
    }

    /** The ring of plot {@code index}: the Chebyshev distance of its cell to the origin. */
    public static int ring(int index) {
        if (index <= 0) return 0;
        int k = (int) Math.ceil((Math.sqrt(index + 1) - 1) / 2);
        // Guard the floating point at the ring's edges: (2k-1)^2 <= index < (2k+1)^2.
        while ((2L * k - 1) * (2L * k - 1) > index) k--;
        while ((2L * k + 1) * (2L * k + 1) <= index) k++;
        return k;
    }

    /** The centre (x, z) of plot {@code index}, relative to the grid's base, {@code spacing} blocks per cell. */
    public static int[] origin(int index, int spacing) {
        int[] c = cell(index);
        return new int[]{c[0] * spacing, c[1] * spacing};
    }

    /** The plot whose cell is nearest to ({@code x}, {@code z}) (relative to the grid's base). */
    public static int nearest(double x, double z, int spacing) {
        return index((int) Math.round(x / spacing), (int) Math.round(z / spacing));
    }

    /** Whether ({@code dx}, {@code dz}) from a plot's centre lies within {@code radius} on both axes. */
    public static boolean within(double dx, double dz, int radius) {
        return Math.abs(dx) <= radius && Math.abs(dz) <= radius;
    }
}
