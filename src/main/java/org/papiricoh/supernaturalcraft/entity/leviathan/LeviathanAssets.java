package org.papiricoh.supernaturalcraft.entity.leviathan;

import java.util.List;
import java.util.Map;

/**
 * The contract between the art ({@code tools/artgen/leviathan_art.py}, {@code leviathan_items_art.py}) and the code (v0.25):
 * every file, clip and bone name both sides rely on. Pure (paths relative to {@code assets/supernaturalcraft/});
 * {@code LeviathanAssetsTest} checks the files.
 */
public final class LeviathanAssets {

    private LeviathanAssets() {
    }

    // --- The Leviathan, found out: a plain man in a work shirt whose head splits into a vertical maw ---------------------
    public static final String GEO = "geo/entity/leviathan.geo.json";
    public static final String TEXTURE = "textures/entity/leviathan.png";
    public static final String ANIM = "animations/entity/leviathan.animation.json";
    /**
     * {@code animation.leviathan.<clip>}: idle/walk/headless loop; the rest play once on the {@code action} controller.
     * {@code reveal} splits the head open (the jaws swing apart, the tongue lolls) and holds it open; {@code bite} lunges with
     * the maw; {@code roar} throws the head back; {@code decapitated} the body drops to its knees and falls; {@code headless}
     * the fallen body twitching; {@code rejoin} the body getting up again once its head is back.
     */
    public static final List<String> CLIPS = List.of("idle", "walk", "headless", "reveal", "bite", "roar", "decapitated", "rejoin");
    public static final List<String> LOOPS = List.of("idle", "walk", "headless");
    public static final List<String> TRIGGERED = CLIPS.stream().filter(c -> !LOOPS.contains(c)).toList();
    /**
     * Bones the code reads, shows or hides. {@code head} turns toward its target and is hidden (with its children) while it is
     * headless; {@code jaw_left}/{@code jaw_right} are the two halves of the maw (children of {@code head}), {@code tongue} and
     * {@code teeth} inside them; {@code goo_neck} is the black stump shown only while headless.
     */
    public static final List<String> BONES = List.of("body", "head", "jaw_left", "jaw_right", "tongue", "teeth", "right_arm",
            "left_arm", "right_leg", "left_leg", "goo_neck");
    /** Bones only the renderer shows or hides (clips may move them, never the renderer). */
    public static final List<String> TOGGLED = List.of("head", "goo_neck");
    public static final int MIN_BONES = 20;
    /** When each clip's moment lands, in ticks from its start. */
    public static final Map<String, Integer> HIT_TICKS = Map.of("bite", 10, "reveal", 12, "decapitated", 6);
    /** Clip lengths in ticks (the art's). */
    public static final Map<String, Integer> CLIP_TICKS = Map.of("reveal", 24, "bite", 18, "roar", 30, "decapitated", 20,
            "rejoin", 30);

    // --- The severed head: crawls back on its tongue ---------------------------------------------------------------------
    public static final String HEAD_GEO = "geo/entity/leviathan_head.geo.json";
    public static final String HEAD_TEXTURE = "textures/entity/leviathan_head.png";
    public static final String HEAD_ANIM = "animations/entity/leviathan_head.animation.json";
    /** {@code animation.leviathan_head.<clip>}: idle and crawl loop (the tongue drags it); snap is a bite, once. */
    public static final List<String> HEAD_CLIPS = List.of("idle", "crawl", "snap");
    public static final List<String> HEAD_BONES = List.of("root", "jaw_left", "jaw_right", "tongue");

    // --- Items, blocks and effects ---------------------------------------------------------------------------------------
    /** Item sprites ({@code textures/item/<id>.png}); the spawn egg uses the vanilla template. */
    public static final List<String> ITEMS = List.of("borax_blend", "borax", "borax_spray", "borax_sprayer", "turducken_slammer",
            "biggersons_burger", "righteous_bone", "anointed_righteous_bone", "fallen_blood", "kings_blood", "alpha_blood",
            "leviathan_tablet");
    /** The black goo on an arena floor (a flat decal like the holy oil slick), and the same goo dried white by borax. */
    public static final String GOO = "textures/block/leviathan_goo.png";
    public static final String GOO_DRIED = "textures/block/leviathan_goo_dried.png";
    /** Effect icons ({@code textures/mob_effect/<id>.png}). */
    public static final List<String> EFFECTS = List.of("sedated", "borax_burn");

    // --- Sounds ({@code supernaturalcraft:leviathan.<id>}) ---------------------------------------------------------------
    public static final List<String> SOUNDS = List.of("ambient", "reveal", "bite", "hurt", "death", "head_crawl", "borax_fizz");
}
