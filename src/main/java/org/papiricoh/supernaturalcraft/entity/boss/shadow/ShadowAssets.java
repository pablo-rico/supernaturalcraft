package org.papiricoh.supernaturalcraft.entity.boss.shadow;

import java.util.List;
import java.util.Map;

/**
 * The contract between this art ({@code tools/artgen/shadow_art.py}) and its code (v0.41): every file, clip and bone both sides
 * rely on. Pure (paths relative to {@code assets/supernaturalcraft/}); an {@code *AssetsTest} checks the files once the art lands.
 *
 * <p>The Shadow, the Empty's keeper (cosmic entity): it borrows a face; here a tall figure of black tar in the rough shape of a person in a long coat, featureless but for a face that forms and melts ({@code face}), with tendrils of tar from its back and arms ({@code tendril_1..6}) and a dripping hem ({@code drip}). Faint violet-black sheen; its eyes and mouth glow a dim violet (glow). Height 2.6 blocks; it grows tendrils with each phase (the renderer shows {@code tendril_n} by phase).
 */
public final class ShadowAssets {

    private ShadowAssets() {
    }

    public static final String RIG = "shadow";
    public static final String GEO = "geo/entity/shadow.geo.json";
    public static final String TEXTURE = "textures/entity/shadow.png";
    /** Glow: its eyes and mouth. */
    public static final String GLOW = "textures/entity/shadow_glowmask.png";
    public static final String ANIM = "animations/entity/shadow.animation.json";

    /**
     * {@code animation.shadow.<clip>} (loops: idle, walk; the rest play once): {@code idle} swaying, dripping; {@code walk} a gliding drift; {@code lash} a tendril whipped forward; {@code engulf} both arms open, tar surging (a wave); {@code sink} melts into the floor; {@code rise} rises out of the floor; {@code lull} a slow sweep of a hand: sleep falls; {@code grab} a tendril seizes and drags; {@code roar} the face splits open, a soundless scream; {@code hurt} a ripple; {@code death} it boils and collapses into a pool.
     */
    public static final List<String> CLIPS = List.of("idle", "walk", "lash", "engulf", "sink", "rise", "lull", "grab", "roar", "hurt", "death");
    public static final List<String> LOOPS = List.of("idle", "walk");
    public static final List<String> TRIGGERED = CLIPS.stream().filter(c -> !LOOPS.contains(c)).toList();

    /** The humanoid rig's bones (as {@code humanoid()} in {@code tools/artgen} names them). */
    public static final List<String> BONES = List.of("body", "head", "right_arm", "left_arm", "right_hand", "left_hand", "right_leg", "left_leg");
    /** Optional bones the code shows, hides or reads (no clip keys them unless its doc says so). */
    public static final List<String> OPTIONAL_BONES = List.of("face", "tendril_1", "tendril_2", "tendril_3", "tendril_4", "tendril_5", "tendril_6", "drip");
    public static final int MIN_BONES = 34;
    public static final float WIDTH = 0.9f, HEIGHT = 2.6f;

    /** When each clip's moment lands, in ticks from its start (the balance adopts them as windups). */
    public static final Map<String, Integer> HIT_TICKS = Map.ofEntries(Map.entry("lash", 8), Map.entry("engulf", 18), Map.entry("sink", 10), Map.entry("rise", 10), Map.entry("lull", 20), Map.entry("grab", 10), Map.entry("roar", 14), Map.entry("hurt", 2), Map.entry("death", 50));
    /** Clip lengths in ticks. */
    public static final Map<String, Integer> CLIP_TICKS = Map.ofEntries(Map.entry("lash", 16), Map.entry("engulf", 34), Map.entry("sink", 24), Map.entry("rise", 24), Map.entry("lull", 40), Map.entry("grab", 22), Map.entry("roar", 30), Map.entry("hurt", 12), Map.entry("death", 90));

    /** Sound ids ({@code shadow.<id>}, registered with {@code AllSounds.registerGroup}; vanilla re-pitched until encoded). */
    public static final List<String> SOUNDS = List.of("lash", "engulf", "lull", "roar", "hurt", "death");
}
