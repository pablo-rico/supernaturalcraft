package org.papiricoh.supernaturalcraft.entity.boss.dick;

import java.util.List;
import java.util.Map;

/**
 * The contract between the art ({@code tools/artgen/dick_roman_art.py}) and the code (v0.25): every file, clip and bone name
 * both sides rely on. Pure (paths relative to {@code assets/supernaturalcraft/}); {@code DickAssetsTest} checks the files.
 */
public final class DickAssets {

    private DickAssets() {
    }

    // --- Dick Roman: a tailored charcoal suit, a red tie, a businessman's smile — and the Leviathan under it ---------------
    public static final String GEO = "geo/entity/dick_roman.geo.json";
    public static final String TEXTURE = "textures/entity/dick_roman.png";
    public static final String ANIM = "animations/entity/dick_roman.animation.json";
    /**
     * {@code animation.dick_roman.<clip>}: idle/walk/maw_idle loop; the rest play once on the {@code action} controller.
     * {@code downsizing} a backhanded sweep of the arm; {@code sign_here} draws the pen and signs the air, pointing;
     * {@code staff_meeting} a snap of the fingers; {@code maw_open} the head splits (phase II) and holds; {@code bite} lunges
     * maw first; {@code swallow} gulps forward; {@code spit} spits out; {@code goo_spew} retches goo in an arc; {@code copy}
     * shudders into someone else; {@code tentacle_slam} both arms down into the floor; {@code melt} sags into goo (phase IV);
     * {@code stagger}; {@code death} bursts from the inside; {@code emerge} steps forward buttoning his jacket.
     */
    public static final List<String> CLIPS = List.of("idle", "walk", "maw_idle", "downsizing", "sign_here", "staff_meeting",
            "maw_open", "bite", "swallow", "spit", "goo_spew", "copy", "tentacle_slam", "melt", "stagger", "death", "emerge");
    public static final List<String> LOOPS = List.of("idle", "walk", "maw_idle");
    public static final List<String> TRIGGERED = CLIPS.stream().filter(c -> !LOOPS.contains(c)).toList();
    /**
     * Bones the code reads, shows or hides. {@code face} (the human face) and {@code maw} (both jaws, teeth and tongue, a
     * child of {@code head}) swap with the synced MAW flag; {@code goo_mantle} (tendrils down his back) is shown in phase IV.
     */
    public static final List<String> BONES = List.of("body", "head", "face", "maw", "jaw_left", "jaw_right", "tongue",
            "right_arm", "left_arm", "right_hand", "left_hand", "right_leg", "left_leg", "pen", "goo_mantle");
    /** Bones the renderer shows or hides: the face/maw swap, the mantle and the props (clips may move them). */
    public static final List<String> TOGGLED = List.of("face", "maw", "goo_mantle", "pen");
    /** Props shown only while their clip plays: the fountain pen in his right hand. */
    public static final Map<String, String> PROPS = Map.of("pen", "sign_here");
    public static final int MIN_BONES = 40;
    /** When each clip's moment lands, in ticks from its start. */
    public static final Map<String, Integer> HIT_TICKS = Map.ofEntries(Map.entry("downsizing", 18), Map.entry("sign_here", 22),
            Map.entry("staff_meeting", 14), Map.entry("bite", 12), Map.entry("swallow", 10), Map.entry("spit", 8),
            Map.entry("goo_spew", 16), Map.entry("copy", 30), Map.entry("tentacle_slam", 18), Map.entry("death", 110));
    /** Clip lengths in ticks (the art's). */
    public static final Map<String, Integer> CLIP_TICKS = Map.ofEntries(Map.entry("downsizing", 30), Map.entry("sign_here", 36),
            Map.entry("staff_meeting", 30), Map.entry("maw_open", 40), Map.entry("bite", 24), Map.entry("swallow", 30),
            Map.entry("spit", 20), Map.entry("goo_spew", 32), Map.entry("copy", 40), Map.entry("tentacle_slam", 30),
            Map.entry("melt", 60), Map.entry("stagger", 16), Map.entry("death", 140), Map.entry("emerge", 50));

    // --- Sounds ({@code supernaturalcraft:dick_roman.<id>}) --------------------------------------------------------------
    public static final List<String> SOUNDS = List.of("arrive", "laugh", "maw", "bite", "goo", "copy", "hurt", "death");
}
