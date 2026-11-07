package org.papiricoh.supernaturalcraft.apocalypse.warehouse;

import org.papiricoh.supernaturalcraft.buildkit.LayoutCatalog;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Alastair's warehouse (v0.40) for the layout dump ({@code LayoutDumpTest}) and {@code tools/structview} (pure). */
public final class WarehouseLayoutCatalog implements LayoutCatalog {

    @Override
    public Map<String, Supplier<LayoutPlan>> layouts() {
        Map<String, Supplier<LayoutPlan>> out = new LinkedHashMap<>();
        out.put("alastairs_warehouse", () -> WarehousePlan.plan(0));
        return out;
    }
}
