package org.papiricoh.supernaturalcraft.entity.boss.michael;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Michael's clips by short name, as agreed with the art (michael contract): the vessel's ({@code animation.michael.}),
 * the true form's ({@code animation.michael_archangel.}) and the Host's ({@code animation.host_angel.}). Kept apart from
 * the entities so JUnit can check the generated files against them.
 */
public final class MichaelAnimations {

    public static final String VESSEL = "animation.michael.", ARCHANGEL = "animation.michael_archangel.",
            HOST = "animation.host_angel.";

    public static final List<String> VESSEL_CLIPS = List.of("idle", "walk", "run", "intro", "transition", "blade_combo_1",
            "blade_combo_2", "blade_combo_3", "forehead_touch", "smite", "parried", "ask_yes", "command", "summon_host",
            "shadow_wings_unfurl", "lance_throw", "lance_recall", "lance_thrust", "lance_sweep", "takeoff", "hover", "dive",
            "land", "wing_buffet", "feather_storm", "transform", "death");
    public static final List<String> ARCHANGEL_CLIPS = List.of("idle", "walk", "emerge", "transition", "lance_sweep_big",
            "lance_thrust", "lance_throw", "lance_recall", "halo_volley", "feather_storm", "wing_buffet", "command", "smite",
            "ask_yes", "roar", "stagger", "takeoff", "hover", "dive", "land", "death");
    public static final List<String> HOST_CLIPS = List.of("idle", "march", "shield_wall", "charge", "slash", "block",
            "disordered", "die");

    /** Loop rather than play once. */
    public static final List<String> LOOPS = List.of("idle", "walk", "run", "hover", "march", "shield_wall", "disordered");
    /** Played once and held on their last frame. */
    public static final List<String> HOLDS = List.of("death", "die", "transform");

    /**
     * What {@code LuciferEntity}'s own triggers mean for him ({@code emerge}, {@code transform_<n>}, {@code death}), by
     * form. A clip his current form lacks is dropped.
     */
    public static final Map<String, String> VESSEL_ALIAS = Map.of("emerge", "intro", "transform_2", "transition",
            "transform_3", "shadow_wings_unfurl", "transform_4", "takeoff", "transform_5", "transform");
    public static final Map<String, String> ARCHANGEL_ALIAS = Map.of("transform_5", "emerge", "transform_6", "roar");

    private MichaelAnimations() {
    }

    /** The one-shot clips of a form (everything but its loops). */
    public static List<String> triggered(List<String> clips) {
        List<String> out = new ArrayList<>();
        for (String c : clips) if (!LOOPS.contains(c)) out.add(c);
        return out;
    }
}
