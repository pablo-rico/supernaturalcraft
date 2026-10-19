package org.papiricoh.supernaturalcraft.client.witch;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Where a few of the witch bosses' bones are (v0.21, P6), as last drawn, so world effects can start from them: the Grand Witch's
 * staff crystal (where her coven's beams land and her spells leave) and Rowena's hands. Kept as offsets from the entity (the
 * matrices are the frame's; the offsets survive to the next frame and follow the entity between).
 */
public final class WitchBones {

    public enum Bone {
        CRYSTAL("crystal"), RIGHT_HAND("right_hand"), LEFT_HAND("left_hand"), HEAD("head");

        final String name;

        Bone(String name) {
            this.name = name;
        }
    }

    private static final Map<Entity, Vec3[]> BONES = new WeakHashMap<>();

    private WitchBones() {
    }

    /** Asks GeckoLib to keep the world matrices of {@code bones} (call before rendering). */
    static void track(BakedGeoModel model, Bone... bones) {
        for (Bone b : bones) model.getBone(b.name).ifPresent(g -> g.setTrackingMatrices(true));
    }

    /** Remembers where {@code bones} were drawn (call after rendering). */
    static void record(Entity e, BakedGeoModel model, float partial, Bone... bones) {
        Vec3[] v = BONES.computeIfAbsent(e, k -> new Vec3[Bone.values().length]);
        Vec3 base = e.getPosition(partial);
        for (Bone b : bones) {
            GeoBone g = model.getBone(b.name).orElse(null);
            if (g == null || !g.isTrackingMatrices()) continue;
            var w = g.getWorldPosition();
            Vec3 off = new Vec3(w.x - base.x, w.y - base.y, w.z - base.z);
            v[b.ordinal()] = off.lengthSqr() < 25 ? off : null;
        }
    }

    /** Where bone {@code b} of {@code e} is this frame, if it has been drawn lately (null otherwise). */
    public static @Nullable Vec3 bone(@Nullable Entity e, Bone b, float partial) {
        if (e == null) return null;
        Vec3[] v = BONES.get(e);
        if (v == null || v[b.ordinal()] == null) return null;
        return e.getPosition(partial).add(v[b.ordinal()]);
    }

    /** As {@link #bone}, or a point {@code up} blocks above the entity's feet (or {@code fallback} with no entity). */
    public static Vec3 boneOr(@Nullable Entity e, Bone b, float partial, double up, Vec3 fallback) {
        Vec3 p = bone(e, b, partial);
        if (p != null) return p;
        return e == null || e.isRemoved() ? fallback : e.getPosition(partial).add(0, up, 0);
    }
}
