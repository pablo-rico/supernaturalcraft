package org.papiricoh.supernaturalcraft.apocalypse.seal;

import java.util.List;
import java.util.Map;

/**
 * The contract between the seals' art ({@code tools/artgen/seal_art.py}, {@code seal_sky_art.py}) and the Seals slice (v0.40).
 * Pure (paths relative to {@code assets/supernaturalcraft/}); {@code SealAssetsTest} checks the files once the art lands.
 */
public final class SealAssets {

    private SealAssets() {
    }

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

    /** A variant's texture ({@code textures/entity/<rig>_<n>.png}): the Witness and the rebel angel have no plain one. */
    public static String texture(String rig, int variant) {
        return "textures/entity/" + rig + "_" + variant + ".png";
    }

    public static String glow(String rig, int variant) {
        return "textures/entity/" + rig + "_" + variant + "_glowmask.png";
    }

    // --- Mobs -------------------------------------------------------------------------------------------------------------
    /**
     * A Witness: a translucent, grey-blue ghost of an ordinary person (a few looks by texture variant), clothes of the
     * decade it died in, a dark wound where it was killed, hollow white eyes (the glow). It flickers and stalks.
     */
    public static final String WITNESS = "witness";
    /** Variants of the Witness ({@code textures/entity/witness_<n>.png}, n from 0). */
    public static final int WITNESS_VARIANTS = 4;
    /** A reaper: a gaunt figure in a plain black suit and long coat, grey skin, black eyes; it stands and watches. */
    public static final String REAPER = "seal_reaper";
    /**
     * A rebel angel: a vessel in a dark suit or an army jacket (variants), an angel blade, grey wings of light it shows when it
     * dies (the glow); the angels' look ({@code angel_art.py}) in a darker key.
     */
    public static final String REBEL = "rebel_angel";
    public static final int REBEL_VARIANTS = 3;

    /** Clips ({@code animation.<rig>.<clip>}) of each mob; loops first. */
    public static final Map<String, List<String>> CLIPS = Map.of(
            WITNESS, List.of("idle", "walk", "flicker", "grasp", "wail", "fade"),
            REAPER, List.of("idle", "walk", "touch", "fade"),
            REBEL, List.of("idle", "walk", "slash", "smite", "death"));
    public static final Map<String, List<String>> LOOPS = Map.of(
            WITNESS, List.of("idle", "walk"),
            REAPER, List.of("idle", "walk"),
            REBEL, List.of("idle", "walk"));
    public static final Map<String, List<String>> BONES = Map.of(
            WITNESS, List.of("body", "head", "right_arm", "left_arm"),
            REAPER, List.of("body", "head", "right_arm", "left_arm"),
            REBEL, List.of("body", "head", "right_arm", "left_arm", "wings"));
    public static final Map<String, Integer> HIT_TICKS = Map.of("grasp", 8, "wail", 10, "touch", 10, "slash", 8, "smite", 14,
            "flicker", 0, "fade", 0, "death", 40);
    public static final Map<String, Integer> CLIP_TICKS = Map.of("grasp", 16, "wail", 30, "touch", 20, "slash", 16, "smite", 26,
            "flicker", 12, "fade", 30, "death", 60);

    // --- Blocks and items -------------------------------------------------------------------------------------------------
    /** The hex focus: a knot of bones, hair and a black candle on a scorched sigil (block model {@code block/seal_hex_focus}). */
    public static final String HEX_FOCUS_MODEL = "models/block/seal_hex_focus.json";
    /** A pool of blood (flat, 1 px high, slightly animated texture {@code block/blood_pool}). */
    public static final String BLOOD_POOL = "textures/block/blood_pool.png";
    /** The burnt shape of wings on the ground (a flat decal, {@code block/wing_scorch}). */
    public static final String WING_SCORCH = "textures/block/wing_scorch.png";
    /** Items: the loot of each family and the compass. */
    public static final List<String> ITEMS = List.of("textures/item/witness_shroud.png", "textures/item/scorched_feather.png",
            "textures/item/plague_ash.png", "textures/item/sacrificial_ash.png", "textures/item/seal_compass.png");

    // --- The sky and the HUD ----------------------------------------------------------------------------------------------
    /** The red moon (replaces the moon from stage 2) and the blood-streaked sky overlay (stage 3+). */
    public static final String RED_MOON = "textures/environment/red_moon.png";
    public static final String BLOOD_SKY = "textures/environment/blood_sky.png";
    /**
     * The seals' page and event HUD sheet ({@code textures/gui/seals.png}, 256×128; {@code seal_art.py}):
     * <ul>
     *   <li>(16·i, 0) 16×16: seal glyph i (0-11), whole; (16·i, 16) 16×16: the same glyph broken.</li>
     *   <li>Family icons, 16×16: (192, 0) the dead and spirits, (208, 0) angels, (224, 0) plagues, (240, 0) demonic rites.</li>
     *   <li>Pips, 8×8: (192, 16) a seal standing, (200, 16) broken, (208, 16) the next to fall (lit).</li>
     *   <li>(0, 32) 182×12: the rite bar's frame, its inside at (3, 3) 176×6; (0, 44) 176×6 the rite's fill (dark red, hot
     *   edge at its right end), (0, 50) 176×6 the held fill (pale gold). Draw a fill cropped from the left to the progress.</li>
     * </ul>
     */
    public static final String GUI_SHEET = "textures/gui/seals.png";
    /** Particles ({@code supernaturalcraft:<id>}): blood rain drops, the hex's motes, a witness's wisp. */
    public static final List<String> PARTICLES = List.of("blood_drop", "hex_mote", "witness_wisp");

    /** Sounds ({@code supernaturalcraft:seal.<id>}, files {@code sounds/seal/<id>.ogg}). */
    public static final List<String> SOUNDS = List.of("warning", "breaking", "broken", "held", "witness_wail", "rite_chant",
            "thunder_far");
}
