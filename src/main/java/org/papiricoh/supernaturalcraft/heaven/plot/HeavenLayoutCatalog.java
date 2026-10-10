package org.papiricoh.supernaturalcraft.heaven.plot;

import org.papiricoh.supernaturalcraft.buildkit.LayoutCatalog;
import org.papiricoh.supernaturalcraft.heaven.roadhouse.RoadhouseLayout;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/** The Heaven layouts (a plot in each style, the Roadhouse) for the layout dump and {@code tools/structview} (pure). */
public final class HeavenLayoutCatalog implements LayoutCatalog {

    @Override
    public Map<String, Supplier<LayoutPlan>> layouts() {
        Map<String, Supplier<LayoutPlan>> out = new LinkedHashMap<>();
        out.put("heaven_plot_hunter", () -> HeavenPlotLayout.plan(42L, HeavenPlotLayout.Style.HUNTER));
        out.put("heaven_plot_angel", () -> HeavenPlotLayout.plan(42L, HeavenPlotLayout.Style.ANGEL));
        out.put("heaven_plot_demon", () -> HeavenPlotLayout.plan(42L, HeavenPlotLayout.Style.DEMON));
        out.put("heaven_roadhouse", () -> RoadhouseLayout.plan(42L));
        return out;
    }
}
