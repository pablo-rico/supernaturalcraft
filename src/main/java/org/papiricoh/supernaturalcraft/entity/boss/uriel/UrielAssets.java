package org.papiricoh.supernaturalcraft.entity.boss.uriel;

import java.util.List;
import java.util.Map;

/**
 * The contract between Uriel's art ({@code tools/artgen/uriel_art.py}) and his code (v0.40, the Seals slice). Pure (paths
 * relative to {@code assets/supernaturalcraft/}); {@code UrielAssetsTest} checks the files once the art lands.
 *
 * <p>Uriel, in his season 4 vessel: a tall, broad, bald man with a short beard, a stern face; a long dark brown leather coat
 * over a black turtleneck, dark trousers, boots. An angel blade in his right hand. Huge wings of grey-gold light at his back
 * ({@code wings}, hidden until phase II: they open with {@code unfurl}); the glow sheet is his eyes and the wings' light.
 * He is a soldier: blade cuts, a palm that cracks the ground, the smite in a cone, and blinding flashes of grace.
 */
public final class UrielAssets {

    private UrielAssets() {
    }

    public static final String RIG = "uriel";
    public static final String GEO = "geo/entity/uriel.geo.json";
    public static final String TEXTURE = "textures/entity/uriel.png";
    public static final String GLOW = "textures/entity/uriel_glowmask.png";
    public static final String ANIM = "animations/entity/uriel.animation.json";

    /**
     * {@code animation.uriel.<clip>}: {@code idle}, {@code walk}, {@code hover} (phase II, feet off the ground, wings slow)
     * loop; the rest play once. {@code slash} a heavy blade cut, {@code thrust} a stab, {@code palm} a palm slammed into the
     * ground (the crack), {@code smite} a hand thrust forward, fingers spread (the cone), {@code flash} arms flung wide
     * (blinding grace), {@code unfurl} the wings opening (phase II), {@code command} a pointed arm (his rebels attack),
     * {@code kneel} beaten, {@code death} the angel's death: light out of eyes and mouth, wings burnt on the ground.
     */
    public static final List<String> CLIPS = List.of("idle", "walk", "hover", "slash", "thrust", "palm", "smite", "flash",
            "unfurl", "command", "kneel", "death");
    public static final List<String> LOOPS = List.of("idle", "walk", "hover");
    public static final List<String> TRIGGERED = CLIPS.stream().filter(c -> !LOOPS.contains(c)).toList();

    public static final List<String> BONES = List.of("body", "head", "right_arm", "left_arm", "right_hand", "left_hand",
            "right_leg", "left_leg", "wings");
    /**
     * Optional bones: the coat's tail, the blade, each wing's root ({@code wing_r}, {@code wing_l}: allegiance_art's light wings,
     * under {@code wings}) and the eyes' shell (lit pale gold; the renderer shows it from phase II and while he dies). The
     * renderer hides {@code wings} and all under it in phase I; no clip scales {@code wings}.
     */
    public static final List<String> OPTIONAL_BONES = List.of("coat_tail", "blade", "wing_r", "wing_l", "eyes");
    public static final int MIN_BONES = 30;
    public static final float WIDTH = 0.6f, HEIGHT = 1.95f;

    public static final Map<String, Integer> HIT_TICKS = Map.ofEntries(Map.entry("slash", 9), Map.entry("thrust", 8),
            Map.entry("palm", 16), Map.entry("smite", 18), Map.entry("flash", 14), Map.entry("unfurl", 20), Map.entry("command", 10),
            Map.entry("kneel", 16), Map.entry("death", 60));
    public static final Map<String, Integer> CLIP_TICKS = Map.ofEntries(Map.entry("slash", 18), Map.entry("thrust", 16),
            Map.entry("palm", 28), Map.entry("smite", 30), Map.entry("flash", 26), Map.entry("unfurl", 36), Map.entry("command", 20),
            Map.entry("kneel", 24), Map.entry("death", 90));

    /** His bust (block model {@code block/uriel_trophy}). */
    public static final String TROPHY_MODEL = "models/block/uriel_trophy.json";
    /** {@code entity.supernaturalcraft.uriel.bar.phase<n>}: his bar's keys. */
    public static final String BAR_PREFIX = "entity.supernaturalcraft.uriel.bar";
    /** His bar's sheet ({@code textures/gui/uriel_bar.png}): scorched gold and grey feathers. */
    public static final String BAR_SHEET = "textures/gui/uriel_bar.png";

    /** Sounds ({@code supernaturalcraft:uriel.<id>}, files {@code sounds/uriel/<id>.ogg}). */
    public static final List<String> SOUNDS = List.of("slash", "palm", "smite", "flash", "wings", "hurt", "death");
    public static final List<String> CINEMATICS = List.of("uriel_garrison");
}
