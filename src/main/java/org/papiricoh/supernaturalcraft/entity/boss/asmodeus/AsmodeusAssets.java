package org.papiricoh.supernaturalcraft.entity.boss.asmodeus;

import java.util.List;
import java.util.Map;

/**
 * The contract between this art ({@code tools/artgen/asmodeus_art.py}) and its code (v0.41): every file, clip and bone both sides
 * rely on. Pure (paths relative to {@code assets/supernaturalcraft/}); an {@code *AssetsTest} checks the files once the art lands.
 *
 * <p>Asmodeus, Prince of Hell (season 13): a portly man with a neat grey goatee and slicked-back hair, an immaculate white three-piece suit, a black string tie, a white wide-brimmed hat ({@code hat}), a cane with a silver head ({@code cane}); a Southern gentleman's smile. He can wear other faces (shapeshifting: the renderer swaps to a vanilla villager model briefly). His eyes burn yellow (glow). He carries a vial of archangel's grace on a chain ({@code vial}, gold glow) and the shard of the Lance at his belt ({@code shard}).
 */
public final class AsmodeusAssets {

    private AsmodeusAssets() {
    }

    public static final String RIG = "asmodeus";
    public static final String GEO = "geo/entity/asmodeus.geo.json";
    public static final String TEXTURE = "textures/entity/asmodeus.png";
    /** Glow: his eyes and the grace vial. */
    public static final String GLOW = "textures/entity/asmodeus_glowmask.png";
    public static final String ANIM = "animations/entity/asmodeus.animation.json";

    /**
     * {@code animation.asmodeus.<clip>} (loops: idle, walk; the rest play once): {@code idle} both hands on the cane; {@code walk} a strolling walk with the cane; {@code cane_strike} a swing of the cane; {@code cane_slam} the cane slammed down: a shockwave; {@code grace_drink} the vial uncorked and drunk: gold light fills him; {@code grace_blast} a palm of gold light; {@code summon} a snap of the fingers (hellhounds); {@code shapeshift} a shimmer, a bow; {@code taunt} a tip of the hat; {@code hurt} a recoil; {@code death} staggers, the grace burns out of his eyes and mouth, falls.
     */
    public static final List<String> CLIPS = List.of("idle", "walk", "cane_strike", "cane_slam", "grace_drink", "grace_blast", "summon", "shapeshift", "taunt", "hurt", "death");
    public static final List<String> LOOPS = List.of("idle", "walk");
    public static final List<String> TRIGGERED = CLIPS.stream().filter(c -> !LOOPS.contains(c)).toList();

    /** The humanoid rig's bones (as {@code humanoid()} in {@code tools/artgen} names them). */
    public static final List<String> BONES = List.of("body", "head", "right_arm", "left_arm", "right_hand", "left_hand", "right_leg", "left_leg");
    /** Optional bones the code shows, hides or reads (no clip keys them unless its doc says so). */
    public static final List<String> OPTIONAL_BONES = List.of("hat", "cane", "vial", "shard", "eyes");
    public static final int MIN_BONES = 30;
    public static final float WIDTH = 0.7f, HEIGHT = 1.9f;

    /** When each clip's moment lands, in ticks from its start (the balance adopts them as windups). */
    public static final Map<String, Integer> HIT_TICKS = Map.ofEntries(Map.entry("cane_strike", 7), Map.entry("cane_slam", 14), Map.entry("grace_drink", 20), Map.entry("grace_blast", 12), Map.entry("summon", 14), Map.entry("shapeshift", 10), Map.entry("taunt", 10), Map.entry("hurt", 2), Map.entry("death", 50));
    /** Clip lengths in ticks. */
    public static final Map<String, Integer> CLIP_TICKS = Map.ofEntries(Map.entry("cane_strike", 16), Map.entry("cane_slam", 26), Map.entry("grace_drink", 40), Map.entry("grace_blast", 24), Map.entry("summon", 26), Map.entry("shapeshift", 24), Map.entry("taunt", 24), Map.entry("hurt", 12), Map.entry("death", 80));

    /** Sound ids ({@code asmodeus.<id>}, registered with {@code AllSounds.registerGroup}; vanilla re-pitched until encoded). */
    public static final List<String> SOUNDS = List.of("cane", "slam", "grace", "summon", "hurt", "death");
}
