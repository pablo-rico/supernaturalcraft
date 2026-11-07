package org.papiricoh.supernaturalcraft.entity.boss.eve;

import java.util.List;
import java.util.Map;

/**
 * The contract between Eve's art ({@code tools/artgen/eve_art.py}) and her code (v0.39): every file, clip and bone name both
 * sides rely on. Pure (paths relative to {@code assets/supernaturalcraft/}); {@code EveAssetsTest} checks the files.
 */
public final class EveAssets {

    private EveAssets() {
    }

    // --- Eve: a young pale woman, long dark hair, an earthy dress tangled with roots and bark, amber eyes, roaches ----------
    public static final String GEO = "geo/entity/eve.geo.json";
    public static final String TEXTURE = "textures/entity/eve.png";
    public static final String GLOWMASK = "textures/entity/eve_glowmask.png";
    public static final String ANIM = "animations/entity/eve.animation.json";
    /**
     * {@code animation.eve.<clip>}: idle/walk loop; the rest play once on the {@code action} controller.
     * {@code lash} a backhand of root-clawed fingers; {@code root_grasp} a hand driven down into the earth (the roots rise
     * under the hunters); {@code birth} arms spread, head back (her children climb out of the ground); {@code roach_swarm}
     * her jaw drops and roaches pour out of her mouth, arm sweeping the lane; {@code thorn_ring} both fists down, the ring of
     * thorns bursts round her; {@code call} arms raised, a long cry (Mother's call); {@code root_field} kneels, both palms on
     * the ground (the whole glade erupts); {@code transform_2|3|4} the bark creeping over her, the roots crowning her;
     * {@code stagger}; {@code death} she withers and crumbles (holds); {@code emerge} she rises out of the roots (holds).
     */
    public static final List<String> CLIPS = List.of("idle", "walk", "lash", "root_grasp", "birth", "roach_swarm", "thorn_ring", "call",
            "root_field", "transform_2", "transform_3", "transform_4", "stagger", "death", "emerge");
    public static final List<String> LOOPS = List.of("idle", "walk");
    public static final List<String> TRIGGERED = CLIPS.stream().filter(c -> !LOOPS.contains(c)).toList();
    /**
     * Bones the code reads, shows or hides. {@code bark_mantle} (bark plates over her shoulders and arms) shows from phase II,
     * {@code root_crown} (a crown of roots round her head) in phase IV. {@code jaw} drops for the swarm; {@code roaches} are
     * the ones crawling over her (the clips move them).
     */
    public static final List<String> BONES = List.of("root", "body", "head", "jaw", "hair", "hair_back", "eyes", "right_arm", "left_arm",
            "right_forearm", "left_forearm", "right_hand", "left_hand", "skirt", "skirt_back", "right_leg", "left_leg", "bark_mantle",
            "root_crown", "roaches");
    /** Bones the renderer shows or hides by phase (clips never scale them). */
    public static final List<String> TOGGLED = List.of("bark_mantle", "root_crown");
    public static final int MIN_BONES = 40;
    /** When each clip's moment lands, in ticks from its start. */
    public static final Map<String, Integer> HIT_TICKS = Map.ofEntries(Map.entry("lash", 12), Map.entry("root_grasp", 18),
            Map.entry("birth", 22), Map.entry("roach_swarm", 14), Map.entry("thorn_ring", 20), Map.entry("call", 24),
            Map.entry("root_field", 26), Map.entry("death", 120));
    /** Clip lengths in ticks (the art's). */
    public static final Map<String, Integer> CLIP_TICKS = Map.ofEntries(Map.entry("lash", 26), Map.entry("root_grasp", 36),
            Map.entry("birth", 44), Map.entry("roach_swarm", 60), Map.entry("thorn_ring", 36), Map.entry("call", 50),
            Map.entry("root_field", 50), Map.entry("transform_2", 80), Map.entry("transform_3", 80), Map.entry("transform_4", 160),
            Map.entry("stagger", 16), Map.entry("death", 160), Map.entry("emerge", 100));
}
