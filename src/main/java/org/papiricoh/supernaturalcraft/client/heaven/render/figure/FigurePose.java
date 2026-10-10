package org.papiricoh.supernaturalcraft.client.heaven.render.figure;

import java.util.Locale;
import java.util.Map;

/**
 * The poses a figure of a memory (or a training copy) can hold (v0.18, pure): rotations added to a humanoid's limbs, how far the
 * whole figure sinks, and whether it lies on the ground. Angles are radians in GeckoLib's runtime convention (as
 * {@code GeoBone.setRotX} takes them): {@code +x} swings a hanging limb forward and tilts a head up, {@code +z} swings a hanging
 * limb toward the figure's right. Vanilla model parts use the opposite sign for x ({@link #vanillaX}).
 * <p>Names come from the scenes ({@code memory.scenes.Figure#pose}); unknown names stand.
 *
 * @param body       the torso (forward lean is negative)
 * @param head       the head (looking down is negative)
 * @param rightArm   the right arm
 * @param leftArm    the left arm
 * @param rightLeg   the right leg, whole (thigh, if the rig has shins)
 * @param leftLeg    the left leg
 * @param rightShin  the right shin (rigs that have one; otherwise the leg keeps {@code rightLeg})
 * @param leftShin   the left shin
 * @param thighs     the legs' rotation instead of {@code rightLeg}/{@code leftLeg} on rigs with shins (null: the same)
 * @param sink       blocks the whole figure is lowered (kneeling, sitting)
 * @param sinkShins  blocks lowered instead when the rig has shins
 * @param lying      lies on its back (fallen, dead)
 */
public record FigurePose(Rot body, Rot head, Rot rightArm, Rot leftArm, Rot rightLeg, Rot leftLeg, Rot rightShin, Rot leftShin,
                         Rot thighs, float sink, float sinkShins, boolean lying) {

    /** A rotation in radians (x, y, z). */
    public record Rot(float x, float y, float z) {
        public static final Rot NONE = new Rot(0, 0, 0);

        public boolean none() {
            return x == 0 && y == 0 && z == 0;
        }
    }

    private static Rot r(float x, float y, float z) {
        return new Rot(x, y, z);
    }

    private static final Rot O = Rot.NONE;

    public static final FigurePose STAND = new FigurePose(O, O, O, O, O, O, O, O, null, 0, 0, false);
    /** On both knees, head bowed (the kneeling copies of Naomi's test, a prayer). */
    public static final FigurePose KNEEL = new FigurePose(r(-0.12f, 0, 0), r(-0.45f, 0, 0), r(0.15f, 0, 0.12f), r(0.15f, 0, -0.12f),
            r(-1.45f, 0, 0.05f), r(-1.45f, 0, -0.05f), r(-1.5f, 0, 0), r(-1.5f, 0, 0), r(0.05f, 0, 0), 0.62f, 0.36f, false);
    /** Sitting on the ground, legs out in front. */
    public static final FigurePose SIT = new FigurePose(r(-0.05f, 0, 0), O, r(0.35f, 0, 0.1f), r(0.35f, 0, -0.1f),
            r(1.45f, 0.12f, 0), r(1.45f, -0.12f, 0), O, O, null, 0.62f, 0.62f, false);
    /** A blow coming down: the right arm high, the body turned into it. */
    public static final FigurePose STRIKE = new FigurePose(r(-0.1f, 0.35f, 0), r(0.05f, -0.2f, 0), r(2.7f, 0, 0.25f), r(-0.4f, 0, -0.3f),
            r(0.35f, 0, 0), r(-0.3f, 0, 0), r(-0.2f, 0, 0), r(-0.3f, 0, 0), null, 0, 0, false);
    /** Fallen: on its back, arms loose. */
    public static final FigurePose FALLEN = new FigurePose(O, r(0.2f, 0.4f, 0), r(0, 0, 0.6f), r(0, 0, -0.9f), r(0, 0, 0.12f), r(0, 0, -0.08f),
            O, O, null, 0, 0, true);
    /** Holding something out with both hands (a deal, an offering). */
    public static final FigurePose OFFER = new FigurePose(O, r(-0.15f, 0, 0), r(1.25f, -0.15f, 0), r(1.25f, 0.15f, 0), O, O, O, O,
            null, 0, 0, false);
    /** Pointing ahead with the right arm. */
    public static final FigurePose POINT = new FigurePose(r(0, 0.15f, 0), O, r(1.55f, 0, 0), O, O, O, O, O, null, 0, 0, false);
    /** Both hands together in front, head bowed. */
    public static final FigurePose PRAY = new FigurePose(O, r(-0.35f, 0, 0), r(0.95f, 0, -0.42f), r(0.95f, 0, 0.42f), O, O, O, O,
            null, 0, 0, false);
    /** Both arms up (a summoning, a triumph). */
    public static final FigurePose RAISE = new FigurePose(O, r(0.3f, 0, 0), r(2.9f, 0, 0.35f), r(2.9f, 0, -0.35f), O, O, O, O, null, 0, 0, false);
    /** Crouched low, an arm over the head. */
    public static final FigurePose COWER = new FigurePose(r(-0.45f, 0, 0), r(-0.3f, 0, 0), r(2.2f, 0, -0.6f), r(0.6f, 0, -0.2f),
            r(0.9f, 0, 0), r(0.9f, 0, 0), r(-1.6f, 0, 0), r(-1.6f, 0, 0), null, 0.35f, 0.3f, false);

    private static final Map<String, FigurePose> BY_NAME = Map.ofEntries(
            Map.entry("stand", STAND), Map.entry("idle", STAND),
            Map.entry("kneel", KNEEL), Map.entry("kneeling", KNEEL),
            Map.entry("sit", SIT), Map.entry("seated", SIT),
            Map.entry("strike", STRIKE), Map.entry("attack", STRIKE),
            Map.entry("fallen", FALLEN), Map.entry("dead", FALLEN), Map.entry("lie", FALLEN),
            Map.entry("offer", OFFER), Map.entry("give", OFFER),
            Map.entry("point", POINT),
            Map.entry("pray", PRAY),
            Map.entry("raise", RAISE), Map.entry("triumph", RAISE), Map.entry("summon", RAISE),
            Map.entry("cower", COWER), Map.entry("crouch", COWER));

    /** The pose by its name (case ignored); unknown or empty names stand. */
    public static FigurePose of(String name) {
        if (name == null) return STAND;
        return BY_NAME.getOrDefault(name.trim().toLowerCase(Locale.ROOT), STAND);
    }

    /** Whether the named pose is one of the low ones (kneeling, sitting, crouched): pets sit for these. */
    public static boolean low(String name) {
        FigurePose p = of(name);
        return p == KNEEL || p == SIT || p == COWER;
    }

    /** The right leg's rotation on a rig with ({@code shins}) or without shins. */
    public Rot rightLeg(boolean shins) {
        return shins && thighs != null ? new Rot(thighs.x(), thighs.y(), thighs.z() + rightLeg.z()) : rightLeg;
    }

    public Rot leftLeg(boolean shins) {
        return shins && thighs != null ? new Rot(thighs.x(), thighs.y(), thighs.z() + leftLeg.z()) : leftLeg;
    }

    /** A vanilla model part's x rotation for one of these (the opposite sign). */
    public static float vanillaX(Rot r) {
        return -r.x();
    }
}
