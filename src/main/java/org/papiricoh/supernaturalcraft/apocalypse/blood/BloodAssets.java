package org.papiricoh.supernaturalcraft.apocalypse.blood;

import java.util.List;

/**
 * The contract between the demon blood's art ({@code tools/artgen/blood_art.py}) and its code (v0.40). Pure (paths relative
 * to {@code assets/supernaturalcraft/}); {@code BloodAssetsTest} checks the files once the art lands.
 */
public final class BloodAssets {

    private BloodAssets() {
    }

    /** The blood powers' wheel icons (24×24, the wheel's {@code gui/allegiance/power/<id>.png}). */
    public static final List<String> POWER_ICONS = List.of("blood_push", "blood_exorcism", "blood_smite");

    /**
     * The HUD sheet ({@code textures/gui/demon_blood.png}, 128×64): the vial frame (top-left, 16×48: glass, a cork, a cracked
     * label), its fill (next to it, 10×40, dark red), the dependency rings (3 frames of 16×16: calm, hungry, withdrawal) and
     * three tier pips (8×8: empty, lit). Layout constants in {@code blood_art.py}.
     */
    public static final String HUD_SHEET = "textures/gui/demon_blood.png";
    /** The withdrawal vignette ({@code textures/misc/withdrawal_vignette.png}, 256×256, dark red edges, clear centre). */
    public static final String VIGNETTE = "textures/misc/withdrawal_vignette.png";
    /** The effect icon ({@code mob_effect/withdrawal.png}, 18×18). */
    public static final String EFFECT_ICON = "textures/mob_effect/withdrawal.png";
    /** The Panic Room door (top and bottom halves, item sprite): a heavy riveted iron door with a peephole and a devil's trap. */
    public static final List<String> DOOR = List.of("textures/block/panic_room_door_top.png", "textures/block/panic_room_door_bottom.png",
            "textures/item/panic_room_door.png");
    /** Ruby's flask (see {@code RubyAssets#FLASK_ITEM}). */
    public static final String FLASK = "textures/item/rubys_flask.png";
    /** Particles: dark red motes of a blood power. */
    public static final List<String> PARTICLES = List.of("blood_mote");
    /** Sounds ({@code supernaturalcraft:blood.<id>}): drinking, the push, the exorcism's tearing, the smite's crush, the craving. */
    public static final List<String> SOUNDS = List.of("drink", "push", "exorcism", "smite", "craving", "door_lock");
}
