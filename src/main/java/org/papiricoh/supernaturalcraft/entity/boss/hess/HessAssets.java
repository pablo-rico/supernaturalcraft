package org.papiricoh.supernaturalcraft.entity.boss.hess;

import java.util.List;
import java.util.Map;

/**
 * The contract between this art ({@code tools/artgen/hess_art.py}) and its code (v0.41): every file, clip and bone both sides
 * rely on. Pure (paths relative to {@code assets/supernaturalcraft/}); an {@code *AssetsTest} checks the files once the art lands.
 *
 * <p>Dr. Mildred Hess, head of the British Men of Letters (season 12): a woman in her sixties, silver hair in a tight bun, thin rimless glasses, a tweed skirt suit (heather brown) with a white blouse and a brooch of the Order's aquarian star; a black leather folio under one arm (the {@code folio} bone) and a slim wand of brass and bone she draws spells with ({@code wand}, right hand). Composed, cold, precise. Shared by the boss and by her envoy at dawn ({@code HessEnvoyEntity}, the same rig and texture).
 */
public final class HessAssets {

    private HessAssets() {
    }

    public static final String RIG = "hess";
    public static final String GEO = "geo/entity/hess.geo.json";
    public static final String TEXTURE = "textures/entity/hess.png";
    public static final String ANIM = "animations/entity/hess.animation.json";

    /**
     * {@code animation.hess.<clip>} (loops: idle, walk; the rest play once): {@code idle} hands folded over the folio; {@code walk} brisk, upright; {@code talk} the envoy speaking: a small open hand, the folio held; {@code wand_cast} a flick of the wand forward (a bolt); {@code ward_raise} wand drawn in a circle overhead (a barrier); {@code summon_agents} two fingers raised, a command; {@code hex_throw} an underhand toss of a hex bag; {@code folio_read} the folio opened, reading a spell aloud; {@code hurt} a recoil; {@code appear} steps out, straightens her jacket; {@code leave} a nod, turning away; {@code death} staggers, glasses fall, she sinks.
     */
    public static final List<String> CLIPS = List.of("idle", "walk", "talk", "wand_cast", "ward_raise", "summon_agents", "hex_throw", "folio_read", "hurt", "appear", "leave", "death");
    public static final List<String> LOOPS = List.of("idle", "walk");
    public static final List<String> TRIGGERED = CLIPS.stream().filter(c -> !LOOPS.contains(c)).toList();

    /** The humanoid rig's bones (as {@code humanoid()} in {@code tools/artgen} names them). */
    public static final List<String> BONES = List.of("body", "head", "right_arm", "left_arm", "right_hand", "left_hand", "right_leg", "left_leg");
    /** Optional bones the code shows, hides or reads (no clip keys them unless its doc says so). */
    public static final List<String> OPTIONAL_BONES = List.of("folio", "wand", "glasses", "brooch");
    public static final int MIN_BONES = 28;
    public static final float WIDTH = 0.6f, HEIGHT = 1.8f;

    /** When each clip's moment lands, in ticks from its start (the balance adopts them as windups). */
    public static final Map<String, Integer> HIT_TICKS = Map.ofEntries(Map.entry("talk", 4), Map.entry("wand_cast", 10), Map.entry("ward_raise", 14), Map.entry("summon_agents", 16), Map.entry("hex_throw", 12), Map.entry("folio_read", 18), Map.entry("hurt", 2), Map.entry("appear", 10), Map.entry("leave", 10), Map.entry("death", 40));
    /** Clip lengths in ticks. */
    public static final Map<String, Integer> CLIP_TICKS = Map.ofEntries(Map.entry("talk", 40), Map.entry("wand_cast", 20), Map.entry("ward_raise", 26), Map.entry("summon_agents", 28), Map.entry("hex_throw", 22), Map.entry("folio_read", 36), Map.entry("hurt", 12), Map.entry("appear", 20), Map.entry("leave", 20), Map.entry("death", 60));

    /** Sound ids ({@code hess.<id>}, registered with {@code AllSounds.registerGroup}; vanilla re-pitched until encoded). */
    public static final List<String> SOUNDS = List.of("cast", "ward", "command", "hex", "hurt", "death");
}
