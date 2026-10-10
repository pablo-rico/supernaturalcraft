package org.papiricoh.supernaturalcraft.entity.boss.naomi;

import org.papiricoh.supernaturalcraft.entity.boss.naomi.arena.ReprogrammingRoomLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Naomi's numbers, kept apart from the entity so they can be tested without a world (v0.18). Two phases, half her true health
 * each (30 000 alone on the power curve, {@code ProgressionScale}): the clinic, then the red lights. Shares are of her true max
 * health, never fixed points (the v0.15 rule). Damage numbers are before her multiplier ({@code Balance.bossDamage} × config);
 * her guards' and copies' are a minion's, off the curve. The chair's struggle is {@link ChairRules}.
 */
public final class NaomiBalance {

    public static final int PHASES = 2;

    private NaomiBalance() {
    }

    /** Health fraction below which {@code phase} ends (phase 1 at a half). */
    public static float threshold(int phase) {
        return phase >= PHASES ? 0f : 0.5f;
    }

    /** Ticks between her attacks in {@code phase}: the red lights hurry her. */
    public static int attackGap(int phase) {
        return phase <= 1 ? 36 : 26;
    }

    /** Her arena: the room and a margin round it. */
    public static final int ARENA_RADIUS = ReprogrammingRoomLayout.RADIUS + 2;
    /** Her arrival (the emerge clip), her change of phase and her fall. */
    public static final int EMERGE_TICKS = 60, TRANSITION_TICKS = 80, DEATH_TICKS = 120;
    /** Clip cue: her light goes out this far into the death clip. */
    public static final int DEATH_LIGHT = 64;

    // --- 1 · Palm of Correction ---------------------------------------------------------------------------------------
    /** A gold circle just ahead of her: a heavy open-handed blow. A raised shield parries it and she staggers. */
    public static final int PALM_WINDUP = 20;
    public static final float PALM_AHEAD = 1.6f, PALM_RADIUS = 2.4f, PALM_DAMAGE = 15f, PALM_SHOVE = 1.4f;
    /** A parried palm (or a broken console) leaves her reeling this long, open to more. */
    public static final int STAGGER_TICKS = 50;
    public static final float STAGGER_VULNERABILITY = 1.3f;

    // --- 2 · Restraint Field --------------------------------------------------------------------------------------------
    /** A gold ring round the hunter: whoever is still inside when it closes is held. */
    public static final int RESTRAINT_WINDUP = 20, RESTRAINT_HOLD = 40;
    public static final float RESTRAINT_RADIUS = 4.5f, RESTRAINT_DAMAGE = 5f;

    // --- 3 · Strap In ---------------------------------------------------------------------------------------------------
    /** A gold line from a chair to the most isolated hunter: still on it when it closes, they are strapped in. The clip's point lands
     * {@link #STRAP_CUE} ticks in, so it is started that long before the windup ends. */
    public static final int STRAP_WINDUP = 30, STRAP_CUE = 20;
    /** Call the Guards: the clip's gesture lands this far in. */
    public static final int GUARDS_WINDUP = 16;
    public static final float STRAP_HALF_WIDTH = 1.1f;
    /** An angel of at least this rank is strapped first ("You are one of ours. Sit."). */
    public static final int STRAP_ANGEL_RANK = 2;

    /**
     * The most isolated of some hunters (flat positions {@code xz[i] = {x, z}}): the one whose nearest other hunter is
     * farthest. -1 for none; a lone hunter is 0.
     */
    public static int mostIsolated(double[][] xz) {
        int best = -1;
        double bestGap = -1;
        for (int i = 0; i < xz.length; i++) {
            double nearest = Double.MAX_VALUE;
            for (int j = 0; j < xz.length; j++) {
                if (i == j) continue;
                double dx = xz[i][0] - xz[j][0], dz = xz[i][1] - xz[j][1];
                nearest = Math.min(nearest, dx * dx + dz * dz);
            }
            if (nearest > bestGap) {
                bestGap = nearest;
                best = i;
            }
        }
        return best;
    }

    /**
     * Whether a point lies on a lane from ({@code ax}, {@code az}) to ({@code bx}, {@code bz}) (flat): within {@code halfWidth}
     * of the segment, from its start to a step past its end.
     */
    public static boolean onLane(double px, double pz, double ax, double az, double bx, double bz, double halfWidth) {
        double vx = bx - ax, vz = bz - az, len2 = vx * vx + vz * vz;
        if (len2 < 1e-6) return (px - ax) * (px - ax) + (pz - az) * (pz - az) <= halfWidth * halfWidth;
        double len = Math.sqrt(len2);
        double along = ((px - ax) * vx + (pz - az) * vz) / len;
        if (along < -0.5 || along > len + 1.0) return false;
        double cx = ax + vx / len * along, cz = az + vz / len * along;
        double dx = px - cx, dz = pz - cz;
        return dx * dx + dz * dz <= halfWidth * halfWidth;
    }

    // --- 4 · Drill Lance ------------------------------------------------------------------------------------------------
    /** A red cone before her and a lunge with the drill; in phase 2 three in a row, each shown first. */
    public static final int LANCE_WINDUP = 15, LANCE_GAP = 16, LANCE_WARN = 15;
    public static final float LANCE_REACH = 4f, LANCE_HALF_ANGLE = 35f, LANCE_DAMAGE = 12f, LANCE_STEP = 1.1f;
    /** The drill leaves a wound that bleeds. */
    public static final int BLEED_TICKS = 100, BLEED_LEVEL = 1;
    /** Against a demon her drill bites deeper (the lance and the chair). */
    public static final float DEMON_DRILL = 1.15f;

    /** Lunges in one Drill Lance. */
    public static int lunges(int phase) {
        return phase >= 2 ? 3 : 1;
    }

    /** Ticks of the Drill Lance's active stage: the first lunge at once, each next one {@link #LANCE_GAP} later. */
    public static int lanceActive(int phase) {
        return 1 + (lunges(phase) - 1) * LANCE_GAP;
    }

    // --- 5 · Memory Wipe ------------------------------------------------------------------------------------------------
    /** A white ring that sweeps out from her; green lanes are the gaps in it. */
    public static final int WIPE_WINDUP = 30, WIPE_TICKS = 20, WIPE_RADIUS = 14, WIPE_GAPS = 3;
    public static final float WIPE_DAMAGE = 7f, WIPE_GAP_HALF_WIDTH = 1.4f;
    /** Caught by it: the screen goes white this long, and slowness this long. */
    public static final int WHITEOUT_TICKS = 40, WIPE_SLOW_TICKS = 60;

    /** How far the wipe has swept {@code t} ticks into its active stage. */
    public static double wipeRadius(int t) {
        double f = Math.max(0, Math.min(1, t / (double) WIPE_TICKS));
        return 1 + (WIPE_RADIUS - 1) * f;
    }

    /** The gaps' directions (yaw degrees, Minecraft's: 0 = south, 90 = west), evenly spread from {@code offset}. */
    public static List<Float> wipeGaps(int count, float offset) {
        List<Float> out = new ArrayList<>();
        for (int i = 0; i < count; i++) out.add((offset + i * 360f / count) % 360f);
        return out;
    }

    /**
     * Whether a point ({@code dx}, {@code dz} from her) stands in one of the gaps: on the green lane drawn out from her along a
     * gap's yaw, {@code halfWidth} either side.
     */
    public static boolean inGap(double dx, double dz, List<Float> gaps, double halfWidth) {
        for (float yaw : gaps) {
            double r = Math.toRadians(yaw);
            double ux = -Math.sin(r), uz = Math.cos(r);
            double along = dx * ux + dz * uz;
            if (along < 0.5) continue;
            double across = Math.abs(dx * uz - dz * ux);
            if (across <= halfWidth) return true;
        }
        return false;
    }

    // --- 6 · Call the Guards --------------------------------------------------------------------------------------------
    /** Guards she calls (a minion's health, off the curve). */
    public static final float GUARD_HEALTH = 60f, GUARD_DAMAGE = 6f;
    public static final int GUARD_SWING = 24;
    /** While at least {@link #WARD_GUARDS} of them stand, she takes {@link #GUARD_WARD} of every blow. */
    public static final int WARD_GUARDS = 2;
    public static final float GUARD_WARD = 0.6f;

    public static int guards(int phase) {
        return phase >= 2 ? 3 : 2;
    }

    /** What a blow is worth on her with {@code standing} guards up. */
    public static float ward(int standing) {
        return standing >= WARD_GUARDS ? GUARD_WARD : 1f;
    }

    // --- 7 · Training Test ----------------------------------------------------------------------------------------------
    /** Kneeling friends and hostile shapes; kill the hostiles without touching a kneeler, within the time. */
    public static final int TEST_WINDUP = 20, TEST_TICKS = 300;
    /** Killing a kneeler: conditioned this long, and she heals this share. */
    public static final int KNEELER_CONDITIONED = 200;
    public static final float KNEELER_HEAL = 0.01f;
    /** Passing ("Unexpected result."): she is stunned this long and takes this much more. */
    public static final int PASS_STUN = 60;
    public static final float PASS_VULNERABILITY = 1.4f;
    /** A copy's health and blow (minions, off the curve). */
    public static final float COPY_HEALTH = 20f, COPY_DAMAGE = 5f;

    public static int kneelers(int phase) {
        return phase >= 2 ? 3 : 2;
    }

    public static int hostiles(int phase) {
        return 3;
    }

    /** Whether a test passes: every hostile down, no kneeler touched, before the end. */
    public static boolean testPassed(int hostilesLeft, boolean kneelerTouched, long now, long deadline) {
        return hostilesLeft == 0 && !kneelerTouched && now <= deadline;
    }

    // --- 8 · Recalibration ----------------------------------------------------------------------------------------------
    /** She steps to her console and heals this share a second, up to {@link #RECAL_CAP}, until {@link #CONSOLE_HITS} blows land on it. */
    public static final int RECAL_WINDUP = 20, CONSOLE_HITS = 6, CONSOLE_HIT_COOLDOWN = 4;
    public static final float RECAL_PER_SECOND = 0.004f, RECAL_CAP = 0.05f;
    /** The longest a recalibration can last (its cap, at its rate). */
    public static final int RECAL_TICKS = (int) Math.ceil(RECAL_CAP / RECAL_PER_SECOND) * 20 + 1;
    /** She only recalibrates when she has lost at least this share of her phase. */
    public static final float RECAL_WORTH = 0.1f;

    /** True health one second at the console heals, given what this recalibration has healed so far ({@code healed}). */
    public static float recalibrationSecond(float trueMax, float healed) {
        float cap = RECAL_CAP * trueMax;
        return Math.max(0, Math.min(RECAL_PER_SECOND * trueMax, cap - healed));
    }

    // --- conditioning ---------------------------------------------------------------------------------------------------
    /** A conditioned hunter's blows on any angel (her included) land at this. */
    public static final float CONDITIONED_FACTOR = 0.6f;
}
