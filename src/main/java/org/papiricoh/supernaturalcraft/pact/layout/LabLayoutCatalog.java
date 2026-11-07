package org.papiricoh.supernaturalcraft.pact.layout;

import org.papiricoh.supernaturalcraft.buildkit.LayoutCatalog;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Crowley's lab (v0.39) for the layout dump ({@code LayoutDumpTest}) and {@code tools/structview} (pure). */
public final class LabLayoutCatalog implements LayoutCatalog {

    @Override
    public Map<String, Supplier<LayoutPlan>> layouts() {
        Map<String, Supplier<LayoutPlan>> out = new LinkedHashMap<>();
        out.put("crowleys_lab", () -> LabPlan.plan(0));
        return out;
    }
}
