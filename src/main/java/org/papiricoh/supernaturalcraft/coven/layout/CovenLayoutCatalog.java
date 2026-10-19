package org.papiricoh.supernaturalcraft.coven.layout;

import org.papiricoh.supernaturalcraft.buildkit.LayoutCatalog;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The v0.21 witches' places for the layout dump ({@code LayoutDumpTest}) and {@code tools/structview} (pure): the abandoned church
 * (two ways round), the witch coven and Rowena's house.
 */
public final class CovenLayoutCatalog implements LayoutCatalog {

    @Override
    public Map<String, Supplier<LayoutPlan>> layouts() {
        Map<String, Supplier<LayoutPlan>> out = new LinkedHashMap<>();
        out.put("coven_church", () -> ChurchLayout.plan(7L, 0));
        out.put("coven_church_east", () -> ChurchLayout.plan(1234567L, 1));
        out.put("coven_witch_coven", () -> CovenLayout.plan(7L, 0));
        out.put("coven_rowena_house", () -> RowenaHouseLayout.plan(0));
        return out;
    }
}
