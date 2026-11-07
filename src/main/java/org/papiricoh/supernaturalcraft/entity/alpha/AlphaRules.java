package org.papiricoh.supernaturalcraft.entity.alpha;

import org.papiricoh.supernaturalcraft.balance.ProgressionScale;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression;
import org.papiricoh.supernaturalcraft.pact.PactRules;

import java.util.List;

/**
 * The Alphas' rules (v0.39), pure: when one comes for a hunter on Crowley's errand, with how many of its kind, how tough it is and
 * how hard it hits. Their health is counted in blows of the real Angel Blade at Death's tier (the season-6 road comes right after
 * him): a hunter there needs a real fight. Tested in JUnit ({@code AlphaRulesTest}); {@code AlphaSpawns} applies them.
 */
public final class AlphaRules {

    private AlphaRules() {
    }

    /** How often (ticks) each hunter on the errand is checked at night, and the chance an Alpha comes at each check. */
    public static final int CHECK_TICKS = 200;
    public static final float CHANCE = 0.015f;
    /** At most this many Alphas alive for one hunter. */
    public static final int MAX_ALIVE = 1;
    /** How far from the hunter it steps out of the dark (blocks). */
    public static final int MIN_DISTANCE = 24, MAX_DISTANCE = 40;
    /** Hunters this near when it falls (and hunting it) share the kill for Crowley. */
    public static final double SLAIN_REACH = 32;
    /** The boss bar shows to hunters this near. */
    public static final double BAR_REACH = 40;
    /** Without its quarry near for this long by day, it goes back into the dark. */
    public static final int LEAVE_TICKS = 60 * 20, LEAVE_REACH = 64;

    /** The power tier they are tuned for: Raphael's (season 6, where they are hunted). */
    public static int tier() {
        return ProgressionScale.of(BossProgression.Boss.RAPHAEL).tier(); // v0.40: Death went down to tier 3; Raphael keeps 4
    }

    /** How many blows of the ascended Angel Blade each takes. */
    public static int hits(PactRules.Alpha a) {
        return switch (a) {
            case VAMPIRE -> 30;
            case WEREWOLF -> 34;
            case SHAPESHIFTER -> 26;
        };
    }

    /**
     * Max health in vanilla points (what the bar shows as a proportion). An ascended blade hits for hundreds, far past the
     * attribute's cap, so every blow is scaled down by {@link #damageScale} before it lands: it takes {@link #hits} real blows.
     */
    public static float health(PactRules.Alpha a) {
        return switch (a) {
            case VAMPIRE -> 300f;
            case WEREWOLF -> 340f;
            case SHAPESHIFTER -> 260f;
        };
    }

    /** What a blow is multiplied by before it lands on Alpha {@code a}: so many ascended Angel Blade blows bring it down. */
    public static float damageScale(PactRules.Alpha a) {
        return health(a) / (hits(a) * ProgressionScale.bladeHit(tier()));
    }

    /** Its melee blow. */
    public static float melee(PactRules.Alpha a) {
        return switch (a) {
            case VAMPIRE -> 12f;
            case WEREWOLF -> 14f;
            case SHAPESHIFTER -> 10f;
        };
    }

    /** How many of its kind come with it: {@code roll} 0..1. */
    public static int pack(PactRules.Alpha a, double roll) {
        int base = switch (a) {
            case VAMPIRE, WEREWOLF -> 2;
            case SHAPESHIFTER -> 1;
        };
        return base + (roll < 0.4 ? 1 : 0);
    }

    /** Whether an Alpha comes now: {@code roll} 0..1, at night, with fewer than {@link #MAX_ALIVE} alive for the hunter. */
    public static boolean comes(double roll, boolean night, int alive) {
        return night && alive < MAX_ALIVE && roll < CHANCE;
    }

    /** Which of the Alphas still hunting the hunter comes ({@code roll} 0..1), or null if none is. */
    public static PactRules.Alpha pick(List<PactRules.Alpha> hunting, double roll) {
        if (hunting.isEmpty()) return null;
        return hunting.get(Math.min(hunting.size() - 1, (int) (roll * hunting.size())));
    }

    // --- signature moves --------------------------------------------------------------------------------------------------
    /** The vampire's lunge: from this far, every so often, landing this blow. */
    public static final double LUNGE_MIN = 5, LUNGE_MAX = 14;
    public static final int LUNGE_COOLDOWN = 140, LUNGE_WINDUP = 8;
    public static final float LUNGE_DAMAGE = 14f;
    /** His draining bite: this much a beat for three beats, healing him as much. */
    public static final int DRAIN_COOLDOWN = 200, DRAIN_BEATS = 3, DRAIN_EVERY = 10;
    public static final float DRAIN_DAMAGE = 5f;
    /** He commands the vampires near (and calls one more if few are left). */
    public static final int COMMAND_COOLDOWN = 400;
    public static final double COMMAND_REACH = 24;
    /** The werewolf's leap: from this far, landing in a ring of this radius. */
    public static final double LEAP_MIN = 6, LEAP_MAX = 16;
    public static final int LEAP_COOLDOWN = 120;
    public static final float LEAP_DAMAGE = 12f, LEAP_RADIUS = 3f;
    /** His slam, in reach: a ring round him. */
    public static final int SLAM_COOLDOWN = 160;
    public static final float SLAM_DAMAGE = 16f, SLAM_RADIUS = 3.5f;
    /** His howl calls a pack (at most so many of his kind about him). */
    public static final int HOWL_COOLDOWN = 500, HOWL_PACK = 2, PACK_CAP = 5;
    /** The shapeshifter takes a hunter's look for this long (and his kin near wear it too). */
    public static final int STEAL_COOLDOWN = 300, STEAL_TICKS = 200;
    public static final double STEAL_REACH = 6;
    /** He sloughs his skin to mend: this share of his health, once in a while, when hurt. */
    public static final int SLOUGH_COOLDOWN = 400;
    public static final float SLOUGH_HEAL = 0.15f, SLOUGH_BELOW = 0.6f;
    /** His grab. */
    public static final int GRAB_COOLDOWN = 120;
    public static final float GRAB_DAMAGE = 12f;
}
