package org.papiricoh.supernaturalcraft.cinematic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * A camera path, read from {@code assets/<ns>/cinematics/<id>.json}. Shots play one after another.
 * Positions are relative to an anchor (usually a boss) in its own frame: +z is where it faces,
 * +x its left, +y up. Pure maths, no client classes, so it can be unit-tested.
 */
public record CameraSequence(List<Shot> shots) {

    public static final Codec<CameraSequence> CODEC = RecordCodecBuilder.create(i -> i.group(
            Shot.CODEC.listOf(1, 64).fieldOf("shots").forGetter(CameraSequence::shots)
    ).apply(i, CameraSequence::new));

    public enum Ease implements StringRepresentable {
        LINEAR, IN, OUT, IN_OUT;

        public static final Codec<Ease> CODEC = StringRepresentable.fromEnum(Ease::values);

        public float apply(float t) {
            return switch (this) {
                case LINEAR -> t;
                case IN -> t * t;
                case OUT -> 1 - (1 - t) * (1 - t);
                case IN_OUT -> (1 - Mth.cos(t * Mth.PI)) / 2;
            };
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** One continuous camera move. {@code look_to} defaults to {@code look}; fov and roll go start → end. */
    public record Shot(int duration, List<Vec3> path, Vec3 look, Optional<Vec3> lookTo, float fov, Optional<Float> fovTo,
                       float roll, Optional<Float> rollTo, float shake, Ease ease) {

        public static final Codec<Shot> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(1, 2400).fieldOf("duration").forGetter(Shot::duration),
                Vec3.CODEC.listOf(1, 32).fieldOf("path").forGetter(Shot::path),
                Vec3.CODEC.optionalFieldOf("look", new Vec3(0, 1.5, 0)).forGetter(Shot::look),
                Vec3.CODEC.optionalFieldOf("look_to").forGetter(Shot::lookTo),
                Codec.floatRange(10, 150).optionalFieldOf("fov", 70f).forGetter(Shot::fov),
                Codec.floatRange(10, 150).optionalFieldOf("fov_to").forGetter(Shot::fovTo),
                Codec.FLOAT.optionalFieldOf("roll", 0f).forGetter(Shot::roll),
                Codec.FLOAT.optionalFieldOf("roll_to").forGetter(Shot::rollTo),
                Codec.floatRange(0, 4).optionalFieldOf("shake", 0f).forGetter(Shot::shake),
                Ease.CODEC.optionalFieldOf("ease", Ease.IN_OUT).forGetter(Shot::ease)
        ).apply(i, Shot::new));

        Frame sample(float t) {
            float e = ease.apply(Mth.clamp(t, 0, 1));
            return new Frame(spline(path, e), lerp(look, lookTo.orElse(look), e),
                    Mth.lerp(e, fov, fovTo.orElse(fov)), Mth.lerp(e, roll, rollTo.orElse(roll)), shake);
        }
    }

    /** Where the camera is, what it looks at (both anchor-relative), and how. */
    public record Frame(Vec3 pos, Vec3 look, float fov, float roll, float shake) {
    }

    public int duration() {
        int d = 0;
        for (Shot s : shots) d += s.duration;
        return d;
    }

    /** The frame {@code ticks} into the sequence; past the end, the last frame holds. */
    public Frame sample(float ticks) {
        float t = Math.max(0, ticks);
        for (Shot s : shots) {
            if (t < s.duration) return s.sample(t / s.duration);
            t -= s.duration;
        }
        return shots.getLast().sample(1f);
    }

    static Vec3 lerp(Vec3 a, Vec3 b, float t) {
        return a.add(b.subtract(a).scale(t));
    }

    /** Uniform Catmull-Rom through every point, the ends repeated; {@code t} in [0, 1] over the whole path. */
    public static Vec3 spline(List<Vec3> pts, float t) {
        int n = pts.size();
        if (n == 1) return pts.getFirst();
        float f = Mth.clamp(t, 0, 1) * (n - 1);
        int i = Math.min((int) f, n - 2);
        float u = f - i;
        Vec3 p0 = pts.get(Math.max(i - 1, 0)), p1 = pts.get(i), p2 = pts.get(i + 1), p3 = pts.get(Math.min(i + 2, n - 1));
        double u2 = u * u, u3 = u2 * u;
        return p0.scale(-0.5 * u3 + u2 - 0.5 * u)
                .add(p1.scale(1.5 * u3 - 2.5 * u2 + 1))
                .add(p2.scale(-1.5 * u3 + 2 * u2 + 0.5 * u))
                .add(p3.scale(0.5 * u3 - 0.5 * u2));
    }

    /** Anchor-relative (+z forward, +x left) to world, for an anchor at {@code origin} facing {@code yawDeg}. */
    public static Vec3 toWorld(Vec3 local, Vec3 origin, float yawDeg) {
        double yaw = Math.toRadians(yawDeg);
        double fx = -Math.sin(yaw), fz = Math.cos(yaw);   // forward
        double lx = fz, lz = -fx;                           // left
        return new Vec3(origin.x + local.x * lx + local.z * fx, origin.y + local.y, origin.z + local.x * lz + local.z * fz);
    }
}
