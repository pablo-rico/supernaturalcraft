package org.papiricoh.supernaturalcraft.british.compound;

import org.papiricoh.supernaturalcraft.buildkit.LayoutCatalog;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/** The British Men of Letters' compound (v0.41) for the layout dump ({@code LayoutDumpTest}) and {@code tools/structview} (pure). */
public final class CompoundLayoutCatalog implements LayoutCatalog {

    @Override
    public Map<String, Supplier<LayoutPlan>> layouts() {
        Map<String, Supplier<LayoutPlan>> out = new LinkedHashMap<>();
        out.put("british_compound", () -> CompoundPlan.plan(0));
        return out;
    }
}
