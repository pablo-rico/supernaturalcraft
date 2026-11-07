package org.papiricoh.supernaturalcraft.apocworld;

import org.papiricoh.supernaturalcraft.apocworld.camp.CampPlan;
import org.papiricoh.supernaturalcraft.apocworld.fortress.FortressPlan;
import org.papiricoh.supernaturalcraft.buildkit.LayoutCatalog;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/** The Apocalypse World's plans (v0.41) for the layout dump ({@code LayoutDumpTest}) and {@code tools/structview} (pure). */
public final class ApocWorldLayoutCatalog implements LayoutCatalog {

    @Override
    public Map<String, Supplier<LayoutPlan>> layouts() {
        Map<String, Supplier<LayoutPlan>> out = new LinkedHashMap<>();
        out.put("au_fortress", () -> FortressPlan.plan(0));
        out.put("au_camp", () -> CampPlan.plan(CampPlan.Kind.CAMP, 0));
        out.put("au_checkpoint", () -> CampPlan.plan(CampPlan.Kind.CHECKPOINT, 0));
        return out;
    }
}
