package org.papiricoh.supernaturalcraft.entity.boss.cain;

import java.util.List;
import java.util.Map;

/**
 * The contract between Cain's art ({@code tools/artgen/cain_art.py}, {@code tools/soundgen/cain_sfx.py}) and his code (v0.21):
 * every file, clip and bone name both sides rely on. Pure (paths relative to {@code assets/supernaturalcraft/}); a
 * {@code CainAssetsTest} checks the files once the art lands.
 *
 * <p>Cain: the beekeeper of the seasons nine-ten vessel (work shirt, worn jacket, beard), the Mark burning on his right
 * forearm. Phase I the Beekeeper, II the Mark awakens, III the Father of Murder (he takes up the First Blade).
 */
public final class CainAssets {

    private CainAssets() {
    }

    public static final String GEO = "geo/entity/cain.geo.json";
    public static final String TEXTURE = "textures/entity/cain.png";
    /** Glow: the Mark on his forearm (shown only from phase II, see {@link #MARK}) and his eyes in phase III. */
    public static final String GLOW = "textures/entity/cain_glowmask.png";
    public static final String ANIM = "animations/entity/cain.animation.json";

    /**
     * {@code animation.cain.<clip>}: idle/walk loop; the rest play once on the {@code action} controller. {@code lunge} a
     * charge along a red line (thrice in a row), {@code swarm} raises a hand and the bees rise, {@code furrow} a ploughing
     * swing that rips a cone, {@code abels_blood} a palm to the ground (spikes ring out), {@code throw_tools} hurls farm
     * tools, {@code execution} a blink behind and a downward stab, {@code mark_flare} the Mark blazes with his arm raised,
     * {@code death} he kneels and holds.
     */
    public static final List<String> CLIPS = List.of("idle", "walk", "lunge", "swarm", "furrow", "abels_blood", "throw_tools",
            "execution", "mark_flare", "death");
    public static final List<String> LOOPS = List.of("idle", "walk");
    public static final List<String> TRIGGERED = CLIPS.stream().filter(c -> !LOOPS.contains(c)).toList();

    /** Bones the code reads, shows or hides. */
    public static final List<String> BONES = List.of("body", "head", "right_arm", "left_arm", "right_hand", "left_hand");
    /** The Mark on his right forearm: a skin-tight shell the renderer shows from phase II. */
    public static final String MARK = "mark";
    /** The First Blade in his right hand: shown in phase III. */
    public static final String FIRST_BLADE = "first_blade";
    /** A shell over his eyes that glows red in the glowmask: shown in phase III. */
    public static final String EYES = "eyes";
    /** Bones only the renderer drives (no clip may key them). */
    public static final List<String> PROCEDURAL = List.of(MARK, FIRST_BLADE, EYES);
    /**
     * The peaceful Cain at his farm ({@code cain_npc}) is drawn with the same rig, texture and file (the Mark, the blade and the
     * eyes hidden), with his own loops: the boss's {@link #LOOPS}, {@code tend} (160 ticks: he tends his hives, played when he
     * idles at home) and {@code talk} (80 ticks: while a hunter is in dialogue with him).
     */
    public static final List<String> NPC_CLIPS = List.of("idle", "walk", "tend", "talk");
    /** Minimum bones of the rig (a detailed vessel, like the archangels'). */
    public static final int MIN_BONES = 40;

    /** When each clip's blow lands, in ticks from its start (proposal; the balance adopts them as windups). */
    public static final Map<String, Integer> HIT_TICKS = Map.of("lunge", 12, "swarm", 14, "furrow", 18, "abels_blood", 22,
            "throw_tools", 14, "execution", 16, "mark_flare", 26, "death", 80);
    /** Clip lengths in ticks (the art's). */
    public static final Map<String, Integer> CLIP_TICKS = Map.of("lunge", 24, "swarm", 30, "furrow", 32, "abels_blood", 40,
            "throw_tools", 28, "execution", 36, "mark_flare", 50, "death", 120);

    /** Sounds ({@code supernaturalcraft:cain.<id>}, files {@code sounds/cain/<id>.ogg}). */
    public static final List<String> SOUNDS = List.of("ambient", "hurt", "death", "lunge", "swarm", "bees", "furrow", "abels_blood",
            "throw", "execution", "mark_flare", "offer_mark");

    /** Cinematics ({@code cinematics/<id>.json}, anchored on Cain). */
    public static final List<String> CINEMATICS = List.of("cain_intro", "cain_p2", "cain_p3", "cain_death");
}
