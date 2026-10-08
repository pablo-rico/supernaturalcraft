package org.papiricoh.supernaturalcraft.entity.boss.gabriel;

import java.util.List;
import java.util.Map;

/**
 * The contract between the art ({@code tools/artgen/gabriel_art.py}, {@code gabriel_items_art.py}, {@code gabriel_gui_art.py},
 * {@code tools/soundgen/gabriel_sfx.py}) and the code (v0.14): every file, clip and bone name both sides rely on. Pure (paths
 * relative to {@code assets/supernaturalcraft/}); {@code GabrielAssetsTest} checks the files against it.
 */
public final class GabrielAssets {

    private GabrielAssets() {
    }

    // --- Gabriel: one detailed rig, five costumes, six golden shadow wings ----------------------------------------------
    public static final String GEO = "geo/entity/gabriel.geo.json";
    /** {@code textures/entity/gabriel_<costume>.png}, one per {@link Channel.Costume} (same UV layout). */
    public static final String TEXTURE = "textures/entity/gabriel_%s.png";
    /** Glow (the wings' light, the eyes in the reveal): one for every costume. */
    public static final String GLOW = "textures/entity/gabriel_glowmask.png";
    public static final String ANIM = "animations/entity/gabriel.animation.json";
    /**
     * {@code animation.gabriel.<clip>}: idle/walk/laugh loop; the rest play once. {@code snap} is his finger-snap (a channel
     * change, a gag appearing), {@code wings_reveal} spreads the six shadow wings and holds.
     */
    public static final List<String> CLIPS = List.of("idle", "walk", "laugh", "throw_pie", "snap", "host_gesture", "buzzer_slam",
            "defib", "spokesman_pose", "wings_reveal", "hit", "death", "emerge");
    /** The loops ({@code base} controller); every other clip is triggered on the {@code action} controller. */
    public static final List<String> LOOPS = List.of("idle", "walk", "laugh");
    public static final List<String> TRIGGERED = CLIPS.stream().filter(c -> !LOOPS.contains(c)).toList();
    /** Bones the code reads, shows or hides. */
    public static final List<String> BONES = List.of("body", "head", "right_arm", "left_arm", "right_hand", "left_hand", "wings",
            "pie", "lollipop", "microphone", "defibrillator", "nurse_cap");
    /**
     * Costume bone groups: the renderer shows the one of the costume worn and hides the rest ({@code JACKET} has none: the
     * base body is his own clothes). The nurse doubles add {@code nurse_cap}.
     */
    public static final Map<Channel.Costume, String> COSTUME_BONES = Map.of(
            Channel.Costume.SWEATER, "costume_sweater",
            Channel.Costume.TUXEDO, "costume_tuxedo",
            Channel.Costume.LAB_COAT, "costume_lab_coat",
            Channel.Costume.SUIT, "costume_suit");
    /** Props hidden unless a clip uses them: the renderer shows each only while its clip plays. */
    public static final Map<String, String> PROP_CLIPS = Map.of(
            "pie", "throw_pie", "microphone", "host_gesture", "defibrillator", "defib");
    /** Minimum bones of the rig (a detailed model, like the rival hunter's). */
    public static final int MIN_BONES = 50;

    // --- Props and the party hat ----------------------------------------------------------------------------------------
    /** The thrown pie (a small GeckoLib model, no animation). */
    public static final String PIE_GEO = "geo/entity/gabriel_pie.geo.json";
    public static final String PIE_TEXTURE = "textures/entity/gabriel_pie.png";
    /** The prank's party hat, drawn on a mob's head (pivot at its base). */
    public static final String PARTY_HAT_GEO = "geo/entity/party_hat.geo.json";
    public static final String PARTY_HAT_TEXTURE = "textures/entity/party_hat.png";

    // --- Items ----------------------------------------------------------------------------------------------------------
    /** Flat items ({@code textures/item/<id>.png}). */
    public static final List<String> ITEMS = List.of("trickster_bait", "trickster_candy", "candy_wrapper", "gabriel_blade");
    /** The remote: a GeckoLib item ({@code geo/item/trickster_remote.geo.json}) with its own inventory icon. */
    public static final String REMOTE_GEO = "geo/item/trickster_remote.geo.json";
    public static final String REMOTE_TEXTURE = "textures/item/trickster_remote.png";
    public static final String REMOTE_ICON = "textures/item/trickster_remote_icon.png";
    /** The trophy: an old television with his grin on the screen (block model facing north, turned by FACING). */
    public static final String TROPHY_MODEL = "models/block/gabriel_trophy.json";

    // --- Interface ------------------------------------------------------------------------------------------------------
    /** 128×32 per sign: {@code laugh}, {@code applause}, {@code on_air} (lit; the HUD dims them itself). */
    public static final String SIGN = "textures/gui/gabriel/sign_%s.png";
    public static final List<String> SIGNS = List.of("laugh", "applause", "on_air");
    /** The channel's on-screen display: 10 digits + "CH" in green OSD type (a 128×16 strip: "CH" at 0, digit n at 16+n*11). */
    public static final String OSD = "textures/gui/gabriel/osd.png";
    /** Static: a 128×128 tile of noise in four frames (2×2), tinted and scrolled by the HUD. */
    public static final String STATIC = "textures/gui/gabriel/static.png";
    /** The quiz panel: 256×96 (question at y 8..40, three answer slots in red/blue/yellow at y 52..88). */
    public static final String QUIZ_PANEL = "textures/gui/gabriel/quiz_panel.png";
    /** The boss bar as a programme banner: 256×32 (frame, fill strip below at y 24..30). */
    public static final String BAR = "textures/gui/gabriel/bar.png";

    // --- Sounds ({@code supernaturalcraft:gabriel.<id>}) ----------------------------------------------------------------
    public static final List<String> SOUNDS = List.of("laugh_track", "applause", "buzzer", "ding", "static", "monitor_beep", "defib",
            "piano", "pie", "snap", "welcome", "jingle_sitcom", "jingle_game_show", "jingle_hospital", "jingle_commercial",
            "ambient", "hurt", "death");

    // --- Cinematics ({@code cinematics/<id>.json}, CameraSequence, anchored on Gabriel) ---------------------------------
    public static final List<String> CINEMATICS = List.of("gabriel_intro", "gabriel_p2", "gabriel_p3", "gabriel_p4", "gabriel_death");
}
