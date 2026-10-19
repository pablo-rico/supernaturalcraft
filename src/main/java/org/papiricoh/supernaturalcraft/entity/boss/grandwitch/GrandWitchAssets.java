package org.papiricoh.supernaturalcraft.entity.boss.grandwitch;

import java.util.List;
import java.util.Map;

/**
 * The contract between the Grand Witch's art ({@code tools/artgen/grand_witch_art.py}, {@code tools/soundgen/coven_sfx.py})
 * and her code (v0.21): every file, clip and bone name both sides rely on. Pure (paths relative to
 * {@code assets/supernaturalcraft/}); a {@code GrandWitchAssetsTest} checks the files once the art lands.
 *
 * <p>The Grand Witch: the Coven's matriarch, tall, in layered black and violet robes with a high collar, silver hair, the
 * coven staff in her right hand. Phase I she is shielded while three Coven Witches channel into her.
 */
public final class GrandWitchAssets {

    private GrandWitchAssets() {
    }

    public static final String GEO = "geo/entity/grand_witch.geo.json";
    public static final String TEXTURE = "textures/entity/grand_witch.png";
    /** Glow: her eyes and the staff's crystal. */
    public static final String GLOW = "textures/entity/grand_witch_glowmask.png";
    public static final String ANIM = "animations/entity/grand_witch.animation.json";

    /**
     * {@code animation.grand_witch.<clip>}: idle/walk/shield loop (shield: staff planted, head bowed, while her coven
     * channels). Once: {@code frost_hex} the staff swept low (ice), {@code inferno} the staff raised (fire), {@code crush} a
     * clenching fist (telekinesis), {@code mass_taglock} arms spread (every taglock at once), {@code smoke_veil} a turn
     * into yellow smoke, {@code death} she sinks and holds.
     */
    public static final List<String> CLIPS = List.of("idle", "walk", "shield", "frost_hex", "inferno", "crush", "mass_taglock",
            "smoke_veil", "death");
    public static final List<String> LOOPS = List.of("idle", "walk", "shield");
    public static final List<String> TRIGGERED = CLIPS.stream().filter(c -> !LOOPS.contains(c)).toList();

    /** Bones the code reads: {@code crystal} (the staff's head: where her spells leave and the coven's beams land). */
    public static final List<String> BONES = List.of("body", "head", "right_arm", "left_arm", "right_hand", "left_hand", "staff",
            "crystal");
    public static final int MIN_BONES = 40;

    /** When each clip's moment lands, in ticks from its start (proposal; the balance adopts them as windups). */
    public static final Map<String, Integer> HIT_TICKS = Map.of("frost_hex", 18, "inferno", 20, "crush", 16, "mass_taglock", 24,
            "smoke_veil", 10, "death", 80);
    /** Clip lengths in ticks (the art's). */
    public static final Map<String, Integer> CLIP_TICKS = Map.of("frost_hex", 34, "inferno", 38, "crush", 30, "mass_taglock", 44,
            "smoke_veil", 22, "death", 120);

    // --- Spoils ---------------------------------------------------------------------------------------------------------
    /** The Coven Staff: a Witchcraft catalyst, GeckoLib (upright, centred) with its own inventory icon. */
    public static final String STAFF_GEO = "geo/item/coven_staff.geo.json";
    public static final String STAFF_TEXTURE = "textures/item/coven_staff.png";
    public static final String STAFF_ICON = "textures/item/coven_staff_icon.png";

    /** Sounds ({@code supernaturalcraft:grand_witch.<id>}, files {@code sounds/grand_witch/<id>.ogg}). */
    public static final List<String> SOUNDS = List.of("ambient", "laugh", "hurt", "death", "shield", "shield_break", "frost_hex",
            "inferno", "crush", "taglock", "smoke_veil");

    /** Cinematics ({@code cinematics/<id>.json}, anchored on her). */
    public static final List<String> CINEMATICS = List.of("grand_witch_intro", "grand_witch_p2", "grand_witch_p3",
            "grand_witch_death");
}
