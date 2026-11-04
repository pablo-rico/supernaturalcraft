package org.papiricoh.supernaturalcraft.entity.boss.crowley;

import java.util.List;
import java.util.Map;

/**
 * The contract between Crowley's art ({@code tools/artgen/crowley_art.py}, {@code crowley_retinue_art.py},
 * {@code crowley_gui_art.py}, {@code tools/soundgen/crowley_sfx.py}) and his code (v0.35): every file, clip and bone name both
 * sides rely on, for him, his retinue (the suited demon and the possessed patient), his companion form and his spoils. Pure
 * (paths relative to {@code assets/supernaturalcraft/}); {@code CrowleyAssetsTest} checks the files once the art lands.
 *
 * <p>Crowley: King of the Crossroads, would-be King of Hell. A short, stocky man in a black suit, black shirt and tie and a
 * long black overcoat; short greying hair, trimmed beard; his eyes flash red. He is no brawler: he snaps his fingers, points,
 * waves people away, and sits on his throne. The boss and the recruited companion share one rig.
 */
public final class CrowleyAssets {

    private CrowleyAssets() {
    }

    public static final String RIG = "crowley";
    public static final String GEO = "geo/entity/crowley.geo.json";
    public static final String TEXTURE = "textures/entity/crowley.png";
    /** Glow: his red eyes (the renderer only draws the glow while {@code CrowleyEntity.eyesLit()}). */
    public static final String GLOW = "textures/entity/crowley_glowmask.png";
    public static final String ANIM = "animations/entity/crowley.animation.json";

    /**
     * {@code animation.crowley.<clip>}: {@code idle}, {@code walk}, {@code throne_idle} (seated, legs crossed, one hand on the
     * armrest) and {@code kneel_idle} (beaten, on one knee, head up) loop; the rest play once. {@code snap} a raised hand's
     * finger snap, {@code point} an arm out at a target, {@code dismiss} a backhanded wave ("off you go"), {@code throne_sit}
     * from standing to seated, {@code throne_rise} from seated to standing, {@code vanish} a shrug as he goes (the body is hidden
     * by the code at its end), {@code appear} a straightened tie, {@code kneel} from standing to one knee, {@code death} the
     * stabbed demon's flare (head back, arms out) and slump, {@code talk} a hand gesture while speaking (the companion).
     */
    public static final List<String> CLIPS = List.of("idle", "walk", "throne_idle", "kneel_idle", "snap", "point", "dismiss",
            "throne_sit", "throne_rise", "vanish", "appear", "kneel", "death", "talk");
    public static final List<String> LOOPS = List.of("idle", "walk", "throne_idle", "kneel_idle");
    public static final List<String> TRIGGERED = CLIPS.stream().filter(c -> !LOOPS.contains(c)).toList();

    public static final List<String> BONES = List.of("body", "head", "right_arm", "left_arm", "right_hand", "left_hand",
            "right_leg", "left_leg");
    /** Optional bones the code may hide or tint: the coat's tail (hidden when seated), the eyes (the glow's shell). */
    public static final List<String> OPTIONAL_BONES = List.of("coat", "coat_tail", "eyes");
    public static final int MIN_BONES = 30;
    /** Hitbox. */
    public static final float WIDTH = 0.6f, HEIGHT = 1.8f;

    /** When each clip's moment lands, in ticks from its start (the balance adopts them as windups). */
    public static final Map<String, Integer> HIT_TICKS = Map.of("snap", 8, "point", 10, "dismiss", 12, "vanish", 14,
            "appear", 6, "kneel", 16, "death", 50, "throne_sit", 16, "throne_rise", 14, "talk", 10);
    /** Clip lengths in ticks. */
    public static final Map<String, Integer> CLIP_TICKS = Map.of("snap", 16, "point", 22, "dismiss", 24, "vanish", 20,
            "appear", 16, "kneel", 24, "death", 80, "throne_sit", 24, "throne_rise", 22, "talk", 30);

    /** {@code entity.supernaturalcraft.crowley.bar.phase<n>}: the HUD's key for his bar (the contract). */
    public static final String BAR_PREFIX = "entity.supernaturalcraft.crowley.bar";
    /** The contract bar's sheet ({@code textures/gui/crowley_bar.png}): see {@code crowley_gui_art.py} for its layout. */
    public static final String BAR_SHEET = "textures/gui/crowley_bar.png";

    // --- His retinue ----------------------------------------------------------------------------------------------------
    /** The suited demon: black suit, white shirt, dark tie, a manila folder under one arm; black eyes. */
    public static final String SUIT = "crowley_suit";
    /** The possessed patient: a stained hospital gown, a half-buckled straitjacket, bare feet, black eyes. */
    public static final String PATIENT = "possessed_patient";

    public static String geo(String rig) {
        return "geo/entity/" + rig + ".geo.json";
    }

    public static String texture(String rig) {
        return "textures/entity/" + rig + ".png";
    }

    public static String glow(String rig) {
        return "textures/entity/" + rig + "_glowmask.png";
    }

    public static String anim(String rig) {
        return "animations/entity/" + rig + ".animation.json";
    }

    /** Retinue clips ({@code animation.<rig>.<clip>}). */
    public static final Map<String, List<String>> RETINUE_CLIPS = Map.of(
            SUIT, List.of("idle", "walk", "strike", "smoke"),
            PATIENT, List.of("idle", "walk", "lunge", "twitch"));
    public static final Map<String, List<String>> RETINUE_LOOPS = Map.of(
            SUIT, List.of("idle", "walk"),
            PATIENT, List.of("idle", "walk"));
    public static final Map<String, List<String>> RETINUE_BONES = Map.of(
            SUIT, List.of("body", "head", "right_arm", "left_arm"),
            PATIENT, List.of("body", "head", "right_arm", "left_arm"));
    /** The suit's {@code folder} (under the left arm); the patient's loose {@code straps}. */
    public static final Map<String, List<String>> RETINUE_OPTIONAL_BONES = Map.of(
            SUIT, List.of("folder"),
            PATIENT, List.of("straps"));
    /** Retinue moments: the suit's {@code strike} hits at 8 of 16, {@code smoke} leaves at 10 of 20; the patient's {@code lunge} at 9 of 18. */
    public static final Map<String, Integer> RETINUE_HIT_TICKS = Map.of("strike", 8, "smoke", 10, "lunge", 9, "twitch", 0);
    public static final Map<String, Integer> RETINUE_CLIP_TICKS = Map.of("strike", 16, "smoke", 20, "lunge", 18, "twitch", 24);

    // --- Spoils ---------------------------------------------------------------------------------------------------------
    /** The Contract Book: a flat sprite (black leather, red ribbon, a crown stamped in gold). */
    public static final String CONTRACT_BOOK = "textures/item/contract_book.png";

    /** Sounds ({@code supernaturalcraft:crowley.<id>}, files {@code sounds/crowley/<id>.ogg}). */
    public static final List<String> SOUNDS = List.of("snap", "smoke_out", "smoke_in", "paper_storm", "hellfire", "seal_break",
            "hounds", "laugh", "hurt", "death", "suit_hurt", "suit_death", "patient_ambient", "patient_hurt", "patient_death");

    /** Cinematics ({@code cinematics/<id>.json}, anchored on Crowley). */
    public static final List<String> CINEMATICS = List.of("crowley_intro", "crowley_throne", "crowley_execution");
}
