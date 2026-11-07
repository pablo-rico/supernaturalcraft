package org.papiricoh.supernaturalcraft.entity.boss.ketch;

import java.util.List;
import java.util.Map;

/**
 * The contract between this art ({@code tools/artgen/ketch_art.py}) and its code (v0.41): every file, clip and bone both sides
 * rely on. Pure (paths relative to {@code assets/supernaturalcraft/}); an {@code *AssetsTest} checks the files once the art lands.
 *
 * <p>Arthur Ketch, the British Men of Letters' operative (season 12): a lean man in his forties, short dark hair, clean-shaven, a sharp dark-grey three-piece suit, black gloves, an earpiece; a suppressed pistol in his right hand ({@code pistol}), a combat knife at his left hip ({@code knife}), grenades on a strap under the jacket ({@code grenades}). Quiet, lethal, polite.
 */
public final class KetchAssets {

    private KetchAssets() {
    }

    public static final String RIG = "ketch";
    public static final String GEO = "geo/entity/ketch.geo.json";
    public static final String TEXTURE = "textures/entity/ketch.png";
    public static final String ANIM = "animations/entity/ketch.animation.json";

    /**
     * {@code animation.ketch.<clip>} (loops: idle, walk, run, aim; the rest play once): {@code idle} pistol low at his side; {@code walk} a measured stalk; {@code run} a sprint, pistol up; {@code aim} both hands on the pistol, aiming; {@code shoot} one suppressed shot (recoil); {@code volley} three quick shots, stepping sideways; {@code knife_slash} a fast backhand cut; {@code knife_lunge} a lunge with the knife; {@code grenade_throw} an overarm throw of a hex grenade; {@code smoke_vanish} crouch and drop a smoke canister; {@code hurt} a recoil; {@code taunt} adjusts his cuffs; {@code death} drops to his knees, falls.
     */
    public static final List<String> CLIPS = List.of("idle", "walk", "run", "aim", "shoot", "volley", "knife_slash", "knife_lunge", "grenade_throw", "smoke_vanish", "hurt", "taunt", "death");
    public static final List<String> LOOPS = List.of("idle", "walk", "run", "aim");
    public static final List<String> TRIGGERED = CLIPS.stream().filter(c -> !LOOPS.contains(c)).toList();

    /** The humanoid rig's bones (as {@code humanoid()} in {@code tools/artgen} names them). */
    public static final List<String> BONES = List.of("body", "head", "right_arm", "left_arm", "right_hand", "left_hand", "right_leg", "left_leg");
    /** Optional bones the code shows, hides or reads (no clip keys them unless its doc says so). */
    public static final List<String> OPTIONAL_BONES = List.of("pistol", "knife", "grenades", "earpiece");
    public static final int MIN_BONES = 30;
    public static final float WIDTH = 0.6f, HEIGHT = 1.85f;

    /** When each clip's moment lands, in ticks from its start (the balance adopts them as windups). */
    public static final Map<String, Integer> HIT_TICKS = Map.ofEntries(Map.entry("shoot", 3), Map.entry("volley", 4), Map.entry("knife_slash", 6), Map.entry("knife_lunge", 8), Map.entry("grenade_throw", 12), Map.entry("smoke_vanish", 8), Map.entry("hurt", 2), Map.entry("taunt", 10), Map.entry("death", 40));
    /** Clip lengths in ticks. */
    public static final Map<String, Integer> CLIP_TICKS = Map.ofEntries(Map.entry("shoot", 10), Map.entry("volley", 30), Map.entry("knife_slash", 14), Map.entry("knife_lunge", 18), Map.entry("grenade_throw", 22), Map.entry("smoke_vanish", 20), Map.entry("hurt", 12), Map.entry("taunt", 24), Map.entry("death", 60));

    /** Sound ids ({@code ketch.<id>}, registered with {@code AllSounds.registerGroup}; vanilla re-pitched until encoded). */
    public static final List<String> SOUNDS = List.of("shot", "knife", "grenade", "smoke", "hurt", "death");
}
