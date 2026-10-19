package org.papiricoh.supernaturalcraft.entity.witch;

import java.util.List;
import java.util.Map;

/**
 * The contract between the witches' art ({@code tools/artgen/witch_art.py}, {@code familiar_art.py}, {@code victims_art.py};
 * {@code tools/soundgen/coven_sfx.py}) and their code (v0.21): every file, clip and bone name both sides rely on. Pure (paths
 * relative to {@code assets/supernaturalcraft/}); a {@code WitchAssetsTest} checks the files once the art lands.
 *
 * <p>Each rig's files are {@code geo/entity/<rig>.geo.json}, {@code textures/entity/<rig>.png} and
 * {@code animations/entity/<rig>.animation.json}, its clips {@code animation.<rig>.<clip>}. Loops run on the {@code base}
 * controller; the rest are triggered on {@code action} with {@code triggerAnim("action", clip)}.
 */
public final class WitchAssets {

    private WitchAssets() {
    }

    // --- Entities and their rigs ----------------------------------------------------------------------------------------
    /** The Hedge Witch: a lone witch in patched layers, a satchel of components; hexes from afar, teleports in smoke. */
    public static final String HEDGE_WITCH = "hedge_witch";
    /** The Coven Witch: the elite, a darker robe with the coven's sigil; shields herself, teleports, channels for her coven. */
    public static final String COVEN_WITCH = "coven_witch";
    /** A witch's familiar (one entity, two rigs by variant): spies, steals components, strengthens its witch. */
    public static final String FAMILIAR = "familiar";
    /** A villager under a witch's curse: hollow-eyed, bound to her, freed by {@code break_hex}. */
    public static final String HEXED_VILLAGER = "hexed_villager";

    /** The familiar's variants; the rig of each is {@code familiar_<variant>}. */
    public static final List<String> FAMILIAR_VARIANTS = List.of("cat", "raven");

    public static String familiarRig(String variant) {
        return FAMILIAR + "_" + variant;
    }

    /** Every rig (= file name). */
    public static final List<String> RIGS = List.of(HEDGE_WITCH, COVEN_WITCH, familiarRig("cat"), familiarRig("raven"), HEXED_VILLAGER);

    public static String geo(String rig) {
        return "geo/entity/" + rig + ".geo.json";
    }

    public static String texture(String rig) {
        return "textures/entity/" + rig + ".png";
    }

    /** Glow mask ({@code AutoGlowingGeoLayer}): the witches' eyes while casting, the hexed villager's eyes, the familiar's eyes. */
    public static String glow(String rig) {
        return "textures/entity/" + rig + "_glowmask.png";
    }

    public static String anim(String rig) {
        return "animations/entity/" + rig + ".animation.json";
    }

    // --- Clips ----------------------------------------------------------------------------------------------------------
    /**
     * Clips per rig. Witches: {@code cast} a thrown hex from the right hand, {@code hex} both hands over a taglock (a curse at a
     * distance), {@code teleport} a crouch into smoke; the Coven Witch's {@code shield} raises her bubble and {@code channel}
     * (a loop) feeds her coven's leader with both arms up. Familiars: the cat {@code sit}s and {@code steal}s with its mouth;
     * the raven flies ({@code fly}, a loop) and {@code steal}s with its beak. The hexed villager {@code attack}s with both arms
     * and plays {@code freed} (a slump, then it straightens) when the hex breaks.
     */
    public static final Map<String, List<String>> CLIPS = Map.of(
            HEDGE_WITCH, List.of("idle", "walk", "cast", "hex", "teleport"),
            COVEN_WITCH, List.of("idle", "walk", "channel", "cast", "hex", "teleport", "shield"),
            familiarRig("cat"), List.of("idle", "walk", "sit", "steal"),
            familiarRig("raven"), List.of("idle", "fly", "steal"),
            HEXED_VILLAGER, List.of("idle", "walk", "attack", "freed"));

    /** Looping clips per rig (the rest are triggered). */
    public static final Map<String, List<String>> LOOPS = Map.of(
            HEDGE_WITCH, List.of("idle", "walk"),
            COVEN_WITCH, List.of("idle", "walk", "channel"),
            familiarRig("cat"), List.of("idle", "walk", "sit"),
            familiarRig("raven"), List.of("idle", "fly"),
            HEXED_VILLAGER, List.of("idle", "walk"));

    public static List<String> triggered(String rig) {
        return CLIPS.get(rig).stream().filter(c -> !LOOPS.get(rig).contains(c)).toList();
    }

    // --- Bones ----------------------------------------------------------------------------------------------------------
    /**
     * Bones the code reads per rig: {@code head} (looks at its target), {@code right_hand}/{@code left_hand} (where spells
     * and stolen items appear), {@code mouth}/{@code beak} (a familiar's stolen component), {@code wings} (the raven's, for the
     * renderer to fold when it lands).
     */
    public static final Map<String, List<String>> BONES = Map.of(
            HEDGE_WITCH, List.of("body", "head", "right_arm", "left_arm", "right_hand", "left_hand"),
            COVEN_WITCH, List.of("body", "head", "right_arm", "left_arm", "right_hand", "left_hand"),
            familiarRig("cat"), List.of("body", "head", "mouth", "tail"),
            familiarRig("raven"), List.of("body", "head", "beak", "wings"),
            HEXED_VILLAGER, List.of("body", "head", "right_arm", "left_arm"));

    /** Props only the renderer shows: the item a familiar carries (shown while it holds one); no clip may key them. */
    public static final Map<String, String> PROPS = Map.of(familiarRig("cat"), "held_item", familiarRig("raven"), "held_item");

    /** When a triggered clip's moment lands, in ticks from its start (the hex leaves the hand, the theft, the blink away). */
    public static final Map<String, Integer> HIT_TICKS = Map.of("cast", 10, "hex", 16, "teleport", 8, "steal", 6, "attack", 8);

    // --- Sounds ({@code supernaturalcraft:witch.<id>}, files {@code sounds/witch/<id>.ogg}) --------------------------------
    public static final List<String> SOUNDS = List.of("hedge_ambient", "hedge_hurt", "hedge_death", "coven_ambient", "coven_hurt",
            "coven_death", "cast", "hex", "teleport", "shield_up", "shield_break", "channel", "cat_ambient", "raven_caw",
            "familiar_steal", "victim_ambient", "victim_freed");
}
