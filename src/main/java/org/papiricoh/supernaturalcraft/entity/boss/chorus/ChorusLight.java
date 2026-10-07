package org.papiricoh.supernaturalcraft.entity.boss.chorus;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Light that only stone can stop. The Chorus's gaze, its Hymn and its last light all ask the
 * same question: is there a block between its light and you? Its own body never shades anyone.
 */
public final class ChorusLight {

    private ChorusLight() {
    }

    /** Whether light from {@code from} reaches {@code e}'s eyes. */
    public static boolean sees(ChorusEntity boss, Vec3 from, Entity e) {
        return boss.level().clip(new ClipContext(from, e.getEyePosition(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, boss))
                .getType() == HitResult.Type.MISS;
    }

    public static boolean inShadow(ChorusEntity boss, Vec3 from, Entity e) {
        return !sees(boss, from, e);
    }

    /** How far a beam from {@code from} toward {@code to} gets before stone stops it. */
    public static Vec3 reach(ChorusEntity boss, Vec3 from, Vec3 to) {
        var hit = boss.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, boss));
        return hit.getType() == HitResult.Type.MISS ? to : hit.getLocation();
    }

    /** Distance from the middle of {@code e} to the segment {@code a}-{@code b}. */
    public static double distanceToBeam(Entity e, Vec3 a, Vec3 b) {
        Vec3 p = e.getBoundingBox().getCenter();
        Vec3 ab = b.subtract(a);
        double len2 = ab.lengthSqr();
        double k = len2 < 1e-6 ? 0 : Math.max(0, Math.min(1, p.subtract(a).dot(ab) / len2));
        // The entity's own half-height counts as reach: a beam at the knees still strikes.
        double d = p.distanceTo(a.add(ab.scale(k)));
        return Math.max(0, d - e.getBbHeight() / 2 + 0.3);
    }
}
