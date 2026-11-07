package org.papiricoh.supernaturalcraft.apocalypse.seal.garrison;

import org.papiricoh.supernaturalcraft.buildkit.LayoutCatalog;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Uriel's garrison (v0.40) for the layout dump ({@code LayoutDumpTest}) and {@code tools/structview} (pure). */
public final class GarrisonLayoutCatalog implements LayoutCatalog {

    @Override
    public Map<String, Supplier<LayoutPlan>> layouts() {
        Map<String, Supplier<LayoutPlan>> out = new LinkedHashMap<>();
        out.put("uriels_garrison", () -> GarrisonPlan.plan(0));
        return out;
    }
}
