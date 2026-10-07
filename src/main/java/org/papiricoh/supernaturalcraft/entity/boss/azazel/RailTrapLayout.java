package org.papiricoh.supernaturalcraft.entity.boss.azazel;

import net.minecraft.util.StringRepresentable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Samuel Colt's devil's trap, laid out in iron rails: a circle with a five-pointed star inside, centred
 * on the arena. Pure geometry: which cells hold a rail and which way each one runs.
 */
public final class RailTrapLayout {

    public static final int RADIUS = 5;

    /** Which way a rail runs across its block. DIAG_A runs along x = z, DIAG_B along x = -z. */
    public enum Shape implements StringRepresentable {
        NS, EW, DIAG_A, DIAG_B, CROSS;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        /** The rail running along a direction given as an angle in degrees (x towards 0, z towards 90). */
        public static Shape along(double degrees) {
            double a = ((degrees % 180) + 180) % 180;
            int k = (int) Math.round(a / 45) % 4;
            return switch (k) {
                case 0 -> EW;
                case 1 -> DIAG_A;
                case 2 -> NS;
                default -> DIAG_B;
            };
        }
    }

    public record Cell(int dx, int dz, Shape shape) {
    }

    private RailTrapLayout() {
    }

    /** Every rail of the trap, ordered round the circle (the order the rails light up as they charge). */
    public static List<Cell> cells() {
        return cells(RADIUS);
    }

    public static List<Cell> cells(int radius) {
        Map<Long, Shape> out = new LinkedHashMap<>();
        for (int dx = -radius - 1; dx <= radius + 1; dx++) {
            for (int dz = -radius - 1; dz <= radius + 1; dz++) {
                if (Math.abs(Math.hypot(dx, dz) - radius) <= 0.5) {
                    double tangent = Math.toDegrees(Math.atan2(dz, dx)) + 90;
                    put(out, dx, dz, Shape.along(tangent));
                }
            }
        }
        // The star: each point joined to the one two along.
        double[][] pts = new double[5][];
        for (int k = 0; k < 5; k++) {
            double a = Math.toRadians(-90 + k * 72);
            pts[k] = new double[]{Math.cos(a) * radius, Math.sin(a) * radius};
        }
        for (int k = 0; k < 5; k++) {
            double[] a = pts[k], b = pts[(k + 2) % 5];
            double deg = Math.toDegrees(Math.atan2(b[1] - a[1], b[0] - a[0]));
            Shape shape = Shape.along(deg);
            double len = Math.hypot(b[0] - a[0], b[1] - a[1]);
            int steps = (int) Math.ceil(len * 4);
            for (int i = 0; i <= steps; i++) {
                double t = i / (double) steps;
                int x = (int) Math.round(a[0] + (b[0] - a[0]) * t), z = (int) Math.round(a[1] + (b[1] - a[1]) * t);
                if (x == 0 && z == 0) continue;
                if (Math.hypot(x, z) > radius + 0.5) continue;
                put(out, x, z, shape);
            }
        }
        List<Cell> cells = new ArrayList<>();
        out.forEach((key, shape) -> cells.add(new Cell((int) (key >> 32), (int) (long) key, shape)));
        cells.sort(Comparator.comparingDouble((Cell c) -> angle(c)).thenComparingDouble(c -> Math.hypot(c.dx, c.dz)));
        return cells;
    }

    private static void put(Map<Long, Shape> out, int dx, int dz, Shape shape) {
        long key = ((long) dx << 32) | (dz & 0xFFFFFFFFL);
        Shape had = out.get(key);
        out.put(key, had == null || had == shape ? shape : Shape.CROSS);
    }

    private static double angle(Cell c) {
        double a = Math.atan2(c.dz, c.dx) + Math.PI / 2;
        return ((a % (Math.PI * 2)) + Math.PI * 2) % (Math.PI * 2);
    }

    /** Whether a point (offset from the centre, in blocks) stands inside the trap. */
    public static boolean inside(double dx, double dz) {
        return dx * dx + dz * dz <= (RADIUS + 0.5) * (RADIUS + 0.5);
    }
}
