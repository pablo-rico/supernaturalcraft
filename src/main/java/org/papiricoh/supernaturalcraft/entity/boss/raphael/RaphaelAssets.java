package org.papiricoh.supernaturalcraft.entity.boss.raphael;

import java.util.List;
import java.util.Map;

/**
 * The contract between the art ({@code tools/artgen/raphael_art.py}, {@code raphael_items_art.py}, {@code garrison_angel} in
 * {@code host_angel_art.py}, {@code tools/soundgen/raphael_sfx.py}) and the code (v0.16): every file, clip and bone name both
 * sides rely on. Pure (paths relative to {@code assets/supernaturalcraft/}); {@code RaphaelAssetsTest} checks the files.
 */
public final class RaphaelAssets {

    private RaphaelAssets() {
    }

    // --- Raphael: the season-five vessel, two pairs of storm-cloud wings --------------------------------------------------
    public static final String GEO = "geo/entity/raphael.geo.json";
    public static final String TEXTURE = "textures/entity/raphael.png";
    /** Glow: his eyes and the veins of light under his skin (drawn only when the renderer says so), the wings' lightning. */
    public static final String GLOW = "textures/entity/raphael_glowmask.png";
    public static final String ANIM = "animations/entity/raphael.animation.json";
    /**
     * {@code animation.raphael.<clip>}: idle/walk/trapped loop; the rest play once on the {@code action} controller.
     * {@code smite} raises his hand ({@link RaphaelBalance#SMITE_WINDUP} ticks) and closes it; {@code snap} is the finger snap;
     * {@code call_lightning} lifts both arms; {@code heal_channel} holds his palms out to the threads; {@code blink} a flinch of
     * wings; {@code thunderclap} a clap; {@code wings_reveal} spreads the wings and holds; {@code emerge} kneels out of a bolt.
     */
    public static final List<String> CLIPS = List.of("idle", "walk", "trapped", "smite", "snap", "call_lightning", "heal_channel",
            "blink", "thunderclap", "wings_reveal", "stagger", "death", "emerge");
    public static final List<String> LOOPS = List.of("idle", "walk", "trapped");
    public static final List<String> TRIGGERED = CLIPS.stream().filter(c -> !LOOPS.contains(c)).toList();
    /**
     * Bones the code reads, shows or hides. {@code wings} (both pairs and their feathers) is shown only while the synced
     * WINGS flag is set; {@code veins} (a skin-tight shell of glowing lines) only in phase 3.
     */
    public static final List<String> BONES = List.of("body", "head", "right_arm", "left_arm", "right_hand", "left_hand", "wings",
            "veins");
    /** Minimum bones of the rig (a detailed model, like Gabriel's and Michael's vessel). */
    public static final int MIN_BONES = 50;
    /**
     * The veins of light are one shell per part (so each follows its limb): {@code veins} over the torso, {@code veins_head},
     * and {@code veins_<limb>} under every arm and leg bone. The renderer shows all of them in phase III and none before.
     */
    public static final List<String> VEIN_BONES = List.of("veins", "veins_head", "veins_right_arm", "veins_right_forearm",
            "veins_right_hand", "veins_left_arm", "veins_left_forearm", "veins_left_hand", "veins_right_leg", "veins_right_shin",
            "veins_left_leg", "veins_left_shin");
    /** Props shown only while their clip plays: the disc of lightning on his right palm during the smite. */
    public static final Map<String, String> PROPS = Map.of("palm_light", "smite");
    /** Bones only the renderer drives (no clip may key them): the props and the veins. */
    public static final List<String> PROCEDURAL = java.util.stream.Stream.concat(PROPS.keySet().stream(), VEIN_BONES.stream()).toList();
    /**
     * When each clip's moment lands, in ticks from its start: the smite's fist closes at {@link RaphaelBalance#SMITE_WINDUP},
     * the snap, the strike of call_lightning, the clap, the blink's flinch, the death's burst of light.
     */
    public static final Map<String, Integer> HIT_TICKS = Map.of("smite", RaphaelBalance.SMITE_WINDUP, "snap", 14, "call_lightning", 20,
            "thunderclap", 10, "blink", 2, "death", 80);
    /** Clip lengths in ticks (the art's), for whoever needs to wait one out. */
    public static final Map<String, Integer> CLIP_TICKS = Map.ofEntries(Map.entry("smite", 40), Map.entry("snap", 28),
            Map.entry("call_lightning", 40), Map.entry("heal_channel", 60), Map.entry("blink", 14), Map.entry("thunderclap", 26),
            Map.entry("wings_reveal", 50), Map.entry("stagger", 16), Map.entry("death", 120), Map.entry("emerge", 64));

    // --- his garrison -------------------------------------------------------------------------------------------------
    /** Angels of his garrison: the Host's rig with a storm-grey texture. */
    public static final String GARRISON_GEO = "geo/entity/host_angel.geo.json";
    public static final String GARRISON_TEXTURE = "textures/entity/garrison_angel.png";
    public static final String GARRISON_ANIM = "animations/entity/host_angel.animation.json";

    // --- Items and blocks ---------------------------------------------------------------------------------------------
    /** The Stormcaller: a GeckoLib catalyst ({@code geo/item/raphaels_stormcaller.geo.json}) with its own inventory icon. */
    public static final String STAFF_GEO = "geo/item/raphaels_stormcaller.geo.json";
    public static final String STAFF_TEXTURE = "textures/item/raphaels_stormcaller.png";
    public static final String STAFF_ICON = "textures/item/raphaels_stormcaller_icon.png";
    /** The trophy: his bust (block model facing north, turned by FACING). */
    public static final String TROPHY_MODEL = "models/block/raphael_trophy.json";
    /** The unlit holy oil on the house's floor (a flat decal, like the Colt's rails). */
    public static final String OIL_SLICK = "textures/block/holy_oil_slick.png";

    // --- Sounds ({@code supernaturalcraft:raphael.<id>}) ----------------------------------------------------------------
    public static final List<String> SOUNDS = List.of("arrive", "thunder", "smite", "snap", "heal", "wings", "trapped", "ambient",
            "hurt", "death", "stormcaller_zap", "stormcaller_heal");

    // --- Cinematics ({@code cinematics/<id>.json}, CameraSequence, anchored on Raphael) ---------------------------------
    public static final List<String> CINEMATICS = List.of("raphael_intro", "raphael_p2", "raphael_p3", "raphael_death");
}
