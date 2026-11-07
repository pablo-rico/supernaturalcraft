package org.papiricoh.supernaturalcraft.apocalypse.ruby;

import java.util.List;
import java.util.Map;

/**
 * The contract between Ruby's art ({@code tools/artgen/ruby_art.py}) and her code (v0.40): one rig for the story NPC
 * ({@code ruby_npc}) and for her fight ({@code ruby}). Pure (paths relative to {@code assets/supernaturalcraft/});
 * {@code RubyAssetsTest} checks the files once the art lands.
 *
 * <p>Ruby, in her season 4 vessel: a young woman, long straight dark brown hair, pale; a fitted black leather jacket over a
 * dark red top, dark jeans, black boots. Her knife at her hip (the {@code knife} bone: the curved, engraved demon-killing
 * blade, the same look as the {@code rubys_knife} item). Her eyes turn black when she shows them (the glow sheet is the
 * black of the eyes over a faint red rim). She is quick and close: knife cuts, a kick, a telekinetic shove, smoke.
 */
public final class RubyAssets {

    private RubyAssets() {
    }

    public static final String RIG = "ruby";
    public static final String GEO = "geo/entity/ruby.geo.json";
    public static final String TEXTURE = "textures/entity/ruby.png";
    /** Her eyes gone black, with a red rim (drawn only while she shows them). */
    public static final String GLOW = "textures/entity/ruby_glowmask.png";
    public static final String ANIM = "animations/entity/ruby.animation.json";

    /**
     * {@code animation.ruby.<clip>}: {@code idle}, {@code walk}, {@code run} and {@code arms_crossed} (the NPC waiting,
     * weight on one leg) loop; the rest play once. {@code appear} steps out of black smoke (flick of the hair), {@code vanish}
     * a half turn as the smoke takes her, {@code talk} a hand gesture, {@code give} holds out a flask (left hand),
     * {@code laugh} (the reveal: head back, arms open), {@code slash} a fast forehand knife cut, {@code lunge} a stab after a
     * step, {@code kick} a spinning kick, {@code shove} a flat palm thrust (telekinesis), {@code smoke_dash} a crouch and a
     * blur forward, {@code hurt} a flinch, {@code death} the demon-light flare and fall.
     */
    public static final List<String> CLIPS = List.of("idle", "walk", "run", "arms_crossed", "appear", "vanish", "talk", "give",
            "laugh", "slash", "lunge", "kick", "shove", "smoke_dash", "hurt", "death");
    public static final List<String> LOOPS = List.of("idle", "walk", "run", "arms_crossed");
    public static final List<String> TRIGGERED = CLIPS.stream().filter(c -> !LOOPS.contains(c)).toList();

    public static final List<String> BONES = List.of("body", "head", "right_arm", "left_arm", "right_hand", "left_hand",
            "right_leg", "left_leg");
    /**
     * Optional bones: her hair (sways), the knife in her hand ({@code knife}: the fight's renderer shows it, the NPC's hides it)
     * or sheathed at her hip ({@code sheath_knife}, the hilt in its {@code sheath}: the NPC's), the flask she gives (every loop
     * keys its scale to 0 and {@code give} to 1, so it shows only while she holds it out), the eyes (glow shell).
     */
    public static final List<String> OPTIONAL_BONES = List.of("hair", "knife", "sheath", "sheath_knife", "flask", "eyes");
    public static final int MIN_BONES = 26;
    public static final float WIDTH = 0.6f, HEIGHT = 1.8f;

    public static final Map<String, Integer> HIT_TICKS = Map.ofEntries(Map.entry("appear", 8), Map.entry("vanish", 10),
            Map.entry("talk", 8), Map.entry("give", 12), Map.entry("laugh", 10), Map.entry("slash", 6), Map.entry("lunge", 10),
            Map.entry("kick", 9), Map.entry("shove", 8), Map.entry("smoke_dash", 6), Map.entry("hurt", 2), Map.entry("death", 50));
    public static final Map<String, Integer> CLIP_TICKS = Map.ofEntries(Map.entry("appear", 18), Map.entry("vanish", 20),
            Map.entry("talk", 30), Map.entry("give", 26), Map.entry("laugh", 40), Map.entry("slash", 12), Map.entry("lunge", 18),
            Map.entry("kick", 16), Map.entry("shove", 16), Map.entry("smoke_dash", 14), Map.entry("hurt", 10), Map.entry("death", 80));

    /** Ruby's flask (item {@code rubys_flask}: a dented steel hip flask, a dark stain at the neck). */
    public static final String FLASK_ITEM = "textures/item/rubys_flask.png";

    /** Sounds ({@code supernaturalcraft:ruby.<id>}, files {@code sounds/ruby/<id>.ogg}). */
    public static final List<String> SOUNDS = List.of("smoke_in", "smoke_out", "laugh", "slash", "shove", "hurt", "death");

    /** Cinematics: the reveal over Lilith's body ({@code cinematics/ruby_reveal.json}, anchored on Ruby). */
    public static final List<String> CINEMATICS = List.of("ruby_reveal");
}
