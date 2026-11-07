package org.papiricoh.supernaturalcraft.british.safehouse;

import org.papiricoh.supernaturalcraft.buildkit.LayoutCatalog;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Kelly Kline's safe house (v0.41) for the layout dump ({@code LayoutDumpTest}) and {@code tools/structview} (pure). */
public final class SafehouseLayoutCatalog implements LayoutCatalog {

    @Override
    public Map<String, Supplier<LayoutPlan>> layouts() {
        Map<String, Supplier<LayoutPlan>> out = new LinkedHashMap<>();
        out.put("kelly_safehouse", () -> SafehousePlan.plan(0));
        return out;
    }
}
