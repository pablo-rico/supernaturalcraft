package org.papiricoh.supernaturalcraft.entity.boss.chuck;

/**
 * Where the parts of the Author's light are (pure: no Minecraft types, tested in JUnit). The ONE source of truth for
 * the hitboxes of his ring weak points (server) and the pose of the ring bones (renderer); the model is built to match
 * (ring bones pivot at the core, each node sits on its ring at radius {@link #RING_RADIUS}).
 *
 * <p><b>Frame.</b> Entity-local, in blocks, from his feet: {@code +x} his LEFT (Bedrock +X, as GeckoLib's mirrored
 * X), {@code +y} up, {@code +z} where he faces. {@link #toWorld} turns it into world offsets for a body yaw.
 *
 * <p><b>Rings.</b> Ring {@code r} is a circle of radius {@code RING_RADIUS[r]} in its own XZ plane, spinning about its
 * own Y at {@code SPIN[r]} radians per tick; its plane is tilted by {@code TILT_X[r]} about X, then turned by
 * {@code TILT_Y[r]} about Y (both radians), and centred on the core. Node {@code n} sits at angle
 * {@code spin·t + n·2π/3} from the ring's +X. In GeckoLib terms (verified numerically against the model; GeckoLib's
 * mirrored X and the renderer's 180° yaw turn flip two signs): the renderer sets bone {@code tilt_r} to
 * {@code setRotX(-TILT_X[r])}, {@code setRotY(+TILT_Y[r])} and its child {@code ring_r} to {@code setRotY(-spin(r, t))};
 * node {@code n} is a child of {@code ring_r} built at Bedrock {@code (R cos θ, CORE, -R sin θ)}, θ = n·2π/3.
 */
public final class ChuckGeometry {

    /** The light stands this tall, in blocks (the divine model is drawn {@link #DIVINE_SCALE} times its size). */
    public static final double DIVINE_HEIGHT = 14.0;
    /** Model units to blocks for the divine form: the model is built 4 blocks tall (64 px) and drawn at ×3.5. */
    public static final float DIVINE_SCALE = 3.5f;
    /** Height of the core above his feet, in blocks. */
    public static final double CORE_Y = 8.5;
    public static final double[] RING_RADIUS = {4.5, 5.75, 7.0, 8.25};
    public static final double[] TILT_X = {Math.toRadians(90), Math.toRadians(62), Math.toRadians(-62), Math.toRadians(20)};
    public static final double[] TILT_Y = {0, Math.toRadians(40), Math.toRadians(-40), Math.toRadians(90)};
    /** Radians per tick: opposite neighbours, slower outward. */
    public static final double[] SPIN = {0.030, -0.022, 0.017, -0.012};
    /** The halo's turn, radians per tick. */
    public static final double HALO_SPIN = 0.008;
    /** A node's hitbox, in blocks (a cube). */
    public static final double NODE_SIZE = 1.6;

    private ChuckGeometry() {
    }

    /** The spin of ring {@code ring} at time {@code t} (game time + partial tick). */
    public static double spin(int ring, double t) {
        return SPIN[ring] * t;
    }

    /** Where node {@code node} of ring {@code ring} is at time {@code t}, entity-local, in blocks: {x, y, z}. */
    public static double[] nodeOffset(int ring, int node, double t) {
        double a = spin(ring, t) + node * (Math.PI * 2 / ChuckBones.NODES);
        double r = RING_RADIUS[ring];
        // On the ring, in its own plane.
        double x = r * Math.cos(a), y = 0, z = r * Math.sin(a);
        // Tilt about X.
        double cx = Math.cos(TILT_X[ring]), sx = Math.sin(TILT_X[ring]);
        double y1 = y * cx - z * sx, z1 = y * sx + z * cx;
        // Turn about Y.
        double cy = Math.cos(TILT_Y[ring]), sy = Math.sin(TILT_Y[ring]);
        double x2 = x * cy + z1 * sy, z2 = -x * sy + z1 * cy;
        return new double[]{x2, y1 + CORE_Y, z2};
    }

    /**
     * An entity-local offset {x (left), y, z (forward)} as a world offset {x, y, z} for a body yaw in degrees
     * (Minecraft's: 0 faces south, +z).
     */
    public static double[] toWorld(double[] local, float yawDegrees) {
        double yaw = Math.toRadians(yawDegrees);
        // Forward (-sin, 0, cos); left (cos, 0, sin).
        double fx = -Math.sin(yaw), fz = Math.cos(yaw), lx = Math.cos(yaw), lz = Math.sin(yaw);
        return new double[]{local[0] * lx + local[2] * fx, local[1], local[0] * lz + local[2] * fz};
    }
}
