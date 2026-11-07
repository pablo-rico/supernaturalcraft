package org.papiricoh.supernaturalcraft.entity.boss.godstiel;

import java.util.List;
import java.util.Map;

/**
 * The contract between Season 6's art ({@code tools/artgen/godstiel_art.py}, {@code godstiel_gui_art.py},
 * {@code tools/soundgen/godstiel_sfx.py}) and its code (v0.39): every file, clip and bone name both sides rely on, for Castiel
 * (the story NPC who visits at dawn), Godstiel (the same angel full of Purgatory's souls and, deeper down, the Leviathans), the
 * King of Hell's look for Crowley's story NPC, and the spoils. Pure (paths relative to {@code assets/supernaturalcraft/});
 * {@code GodstielAssetsTest} checks the files once the art lands.
 *
 * <p>Castiel: a slim man of average height in a long beige trench coat (open, its tails to the knees), a dark navy suit, a
 * white shirt, a loose blue tie askew; dark messy hair, stubble, blue eyes, a level stare. One rig serves the NPC and the boss:
 * the NPC wears {@link #TEXTURE_NPC} with every {@link #TOGGLED} bone hidden; the boss wears {@link #TEXTURE} and
 * {@link #GLOW} (soul-blue eyes and, phase by phase, the cracks), and the code shows the toggled bones by phase.
 */
public final class GodstielAssets {

    private GodstielAssets() {
    }

    public static final String RIG = "castiel";
    public static final String GEO = "geo/entity/castiel.geo.json";
    /** The angel as he was: no glow, no cracks (the story NPC, and Castiel forgiven). */
    public static final String TEXTURE_NPC = "textures/entity/castiel.png";
    /** The new God: the same clothes, paler skin, light under it. */
    public static final String TEXTURE = "textures/entity/godstiel.png";
    /**
     * Glow (only with {@link #TEXTURE}): the eyes (soul-blue), the light inside the cracks and the faint halo ring. The
     * goo veins and the mouths are dark and never glow.
     */
    public static final String GLOW = "textures/entity/godstiel_glowmask.png";
    /** Phase V: the coat and skin soaked black, the veins all over (the "drowned" new God at the reservoir). */
    public static final String TEXTURE_SOAKED = "textures/entity/godstiel_soaked.png";
    /** The glow for {@link #TEXTURE_SOAKED} (the same eyes and cracks; AutoGlowingGeoLayer finds it by name). */
    public static final String GLOW_SOAKED = "textures/entity/godstiel_soaked_glowmask.png";
    public static final String ANIM = "animations/entity/castiel.animation.json";

    /**
     * {@code animation.castiel.<clip>}. Loops: {@code idle} (still, the slight head tilt), {@code walk}, {@code hover} (boss:
     * feet a little off the ground, coat tails drifting), {@code struggle_idle} (doubled over, both hands at the head, shaking:
     * the inner fight, held while it lasts), {@code kneel_idle} (beaten, on both knees, head bowed).
     *
     * <p>Once, on the {@code action} controller:
     * <ul>
     *   <li>NPC: {@code appear} (steps out of a flutter of wings, coat settling), {@code vanish} (a flutter, the body is hidden by
     *   the code at its end), {@code talk} (a small open-hand gesture), {@code nod}, {@code turn_away} (looks off, ashamed).</li>
     *   <li>Phase I-II, the new God: {@code snap} (right hand raised, a finger snap: the smiting), {@code judgment} (both arms
     *   raised, palms out: the heretics burst), {@code smite} (right palm thrust forward), {@code radiance} (arms flung wide, head
     *   back: the blinding wave), {@code ascend} (rises a block, arms half out), {@code descend} (lands).</li>
     *   <li>Phase III-V, the Leviathans: {@code crack} (convulses, the cracks open: a phase change), {@code bite} (the chest mouth
     *   lunges forward), {@code sweep} (both tentacles sweep left to right at waist height), {@code slam} (the tentacles rise and
     *   slam down ahead), {@code retch} (goo spews forward in an arc), {@code surge} (arms down and out, goo erupts around him),
     *   {@code struggle} (into the inner fight: hands fly to the head), {@code recover} (straightens out of it).</li>
     *   <li>The end: {@code kneel} (to both knees), {@code sink} (falls back into the water: condemned; hidden at its end),
     *   {@code rise} (forgiven: stands, the goo pours off), {@code death} (the rematch: the Leviathans tear out, he collapses).</li>
     * </ul>
     */
    public static final List<String> CLIPS = List.of("idle", "walk", "hover", "struggle_idle", "kneel_idle",
            "appear", "vanish", "talk", "nod", "turn_away",
            "snap", "judgment", "smite", "radiance", "ascend", "descend",
            "crack", "bite", "sweep", "slam", "retch", "surge", "struggle", "recover",
            "kneel", "sink", "rise", "death");
    public static final List<String> LOOPS = List.of("idle", "walk", "hover", "struggle_idle", "kneel_idle");
    public static final List<String> TRIGGERED = CLIPS.stream().filter(c -> !LOOPS.contains(c)).toList();

    /** Bones the code reads (positions for FX) or toggles. Arms and legs have an elbow/knee child so the clips can bend them. */
    public static final List<String> BONES = List.of("body", "torso", "head", "right_arm", "left_arm", "right_forearm",
            "left_forearm", "right_hand", "left_hand", "right_leg", "left_leg", "coat_tail_left", "coat_tail_right", "tie",
            "eyes", "halo", "cracks_1", "cracks_2", "cracks_3", "veins", "chest_mouth", "chest_jaw", "face_mouth",
            "tentacle_left", "tentacle_right", "wings");
    /**
     * Bones the renderer shows or hides by phase (clips may move them, never show them): {@code halo} (I-II), {@code wings}
     * (shadow wings, only during {@code judgment} and {@code radiance}), {@code cracks_1..3} (light cracks: III, IV, V add one
     * set each), {@code veins} (black goo veins: IV-V), {@code chest_mouth} with its {@code chest_jaw} (a Leviathan maw splitting
     * the shirt: III-V), {@code face_mouth} (the jaw splits down to the collar: V), {@code tentacle_left/right} (goo tendrils from
     * under the coat: IV-V).
     */
    public static final List<String> TOGGLED = List.of("halo", "wings", "cracks_1", "cracks_2", "cracks_3", "veins",
            "chest_mouth", "chest_jaw", "face_mouth", "tentacle_left", "tentacle_right");
    /**
     * A toggled set that covers several limbs is one bone per limb (a bone follows only its own parent): the bone in
     * {@link #TOGGLED} lives on the torso and these parts on the head and the hands. The renderer shows and hides each part
     * together with its key (the cracks on the face and the hands, the veins on the face and the hands).
     */
    public static final Map<String, List<String>> TOGGLED_PARTS = Map.of(
            "cracks_1", List.of("cracks_1_face", "cracks_1_right_hand", "cracks_1_left_hand"),
            "cracks_2", List.of("cracks_2_face", "cracks_2_right_hand", "cracks_2_left_hand"),
            "cracks_3", List.of("cracks_3_face", "cracks_3_right_hand", "cracks_3_left_hand"),
            "veins", List.of("veins_face", "veins_right_hand", "veins_left_hand"));
    public static final int MIN_BONES = 45;
    /** Hitbox (a man; the tentacles are telegraphed, not hit). */
    public static final float WIDTH = 0.6f, HEIGHT = 1.9f;

    /** When each clip's moment lands, in ticks from its start (the balance adopts them as windups). */
    public static final Map<String, Integer> HIT_TICKS = Map.ofEntries(Map.entry("snap", 10), Map.entry("judgment", 24),
            Map.entry("smite", 12), Map.entry("radiance", 20), Map.entry("crack", 20), Map.entry("bite", 12),
            Map.entry("sweep", 16), Map.entry("slam", 18), Map.entry("retch", 14), Map.entry("surge", 22),
            Map.entry("appear", 8), Map.entry("vanish", 12), Map.entry("talk", 10), Map.entry("death", 100));
    /** Clip lengths in ticks (the art's). */
    public static final Map<String, Integer> CLIP_TICKS = Map.ofEntries(Map.entry("appear", 24), Map.entry("vanish", 20),
            Map.entry("talk", 30), Map.entry("nod", 16), Map.entry("turn_away", 30),
            Map.entry("snap", 20), Map.entry("judgment", 40), Map.entry("smite", 22), Map.entry("radiance", 36),
            Map.entry("ascend", 20), Map.entry("descend", 16),
            Map.entry("crack", 40), Map.entry("bite", 24), Map.entry("sweep", 30), Map.entry("slam", 30),
            Map.entry("retch", 30), Map.entry("surge", 40), Map.entry("struggle", 16), Map.entry("recover", 20),
            Map.entry("kneel", 30), Map.entry("sink", 80), Map.entry("rise", 50), Map.entry("death", 140));

    // --- His HUD ------------------------------------------------------------------------------------------------------
    /** {@code entity.supernaturalcraft.godstiel.bar.phase<n>}: the HUD's key for his bar. */
    public static final String BAR_PREFIX = "entity.supernaturalcraft.godstiel.bar";
    /**
     * The bar's sheet ({@code textures/gui/godstiel_bar.png}, 256x128; the layout is {@code godstiel_gui_art.py}'s docstring and
     * {@code client.godstiel.GodstielGui}'s constants):
     * <ul>
     *   <li>(0,0) 256x32 the frame: a stained-glass / halo frame, white and pale gold, the fill window at (24,12) 208x8;</li>
     *   <li>(0,32) 208x8 the light fill (soul blue, a bright core); (0,40) 208x8 the goo fill (black, oily sheen);</li>
     *   <li>(0,48) 8 frames of 16x12 the goo's leading edge (a drip, animated), at (16*i, 48);</li>
     *   <li>(0,64) 256x32 the frame's cracks (alpha only, drawn over the frame from phase III, darker each phase);</li>
     *   <li>(0,96) 5 phase pips of 16x16 lit at (16*i, 96) and corrupted at (16*i, 112), i = phase - 1;</li>
     *   <li>(128,96) 32x32 the seal of the title card (a halo pierced by a black drop).</li>
     * </ul>
     */
    public static final String BAR_SHEET = "textures/gui/godstiel_bar.png";

    // --- Crowley, King of Hell (the story NPC; Crowley's own rig and clips) -------------------------------------------
    /** Crowley as King of Hell (season 6): his rig, his clips, this texture (a crimson pocket square, a signet ring). */
    public static final String CROWLEY_KING_TEXTURE = "textures/entity/crowley_king.png";

    // --- Spoils -------------------------------------------------------------------------------------------------------
    /** The Blade of the New God: a 3D melee weapon ({@code geoWeapon}); its light is the bone {@code light}. */
    public static final String BLADE = "new_god_blade";
    public static final String BLADE_GEO = "geo/item/new_god_blade.geo.json";
    public static final String BLADE_TEXTURE = "textures/item/new_god_blade.png";
    public static final String BLADE_ANIM = "animations/item/new_god_blade.animation.json";
    /** {@code idle} (light at scale 0), {@code charge} (the light rises), {@code flash}. */
    public static final List<String> BLADE_CLIPS = List.of("idle", "charge", "flash");
    /** 16x16 item sprites: Castiel's trench coat (a chestplate), the grace shard, the "Judgment" page. */
    public static final List<String> ITEMS = List.of("castiels_trenchcoat", "grace_shard", "judgment_page");
    /** The coat worn: a vanilla-layout armour texture (64x32). */
    public static final String COAT_ARMOR = "textures/models/armor/castiels_trenchcoat_layer_1.png";

    // --- FX sprites (16x16 particles, white-alpha so the code tints them) ---------------------------------------------
    /** {@code textures/particle/<id>.png}: a soul out of Purgatory, a mote of grace, a goo droplet, a feather of shadow. */
    public static final List<String> PARTICLES = List.of("purgatory_soul", "grace_mote", "goo_drop", "shadow_feather");

    // --- Sounds ({@code supernaturalcraft:godstiel.<id>}) --------------------------------------------------------------
    public static final List<String> SOUNDS = List.of("wings", "snap", "smite", "judgment", "radiance", "crack", "voices",
            "bite", "goo", "struggle", "hurt", "death");
}
