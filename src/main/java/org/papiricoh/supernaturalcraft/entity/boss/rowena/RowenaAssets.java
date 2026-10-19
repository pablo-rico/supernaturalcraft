package org.papiricoh.supernaturalcraft.entity.boss.rowena;

import java.util.List;
import java.util.Map;

/**
 * The contract between Rowena's art ({@code tools/artgen/rowena_art.py}, {@code tools/soundgen/coven_sfx.py}) and her code
 * (v0.21): every file, clip and bone name both sides rely on, for the boss and for {@code rowena_npc} (the same woman at
 * home, teaching pages for a price). Pure (paths relative to {@code assets/supernaturalcraft/}); a {@code RowenaAssetsTest}
 * checks the files once the art lands.
 *
 * <p>Rowena: red hair pinned up, a Victorian-cut dress in deep green and black, rings, a grimoire at her hip. Phase III she
 * levitates ({@code isAerialPhase}) and recites over three bowls.
 */
public final class RowenaAssets {

    private RowenaAssets() {
    }

    /** One rig, texture and animation file for the boss and the NPC. */
    public static final String GEO = "geo/entity/rowena.geo.json";
    public static final String TEXTURE = "textures/entity/rowena.png";
    /** Glow: her eyes and rings while casting (drawn only when the renderer says so). */
    public static final String GLOW = "textures/entity/rowena_glowmask.png";
    public static final String ANIM = "animations/entity/rowena.animation.json";

    /**
     * Boss clips ({@code animation.rowena.<clip>}): idle/walk/levitate/recite loop (levitate: phase III's hover, skirts
     * stirring; recite: the grimoire open, one hand drawing). Once: {@code fling} a flick of the wrist (telekinesis),
     * {@code ignis} a palm thrust (fire), {@code glacies} both hands pushed down (ice), {@code summon_victims} arms spread
     * wide, {@code teleport} a turn into yellow smoke, {@code death} she falls to her knees and holds.
     */
    public static final List<String> CLIPS = List.of("idle", "walk", "levitate", "recite", "fling", "ignis", "glacies",
            "summon_victims", "teleport", "death");
    public static final List<String> LOOPS = List.of("idle", "walk", "levitate", "recite");
    public static final List<String> TRIGGERED = CLIPS.stream().filter(c -> !LOOPS.contains(c)).toList();
    /**
     * NPC clips (same file): idle/walk/talk loop; {@code read} turns pages (a loop at her desk), {@code give} hands something
     * over (a page, the cure).
     */
    public static final List<String> NPC_CLIPS = List.of("idle", "walk", "talk", "read", "give");
    public static final List<String> NPC_LOOPS = List.of("idle", "walk", "talk", "read");

    public static final List<String> BONES = List.of("body", "head", "right_arm", "left_arm", "right_hand", "left_hand");
    /**
     * The open grimoire in her left hand ({@code grimoire_open}), animated by the clips themselves (scaled in) while
     * {@code recite} or {@code read} plays.
     */
    public static final Map<String, List<String>> PROPS = Map.of("grimoire_open", List.of("recite", "read"));
    /** The closed grimoire at her hip ({@code grimoire}): the renderer hides it (and its children) while {@code recite} or {@code read} plays. */
    public static final List<String> PROCEDURAL = List.of("grimoire");
    public static final int MIN_BONES = 40;

    /** When each clip's moment lands, in ticks from its start (proposal; the balance adopts them as windups). */
    public static final Map<String, Integer> HIT_TICKS = Map.of("fling", 10, "ignis", 16, "glacies", 18, "summon_victims", 20,
            "teleport", 8, "death", 80);
    /** Clip lengths in ticks (the art's). */
    public static final Map<String, Integer> CLIP_TICKS = Map.of("fling", 22, "ignis", 30, "glacies", 34, "summon_victims", 40,
            "teleport", 20, "death", 120);

    // --- Spoils ---------------------------------------------------------------------------------------------------------
    /** Rowena's Grimoire: a catalyst, a flat sprite. */
    public static final String GRIMOIRE_ICON = "textures/item/rowenas_grimoire.png";

    /** Sounds ({@code supernaturalcraft:rowena.<id>}, files {@code sounds/rowena/<id>.ogg}). */
    public static final List<String> SOUNDS = List.of("ambient", "laugh", "talk", "hurt", "death", "fling", "ignis", "glacies",
            "summon", "recite", "teleport");

    /** Cinematics ({@code cinematics/<id>.json}, anchored on Rowena). */
    public static final List<String> CINEMATICS = List.of("rowena_intro", "rowena_p2", "rowena_p3", "rowena_death");
}
