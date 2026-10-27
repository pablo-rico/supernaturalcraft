package org.papiricoh.supernaturalcraft.hell.cage;

import org.papiricoh.supernaturalcraft.buildkit.LayoutCatalog;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Lucifer's Cage for the layout dump ({@code LayoutDumpTest}) and {@code tools/structview}. */
public final class CageLayoutCatalog implements LayoutCatalog {

    @Override
    public Map<String, Supplier<LayoutPlan>> layouts() {
        Map<String, Supplier<LayoutPlan>> out = new LinkedHashMap<>();
        out.put(CagePlan.NAME, () -> CagePlan.canvas().toPlan());
        return out;
    }
}
