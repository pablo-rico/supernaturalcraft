package org.papiricoh.supernaturalcraft.entity.boss.alastair;

import java.util.List;
import java.util.Map;

/**
 * The contract between Alastair's art ({@code tools/artgen/alastair_art.py}, {@code alastair_gui_art.py}) and his code (v0.40):
 * every file, clip and bone both sides rely on. Pure (paths relative to {@code assets/supernaturalcraft/});
 * {@code AlastairAssetsTest} checks the files once the art lands.
 *
 * <p>Alastair: Hell's finest torturer, in his vessel of season 4: a lean, tall older man, close-cropped grey hair, a narrow
 * pale face with a thin smile; white eyes when he shows them (the glow). A dark grey overcoat over a black suit, a white
 * shirt buttoned to the neck without a tie, black gloves. A leather roll of instruments at his belt (the {@code tools}
 * bone); a long scalpel in his right hand ({@code scalpel}). He is unhurried and polite; he hums. He fights with his
 * instruments, hooks and chains thrown from the dark, and a rack of black iron that rises from the floor ({@link #RACK},
 * its own small rig).
 */
public final class AlastairAssets {

    private AlastairAssets() {
    }

    public static final String RIG = "alastair";
    public static final String GEO = "geo/entity/alastair.geo.json";
    public static final String TEXTURE = "textures/entity/alastair.png";
    /** Glow: his white eyes (drawn only while {@code AlastairEntity.eyesLit()}). */
    public static final String GLOW = "textures/entity/alastair_glowmask.png";
    public static final String ANIM = "animations/entity/alastair.animation.json";

    /**
     * {@code animation.alastair.<clip>}: {@code idle}, {@code walk}, {@code bound_idle} (phase I: arms spread and chained
     * up, kneeling in the trap, head tilted, talking) and {@code hum_idle} (hands folded behind his back, swaying) loop; the
     * rest play once. {@code break_free} rips the chains (arms snap down) and stands; {@code scalpel_slash} a quick
     * backhand cut; {@code scalpel_throw} three blades fanned from the left hand; {@code hook_throw} an overhand cast of a
     * chain-hook (the hook entity flies at its hit tick); {@code chain_lash} a whip of a chain along the ground, left to
     * right; {@code rack_call} both hands raised, palms up (the rack rises); {@code smoke} a backward lean as the white
     * smoke pours from his mouth (phase III); {@code summon} one hand down, fingers spread; {@code taunt} a tilted head and a
     * small bow; {@code hurt_holy} a recoil, hand to face (holy water, salt); {@code death} the demon-light flare (head back,
     * arms out) and fall.
     */
    public static final List<String> CLIPS = List.of("idle", "walk", "bound_idle", "hum_idle", "break_free", "scalpel_slash",
            "scalpel_throw", "hook_throw", "chain_lash", "rack_call", "smoke", "summon", "taunt", "hurt_holy", "death");
    public static final List<String> LOOPS = List.of("idle", "walk", "bound_idle", "hum_idle");
    public static final List<String> TRIGGERED = CLIPS.stream().filter(c -> !LOOPS.contains(c)).toList();

    public static final List<String> BONES = List.of("body", "head", "right_arm", "left_arm", "right_hand", "left_hand",
            "right_leg", "left_leg");
    /**
     * Optional bones: his coat's tails, the roll of instruments, the scalpel (the renderer hides it in phase I), the eyes (glow
     * shell; the renderer shows it while they are lit). No clip keys {@code scalpel} or {@code eyes}.
     */
    public static final List<String> OPTIONAL_BONES = List.of("coat_tail", "tools", "scalpel", "eyes");
    public static final int MIN_BONES = 30;
    public static final float WIDTH = 0.6f, HEIGHT = 1.9f;

    /** When each clip's moment lands, in ticks from its start (the balance adopts them as windups). */
    public static final Map<String, Integer> HIT_TICKS = Map.ofEntries(Map.entry("break_free", 20), Map.entry("scalpel_slash", 7),
            Map.entry("scalpel_throw", 12), Map.entry("hook_throw", 14), Map.entry("chain_lash", 16), Map.entry("rack_call", 22),
            Map.entry("smoke", 10), Map.entry("summon", 14), Map.entry("taunt", 10), Map.entry("hurt_holy", 2), Map.entry("death", 50));
    /** Clip lengths in ticks. */
    public static final Map<String, Integer> CLIP_TICKS = Map.ofEntries(Map.entry("break_free", 36), Map.entry("scalpel_slash", 14),
            Map.entry("scalpel_throw", 22), Map.entry("hook_throw", 26), Map.entry("chain_lash", 30), Map.entry("rack_call", 34),
            Map.entry("smoke", 40), Map.entry("summon", 26), Map.entry("taunt", 24), Map.entry("hurt_holy", 14), Map.entry("death", 80));

    // --- The rack and his instruments -------------------------------------------------------------------------------------
    /**
     * The rack: a black iron frame of four posts and crossbars with chains and cuffs, 2 blocks wide, 1 deep, 2.5 high; it rises
     * from the floor ({@code animation.alastair_rack.rise}, then {@code idle}, {@code clamp} when it catches someone,
     * {@code sink}). Its bones: {@code frame}, {@code chains}, {@code cuffs}.
     */
    public static final String RACK = "alastair_rack";
    public static final List<String> RACK_CLIPS = List.of("rise", "idle", "clamp", "sink");
    public static final Map<String, Integer> RACK_CLIP_TICKS = Map.of("rise", 20, "clamp", 10, "sink", 20);
    /** The chain-hook he throws (an entity): a rusty iron hook on a length of chain, drawn by its own small geo. */
    public static final String HOOK = "alastair_hook";
    /** His thrown scalpel (a projectile, flat sprite stretched like an arrow). */
    public static final String THROWN_SCALPEL = "textures/entity/alastair_thrown_scalpel.png";

    // --- Spoils ----------------------------------------------------------------------------------------------------------
    /** Alastair's scalpel (item): a long surgical blade, dark steel with a bone handle, a dried red line on the edge. */
    public static final String SCALPEL_ITEM = "textures/item/alastair_scalpel.png";
    /** His bust (block model {@code block/alastair_trophy}, the trophies' shared base). */
    public static final String TROPHY_MODEL = "models/block/alastair_trophy.json";

    // --- The HUD ---------------------------------------------------------------------------------------------------------
    /** {@code entity.supernaturalcraft.alastair.bar.phase<n>}: his bar's keys (the HUD recognises them). */
    public static final String BAR_PREFIX = "entity.supernaturalcraft.alastair.bar";
    /** His bar's sheet ({@code textures/gui/alastair_bar.png}): rusted iron with hanging hooks; layout in {@code alastair_gui_art.py}. */
    public static final String BAR_SHEET = "textures/gui/alastair_bar.png";

    /** Sounds ({@code supernaturalcraft:alastair.<id>}, files {@code sounds/alastair/<id>.ogg}). */
    public static final List<String> SOUNDS = List.of("hum", "chains", "chain_break", "hook_throw", "hook_hit", "scalpel",
            "rack_rise", "rack_clamp", "smoke", "laugh", "hurt", "death");

    /** Cinematics ({@code cinematics/<id>.json}, anchored on him). */
    public static final List<String> CINEMATICS = List.of("alastair_trap", "alastair_free");
}
