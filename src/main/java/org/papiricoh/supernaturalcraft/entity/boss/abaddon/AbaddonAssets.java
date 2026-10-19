package org.papiricoh.supernaturalcraft.entity.boss.abaddon;

import java.util.List;
import java.util.Map;

/**
 * The contract between Abaddon's art ({@code tools/artgen/abaddon_art.py}, {@code tools/soundgen/abaddon_sfx.py}) and her code
 * (v0.21): every file, clip and bone name both sides rely on, for her and her retinue (the Knight and the Herald), and the
 * spoils. Pure (paths relative to {@code assets/supernaturalcraft/}); an {@code AbaddonAssetsTest} checks the files once the
 * art lands.
 *
 * <p>Abaddon: Knight of Hell, red-haired vessel in a long dark coat, black eyes. She leaves her vessel as black smoke in
 * phase II (the renderer hides the whole model while she is smoke) to possess a knight.
 */
public final class AbaddonAssets {

    private AbaddonAssets() {
    }

    public static final String GEO = "geo/entity/abaddon.geo.json";
    public static final String TEXTURE = "textures/entity/abaddon.png";
    /** Glow: the black eyes' red rim and the brand on her palm during {@code brand}. */
    public static final String GLOW = "textures/entity/abaddon_glowmask.png";
    public static final String ANIM = "animations/entity/abaddon.animation.json";

    /**
     * {@code animation.abaddon.<clip>}: idle/walk/trapped loop (trapped: caught in a devil's trap, straining); the rest play
     * once. {@code herald_mark} points at a target for her herald, {@code hellfire_sweep} a sweeping arm (a cone of fire),
     * {@code smoke_out} throws her head back as the smoke leaves, {@code possess} the smoke's dive into a knight (the knight
     * then loops {@code possessed}), {@code telekinetic_pin} a clenched fist held out,
     * {@code brand} a palm pressed down (the knights' seal), {@code death} she burns from inside and holds.
     */
    public static final List<String> CLIPS = List.of("idle", "walk", "trapped", "herald_mark", "hellfire_sweep", "smoke_out",
            "possess", "telekinetic_pin", "brand", "death");
    public static final List<String> LOOPS = List.of("idle", "walk", "trapped");
    public static final List<String> TRIGGERED = CLIPS.stream().filter(c -> !LOOPS.contains(c)).toList();

    public static final List<String> BONES = List.of("body", "head", "right_arm", "left_arm", "right_hand", "left_hand");
    /**
     * The Brand of the Knights on her palm: a shell that slides out of her hand only in the {@code brand} clip (the clip keys
     * it; the renderer never touches it).
     */
    public static final String BRAND = "brand";
    /** Minimum bones of the rig. */
    public static final int MIN_BONES = 40;

    /** When each clip's moment lands, in ticks from its start (proposal; the balance adopts them as windups). */
    public static final Map<String, Integer> HIT_TICKS = Map.of("herald_mark", 10, "hellfire_sweep", 18, "smoke_out", 20,
            "possess", 12, "telekinetic_pin", 16, "brand", 24, "death", 80);
    /** Clip lengths in ticks (the art's). */
    public static final Map<String, Integer> CLIP_TICKS = Map.of("herald_mark", 24, "hellfire_sweep", 34, "smoke_out", 36,
            "possess", 24, "telekinetic_pin", 32, "brand", 44, "death", 120);

    // --- Her retinue ----------------------------------------------------------------------------------------------------
    /** The Knight: an armoured warrior-demon in blackened plate; possessed by Abaddon it glows from inside. */
    public static final String KNIGHT = "abaddon_knight";
    /** The Herald: a robed demon who marks Abaddon's targets. */
    public static final String HERALD = "abaddon_herald";

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

    /** Retinue clips ({@code animation.<rig>.<clip>}): the knight's {@code possessed} is a loop (Abaddon inside). */
    public static final Map<String, List<String>> RETINUE_CLIPS = Map.of(
            KNIGHT, List.of("idle", "walk", "possessed", "slash", "guard"),
            HERALD, List.of("idle", "walk", "mark", "cast"));
    public static final Map<String, List<String>> RETINUE_LOOPS = Map.of(
            KNIGHT, List.of("idle", "walk", "possessed"),
            HERALD, List.of("idle", "walk"));
    public static final Map<String, List<String>> RETINUE_BONES = Map.of(
            KNIGHT, List.of("body", "head", "right_arm", "left_arm", "right_hand", "left_hand"),
            HERALD, List.of("body", "head", "right_arm", "left_arm", "right_hand", "left_hand"));
    /**
     * Bones of the retinue the code (or FX) may show, hide or tint, never required: the knight's {@code blade} (at its rest
     * rotation: no clip keys it), {@code blade_veins} (they glow while possessed), {@code shield} (raised in {@code guard}),
     * {@code visor}; the herald's {@code bell} and its {@code clapper} (rung in {@code mark}).
     */
    public static final Map<String, List<String>> RETINUE_OPTIONAL_BONES = Map.of(
            KNIGHT, List.of("blade", "blade_veins", "shield", "visor"),
            HERALD, List.of("bell", "clapper"));
    /**
     * Retinue clip moments (ticks from the clip's start): the knight's {@code slash} hits at 10 of 20; {@code guard} raises the
     * shield at 5 of 24 (it blocks blows from the front from then to the end); the herald's {@code mark} lands at 9 of 20,
     * {@code cast} at 14 of 28.
     */
    public static final Map<String, Integer> RETINUE_HIT_TICKS = Map.of("slash", 10, "guard", 5, "mark", 9, "cast", 14);
    public static final Map<String, Integer> RETINUE_CLIP_TICKS = Map.of("slash", 20, "guard", 24, "mark", 20, "cast", 28);
    /** The knight's hitbox (he is big). */
    public static final float KNIGHT_WIDTH = 0.9f, KNIGHT_HEIGHT = 2.25f;

    // --- Spoils ---------------------------------------------------------------------------------------------------------
    /** The Book of the Damned: a flat sprite. */
    public static final String BOOK_OF_THE_DAMNED = "textures/item/book_of_the_damned.png";
    /** The Knights' Cleaver: a GeckoLib melee weapon (upright, centred) with its own inventory icon. */
    public static final String CLEAVER_GEO = "geo/item/knights_cleaver.geo.json";
    public static final String CLEAVER_TEXTURE = "textures/item/knights_cleaver.png";
    public static final String CLEAVER_ICON = "textures/item/knights_cleaver_icon.png";
    /** Its glow: the {@code veins} along the blade. */
    public static final String CLEAVER_GLOW = "textures/item/knights_cleaver_glowmask.png";
    public static final String CLEAVER_ANIM = "animations/item/knights_cleaver.animation.json";
    /** {@code animation.knights_cleaver.<clip>}: {@code idle} loops (the veins pulse), {@code cleave} plays on its wave. */
    public static final List<String> CLEAVER_CLIPS = List.of("idle", "cleave");
    /** The cleaver model's height in pixels ({@code geoWeapon}'s scale). */
    public static final float CLEAVER_HEIGHT = 28.4f;
    /** The bust: ART's own block model (on the trophy base), turned by FACING. */
    public static final String TROPHY_MODEL = "models/block/abaddon_trophy.json";

    /** Sounds ({@code supernaturalcraft:abaddon.<id>}, files {@code sounds/abaddon/<id>.ogg}). */
    public static final List<String> SOUNDS = List.of("ambient", "laugh", "hurt", "death", "sweep", "smoke", "possess", "pin",
            "brand", "trapped", "knight_ambient", "knight_hurt", "knight_death", "herald_mark");

    /** Cinematics ({@code cinematics/<id>.json}, anchored on Abaddon). */
    public static final List<String> CINEMATICS = List.of("abaddon_intro", "abaddon_p2", "abaddon_p3", "abaddon_death");
}
