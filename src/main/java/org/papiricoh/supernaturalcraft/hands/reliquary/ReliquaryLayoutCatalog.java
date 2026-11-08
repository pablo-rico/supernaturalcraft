package org.papiricoh.supernaturalcraft.hands.reliquary;

import org.papiricoh.supernaturalcraft.buildkit.LayoutCatalog;
import org.papiricoh.supernaturalcraft.darkness.S11Assets;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The reliquaries' plans (v0.42) for the layout dump ({@code LayoutDumpTest}: list this class in {@code SN_LAYOUT_CATALOGS} or
 * its {@code CATALOGS}) and {@code tools/structview} (pure).
 */
public final class ReliquaryLayoutCatalog implements LayoutCatalog {

    @Override
    public Map<String, Supplier<LayoutPlan>> layouts() {
        Map<String, Supplier<LayoutPlan>> out = new LinkedHashMap<>();
        for (S11Assets.Hand h : S11Assets.Hand.values()) out.put("reliquary_" + h.reliquary, () -> ReliquaryPlans.canvas(h, 0).toPlan());
        return out;
    }
}
