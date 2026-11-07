package org.papiricoh.supernaturalcraft.entity.alpha;

import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.pact.PactRules;

import java.util.UUID;

/**
 * One of the three Alphas (v0.39): the first vampire, the first werewolf, the first shapeshifter, each made by Eve. They come at
 * night for a hunter on Crowley's errand ({@code AlphaSpawns}); each is its kind's own entity class (so dead man's blood, beheading
 * and silver work on them as on their children), sharing the Alpha's ways through {@link AlphaCore}.
 */
public interface Alpha {

    PactRules.Alpha alpha();

    AlphaCore core();

    /** The hunter it came for, if any. */
    default @Nullable UUID quarry() {
        return core().quarry();
    }
}
