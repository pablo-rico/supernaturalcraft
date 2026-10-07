package org.papiricoh.supernaturalcraft.entity.boss.metatron;

import java.util.List;

/**
 * Metatron's clips by short name ("animation.metatron." + name), and his constructs'. Must match
 * tools/artgen/metatron_art.py; MetatronAssetsTest checks the generated files.
 */
public final class MetatronAnimations {

    public static final List<String> TRIGGERED = List.of(
            "emerge", "death", "transform_2", "transform_3", "transform_4",
            "blade_rush", "smite", "ink_bolt", "command", "the_fall", "the_word", "teleport");

    public static final List<String> LOOPS = List.of("idle", "walk", "idle_fly", "wings_idle", "wings_fly");

    /** The Hand's clips ("animation.scribe_hand." + name). */
    public static final List<String> HAND_TRIGGERED = List.of("appear", "write", "slam", "sweep");
    /** The Book's clips ("animation.scribe_book." + name). */
    public static final List<String> BOOK_TRIGGERED = List.of("slam", "close", "open", "storm");

    private MetatronAnimations() {
    }
}
