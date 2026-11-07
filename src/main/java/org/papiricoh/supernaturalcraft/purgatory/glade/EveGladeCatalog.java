package org.papiricoh.supernaturalcraft.purgatory.glade;

import org.papiricoh.supernaturalcraft.buildkit.LayoutCatalog;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Eve's glade (v0.39) for the layout dump and {@code tools/structview} (pure): {@code SN_LAYOUT_CATALOGS=} this class's name with
 * {@code ./gradlew test --tests '*LayoutDumpTest'}.
 */
public final class EveGladeCatalog implements LayoutCatalog {

    @Override
    public Map<String, Supplier<LayoutPlan>> layouts() {
        Map<String, Supplier<LayoutPlan>> out = new LinkedHashMap<>();
        out.put("eve_glade", () -> EveGladePlan.plan(42L));
        return out;
    }
}
