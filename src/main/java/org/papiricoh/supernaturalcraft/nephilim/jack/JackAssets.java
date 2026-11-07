package org.papiricoh.supernaturalcraft.nephilim.jack;

import java.util.List;
import java.util.Map;

/**
 * The contract between this art ({@code tools/artgen/jack_art.py}) and its code (v0.41): every file, clip and bone both sides
 * rely on. Pure (paths relative to {@code assets/supernaturalcraft/}); an {@code *AssetsTest} checks the files once the art lands.
 *
 * <p>Jack Kline, the nephilim (seasons 13-14): looks like a young man of about eighteen, boyish face, dark brown slightly shaggy hair, a tan canvas jacket over a grey hoodie and a plain dark t-shirt, jeans, boots. Earnest and curious. His eyes glow gold when his power shows (glow). Shared by the companion ({@code jack}) and the boss ({@code soulless_jack}: same rig, {@link #SOULLESS_TEXTURE}, the eyes always gold, a colder palette, no smile).
 */
public final class JackAssets {

    private JackAssets() {
    }

    public static final String RIG = "jack";
    public static final String GEO = "geo/entity/jack.geo.json";
    public static final String TEXTURE = "textures/entity/jack.png";
    /** Glow: his eyes and the aura (always on for the soulless texture). */
    public static final String GLOW = "textures/entity/jack_glowmask.png";
    public static final String ANIM = "animations/entity/jack.animation.json";

    /**
     * {@code animation.jack.<clip>} (loops: idle, walk, run, float_idle; the rest play once): {@code idle} hands in his jacket pockets, looking around; {@code walk} a loose walk; {@code run} run; {@code float_idle} lifted off the ground, arms a little out (soulless); {@code smite} a palm thrust: a gold blast; {@code wave_aside} a backhand wave (telekinetic sweep); {@code shield} both hands up: a gold shell; {@code heal} a hand on someone's shoulder; {@code burn_grace} arms flung out, gold pouring out of him (he spends his grace); {@code snap} a flick of fingers: something unmade (soulless); {@code nova} curled up, then flung open: a burst; {@code talk} a small shrug and a look; {@code hurt} a recoil; {@code appear} a gold flash, standing; {@code leave} turns away; {@code death} the gold goes out of his eyes; he falls, and black tar takes him.
     */
    public static final List<String> CLIPS = List.of("idle", "walk", "run", "float_idle", "smite", "wave_aside", "shield", "heal", "burn_grace", "snap", "nova", "talk", "hurt", "appear", "leave", "death");
    public static final List<String> LOOPS = List.of("idle", "walk", "run", "float_idle");
    public static final List<String> TRIGGERED = CLIPS.stream().filter(c -> !LOOPS.contains(c)).toList();

    /** The humanoid rig's bones (as {@code humanoid()} in {@code tools/artgen} names them). */
    public static final List<String> BONES = List.of("body", "head", "right_arm", "left_arm", "right_hand", "left_hand", "right_leg", "left_leg");
    /** Optional bones the code shows, hides or reads (no clip keys them unless its doc says so). */
    public static final List<String> OPTIONAL_BONES = List.of("eyes", "aura", "hood");
    public static final int MIN_BONES = 28;
    public static final float WIDTH = 0.6f, HEIGHT = 1.8f;

    /** When each clip's moment lands, in ticks from its start (the balance adopts them as windups). */
    public static final Map<String, Integer> HIT_TICKS = Map.ofEntries(Map.entry("smite", 10), Map.entry("wave_aside", 8), Map.entry("shield", 6), Map.entry("heal", 14), Map.entry("burn_grace", 30), Map.entry("snap", 10), Map.entry("nova", 30), Map.entry("talk", 4), Map.entry("hurt", 2), Map.entry("appear", 10), Map.entry("leave", 10), Map.entry("death", 50));
    /** Clip lengths in ticks. */
    public static final Map<String, Integer> CLIP_TICKS = Map.ofEntries(Map.entry("smite", 20), Map.entry("wave_aside", 16), Map.entry("shield", 30), Map.entry("heal", 30), Map.entry("burn_grace", 60), Map.entry("snap", 20), Map.entry("nova", 50), Map.entry("talk", 30), Map.entry("hurt", 12), Map.entry("appear", 20), Map.entry("leave", 20), Map.entry("death", 80));

    /** Sound ids ({@code jack.<id>}, registered with {@code AllSounds.registerGroup}; vanilla re-pitched until encoded). */
    public static final List<String> SOUNDS = List.of("smite", "shield", "burn", "snap", "nova", "hurt", "death");

    public static final String SOULLESS = "soulless_jack";
    public static final String SOULLESS_TEXTURE = "textures/entity/soulless_jack.png";
    public static final String SOULLESS_GLOW = "textures/entity/soulless_jack_glowmask.png";
}
