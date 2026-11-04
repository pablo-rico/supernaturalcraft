package org.papiricoh.supernaturalcraft.asylum.layout;

import org.papiricoh.supernaturalcraft.buildkit.LayoutCatalog;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Crowley's asylum (v0.35) for the layout dump ({@code LayoutDumpTest}) and {@code tools/structview} (pure). */
public final class AsylumLayoutCatalog implements LayoutCatalog {

    @Override
    public Map<String, Supplier<LayoutPlan>> layouts() {
        Map<String, Supplier<LayoutPlan>> out = new LinkedHashMap<>();
        out.put("asylum", () -> AsylumPlan.plan(0));
        return out;
    }
}
