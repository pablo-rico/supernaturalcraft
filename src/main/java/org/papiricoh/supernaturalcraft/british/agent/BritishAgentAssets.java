package org.papiricoh.supernaturalcraft.british.agent;

import java.util.List;
import java.util.Map;

/**
 * The contract between this art ({@code tools/artgen/british_art.py}) and its code (v0.41): every file, clip and bone both sides
 * rely on. Pure (paths relative to {@code assets/supernaturalcraft/}); an {@code *AssetsTest} checks the files once the art lands.
 *
 * <p>A British Men of Letters field agent (Hess's minions and the compound's guards): black tactical vest over a white shirt and a dark tie, black trousers, a short carbine ({@code rifle}) slung; some wear a black beret (texture variants {@code british_agent_0..2}).
 */
public final class BritishAgentAssets {

    private BritishAgentAssets() {
    }

    public static final String RIG = "british_agent";
    public static final String GEO = "geo/entity/british_agent.geo.json";
    public static final String TEXTURE = "textures/entity/british_agent.png";
    public static final String ANIM = "animations/entity/british_agent.animation.json";

    /**
     * {@code animation.british_agent.<clip>} (loops: idle, walk, run; the rest play once): {@code idle} carbine at low ready; {@code walk} patrol walk; {@code run} run; {@code shoot} a burst; {@code melee} a rifle-butt strike; {@code death} falls.
     */
    public static final List<String> CLIPS = List.of("idle", "walk", "run", "shoot", "melee", "death");
    public static final List<String> LOOPS = List.of("idle", "walk", "run");
    public static final List<String> TRIGGERED = CLIPS.stream().filter(c -> !LOOPS.contains(c)).toList();

    /** The humanoid rig's bones (as {@code humanoid()} in {@code tools/artgen} names them). */
    public static final List<String> BONES = List.of("body", "head", "right_arm", "left_arm", "right_hand", "left_hand", "right_leg", "left_leg");
    /** Optional bones the code shows, hides or reads (no clip keys them unless its doc says so). */
    public static final List<String> OPTIONAL_BONES = List.of("rifle", "vest", "beret");
    public static final int MIN_BONES = 20;
    public static final float WIDTH = 0.6f, HEIGHT = 1.8f;

    /** When each clip's moment lands, in ticks from its start (the balance adopts them as windups). */
    public static final Map<String, Integer> HIT_TICKS = Map.ofEntries(Map.entry("shoot", 3), Map.entry("melee", 5), Map.entry("death", 20));
    /** Clip lengths in ticks. */
    public static final Map<String, Integer> CLIP_TICKS = Map.ofEntries(Map.entry("shoot", 10), Map.entry("melee", 12), Map.entry("death", 30));

    /** Sound ids ({@code british_agent.<id>}, registered with {@code AllSounds.registerGroup}; vanilla re-pitched until encoded). */
    public static final List<String> SOUNDS = List.of("shot", "hurt", "death");
}
