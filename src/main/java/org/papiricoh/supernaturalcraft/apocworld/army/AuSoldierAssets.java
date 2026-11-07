package org.papiricoh.supernaturalcraft.apocworld.army;

import java.util.List;
import java.util.Map;

/**
 * The contract between this art ({@code tools/artgen/au_army_art.py}) and its code (v0.41): every file, clip and bone both sides
 * rely on. Pure (paths relative to {@code assets/supernaturalcraft/}); an {@code *AssetsTest} checks the files once the art lands.
 *
 * <p>Michael's soldiers in the Apocalypse World: angels in vessels wearing grey-black military fatigues with a gold armband, a dark helmet ({@code helmet}), an angel blade ({@code blade}); their eyes glow white when they smite (glow). Variants {@code au_angel_soldier_0..3} (vessels). And the prisoners in the camps: ragged survivors ({@code au_prisoner}, the vanilla-player-sized humanoid rig {@link #PRISONER}, texture variants {@code au_prisoner_0..3}).
 */
public final class AuSoldierAssets {

    private AuSoldierAssets() {
    }

    public static final String RIG = "au_angel_soldier";
    public static final String GEO = "geo/entity/au_angel_soldier.geo.json";
    public static final String TEXTURE = "textures/entity/au_angel_soldier.png";
    /** Glow: the eyes. */
    public static final String GLOW = "textures/entity/au_angel_soldier_glowmask.png";
    public static final String ANIM = "animations/entity/au_angel_soldier.animation.json";

    /**
     * {@code animation.au_angel_soldier.<clip>} (loops: idle, walk, run; the rest play once): {@code idle} blade low; {@code walk} march; {@code run} run; {@code slash} a cut; {@code smite} a palm on the face (light); {@code death} light from the eyes, falls.
     */
    public static final List<String> CLIPS = List.of("idle", "walk", "run", "slash", "smite", "death");
    public static final List<String> LOOPS = List.of("idle", "walk", "run");
    public static final List<String> TRIGGERED = CLIPS.stream().filter(c -> !LOOPS.contains(c)).toList();

    /** The humanoid rig's bones (as {@code humanoid()} in {@code tools/artgen} names them). */
    public static final List<String> BONES = List.of("body", "head", "right_arm", "left_arm", "right_hand", "left_hand", "right_leg", "left_leg");
    /** Optional bones the code shows, hides or reads (no clip keys them unless its doc says so). */
    public static final List<String> OPTIONAL_BONES = List.of("helmet", "blade", "armband", "eyes");
    public static final int MIN_BONES = 22;
    public static final float WIDTH = 0.6f, HEIGHT = 1.9f;

    /** When each clip's moment lands, in ticks from its start (the balance adopts them as windups). */
    public static final Map<String, Integer> HIT_TICKS = Map.ofEntries(Map.entry("slash", 6), Map.entry("smite", 10), Map.entry("death", 20));
    /** Clip lengths in ticks. */
    public static final Map<String, Integer> CLIP_TICKS = Map.ofEntries(Map.entry("slash", 14), Map.entry("smite", 20), Map.entry("death", 30));

    /** Sound ids ({@code au_angel_soldier.<id>}, registered with {@code AllSounds.registerGroup}; vanilla re-pitched until encoded). */
    public static final List<String> SOUNDS = List.of("smite", "hurt", "death");

    /** The camps' prisoners: {@code idle} (huddled), {@code walk}, {@code cower}, {@code cheer} (freed). */
    public static final String PRISONER = "au_prisoner";
    public static final List<String> PRISONER_CLIPS = List.of("idle", "walk", "cower", "cheer");
}
