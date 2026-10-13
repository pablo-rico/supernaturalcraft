package org.papiricoh.supernaturalcraft.client.flight;

/**
 * The body of someone flying on wings (pure, v0.20): fed once a tick with how they move, it eases between two poses and
 * gives the angles for a frame. Hovering, the body leans a little forward and bobs with the wingbeat, legs dangling and
 * swaying out of step, arms loose and a little out. Cruising, the body lies along the flight like a swimmer's (steeper in
 * a dive, more upright climbing), legs trailing together, arms swept back along the sides, banking into turns. The head
 * turns back up so the eyes keep looking where the player looks.
 * <p>
 * Angles in degrees: {@code bodyPitch} forward, {@code bodyRoll} toward the turn, limbs' {@code back} swinging a hanging
 * limb backward and {@code out} away from the body; {@code rise} in model pixels.
 */
public final class FlightPose {

    /** Ticks of one wingbeat (the bob and the sway ride on it). */
    public static final float BEAT_TICKS = 20f;
    /** Horizontal speeds (blocks a tick) where cruising begins and where it is full. */
    public static final float CRUISE_FROM = 0.08f, CRUISE_FULL = 0.5f;
    /** Vertical speed (blocks a tick) that counts as a full climb or dive. */
    public static final float CLIMB_FULL = 0.4f;
    /** Yaw turned in a tick (degrees) that banks fully. */
    public static final float BANK_FULL = 8f;
    public static final float HOVER_PITCH = 10f, CRUISE_PITCH = 72f, CLIMB_PITCH = 22f, MAX_PITCH = 95f, MAX_ROLL = 28f;
    /** How much of the body's lean the head takes back (it cannot bend back further than {@link #NECK}). */
    public static final float HEAD_TAKES_BACK = 0.85f, NECK = 75f;

    /** One frame's pose; {@code blend} 0 is the player's own (walking) pose, 1 is all flight. */
    public record Frame(float blend, float bodyPitch, float bodyRoll, float rise, float headPitch,
                        float armBack, float armOut, float rightLegBack, float leftLegBack, float legOut) {
        public static final Frame REST = new Frame(0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    private float blend, lean, climb, bank, time;
    private float blend0, lean0, climb0, bank0, time0;

    /** One tick: {@code flying} on wings, moving {@code horizontal}/{@code vertical} blocks, having turned {@code yawDelta}°. */
    public void tick(boolean flying, double horizontal, double vertical, float yawDelta) {
        blend0 = blend;
        lean0 = lean;
        climb0 = climb;
        bank0 = bank;
        time0 = time;
        time += 1;
        blend = approach(blend, flying ? 1 : 0, 0.2f);
        float cruise = clamp((float) ((horizontal - CRUISE_FROM) / (CRUISE_FULL - CRUISE_FROM)), 0, 1);
        lean = approach(lean, flying ? cruise : 0, 0.15f);
        climb = approach(climb, flying ? clamp((float) (vertical / CLIMB_FULL), -1, 1) : 0, 0.15f);
        bank = approach(bank, flying ? clamp(wrap(yawDelta) / BANK_FULL, -1, 1) : 0, 0.12f);
    }

    /** Whether there is anything to show (the pose is still easing out after landing). */
    public boolean active() {
        return blend > 0.001f || blend0 > 0.001f;
    }

    public Frame frame(float partial) {
        float b = lerp(blend0, blend, partial);
        if (b <= 0.001f) return Frame.REST;
        float l = lerp(lean0, lean, partial), c = lerp(climb0, climb, partial), k = lerp(bank0, bank, partial);
        float t = lerp(time0, time, partial);
        float beat = (float) (t * Math.PI * 2 / BEAT_TICKS);
        float hover = 1 - l;

        float pitch = lerp(HOVER_PITCH - 8 * c, CRUISE_PITCH - CLIMB_PITCH * c, l) + 2 * (float) Math.sin(beat + Math.PI / 2) * hover;
        pitch = clamp(pitch, -10, MAX_PITCH);
        float roll = k * MAX_ROLL * l;
        // The wings pull on the downstroke: the body lifts with it, less so at speed.
        float rise = (float) Math.sin(beat) * (1 - 0.6f * l);
        float head = -Math.min(NECK, Math.max(0, pitch) * HEAD_TAKES_BACK);

        float sway = (float) Math.sin(beat * 0.5f), sway2 = (float) Math.sin(beat * 0.5f + 1.6f);
        float flutter = (float) Math.sin(beat), flutter2 = (float) Math.sin(beat + 0.8f);
        float armBack = lerp(14 + 4 * (float) Math.sin(beat * 0.5f + 0.5f), 18, l);
        float armOut = lerp(14, 6, l) + 4 * Math.abs(k) * l;
        float rightLeg = lerp(10 + 7 * sway, 4 + 2 * flutter, l) + 6 * Math.max(0, c) * hover;
        float leftLeg = lerp(16 + 7 * sway2, 6 + 2 * flutter2, l) + 6 * Math.max(0, c) * hover;
        float legOut = lerp(3, 1, l);
        return new Frame(b, pitch * b, roll * b, rise * b, head, armBack, armOut, rightLeg, leftLeg, legOut);
    }

    static float approach(float from, float to, float rate) {
        return from + (to - from) * rate;
    }

    static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    /** A yaw step brought into -180..180 (a turn through north is not a full circle). */
    static float wrap(float deg) {
        float d = deg % 360;
        if (d >= 180) d -= 360;
        if (d < -180) d += 360;
        return d;
    }
}
