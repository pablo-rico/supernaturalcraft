package org.papiricoh.supernaturalcraft.entity.boss.aumichael;

import java.util.List;
import java.util.Map;

/**
 * The contract between this art ({@code tools/artgen/au_michael_art.py}) and its code (v0.41): every file, clip and bone both sides
 * rely on. Pure (paths relative to {@code assets/supernaturalcraft/}); an {@code *AssetsTest} checks the files once the art lands.
 *
 * <p>The Michael of the Apocalypse World (season 13), the general who won his war: in his vessel, a broad-shouldered man in his fifties with short grey hair and a hard face, in a long dark military greatcoat with silver insignia over black armour plates, black gloves; an archangel blade in his right hand ({@code blade}). Two pairs of vast dark-gold wings of light ({@code wings}, glow) open in his later phases. Blue-white eyes when he shows them.
 */
public final class AuMichaelAssets {

    private AuMichaelAssets() {
    }

    public static final String RIG = "au_michael";
    public static final String GEO = "geo/entity/au_michael.geo.json";
    public static final String TEXTURE = "textures/entity/au_michael.png";
    /** Glow: his wings and his eyes. */
    public static final String GLOW = "textures/entity/au_michael_glowmask.png";
    public static final String ANIM = "animations/entity/au_michael.animation.json";

    /**
     * {@code animation.au_michael.<clip>} (loops: idle, walk, hover_idle; the rest play once): {@code idle} hands clasped behind his back; {@code walk} a commander's walk; {@code hover_idle} wings open, held in the air; {@code blade_slash} a heavy cut; {@code blade_combo} three cuts; {@code smite} a raised palm: holy fire; {@code wing_beat} the wings beaten forward (a gale); {@code command} a pointing order (his soldiers come); {@code lightning} both arms up (the burned sky answers); {@code rift_flee} turns, wings wrapping him, into the rift; {@code hurt} a recoil; {@code death} down on one knee (he never dies here: he flees).
     */
    public static final List<String> CLIPS = List.of("idle", "walk", "hover_idle", "blade_slash", "blade_combo", "smite", "wing_beat", "command", "lightning", "rift_flee", "hurt", "death");
    public static final List<String> LOOPS = List.of("idle", "walk", "hover_idle");
    public static final List<String> TRIGGERED = CLIPS.stream().filter(c -> !LOOPS.contains(c)).toList();

    /** The humanoid rig's bones (as {@code humanoid()} in {@code tools/artgen} names them). */
    public static final List<String> BONES = List.of("body", "head", "right_arm", "left_arm", "right_hand", "left_hand", "right_leg", "left_leg");
    /** Optional bones the code shows, hides or reads (no clip keys them unless its doc says so). */
    public static final List<String> OPTIONAL_BONES = List.of("wings", "blade", "coat_tail", "eyes");
    public static final int MIN_BONES = 36;
    public static final float WIDTH = 0.7f, HEIGHT = 2.0f;

    /** When each clip's moment lands, in ticks from its start (the balance adopts them as windups). */
    public static final Map<String, Integer> HIT_TICKS = Map.ofEntries(Map.entry("blade_slash", 7), Map.entry("blade_combo", 18), Map.entry("smite", 12), Map.entry("wing_beat", 10), Map.entry("command", 14), Map.entry("lightning", 22), Map.entry("rift_flee", 40), Map.entry("hurt", 2), Map.entry("death", 40));
    /** Clip lengths in ticks. */
    public static final Map<String, Integer> CLIP_TICKS = Map.ofEntries(Map.entry("blade_slash", 16), Map.entry("blade_combo", 36), Map.entry("smite", 24), Map.entry("wing_beat", 20), Map.entry("command", 26), Map.entry("lightning", 40), Map.entry("rift_flee", 80), Map.entry("hurt", 12), Map.entry("death", 60));

    /** Sound ids ({@code au_michael.<id>}, registered with {@code AllSounds.registerGroup}; vanilla re-pitched until encoded). */
    public static final List<String> SOUNDS = List.of("slash", "smite", "gale", "thunder", "hurt", "flee");
}
