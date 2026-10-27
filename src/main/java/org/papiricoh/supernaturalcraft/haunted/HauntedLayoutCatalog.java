package org.papiricoh.supernaturalcraft.haunted;

import org.papiricoh.supernaturalcraft.buildkit.LayoutCatalog;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The haunted house for the layout dump ({@code LayoutDumpTest}) and {@code tools/structview} (pure): one of each turret side,
 * and the wreck (ruin 2), all facing south.
 */
public final class HauntedLayoutCatalog implements LayoutCatalog {

    /** Seeds picked for the dump: unmirrored and kept up, mirrored and falling in, a wreck with all four ghosts. */
    static final long PLAIN = first(s -> !HauntedHouseLayout.mirrored(s) && HauntedHouseLayout.ruin(s) == 0),
            MIRRORED = first(s -> HauntedHouseLayout.mirrored(s) && HauntedHouseLayout.ruin(s) == 1),
            WRECK = first(s -> HauntedHouseLayout.ruin(s) == 2 && HauntedHouseLayout.hidings(s).size() == 4);

    static long first(java.util.function.LongPredicate p) {
        for (long s = 1; ; s++) if (p.test(s)) return s;
    }

    @Override
    public Map<String, Supplier<LayoutPlan>> layouts() {
        Map<String, Supplier<LayoutPlan>> out = new LinkedHashMap<>();
        out.put("haunted_house", () -> HauntedHouseLayout.plan(PLAIN, 0));
        out.put("haunted_house_mirrored", () -> HauntedHouseLayout.plan(MIRRORED, 0));
        out.put("haunted_house_wreck", () -> HauntedHouseLayout.plan(WRECK, 0));
        return out;
    }
}
