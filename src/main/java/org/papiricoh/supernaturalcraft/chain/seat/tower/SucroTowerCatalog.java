package org.papiricoh.supernaturalcraft.chain.seat.tower;

import org.papiricoh.supernaturalcraft.buildkit.LayoutCatalog;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/** The SucroCorp tower for the layout dump ({@code LayoutDumpTest}) and {@code tools/structview}: two review seeds. */
public final class SucroTowerCatalog implements LayoutCatalog {

    /** Seeds drawn for the visual review. */
    public static final long[] REVIEW_SEEDS = {1L, 6L};

    @Override
    public Map<String, Supplier<LayoutPlan>> layouts() {
        Map<String, Supplier<LayoutPlan>> out = new LinkedHashMap<>();
        for (long seed : REVIEW_SEEDS) out.put(SucroTower.NAME + "_" + seed, () -> SucroTower.plan(seed));
        return out;
    }
}
