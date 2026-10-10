package org.papiricoh.supernaturalcraft.entity.boss.naomi;

import java.util.List;

/**
 * The clip names the code plays (v0.18, pure; JUnit checks them against {@code HeavenAssets.NAOMI_CLIPS} and
 * {@code CHAIR_CLIPS}): Naomi's ({@code animation.naomi.<clip>}), her chair's ({@code animation.reprogramming_chair.<clip>}) and
 * her guards' (the Host's rig, {@code animation.host_angel.<clip>}).
 */
public final class NaomiAnimations {

    private NaomiAnimations() {
    }

    public static final String PREFIX = "animation.naomi.";
    /** Loops on the base controller: standing, walking, typing at her console. */
    public static final String IDLE = "idle", WALK = "walk", RECALIBRATE = "recalibrate";
    /** One-shots on {@code action}. */
    public static final String PALM_STRIKE = "palm_strike", RESTRAINT = "restraint", STRAP_IN = "strap_in", DRILL_LANCE = "drill_lance",
            WIPE = "wipe", CALL_GUARDS = "call_guards", TEST = "test", STAGGER = "stagger", DEATH = "death", EMERGE = "emerge";

    public static final List<String> LOOPS = List.of(IDLE, WALK, RECALIBRATE);
    public static final List<String> TRIGGERED = List.of(PALM_STRIKE, RESTRAINT, STRAP_IN, DRILL_LANCE, WIPE, CALL_GUARDS, TEST,
            STAGGER, DEATH, EMERGE);
    /** Clips that hold their last frame. */
    public static final List<String> HOLDS = List.of(DEATH, EMERGE);
    /** Clips during which her hand drill is out (the synced DRILL flag; the renderer shows the {@code drill} bone). */
    public static final List<String> DRILL_CLIPS = List.of(DRILL_LANCE);

    public static final String CHAIR_PREFIX = "animation.reprogramming_chair.";
    public static final String CHAIR_IDLE = "idle", CHAIR_STRAP = "strap", CHAIR_DRILL_DOWN = "drill_down", CHAIR_RELEASE = "release";
    public static final List<String> CHAIR_TRIGGERED = List.of(CHAIR_STRAP, CHAIR_DRILL_DOWN, CHAIR_RELEASE);

    /** Her guards wear the Host's rig: its clips. */
    public static final String HOST_PREFIX = "animation.host_angel.";
    public static final String HOST_IDLE = "idle", HOST_MARCH = "march", HOST_SLASH = "slash", HOST_DIE = "die";
}
