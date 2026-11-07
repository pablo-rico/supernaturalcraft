package org.papiricoh.supernaturalcraft.entity.alpha;

import java.util.List;
import java.util.Map;

/**
 * The contract between the Alphas' art ({@code tools/artgen/alphas_art.py}) and their code (v0.39): files, clips and bones by
 * creature id ({@code alpha_vampire}, {@code alpha_werewolf}, {@code alpha_shapeshifter} = entity id = file name). Pure;
 * {@code AlphaAssetsTest} checks the files.
 *
 * <p>Each Alpha keeps the clip names its kind's code already triggers ({@code VampireEntity}: bite, hiss, fangs_out, rise,
 * decapitated; {@code WerewolfEntity}: turn, claw, pounce, howl, flee; {@code ShapeshifterEntity}: attack, revealed, shed) and
 * adds its own.
 */
public final class AlphaAssets {

    private AlphaAssets() {
    }

    public static final List<String> CREATURES = List.of("alpha_vampire", "alpha_werewolf", "alpha_shapeshifter");

    public static String geo(String id) {
        return "geo/entity/" + id + ".geo.json";
    }

    public static String texture(String id) {
        return "textures/entity/" + id + ".png";
    }

    public static String glowmask(String id) {
        return "textures/entity/" + id + "_glowmask.png";
    }

    public static String anim(String id) {
        return "animations/entity/" + id + ".animation.json";
    }

    /** {@code animation.<id>.<clip>}. Loops run on the {@code base} controller, the rest are triggered on {@code action}. */
    public static final Map<String, List<String>> CLIPS = Map.of(
            // lunge: a blurring dash, arms out; drain: holds the victim's shoulder and bites deep; command: a lifted hand, a hiss.
            "alpha_vampire", List.of("idle", "walk", "run", "stunned", "bite", "hiss", "fangs_out", "rise", "decapitated", "lunge",
                    "drain", "command"),
            // leap: crouch and spring; slam: both arms down; howl: head back, the pack's call.
            "alpha_werewolf", List.of("idle", "walk", "run", "claw", "pounce", "howl", "flee", "turn", "leap", "slam"),
            // steal: a hand to the hunter's face, his own face ripples; slough: shrugs off a sheet of skin; lunge: a grab.
            "alpha_shapeshifter", List.of("idle", "walk", "attack", "revealed", "shed", "steal", "slough", "lunge"));

    public static final Map<String, List<String>> LOOPS = Map.of(
            "alpha_vampire", List.of("idle", "walk", "run", "stunned"),
            "alpha_werewolf", List.of("idle", "walk", "run"),
            "alpha_shapeshifter", List.of("idle", "walk"));

    public static List<String> triggered(String id) {
        return CLIPS.get(id).stream().filter(c -> !LOOPS.get(id).contains(c)).toList();
    }

    /** Bones the code reads, shows or hides. The vampire's {@code fangs} (the second row, on {@code fang_row}) is toggled. */
    public static final Map<String, List<String>> BONES = Map.of(
            "alpha_vampire", List.of("root", "body", "head", "fangs", "fang_row", "right_arm", "left_arm", "right_forearm", "left_forearm",
                    "right_hand", "left_hand", "right_leg", "left_leg", "coat_tail"),
            "alpha_werewolf", List.of("root", "body", "chest", "head", "jaw", "tail", "right_arm", "left_arm", "right_forearm", "left_forearm",
                    "right_hand", "left_hand", "right_leg", "left_leg", "right_shin", "left_shin"),
            "alpha_shapeshifter", List.of("root", "body", "head", "jaw", "right_arm", "left_arm", "right_forearm", "left_forearm",
                    "right_hand", "left_hand", "right_leg", "left_leg", "skin_flaps"));
    public static final Map<String, List<String>> TOGGLED = Map.of("alpha_vampire", List.of("fangs"), "alpha_werewolf", List.of(),
            "alpha_shapeshifter", List.of());
    public static final Map<String, Integer> MIN_BONES = Map.of("alpha_vampire", 26, "alpha_werewolf", 28, "alpha_shapeshifter", 24);
    /** When each signature clip's moment lands, in ticks from its start. */
    public static final Map<String, Integer> HIT_TICKS = Map.of("lunge", 8, "drain", 16, "command", 14, "leap", 12, "slam", 12, "howl", 10,
            "steal", 16, "slough", 10);
    /** Clip lengths in ticks (the art's), per creature. */
    public static final Map<String, Map<String, Integer>> CLIP_TICKS = Map.of(
            "alpha_vampire", Map.of("bite", 16, "hiss", 20, "fangs_out", 10, "rise", 30, "decapitated", 30, "lunge", 20, "drain", 40,
                    "command", 30),
            "alpha_werewolf", Map.of("claw", 14, "pounce", 18, "howl", 40, "flee", 16, "turn", 20, "leap", 30, "slam", 26),
            "alpha_shapeshifter", Map.of("attack", 14, "revealed", 24, "shed", 30, "steal", 32, "slough", 24, "lunge", 18));
}
