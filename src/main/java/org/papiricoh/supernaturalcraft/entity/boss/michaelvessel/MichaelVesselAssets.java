package org.papiricoh.supernaturalcraft.entity.boss.michaelvessel;

import java.util.List;
import java.util.Map;

/**
 * The contract between this art ({@code tools/artgen/michael_vessel_art.py}) and its code (v0.41): every file, clip and bone both sides
 * rely on. Pure (paths relative to {@code assets/supernaturalcraft/}); an {@code *AssetsTest} checks the files once the art lands.
 *
 * <p>Michael in this world (season 14), in a hunter's body: a man in his late thirties, short dark-blond hair, a dark charcoal suit and long black overcoat, white shirt, no tie, black gloves; ice-blue eyes (glow) and, when his grace flares, faint blue-white wing-shadows ({@code wings}, glow). Cold, amused, impeccable. The same rig is his projection in the mindscape (the renderer tints it white).
 */
public final class MichaelVesselAssets {

    private MichaelVesselAssets() {
    }

    public static final String RIG = "michael_vessel";
    public static final String GEO = "geo/entity/michael_vessel.geo.json";
    public static final String TEXTURE = "textures/entity/michael_vessel.png";
    /** Glow: his eyes and the wing-shadows. */
    public static final String GLOW = "textures/entity/michael_vessel_glowmask.png";
    public static final String ANIM = "animations/entity/michael_vessel.animation.json";

    /**
     * {@code animation.michael_vessel.<clip>} (loops: idle, walk, lock; the rest play once): {@code idle} hands in coat pockets; {@code walk} an unhurried walk; {@code snap} a snap of the fingers (monsters made better); {@code choke} a raised hand, closing (telekinetic choke); {@code smite} a palm of blue light; {@code wing_flare} the wings flared; {@code teleport} a flicker away; {@code slash} an archangel blade drawn and cut; {@code lock} mindscape: hammering on the door of the box he is shut in; {@code hurt} a recoil; {@code expelled} light pouring out of him upward (thrown out of the vessel); {@code death} the same light, he falls.
     */
    public static final List<String> CLIPS = List.of("idle", "walk", "snap", "choke", "smite", "wing_flare", "teleport", "slash", "lock", "hurt", "expelled", "death");
    public static final List<String> LOOPS = List.of("idle", "walk", "lock");
    public static final List<String> TRIGGERED = CLIPS.stream().filter(c -> !LOOPS.contains(c)).toList();

    /** The humanoid rig's bones (as {@code humanoid()} in {@code tools/artgen} names them). */
    public static final List<String> BONES = List.of("body", "head", "right_arm", "left_arm", "right_hand", "left_hand", "right_leg", "left_leg");
    /** Optional bones the code shows, hides or reads (no clip keys them unless its doc says so). */
    public static final List<String> OPTIONAL_BONES = List.of("wings", "coat_tail", "eyes", "blade");
    public static final int MIN_BONES = 30;
    public static final float WIDTH = 0.6f, HEIGHT = 1.85f;

    /** When each clip's moment lands, in ticks from its start (the balance adopts them as windups). */
    public static final Map<String, Integer> HIT_TICKS = Map.ofEntries(Map.entry("snap", 8), Map.entry("choke", 14), Map.entry("smite", 12), Map.entry("wing_flare", 10), Map.entry("teleport", 6), Map.entry("slash", 7), Map.entry("hurt", 2), Map.entry("expelled", 40), Map.entry("death", 40));
    /** Clip lengths in ticks. */
    public static final Map<String, Integer> CLIP_TICKS = Map.ofEntries(Map.entry("snap", 16), Map.entry("choke", 30), Map.entry("smite", 24), Map.entry("wing_flare", 22), Map.entry("teleport", 12), Map.entry("slash", 14), Map.entry("hurt", 12), Map.entry("expelled", 70), Map.entry("death", 70));

    /** Sound ids ({@code michael_vessel.<id>}, registered with {@code AllSounds.registerGroup}; vanilla re-pitched until encoded). */
    public static final List<String> SOUNDS = List.of("snap", "choke", "smite", "flare", "hurt", "expelled");
}
