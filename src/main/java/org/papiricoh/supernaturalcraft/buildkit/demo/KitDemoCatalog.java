package org.papiricoh.supernaturalcraft.buildkit.demo;

import org.papiricoh.supernaturalcraft.buildkit.Canvas;
import org.papiricoh.supernaturalcraft.buildkit.LayoutCatalog;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The kit's showcase (pure): builds made only with {@code buildkit}, to prove it and the renderer. Not used by the game.
 * {@code ./gradlew test --tests '*LayoutDumpTest'} dumps them; {@code tools/structview} renders them.
 */
public final class KitDemoCatalog implements LayoutCatalog {

    @Override
    public Map<String, Supplier<LayoutPlan>> layouts() {
        Map<String, Supplier<LayoutPlan>> out = new LinkedHashMap<>();
        out.put("kit_cottage", () -> DemoCottage.build().toPlan());
        out.put("kit_shop", () -> DemoShop.build().toPlan());
        out.put("kit_island", () -> DemoIsland.build().toPlan());
        return out;
    }

    /** The canvases themselves (the demos' tests check them). */
    public static Map<String, Supplier<Canvas>> canvases() {
        Map<String, Supplier<Canvas>> out = new LinkedHashMap<>();
        out.put("kit_cottage", DemoCottage::build);
        out.put("kit_shop", DemoShop::build);
        out.put("kit_island", DemoIsland::build);
        return out;
    }
}
