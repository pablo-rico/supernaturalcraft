package org.papiricoh.supernaturalcraft.entity.boss.chuck;

import java.util.List;

/**
 * Every GeckoLib clip of the Author's fight, by short name. THE CONTRACT between the art (tools/artgen/chuck_art.py,
 * chuck_divine_art.py, author_hand_art.py, allies_art.py) and the code: ChuckAssetsTest / AlliesAssetsTest check that
 * every name here exists in the generated files. Clips may be added; none of these may be renamed.
 *
 * <p>No clip may keyframe the bones the renderer drives ({@link ChuckBones#PROCEDURAL}).
 */
public final class ChuckAnimations {

    // --- the man: geo/entity/chuck.geo.json, animations/entity/chuck.animation.json --------------------------
    /** "animation.chuck." + name. Also worn by the Author at home (the NPC in the cabin). */
    public static final String HUMAN = "animation.chuck.";
    /** Loops: standing, walking, seated in his chair, seated typing, standing talking with his hands. */
    public static final List<String> HUMAN_LOOPS = List.of("idle", "walk", "sit", "sit_type", "talk");
    /**
     * One-shots of the man: {@code rise} (gets up from the chair), {@code emerge} (the fight begins: straightens his
     * clothes, the cabin unwrites around him), {@code snap}, {@code point} (narrating at a hunter), {@code shove} and
     * {@code backhand} (melee), {@code throw_glass} (whiskey glass, thrown), {@code type_air} (types on nothing: the keys
     * rain), {@code backspace}, {@code rewrite} (a rule changes), {@code transform_2} (flannel to suit),
     * {@code transform_3} (the man comes apart into light), {@code death} (unused by the man, kept for the base class).
     */
    public static final List<String> HUMAN_TRIGGERED = List.of(
            "rise", "emerge", "snap", "point", "shove", "backhand", "throw_glass", "type_air", "backspace", "rewrite",
            "transform_2", "transform_3", "death");

    // --- the light: geo/entity/chuck_divine.geo.json, animations/entity/chuck_divine.animation.json ------------
    /** "animation.chuck_divine." + name. */
    public static final String DIVINE = "animation.chuck_divine.";
    /** Loops: hovering, drifting. */
    public static final List<String> DIVINE_LOOPS = List.of("idle", "drift");
    /**
     * One-shots of the light: {@code reveal} (first moment as the light), {@code transform_4}, {@code transform_5},
     * {@code snap}, {@code type_rain}, {@code line_sweep}, {@code backspace}, {@code rewrite}, {@code echo_call} (writes
     * an old enemy in ink), {@code narrate}, {@code crack} (the script breaks: a damage window opens), {@code recoil}
     * (a hunter disobeyed), {@code held} (the finale: held by the brothers and the angel, holds), {@code approve}
     * (smiles, lowers his hands), {@code death} (fades into the page).
     */
    public static final List<String> DIVINE_TRIGGERED = List.of(
            "reveal", "transform_4", "transform_5", "snap", "type_rain", "line_sweep", "backspace", "rewrite", "echo_call",
            "narrate", "crack", "recoil", "held", "approve", "death");

    // --- the hands: geo/entity/author_hand.geo.json (a right hand; the left is mirrored) -----------------------
    /** "animation.author_hand." + name; loop {@code idle}. */
    public static final String HAND = "animation.author_hand.";
    public static final List<String> HAND_TRIGGERED = List.of("appear", "slam", "grab", "snap", "sweep", "write", "vanish");

    // --- Dean, Sam and Castiel: geo/entity/hunter_{dean,sam,castiel}.geo.json share one rig and ------------------
    // animations/entity/hunter_ally.animation.json
    /** "animation.hunter_ally." + name; loop {@code idle}. */
    public static final String ALLY = "animation.hunter_ally.";
    /** {@code appear} (steps out of the light), {@code hold} (grips Chuck, holds), {@code talk}, {@code nod}, {@code fade}. */
    public static final List<String> ALLY_TRIGGERED = List.of("appear", "hold", "talk", "nod", "fade");

    private ChuckAnimations() {
    }
}
