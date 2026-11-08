package org.papiricoh.supernaturalcraft.entity.boss.amara;

import java.util.List;
import java.util.Map;

/**
 * The contract between the Darkness's art ({@code tools/artgen/amara_art.py}, {@code amara_form_art.py},
 * {@code amara_gui_art.py}, {@code tools/soundgen/amara_sfx.py}, {@code music.py}) and her code (v0.42, the season 11 rework):
 * every file, clip and bone name both sides rely on. Pure (paths relative to {@code assets/supernaturalcraft/});
 * {@code AmaraAssetsTest} checks the files once the art lands.
 *
 * <p>Two rigs, one entity (a composite renderer delegates to either, as Chuck's does):
 * <ul>
 *   <li>{@link #RIG_MASS} — phase I, the Darkness before Creation: the old mass with its rings, tentacles and cysts, renewed.
 *   Its bone names are <b>frozen</b> ({@link #MASS_FROZEN}): the hit boxes ({@code AmaraEntity.partOffset}), her shades and
 *   Chuck's ink echo ({@code InkEchoRenderer.LOOKS}) all name them.</li>
 *   <li>{@link #RIG_FORM} — phases II-VI and her dawn visits: Amara as the show has her, a pale woman with long dark hair in a
 *   long black dress, her eyes going black when she uses her power. Ages are textures plus scale plus toggled bones
 *   ({@link #AGE_SCALE}, {@link #TOGGLED}); the baby is the {@code bundle} alone (every other bone hidden).</li>
 * </ul>
 * The cosmic face of phase V and the black sun are not models: they are the client's sky ({@code client.amara.AmaraSky}), driven
 * by {@code AmaraFxPayload.SKY} and {@link AmaraLook#skyStage()}.
 */
public final class AmaraAssets {

    private AmaraAssets() {
    }

    // --- Phase I: the Mass ------------------------------------------------------------------------------------------------
    public static final String RIG_MASS = "amara";
    public static final String MASS_GEO = "geo/entity/amara.geo.json";
    public static final String MASS_TEXTURE = "textures/entity/amara.png";
    public static final String MASS_GLOW = "textures/entity/amara_glowmask.png";
    public static final String MASS_ANIM = "animations/entity/amara.animation.json";
    /**
     * Bones of {@link #RIG_MASS} the code names: never rename them. {@code ring_<i>} are moved from {@code partOffset},
     * {@code tentacle_<i>}/{@code cyst_<i>} toggled by part, {@code form*} drawn alone for her shades and the ink echo.
     */
    public static final List<String> MASS_FROZEN = List.of("root", "mass", "core", "shell_n", "shell_s", "shell_e", "shell_w",
            "shell_up", "shell_down", "veil", "motes",
            "ring_0", "ring_1", "ring_2", "ring_3",
            "tentacle_0", "tentacle_1", "tentacle_2", "tentacle_3", "tentacle_4", "tentacle_5",
            "cyst_0", "cyst_1", "cyst_2", "cyst_3", "cyst_4", "cyst_5",
            "form", "form_torso", "form_head", "form_crown", "form_halo", "form_hair",
            "form_arm_left", "form_arm_right", "form_leg_left", "form_leg_right");
    /** {@code animation.amara.<clip>} loops of the mass. */
    public static final List<String> MASS_LOOPS = List.of("idle", "idle_open", "form_idle");
    /**
     * Once, on {@code action}: the old names stay (the attacks of phase I keep them) and {@code shatter} is new — the mass
     * bursts and pours inward into the girl (phase I → II; the code hides the mass at its end and shows {@link #RIG_FORM}).
     * Each tentacle attack ({@code slam}, {@code sweep}, {@code grasp}, {@code spikes}, {@code bloom}) and each core pulse
     * ({@code flare}, {@code totality}, {@code collapse}) is its own motion, not a shared template.
     */
    public static final List<String> MASS_TRIGGERED = List.of(
            "emerge", "expose", "close", "transform_2", "transform_3", "transform_4", "death",
            "cast", "orbit", "well", "lance", "rain", "snuff", "spawn",
            "slam", "sweep", "grasp", "spikes", "bloom", "flare", "totality", "collapse", "step", "unmake",
            "shatter");

    // --- Phases II-VI and the visits: the woman ---------------------------------------------------------------------------
    public static final String RIG_FORM = "amara_form";
    public static final String FORM_GEO = "geo/entity/amara_form.geo.json";
    public static final String FORM_ANIM = "animations/entity/amara_form.animation.json";

    /** Who she is at a moment: a visit's age, or the fight's form. The texture, the scale and the shown bones follow it. */
    public enum Age {
        /** A newborn swaddled in a black cloth inside a curl of smoke (the {@code bundle} bone only): the first visit. */
        BABY("amara_baby", 0.30f),
        /** A girl of about eight, plain black dress, hair to the shoulders: the visits after Superior, and phase II. */
        GIRL("amara_girl", 0.58f),
        /** A girl of about fifteen, longer hair: the visits after the first Hand. */
        TEEN("amara_teen", 0.86f),
        /** The woman: the last visits and phases III, IV and VI. */
        WOMAN("amara_woman", 1.0f),
        /** Phase V: the woman whose skin and dress are the night sky (stars under the skin, the dress a nebula). */
        VOID("amara_void", 1.25f);

        public final String texture;
        public final float scale;

        Age(String texture, float scale) {
            this.texture = texture;
            this.scale = scale;
        }

        public String texturePath() {
            return "textures/entity/" + texture + ".png";
        }

        /** Her eyes and veins when her power shows (black eyes with a violet rim, the veins of smoke): AutoGlowingGeoLayer. */
        public String glowPath() {
            return "textures/entity/" + texture + "_glowmask.png";
        }
    }

    /** Model scale per age (also in {@link Age#scale}): the hitbox follows. */
    public static final Map<Age, Float> AGE_SCALE = Map.of(Age.BABY, 0.30f, Age.GIRL, 0.58f, Age.TEEN, 0.86f,
            Age.WOMAN, 1.0f, Age.VOID, 1.25f);

    /** Bones the code reads (FX positions) or toggles. Arms and legs bend at elbow and knee children. */
    public static final List<String> FORM_BONES = List.of("body", "torso", "head", "hair", "hair_back", "eyes",
            "right_arm", "left_arm", "right_forearm", "left_forearm", "right_hand", "left_hand",
            "right_leg", "left_leg", "dress", "dress_tail",
            "bundle", "smoke_skirt", "tendrils", "corona", "tears", "chains");
    /**
     * Bones the renderer shows or hides (clips move them, never show them):
     * <ul>
     *   <li>{@code bundle} — the baby (BABY only; then every humanoid bone is hidden);</li>
     *   <li>{@code smoke_skirt} — below the knees she is smoke, trailing (phase V, and while she floats in III and IV);</li>
     *   <li>{@code tendrils} — four smoke tendrils from her back (III-VI);</li>
     *   <li>{@code corona} — a black corona behind her head, a ring of dark rays (V-VI);</li>
     *   <li>{@code tears} — light running down her cheeks (the reconciliation only);</li>
     *   <li>{@code chains} — smoke chains from her hands (the visit that shows Casifer taken, and phase IV's {@code wound}).</li>
     * </ul>
     */
    public static final List<String> TOGGLED = List.of("bundle", "smoke_skirt", "tendrils", "corona", "tears", "chains");
    /** Least bones the form rig must have (a real humanoid, not the old stick figure). */
    public static final int FORM_MIN_BONES = 40;

    /** {@code animation.amara_form.<clip>} loops. */
    public static final List<String> FORM_LOOPS = List.of("idle", "walk", "float", "child_idle", "bundle_idle",
            "kneel_idle", "embrace_idle", "struggle_idle");
    /**
     * Once, on {@code action}:
     * <ul>
     *   <li>visits: {@code appear} (forms out of smoke), {@code vanish} (unravels into smoke; hidden by the code at its end),
     *   {@code talk}, {@code tilt} (head tilt, curious), {@code reach} (reaches a hand out to the hunter), {@code feed} (inhales: a
     *   soul is drawn out of a villager into her mouth), {@code laugh} (the girl);</li>
     *   <li>phase II, the girl: {@code giggle_step} (vanishes into her own shadow, reappears), {@code hide} (curls into smoke),
     *   {@code found} (pops up, arms wide, "found you"), {@code toss} (throws a ball of darkness underhand), {@code tantrum}
     *   (stamps, a shock ring);</li>
     *   <li>phase III, the woman: {@code tk_lift} (both hands rise, palms up), {@code tk_throw} (flings both hands forward),
     *   {@code drain} (draws a soul out of the nearest hunter, one hand at her mouth), {@code strike} (a backhand), {@code combo}
     *   (three blows), {@code push} (a palm shove, the air bends);</li>
     *   <li>phase IV, the siblings: {@code wound} (a fist closes, Chuck's light tears; smoke chains to him), {@code wall} (sweeps an
     *   arm, a wall of smoke rises), {@code scream} (head back, rage at her brother);</li>
     *   <li>phase V, the void: {@code crumble} (arms out and down: the ground breaks), {@code pull} (one hand closes overhead: all
     *   is drawn to the black sun), {@code starfall} (points: stars fall), {@code ascend} (rises into the sky);</li>
     *   <li>phase VI, the dying sun: {@code countdown} (hands together, the sun dims), {@code enrage} (the corona flares);</li>
     *   <li>transitions: {@code become_girl} (out of the mass's {@code shatter}), {@code become_woman} (the girl grows in a turn of
     *   smoke), {@code become_void} (her skin turns to night), {@code become_sun} (sinks to the ground, spent);</li>
     *   <li>the end: {@code kneel}, {@code embrace} (with Chuck), {@code depart} (both rise and fade into light; hidden at its end).</li>
     * </ul>
     */
    public static final List<String> FORM_CLIPS = List.of("idle", "walk", "float", "child_idle", "bundle_idle",
            "kneel_idle", "embrace_idle", "struggle_idle",
            "appear", "vanish", "talk", "tilt", "reach", "feed", "laugh",
            "giggle_step", "hide", "found", "toss", "tantrum",
            "tk_lift", "tk_throw", "drain", "strike", "combo", "push",
            "wound", "wall", "scream",
            "crumble", "pull", "starfall", "ascend",
            "countdown", "enrage",
            "become_girl", "become_woman", "become_void", "become_sun",
            "kneel", "embrace", "depart");
    public static final List<String> FORM_TRIGGERED = FORM_CLIPS.stream().filter(c -> !FORM_LOOPS.contains(c)).toList();

    /** When each clip's moment lands, in ticks from its start (the balance adopts them as windups). */
    public static final Map<String, Integer> HIT_TICKS = Map.ofEntries(
            Map.entry("giggle_step", 8), Map.entry("found", 6), Map.entry("toss", 12), Map.entry("tantrum", 14),
            Map.entry("tk_lift", 16), Map.entry("tk_throw", 10), Map.entry("drain", 20), Map.entry("strike", 8),
            Map.entry("combo", 10), Map.entry("push", 10), Map.entry("wound", 18), Map.entry("wall", 14),
            Map.entry("crumble", 24), Map.entry("pull", 20), Map.entry("starfall", 16), Map.entry("enrage", 20),
            Map.entry("feed", 24), Map.entry("shatter", 40));
    /** Clip lengths in ticks (the art's). */
    public static final Map<String, Integer> CLIP_TICKS = Map.ofEntries(
            Map.entry("appear", 30), Map.entry("vanish", 24), Map.entry("talk", 30), Map.entry("tilt", 16),
            Map.entry("reach", 30), Map.entry("feed", 50), Map.entry("laugh", 24),
            Map.entry("giggle_step", 20), Map.entry("hide", 16), Map.entry("found", 20), Map.entry("toss", 24),
            Map.entry("tantrum", 30), Map.entry("tk_lift", 30), Map.entry("tk_throw", 20), Map.entry("drain", 50),
            Map.entry("strike", 16), Map.entry("combo", 40), Map.entry("push", 20), Map.entry("wound", 40),
            Map.entry("wall", 30), Map.entry("scream", 40), Map.entry("crumble", 50), Map.entry("pull", 40),
            Map.entry("starfall", 30), Map.entry("ascend", 40), Map.entry("countdown", 60), Map.entry("enrage", 40),
            Map.entry("become_girl", 60), Map.entry("become_woman", 80), Map.entry("become_void", 100),
            Map.entry("become_sun", 80), Map.entry("kneel", 30), Map.entry("embrace", 80), Map.entry("depart", 120),
            Map.entry("shatter", 60));
    /** The woman's hitbox at scale 1 (scaled by her age). */
    public static final float FORM_WIDTH = 0.6f, FORM_HEIGHT = 1.85f;

    // --- HUD ------------------------------------------------------------------------------------------------------------------
    /** {@code entity.supernaturalcraft.amara.bar.phase<n>} (n = 1..6): the HUD's key for her bar. */
    public static final String BAR_PREFIX = "entity.supernaturalcraft.amara.bar";
    /** The HUD's sheet, laid out by {@link AmaraHudSheet}. */
    public static final String HUD_SHEET = "textures/gui/amara_hud.png";

    // --- Spoils ---------------------------------------------------------------------------------------------------------------
    /**
     * 16x16 item sprites: {@code amaras_light} (new: her gift at the reconciliation, a mote of dawn in a black glass bead),
     * {@code void_essence} and {@code eclipse_sight} ("The Sight", redrawn: an eye of black glass with a gold iris).
     */
    public static final List<String> ITEMS = List.of("amaras_light", "void_essence", "eclipse_sight");

    // --- FX sprites (16x16, white-alpha so the code tints them; textures/particle/<id>.png) -----------------------------------
    /**
     * {@code void_mote_0..3} (old, kept), {@code darkness_ash} (a flake of unmade ground, 4 frames {@code darkness_ash_0..3}),
     * {@code soul_wisp} (a soul being drawn out), {@code star_mote} (a falling star), {@code ink_drop} (a drop of the darkness),
     * {@code dawn_mote} (a mote of returning sunlight).
     */
    public static final List<String> PARTICLES = List.of("void_mote_0", "void_mote_1", "void_mote_2", "void_mote_3",
            "darkness_ash_0", "darkness_ash_1", "darkness_ash_2", "darkness_ash_3",
            "soul_wisp", "star_mote", "ink_drop", "dawn_mote");

    // --- Sounds ({@code supernaturalcraft:amara.<id>}; the old {@code entity.amara.*} stay for phase I) ------------------------
    public static final String SOUND_GROUP = "amara";
    public static final List<String> SOUNDS = List.of("whisper", "giggle", "child_scream", "hum", "tk", "drain", "strike",
            "wound", "wall", "crumble", "island", "pull", "starfall", "countdown", "heartbeat", "enrage", "sun_die",
            "reconcile", "dawn", "visit", "feed");
    /** {@code supernaturalcraft:music.amara.phase<n>}, one cue per phase (no longer shared with the Empty). */
    public static final String MUSIC_GROUP = "music.amara";
    public static final List<String> MUSIC = List.of("phase1", "phase2", "phase3", "phase4", "phase5", "phase6");

    // --- Cinematics (assets/supernaturalcraft/cinematics/<id>.json) -----------------------------------------------------------
    /** Multi-shot: the intro, each phase's entrance, Chuck's arrival in IV, the reconciliation. */
    public static final List<String> CINEMATICS = List.of("amara_intro", "amara_p2", "amara_p3", "amara_p4", "amara_p5",
            "amara_p6", "amara_reconcile");
}
