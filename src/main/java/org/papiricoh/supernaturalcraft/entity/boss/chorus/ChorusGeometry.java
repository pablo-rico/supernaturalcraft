package org.papiricoh.supernaturalcraft.entity.boss.chorus;

import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Where every piece of the Broken Chorus is, shared by the server's hit boxes and the client's
 * model so the two can never drift apart.
 *
 * <p>The numbers mirror {@code tools/artgen/chorus_art.py} (checked by {@code ChorusAssetsTest}).
 * Positions are computed exactly as GeckoLib draws the bones: Bedrock pixels with X mirrored,
 * each bone rotating about its pivot by {@code rotationZYX(z, y, x)} (child first, then its
 * parents), the model scaled by {@link #MODEL_SCALE} and turned 180° (the boss keeps yaw 0).
 * Rotations here are the raw radians the renderer passes to {@code GeoBone.setRot*}; baked rest
 * rotations from the JSON are converted the way GeckoLib bakes them ({@code -x, -y, z}).
 */
public final class ChorusGeometry {

    public static final float MODEL_SCALE = 2.0f;
    /** Model pixels per world block. */
    public static final float PX = 16f / MODEL_SCALE;
    public static final float CORE_Y = 64, HEADS_Y = 24;

    public static final String[] WHEEL_NAMES = {"outer", "mid", "inner"};
    public static final float[] WHEEL_RADIUS = {60f, 46.4f, 33.6f};
    public static final int[] WHEEL_EYES = {5, 4, 3};
    private static final float[] FIRST_EYE = {0.3f, 0.7f, 1.1f};
    public static final float EYE_OUT = 3.0f;
    public static final float[] WHEEL_TILT_Y = {0f, 1.05f, 2.1f};
    public static final float[] WHEEL_TILT_Z = {0.25f, -0.35f, 0.5f};

    public static final String[] FACE_NAMES = {"man", "lion", "ox", "eagle"};
    /** Face centres in Bedrock pixels: the man looks ahead, the lion right, the ox left, the eagle behind. */
    public static final float[][] FACE_CENTRES = {{0, 24, -12}, {-12, 24, 0}, {12, 24, 0}, {0, 24, 12}};

    public static final String[] WING_NAMES = {"top_l", "top_r", "mid_l", "mid_r", "low_l", "low_r"};
    public static final float[][] SHOULDERS = {{9, 74, 3}, {-9, 74, 3}, {10, 62, 5}, {-10, 62, 5}, {8, 44, 4}, {-8, 44, 4}};
    /** Distance along the wing (px) to the middle of its hit box, and how far below the arm (the hanging feathers). */
    private static final float[] WING_REACH = {40, 40, 36, 36, 26, 26};
    private static final float WING_DROP = 9;
    private static final float[] WING_LIFT = {0.55f, 0.55f, 0.05f, 0.05f, -0.6f, -0.6f};
    private static final float[] LANDED_LIFT = {-0.35f, -0.35f, -0.55f, -0.55f, -0.75f, -0.75f};
    private static final float[] FLAP = {0.18f, 0.18f, 0.22f, 0.22f, 0.1f, 0.1f};

    public static final int FACES = 4, WINGS = 6, EYES = 12;

    private ChorusGeometry() {
    }

    // --- wheels and eyes ------------------------------------------------------------------

    public static int wheelOf(int eye) {
        return eye < 5 ? 0 : eye < 9 ? 1 : 2;
    }

    /** The eye's angle around its wheel, as baked into its bone (Bedrock rotation X, radians). */
    public static float eyeTheta(int eye) {
        int w = wheelOf(eye), k = eye - (w == 0 ? 0 : w == 1 ? 5 : 9);
        return FIRST_EYE[w] + (float) (2 * Math.PI * k / WHEEL_EYES[w]);
    }

    /** A wheel's raw bone rotation: it spins on its own axis while that axis slowly wanders. */
    public static Vector3f wheelRot(int wheel, double time, double spin) {
        return new Vector3f((float) spin,
                WHEEL_TILT_Y[wheel] + 0.35f * (float) Math.sin(0.011 * time + wheel * 2.1),
                WHEEL_TILT_Z[wheel] + 0.25f * (float) Math.sin(0.017 * time + wheel * 1.3));
    }

    /** Centre of an eye, relative to the boss's feet, for its wheel's rotation. */
    public static Vec3 eyeOffset(int eye, Vector3f wheelRot) {
        int w = wheelOf(eye);
        Vector3f p = point(0, CORE_Y + WHEEL_RADIUS[w] + EYE_OUT, 0);
        bone(p, 0, CORE_Y, 0, -eyeTheta(eye), 0, 0);
        bone(p, 0, CORE_Y, 0, wheelRot.x, wheelRot.y, wheelRot.z);
        return world(p);
    }

    // --- body: faces and wings -------------------------------------------------------------

    /** The raw Y rotation of the body for a heading (degrees, Minecraft yaw). */
    public static float bodyTurn(float headingDeg) {
        return (float) -Math.toRadians(headingDeg);
    }

    public static Vec3 faceOffset(int face, float headingDeg) {
        float[] c = FACE_CENTRES[face];
        Vector3f p = point(c[0], c[1], c[2]);
        bone(p, 0, CORE_Y, 0, 0, bodyTurn(headingDeg), 0);
        return world(p);
    }

    public static boolean leftWing(int wing) {
        return wing % 2 == 0;
    }

    /**
     * A wing root's raw rotation. {@code landed} (0..1) lays the wings down on the floor,
     * {@code veil} (0..1) folds the top pair forward over the body.
     */
    public static Vector3f wingRot(int wing, double time, float landed, float veil) {
        int level = wing / 2;
        float flap = FLAP[wing] * (1 - landed) * (float) Math.sin(0.12 * time + level);
        float lift = WING_LIFT[wing] + flap;
        lift += (LANDED_LIFT[wing] - WING_LIFT[wing]) * landed;
        float sweep = 0;
        if (level == 0) {
            lift += (0.25f - lift) * veil;
            sweep = 1.25f * veil;
        }
        boolean left = leftWing(wing);
        return new Vector3f(0, left ? -sweep : sweep, left ? -lift : lift);
    }

    public static Vec3 wingOffset(int wing, float headingDeg, Vector3f wingRot) {
        float[] s = SHOULDERS[wing];
        float side = leftWing(wing) ? 1 : -1;
        Vector3f p = point(s[0] + side * WING_REACH[wing], s[1] - WING_DROP, s[2]);
        bone(p, s[0], s[1], s[2], wingRot.x, wingRot.y, wingRot.z);
        bone(p, 0, CORE_Y, 0, 0, bodyTurn(headingDeg), 0);
        return world(p);
    }

    public static Vec3 coreOffset() {
        return new Vec3(0, CORE_Y / PX, 0);
    }

    // --- GeckoLib's transform chain ----------------------------------------------------------

    /** A Bedrock pixel position in GeckoLib's render space (X mirrored, blocks). */
    static Vector3f point(float x, float y, float z) {
        return new Vector3f(-x / 16f, y / 16f, z / 16f);
    }

    /** One bone: rotation about its (Bedrock pixel) pivot, as GeckoLib applies it. */
    static void bone(Vector3f p, float pivotX, float pivotY, float pivotZ, float rx, float ry, float rz) {
        Vector3f pivot = new Vector3f(-pivotX / 16f, pivotY / 16f, pivotZ / 16f);
        p.sub(pivot);
        new Quaternionf().rotationZYX(rz, ry, rx).transform(p);
        p.add(pivot);
    }

    /** Render space to a world offset from the boss's feet: scaled, and turned 180° (yaw 0). */
    static Vec3 world(Vector3f p) {
        return new Vec3(-p.x * MODEL_SCALE, p.y * MODEL_SCALE, -p.z * MODEL_SCALE);
    }
}
