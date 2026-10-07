package org.papiricoh.supernaturalcraft.entity.boss.amara;

import java.util.List;

/** One-shot animations the server may trigger; {@code amara.names.txt} from the art tools must list them all. */
public final class AmaraAnimations {

    public static final List<String> TRIGGERED = List.of(
            "emerge", "expose", "close", "transform_2", "transform_3", "transform_4", "death",
            "cast", "orbit", "well", "lance", "rain", "snuff", "spawn",
            "slam", "sweep", "grasp", "spikes", "bloom", "flare", "totality", "collapse", "step", "unmake");

    private AmaraAnimations() {
    }
}
