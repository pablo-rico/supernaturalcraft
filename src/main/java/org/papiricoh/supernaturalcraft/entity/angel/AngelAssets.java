package org.papiricoh.supernaturalcraft.entity.angel;

import java.util.List;
import java.util.Map;

/**
 * The contract between the angels' art ({@code tools/artgen/angel_art.py}, {@code tools/soundgen/angel_sfx.py}) and their code
 * (v0.22): every file, clip and bone name both sides rely on. Pure (paths relative to {@code assets/supernaturalcraft/}); an
 * {@code AngelAssetsTest} checks the files once the art lands.
 *
 * <p>Angels and fallen angels are vessels in street clothes, as in the show: an angel's eyes burn blue and the shadow of its
 * wings shows only when it strikes or flies ({@code wings} hidden otherwise); a fallen angel's clothes are worn and torn, its
 * eyes dimmed, and its broken, burnt wings ({@code wings}, look {@code burnt}) always show. Both share one rig.
 */
public final class AngelAssets {

    private AngelAssets() {
    }

    public static final String GEO = "geo/entity/angel.geo.json";
    public static final String ANIM = "animations/entity/angel.animation.json";
    public static final int ANGEL_VESSELS = 4, FALLEN_VESSELS = 3;

    /** {@code textures/entity/angel_<n>.png} or {@code fallen_angel_<n>.png}, each with its {@code _glowmask} (the eyes). */
    public static String texture(boolean fallen, int vessel) {
        return "textures/entity/" + (fallen ? "fallen_angel_" : "angel_") + vessel + ".png";
    }

    public static String glowmask(boolean fallen, int vessel) {
        return texture(fallen, vessel).replace(".png", "_glowmask.png");
    }

    /**
     * {@code animation.angel.<clip>}: idle and walk loop; the rest play once. {@code stab} an overhand stab with the blade held
     * point down; {@code teleport} a crouch and a beat of the wings (the angel vanishes on its last frame); {@code smitten} the
     * death of an angel struck by an angel blade: rigid, head thrown back, arms flung out, held for {@link #SMITTEN_TICKS};
     * {@code leap} a fallen angel's lunge.
     */
    public static final List<String> CLIPS = List.of("idle", "walk", "stab", "teleport", "smitten", "leap");
    public static final List<String> LOOPS = List.of("idle", "walk");

    public static final List<String> BONES = List.of("body", "head", "right_arm", "left_arm", "right_leg", "left_leg");
    /** Locators in {@code head} (no cubes): where the light pours out of a smitten angel. Facing the model's front (north). */
    public static final List<String> LIGHT_LOCATORS = List.of("eye_l", "eye_r", "mouth");
    /** The wings' root (the rig of {@code allegiance_art.build_wings}); shown or hidden whole by the renderer. */
    public static final String WINGS = "wings";
    /** Where the held blade is drawn (in the right hand). */
    public static final String RIGHT_ITEM = "right_item";

    /** How long a smitten angel stands burning before it falls, in ticks. */
    public static final int SMITTEN_TICKS = 30;
    /** How long its wings stay burnt into the ground, in ticks. */
    public static final int SCORCH_TICKS = 1200;

    /** The burnt shape of a smitten angel's wings, laid on the ground (alpha). */
    public static final String WING_SCORCH = "textures/misc/wing_scorch.png";
    /** One falling angel in the sky of the Fall: a streak of light with its tail (alpha, additive). */
    public static final String FALL_STREAK = "textures/misc/fall_streak.png";

    /**
     * Sounds, {@code angel.<id>} → {@code sounds/angel/<id>.ogg}: {@code ambient}, {@code hurt}, {@code death} (an angel's);
     * {@code fallen_ambient}, {@code fallen_hurt}; {@code teleport} (a beat of wings); {@code smite_ring} (the high scream and
     * rising hum while the light pours out), {@code smite_burst} (the flash); {@code fall_streak} (a far whistle overhead) and
     * {@code fall_impact} (a far boom where one lands).
     */
    public static final List<String> SOUNDS = List.of("ambient", "hurt", "death", "fallen_ambient", "fallen_hurt", "teleport",
            "smite_ring", "smite_burst", "fall_streak", "fall_impact");

    /** When each clip's moment lands, in ticks from its start. */
    public static final Map<String, Integer> HIT_TICKS = Map.of("stab", 6, "teleport", 8, "leap", 5);
}
