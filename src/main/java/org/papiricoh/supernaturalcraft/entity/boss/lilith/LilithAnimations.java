package org.papiricoh.supernaturalcraft.entity.boss.lilith;

import java.util.List;

/**
 * Lilith's clips by short name ("animation.lilith." + name). Must match tools/artgen/lilith_art.py;
 * LilithAssetsTest checks the generated file. The borrowed attacks keep their own clip names.
 */
public final class LilithAnimations {

    public static final List<String> TRIGGERED = List.of(
            "emerge", "death", "transform_2", "transform_3",
            "contract", "white_light", "shards", "smother", "rend", "flash", "hounds", "judgement", "teleport");

    public static final List<String> LOOPS = List.of("idle", "walk", "idle_fly", "wings_idle", "wings_fly");

    private LilithAnimations() {
    }
}
