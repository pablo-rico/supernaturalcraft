package org.papiricoh.supernaturalcraft.cain.layout;

import org.papiricoh.supernaturalcraft.buildkit.LayoutCatalog;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Cain's farm (v0.21) for the layout dump ({@code LayoutDumpTest}) and {@code tools/structview} (pure). */
public final class CainLayoutCatalog implements LayoutCatalog {

    @Override
    public Map<String, Supplier<LayoutPlan>> layouts() {
        Map<String, Supplier<LayoutPlan>> out = new LinkedHashMap<>();
        out.put("cain_farm", () -> CainFarmLayout.plan(0));
        return out;
    }
}
