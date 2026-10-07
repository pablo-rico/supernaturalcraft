package org.papiricoh.supernaturalcraft.bowl.spell;

import org.jetbrains.annotations.Nullable;

/**
 * Pure: where a bound creature that strayed is pulled back to. Bindings are measured from the
 * bowl's centre; creatures that walk are held in a cylinder (height ignored), creatures that
 * fly or drift (ghosts) in a sphere.
 */
public final class BindingMath {

    /** How far inside the edge a pulled creature lands, so it is not pulled again at once. */
    public static final double INSET = 0.5;

    private BindingMath() {
    }

    /** Whether (px, py, pz) is outside the binding around (ax, ay, az). */
    public static boolean outside(double ax, double ay, double az, double px, double py, double pz, double radius, boolean spherical) {
        double dx = px - ax, dy = spherical ? py - ay : 0, dz = pz - az;
        return dx * dx + dy * dy + dz * dz > radius * radius;
    }

    /**
     * The point just inside the edge, on the line from the anchor to the creature, or null if it is
     * still within {@code radius}. For a cylinder the creature keeps its height.
     */
    @Nullable
    public static double[] pullBack(double ax, double ay, double az, double px, double py, double pz, double radius, boolean spherical) {
        if (!outside(ax, ay, az, px, py, pz, radius, spherical)) return null;
        double dx = px - ax, dy = spherical ? py - ay : 0, dz = pz - az;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double keep = Math.max(0, radius - INSET) / len;
        return new double[]{ax + dx * keep, spherical ? ay + dy * keep : py, az + dz * keep};
    }
}
