package org.papiricoh.supernaturalcraft.entity.boss.dagon;

import java.util.List;
import java.util.Map;

/**
 * The contract between this art ({@code tools/artgen/dagon_art.py}) and its code (v0.41): every file, clip and bone both sides
 * rely on. Pure (paths relative to {@code assets/supernaturalcraft/}); an {@code *AssetsTest} checks the files once the art lands.
 *
 * <p>Dagon, Prince of Hell (season 12), in her vessel: a tall woman with long straight dark hair, pale skin, a long black high-collared coat over a dark red dress, black boots; eyes of burning red-gold when her power shows (glow). Cold and contemptuous; she rarely moves her feet. Fire and telekinesis: one raised hand bends the air ({@code aura} shell).
 */
public final class DagonAssets {

    private DagonAssets() {
    }

    public static final String RIG = "dagon";
    public static final String GEO = "geo/entity/dagon.geo.json";
    public static final String TEXTURE = "textures/entity/dagon.png";
    /** Glow: her eyes and the aura while her power shows. */
    public static final String GLOW = "textures/entity/dagon_glowmask.png";
    public static final String ANIM = "animations/entity/dagon.animation.json";

    /**
     * {@code animation.dagon.<clip>} (loops: idle, walk, hover_idle, guard; the rest play once): {@code idle} still, hands at her sides; {@code walk} a slow glide-like walk; {@code hover_idle} lifted a little off the ground, coat drifting (phase III); {@code fling} a flick of the hand sideways (telekinetic throw); {@code crush} a slowly closing fist; {@code fire_palm} a palm thrust: a jet of hellfire; {@code fire_ring} both arms swept down: a ring of fire; {@code pin} a pointing finger, pinning a target in the air; {@code scream} head back, mouth open: the shriek; {@code guard} standing before the safe house door, arms crossed; {@code hurt} a recoil; {@code death} the burning eyes flare, she arches back and burns from inside.
     */
    public static final List<String> CLIPS = List.of("idle", "walk", "hover_idle", "fling", "crush", "fire_palm", "fire_ring", "pin", "scream", "guard", "hurt", "death");
    public static final List<String> LOOPS = List.of("idle", "walk", "hover_idle", "guard");
    public static final List<String> TRIGGERED = CLIPS.stream().filter(c -> !LOOPS.contains(c)).toList();

    /** The humanoid rig's bones (as {@code humanoid()} in {@code tools/artgen} names them). */
    public static final List<String> BONES = List.of("body", "head", "right_arm", "left_arm", "right_hand", "left_hand", "right_leg", "left_leg");
    /** Optional bones the code shows, hides or reads (no clip keys them unless its doc says so). */
    public static final List<String> OPTIONAL_BONES = List.of("coat_tail", "hair", "aura", "eyes");
    public static final int MIN_BONES = 28;
    public static final float WIDTH = 0.6f, HEIGHT = 1.95f;

    /** When each clip's moment lands, in ticks from its start (the balance adopts them as windups). */
    public static final Map<String, Integer> HIT_TICKS = Map.ofEntries(Map.entry("fling", 10), Map.entry("crush", 18), Map.entry("fire_palm", 12), Map.entry("fire_ring", 20), Map.entry("pin", 8), Map.entry("scream", 14), Map.entry("hurt", 2), Map.entry("death", 50));
    /** Clip lengths in ticks. */
    public static final Map<String, Integer> CLIP_TICKS = Map.ofEntries(Map.entry("fling", 20), Map.entry("crush", 30), Map.entry("fire_palm", 22), Map.entry("fire_ring", 34), Map.entry("pin", 16), Map.entry("scream", 30), Map.entry("hurt", 12), Map.entry("death", 80));

    /** Sound ids ({@code dagon.<id>}, registered with {@code AllSounds.registerGroup}; vanilla re-pitched until encoded). */
    public static final List<String> SOUNDS = List.of("fling", "crush", "fire", "scream", "hurt", "death");
}
