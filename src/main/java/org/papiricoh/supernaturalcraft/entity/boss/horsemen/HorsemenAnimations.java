package org.papiricoh.supernaturalcraft.entity.boss.horsemen;

import java.util.ArrayList;
import java.util.List;

/**
 * The Horsemen's clips by short name ({@code "animation.<id>." + name}), as agreed with the art (horsemen contract).
 * Kept apart from the entities so JUnit can check the generated files against them.
 */
public final class HorsemenAnimations {

    /** Every Horseman has these. */
    public static final List<String> COMMON = List.of("idle", "walk", "intro", "transition", "death", "mount",
            "mounted_idle", "mounted_gallop", "mounted_charge");
    /** Loop rather than play once. */
    public static final List<String> LOOPS = List.of("idle", "walk", "mounted_idle", "mounted_gallop", "wheel_idle", "wheel_roll", "drain");

    public static final List<String> WAR = List.of("sword_combo", "sword_heavy", "parried", "rage_roar", "illusion_cast", "mounted_sweep");
    public static final List<String> FAMINE = List.of("wheel_idle", "wheel_roll", "stand_up", "devour", "grab", "drain", "mounted_grab");
    public static final List<String> PESTILENCE = List.of("cough", "cough_cone", "swarm_call", "cloud_cast", "mounted_spray");
    public static final List<String> DEATH = List.of("cane_strike", "scythe_reap", "scythe_throw", "summon_reapers", "shadow_step",
            "world_flip", "mounted_reap");

    public static final List<String> REAPER = List.of("idle", "walk", "attack");
    public static final List<String> STEED = List.of("idle", "walk", "gallop", "rear");
    public static final List<String> WAR_STANDARD = List.of("idle");

    /** Bones the code shows and hides: no clip may key them. */
    public static final String STEED_BONE = "steed", WHEELCHAIR_BONE = "wheelchair", CANE_BONE = "cane", SCYTHE_BONE = "scythe",
            SADDLE_BONE = "saddle", SWORD_BONE = "sword", RIDER_BONE = "rider";

    private HorsemenAnimations() {
    }

    /** One Horseman's own clips (besides the common ones). */
    public static List<String> own(String id) {
        return switch (id) {
            case "war" -> WAR;
            case "famine" -> FAMINE;
            case "pestilence" -> PESTILENCE;
            default -> DEATH;
        };
    }

    /** Every clip a Horseman's model must have. */
    public static List<String> all(String id) {
        List<String> out = new ArrayList<>(COMMON);
        out.addAll(own(id));
        return out;
    }

    /** The clips played once, on demand (the common one-shots plus his own, minus his loops). */
    public static List<String> triggered(String id) {
        List<String> out = new ArrayList<>();
        for (String c : all(id)) if (!LOOPS.contains(c)) out.add(c);
        return out;
    }
}
