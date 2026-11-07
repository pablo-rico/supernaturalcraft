package org.papiricoh.supernaturalcraft.british.birth;

import java.util.List;
import java.util.Map;

/**
 * The contract between this art ({@code tools/artgen/birth_art.py}) and its code (v0.41): every file, clip and bone both sides
 * rely on. Pure (paths relative to {@code assets/supernaturalcraft/}); an {@code *AssetsTest} checks the files once the art lands.
 *
 * <p>The two figures of Jack's birth scene (staged, never fighting). Kelly Kline: a young woman, long wavy brown hair, a loose grey sweater over a long dark skirt, heavily pregnant ({@code belly}, scaled down after the birth); she lies on a cot, then her light fades. And Lucifer in his last vessel of season 12, President Jimmy Rourke: a tall grey-haired older man in a dark navy suit, white shirt, red tie, an American flag pin ({@code ROURKE} rig, {@code rourke_lucifer}); red eyes when he shows them (glow).
 */
public final class BirthAssets {

    private BirthAssets() {
    }

    public static final String RIG = "kelly_kline";
    public static final String GEO = "geo/entity/kelly_kline.geo.json";
    public static final String TEXTURE = "textures/entity/kelly_kline.png";
    public static final String ANIM = "animations/entity/kelly_kline.animation.json";

    /**
     * {@code animation.kelly_kline.<clip>} (loops: idle, labor; the rest play once): {@code idle} sitting up on the cot, a hand on her belly; {@code labor} lying back, gripping the cot; {@code glow} the light pouring from her: head back, the room whitens; {@code fade} she sinks back, still.
     */
    public static final List<String> CLIPS = List.of("idle", "labor", "glow", "fade");
    public static final List<String> LOOPS = List.of("idle", "labor");
    public static final List<String> TRIGGERED = CLIPS.stream().filter(c -> !LOOPS.contains(c)).toList();

    /** The humanoid rig's bones (as {@code humanoid()} in {@code tools/artgen} names them). */
    public static final List<String> BONES = List.of("body", "head", "right_arm", "left_arm", "right_hand", "left_hand", "right_leg", "left_leg");
    /** Optional bones the code shows, hides or reads (no clip keys them unless its doc says so). */
    public static final List<String> OPTIONAL_BONES = List.of("belly", "hair");
    public static final int MIN_BONES = 20;
    public static final float WIDTH = 0.6f, HEIGHT = 1.75f;

    /** When each clip's moment lands, in ticks from its start (the balance adopts them as windups). */
    public static final Map<String, Integer> HIT_TICKS = Map.ofEntries(Map.entry("glow", 20), Map.entry("fade", 40));
    /** Clip lengths in ticks. */
    public static final Map<String, Integer> CLIP_TICKS = Map.ofEntries(Map.entry("glow", 60), Map.entry("fade", 80));

    /** Sound ids ({@code kelly_kline.<id>}, registered with {@code AllSounds.registerGroup}; vanilla re-pitched until encoded). */
    public static final List<String> SOUNDS = List.of("labor", "glow", "cry");

    /** Lucifer in President Rourke's body: {@code animation.rourke_lucifer.<clip>}. */
    public static final String ROURKE = "rourke_lucifer";
    public static final String ROURKE_GEO = "geo/entity/rourke_lucifer.geo.json";
    public static final String ROURKE_TEXTURE = "textures/entity/rourke_lucifer.png";
    public static final String ROURKE_GLOW = "textures/entity/rourke_lucifer_glowmask.png";
    public static final String ROURKE_ANIM = "animations/entity/rourke_lucifer.animation.json";
    /** {@code idle}, {@code walk} loop; {@code reach} (a hand held out to the newborn), {@code thrown} (hurled back by the birth's light). */
    public static final List<String> ROURKE_CLIPS = List.of("idle", "walk", "reach", "thrown");
}
